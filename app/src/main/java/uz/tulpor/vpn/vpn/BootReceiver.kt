package uz.tulpor.vpn.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.tulpor.vpn.TulporApp

/** Reconnects after reboot / app update when "auto-connect" is enabled. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as TulporApp
                if (app.settings.current().autoConnect && VpnService.prepare(context) == null) {
                    TulporVpnService.connect(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
