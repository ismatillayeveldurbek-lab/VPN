package uz.tulpor.vpn.vpn

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.TrafficStats
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.os.Process
import android.os.SystemClock
import android.service.quicksettings.TileService
import android.content.ComponentName
import android.text.format.Formatter
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.tulpor.vpn.MainActivity
import uz.tulpor.vpn.R
import uz.tulpor.vpn.TulporApp
import uz.tulpor.vpn.data.AppSettings
import uz.tulpor.vpn.data.ConfigBuilder
import uz.tulpor.vpn.data.Countries
import uz.tulpor.vpn.data.Server
import uz.tulpor.vpn.data.SplitMode
import uz.tulpor.tulpor.Tulpor

class TulporVpnService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private var tun: ParcelFileDescriptor? = null
    private var statsJob: Job? = null
    private var currentServer: Server? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISCONNECT -> {
                scope.launch { disconnect(stopService = true) }
                return START_NOT_STICKY
            }
            else -> {
                // ACTION_CONNECT, or null when the system restarts us / always-on VPN starts us.
                goForeground(buildNotification(getString(R.string.status_connecting), null))
                scope.launch { connect() }
            }
        }
        return START_STICKY
    }

    private suspend fun connect() = lock.withLock {
        VpnState.set(VpnStatus.Connecting)
        refreshTile()
        try {
            val app = application as TulporApp
            val settings = app.settings.current()
            ConnectionLog.start(this, "Ulanish boshlandi · Tulpor ${uz.tulpor.vpn.BuildConfig.VERSION_NAME} · Xray ${runCatching { Tulpor.version() }.getOrDefault("?")}")
            val server = pickServer(app.servers.all(), settings)
            ConnectionLog.add(this, "Server: ${server.name} (${server.address}:${server.port}, ${server.network}/${server.security})")

            stopEngine()
            val pfd = buildTun(settings) ?: throw IllegalStateException(getString(R.string.error_vpn_permission))
            tun = pfd
            ConnectionLog.add(this, "VPN interfeysi tayyor, split=${settings.splitMode}, .uz to'g'ridan=${settings.bypassUz}")
            val options = ConfigBuilder.Options(
                bypassUz = settings.bypassUz,
                errorLogPath = ConnectionLog.file(this).absolutePath,
            )
            Tulpor.start(ConfigBuilder.vpnConfig(server, options), pfd.fd.toLong())
            ConnectionLog.add(this, "Ulandi ✓")

            currentServer = server
            VpnState.set(VpnStatus.Connected(server, SystemClock.elapsedRealtime()))
            notify(buildNotification(getString(R.string.status_connected), server))
            startStats(server)
        } catch (e: Throwable) {
            Log.e(TAG, "connect failed", e)
            ConnectionLog.add(this, "XATO: ${e.message ?: e.javaClass.simpleName}")
            stopEngine()
            VpnState.set(VpnStatus.Failed(e.message ?: e.javaClass.simpleName))
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        refreshTile()
    }

    /**
     * Chooses the server and makes sure it actually answers before we take over the
     * phone's traffic, so the user gets a clear error instead of "Connected" with no internet.
     */
    private suspend fun pickServer(all: List<Server>, settings: AppSettings): Server {
        val usable = all.filter { !it.premium || settings.isPremium }
        if (usable.isEmpty()) throw IllegalStateException(getString(R.string.error_no_server))

        val chosen = usable.firstOrNull { it.id == settings.selectedServerId }
        if (chosen != null) {
            val cached = Pinger.results.value[chosen.id]
            val delay = if (cached != null && cached >= 0) cached else Pinger.measure(chosen)
            ConnectionLog.add(this, "Tekshiruv ${chosen.name}: ${if (delay >= 0) "$delay ms" else "javob yo'q"}")
            if (delay < 0) throw IllegalStateException(getString(R.string.error_server_unreachable, chosen.name))
            return chosen
        }
        ConnectionLog.add(this, "Eng tez server qidirilmoqda (${usable.size} ta)…")
        val fastest = Pinger.fastest(usable)
        Pinger.results.value.forEach { (id, d) ->
            usable.firstOrNull { it.id == id }?.let { ConnectionLog.add(this, "  ${it.name}: ${if (d >= 0) "$d ms" else "javob yo'q"}") }
        }
        return fastest ?: throw IllegalStateException(getString(R.string.error_none_reachable))
    }

    private fun buildTun(settings: AppSettings): ParcelFileDescriptor? {
        val b = Builder()
            .setSession(getString(R.string.app_name))
            .setMtu(ConfigBuilder.TUN_MTU)
            .addAddress("172.19.0.1", 30)
            .addRoute("0.0.0.0", 0)
            .addAddress("fdfe:dcba:9876::1", 126)
            .addRoute("::", 0)
        ConfigBuilder.DNS_SERVERS.forEach { b.addDnsServer(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) b.setMetered(false)

        val apps = settings.splitApps.filter { isInstalled(it) && it != packageName }
        when {
            settings.splitMode == SplitMode.ONLY_SELECTED && apps.isNotEmpty() ->
                apps.forEach { b.addAllowedApplication(it) }
            else -> {
                // Our own traffic (the tunnel itself) must never loop back into the VPN.
                b.addDisallowedApplication(packageName)
                if (settings.splitMode == SplitMode.EXCEPT_SELECTED) apps.forEach { b.addDisallowedApplication(it) }
            }
        }
        b.setConfigureIntent(
            PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        )
        return b.establish()
    }

    private fun isInstalled(pkg: String) = try {
        packageManager.getApplicationInfo(pkg, 0); true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    private fun startStats(server: Server) {
        statsJob?.cancel()
        statsJob = scope.launch {
            val uid = Process.myUid()
            val rx0 = TrafficStats.getUidRxBytes(uid)
            val tx0 = TrafficStats.getUidTxBytes(uid)
            var lastRx = rx0
            var lastTx = tx0
            var tick = 0
            while (isActive) {
                delay(1000)
                val rx = TrafficStats.getUidRxBytes(uid)
                val tx = TrafficStats.getUidTxBytes(uid)
                val t = Traffic(
                    downBps = (rx - lastRx).coerceAtLeast(0),
                    upBps = (tx - lastTx).coerceAtLeast(0),
                    downTotal = (rx - rx0).coerceAtLeast(0),
                    upTotal = (tx - tx0).coerceAtLeast(0),
                )
                lastRx = rx; lastTx = tx
                VpnState.setTraffic(t)
                if (++tick % 2 == 0) notify(buildNotification(getString(R.string.status_connected), server, t))
            }
        }
    }

    private suspend fun disconnect(stopService: Boolean) = lock.withLock {
        VpnState.set(VpnStatus.Disconnecting)
        if (currentServer != null) ConnectionLog.add(this, "Uzildi")
        stopEngine()
        currentServer = null
        VpnState.set(VpnStatus.Disconnected)
        refreshTile()
        if (stopService) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun stopEngine() {
        statsJob?.cancel()
        statsJob = null
        runCatching { Tulpor.stop() }
        runCatching { tun?.close() }
        tun = null
    }

    override fun onRevoke() {
        ConnectionLog.add(this, "VPN tizim tomonidan to'xtatildi (boshqa VPN yoqildi yoki ruxsat olib tashlandi)")
        // Another VPN took over or the user revoked permission in system settings.
        scope.launch { disconnect(stopService = true) }
    }

    override fun onDestroy() {
        stopEngine()
        if (VpnState.status.value !is VpnStatus.Failed) VpnState.set(VpnStatus.Disconnected)
        refreshTile()
        scope.cancel()
        super.onDestroy()
    }

    // --- notification -------------------------------------------------------------------

    private fun goForeground(n: Notification) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, n, type)
    }

    private fun notify(n: Notification) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT < 33
        ) {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, n)
        }
    }

    private fun buildNotification(title: String, server: Server?, t: Traffic? = null): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this, 1, Intent(this, TulporVpnService::class.java).setAction(ACTION_DISCONNECT),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val text = buildString {
            if (server != null) append(serverLabel(server))
            if (t != null) {
                append("  ↓ ").append(Formatter.formatShortFileSize(this@TulporVpnService, t.downBps)).append("/s")
                append("  ↑ ").append(Formatter.formatShortFileSize(this@TulporVpnService, t.upBps)).append("/s")
            }
        }
        return NotificationCompat.Builder(this, TulporApp.CHANNEL_VPN)
            .setSmallIcon(R.drawable.ic_stat_tulpor)
            .setColor(getColor(R.color.notification_accent))
            .setContentTitle("${getString(R.string.app_name)} · $title")
            .setContentText(text.ifBlank { null })
            .setContentIntent(open)
            .addAction(0, getString(R.string.action_disconnect), stop)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun serverLabel(s: Server): String {
        val country = if (s.country.isNotBlank()) Countries.displayName(s.country) else s.name
        return if (s.city.isNotBlank()) "$country · ${s.city}" else country
    }

    private fun refreshTile() {
        runCatching {
            TileService.requestListeningState(this, ComponentName(this, TulporTileService::class.java))
        }
    }

    companion object {
        private const val TAG = "TulporVpn"
        private const val NOTIFICATION_ID = 1
        const val ACTION_CONNECT = "uz.tulpor.vpn.CONNECT"
        const val ACTION_DISCONNECT = "uz.tulpor.vpn.DISCONNECT"

        /** Starts the VPN. The caller must have obtained permission via [VpnService.prepare]. */
        fun connect(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, TulporVpnService::class.java).setAction(ACTION_CONNECT)
            )
        }

        fun disconnect(context: Context) {
            context.startService(Intent(context, TulporVpnService::class.java).setAction(ACTION_DISCONNECT))
        }
    }
}
