package uz.tulpor.vpn

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import uz.tulpor.vpn.ui.MainViewModel
import uz.tulpor.vpn.ui.TulporRoot
import uz.tulpor.vpn.ui.theme.TulporTheme
import uz.tulpor.vpn.vpn.VpnStatus

class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()
    private var pendingImport by mutableStateOf<String?>(null)

    private val vpnPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (VpnService.prepare(this) == null) vm.connect()
    }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleIntent(intent)

        setContent {
            TulporTheme {
                TulporRoot(
                    vm = vm,
                    pendingImport = pendingImport,
                    onImportConsumed = { pendingImport = null },
                    onToggleVpn = ::toggleVpn,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString?.takeIf { it.startsWith("vless://", true) }?.let { pendingImport = it }
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.contains("vless://", true) }
                ?.let { pendingImport = it }
        }
        if (intent.getBooleanExtra(EXTRA_CONNECT, false)) {
            intent.removeExtra(EXTRA_CONNECT)
            startVpn()
        }
    }

    private fun toggleVpn() {
        when (vm.status.value) {
            is VpnStatus.Connected, VpnStatus.Connecting -> vm.disconnect()
            else -> startVpn()
        }
    }

    private fun startVpn() {
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        val prepare = VpnService.prepare(this)
        if (prepare != null) vpnPermission.launch(prepare) else vm.connect()
    }

    companion object {
        const val EXTRA_CONNECT = "connect"
    }
}
