package uz.tulpor.vpn.vpn

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import uz.tulpor.vpn.MainActivity
import uz.tulpor.vpn.R

/** Quick Settings tile that toggles the VPN. */
class TulporTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        update()
    }

    override fun onClick() {
        super.onClick()
        when (VpnState.status.value) {
            is VpnStatus.Connected, VpnStatus.Connecting -> TulporVpnService.disconnect(this)
            else -> {
                if (VpnService.prepare(this) != null) {
                    openApp()
                } else {
                    TulporVpnService.connect(this)
                }
            }
        }
        update()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MainActivity.EXTRA_CONNECT, true)
        if (Build.VERSION.SDK_INT >= 34) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    private fun update() {
        val tile = qsTile ?: return
        val status = VpnState.status.value
        tile.state = when (status) {
            is VpnStatus.Connected -> Tile.STATE_ACTIVE
            VpnStatus.Connecting -> Tile.STATE_ACTIVE
            else -> Tile.STATE_INACTIVE
        }
        tile.label = getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = getString(
                when (status) {
                    is VpnStatus.Connected -> R.string.status_connected
                    VpnStatus.Connecting -> R.string.status_connecting
                    else -> R.string.status_disconnected
                }
            )
        }
        tile.updateTile()
    }
}
