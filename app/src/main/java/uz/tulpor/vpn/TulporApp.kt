package uz.tulpor.vpn

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import uz.tulpor.vpn.data.ServerRepository
import uz.tulpor.vpn.data.SettingsRepository
import uz.tulpor.tulpor.Tulpor

class TulporApp : Application() {
    lateinit var settings: SettingsRepository
        private set
    lateinit var servers: ServerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settings = SettingsRepository(this)
        servers = ServerRepository(this)
        Tulpor.touch() // load the native engine early
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_VPN, getString(R.string.channel_vpn), NotificationManager.IMPORTANCE_LOW).apply {
                setShowBadge(false)
            }
        )
    }

    companion object {
        const val CHANNEL_VPN = "vpn"
        lateinit var instance: TulporApp
            private set
    }
}
