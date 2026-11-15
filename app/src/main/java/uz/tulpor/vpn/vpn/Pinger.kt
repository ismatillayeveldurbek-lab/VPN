package uz.tulpor.vpn.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import uz.tulpor.vpn.data.ConfigBuilder
import uz.tulpor.vpn.data.Server
import uz.tulpor.tulpor.Tulpor

/** Measures real latency (an HTTP request through the tunnel) for servers. */
object Pinger {
    /** Server id -> delay in ms; -1 means unreachable. Missing means not measured yet. */
    private val _results = MutableStateFlow<Map<String, Long>>(emptyMap())
    val results: StateFlow<Map<String, Long>> = _results.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val limit = Semaphore(4)

    suspend fun measure(server: Server): Long = limit.withPermit {
        withContext(Dispatchers.IO) {
            val d = runCatching {
                Tulpor.measureDelay(ConfigBuilder.pingConfig(server), ConfigBuilder.PING_URL, 6000)
            }.getOrDefault(-1L)
            _results.update { it + (server.id to d) }
            d
        }
    }

    suspend fun measureAll(servers: List<Server>) = coroutineScope {
        _running.value = true
        try {
            servers.map { async { measure(it) } }.awaitAll()
        } finally {
            _running.value = false
        }
    }

    /** Picks the reachable server with the lowest delay, measuring first if needed. */
    suspend fun fastest(servers: List<Server>): Server? {
        if (servers.isEmpty()) return null
        val missing = servers.filter { it.id !in _results.value }
        if (missing.isNotEmpty()) measureAll(missing)
        val res = _results.value
        return servers
            .filter { (res[it.id] ?: -1) >= 0 }
            .minByOrNull { res[it.id]!! }
            ?: run {
                // Results may be stale: measure everything once more before giving up.
                measureAll(servers)
                servers.filter { (_results.value[it.id] ?: -1) >= 0 }.minByOrNull { _results.value[it.id]!! }
            }
    }
}
