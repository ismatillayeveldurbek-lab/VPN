package uz.tulpor.vpn.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.tulpor.vpn.R
import uz.tulpor.vpn.TulporApp
import uz.tulpor.vpn.data.AppSettings
import uz.tulpor.vpn.data.Server
import uz.tulpor.vpn.data.SplitMode
import uz.tulpor.vpn.vpn.Pinger
import uz.tulpor.vpn.vpn.Traffic
import uz.tulpor.vpn.vpn.TulporVpnService
import uz.tulpor.vpn.vpn.VpnState
import uz.tulpor.vpn.vpn.VpnStatus

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val tapp = app as TulporApp

    val settings: StateFlow<AppSettings> =
        tapp.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())
    val servers: StateFlow<List<Server>> =
        tapp.servers.servers.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val status: StateFlow<VpnStatus> = VpnState.status
    val traffic: StateFlow<Traffic> = VpnState.traffic
    val pings: StateFlow<Map<String, Long>> = Pinger.results
    val pinging: StateFlow<Boolean> = Pinger.running

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    private fun say(resId: Int, vararg args: Any) {
        _messages.tryEmit(getApplication<Application>().getString(resId, *args))
    }

    fun connect() = TulporVpnService.connect(getApplication())
    fun disconnect() = TulporVpnService.disconnect(getApplication())

    fun selectServer(id: String) = viewModelScope.launch {
        tapp.settings.setSelectedServer(id)
        // Switching while connected: reconnect to the new server.
        if (status.value is VpnStatus.Connected) connect()
    }

    fun addLinks(text: String, subscriptionUrl: String, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        var ok = false
        if (text.isNotBlank()) {
            val n = tapp.servers.addFromText(text)
            if (n > 0) {
                say(R.string.servers_added, n); ok = true
            } else {
                say(R.string.no_valid_links)
            }
        }
        if (subscriptionUrl.isNotBlank()) {
            tapp.settings.setSubscriptionUrl(subscriptionUrl.trim())
            ok = refreshSubscriptionInternal(subscriptionUrl.trim()) || ok
        }
        if (ok) pingAll()
        onDone(ok)
    }

    fun refreshSubscription() = viewModelScope.launch {
        val url = settings.value.subscriptionUrl
        if (url.isNotBlank() && refreshSubscriptionInternal(url)) pingAll()
    }

    private suspend fun refreshSubscriptionInternal(url: String): Boolean = try {
        val n = tapp.servers.refreshSubscription(url)
        say(R.string.servers_added, n); true
    } catch (_: Exception) {
        say(R.string.subscription_failed); false
    }

    fun removeServer(id: String) = viewModelScope.launch {
        tapp.servers.remove(id)
        if (settings.value.selectedServerId == id) tapp.settings.setSelectedServer(AppSettings.AUTO_SERVER)
    }

    fun pingAll() = viewModelScope.launch { Pinger.measureAll(servers.value) }

    fun setAutoConnect(v: Boolean) = viewModelScope.launch { tapp.settings.setAutoConnect(v) }
    fun setBypassUz(v: Boolean) = viewModelScope.launch { tapp.settings.setBypassUz(v) }
    fun setSplitMode(v: SplitMode) = viewModelScope.launch { tapp.settings.setSplitMode(v) }
    fun setSplitApps(v: Set<String>) = viewModelScope.launch { tapp.settings.setSplitApps(v) }

    /** Picks which server the home screen shows when "auto" is selected and not connected. */
    fun displayServer(): Server? {
        val st = status.value
        if (st is VpnStatus.Connected) return st.server
        val id = settings.value.selectedServerId
        return servers.value.firstOrNull { it.id == id }
    }

    init {
        // Measure latency once on start so "fastest server" is ready.
        viewModelScope.launch {
            servers.first { it.isNotEmpty() }
            if (Pinger.results.value.isEmpty()) Pinger.measureAll(servers.value)
        }
    }
}
