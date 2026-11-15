package uz.tulpor.vpn.vpn

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uz.tulpor.vpn.data.Server

sealed interface VpnStatus {
    data object Disconnected : VpnStatus
    data object Connecting : VpnStatus
    data class Connected(val server: Server, val sinceElapsedMs: Long) : VpnStatus
    data object Disconnecting : VpnStatus
    data class Failed(val message: String) : VpnStatus
}

data class Traffic(
    val downBps: Long = 0,
    val upBps: Long = 0,
    val downTotal: Long = 0,
    val upTotal: Long = 0,
)

/** Process-wide VPN state shared by the service, UI, tile and notification. */
object VpnState {
    private val _status = MutableStateFlow<VpnStatus>(VpnStatus.Disconnected)
    val status: StateFlow<VpnStatus> = _status.asStateFlow()

    private val _traffic = MutableStateFlow(Traffic())
    val traffic: StateFlow<Traffic> = _traffic.asStateFlow()

    internal fun set(status: VpnStatus) {
        _status.value = status
        if (status !is VpnStatus.Connected) _traffic.value = Traffic()
    }

    internal fun setTraffic(t: Traffic) {
        _traffic.value = t
    }
}
