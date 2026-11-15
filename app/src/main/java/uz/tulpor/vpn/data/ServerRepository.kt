package uz.tulpor.vpn.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/** Stores servers added by the user or fetched from a subscription URL. */
class ServerRepository(private val context: Context) {
    private val key = stringPreferencesKey("servers_json")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

    private val builtin: List<Server> by lazy {
        runCatching {
            context.assets.open("servers.json").bufferedReader().use { it.readText() }
                .let { json.decodeFromString<List<Server>>(it) }
                .map { it.copy(source = Server.SOURCE_BUILTIN) }
        }.getOrDefault(emptyList())
    }

    val servers: Flow<List<Server>> = context.dataStore.data.map { p ->
        val stored = p[key]?.let { runCatching { json.decodeFromString<List<Server>>(it) }.getOrNull() }
        builtin + stored.orEmpty()
    }

    suspend fun all(): List<Server> = servers.first()

    /** Adds all vless:// links found in [text]. Returns how many new servers were added. */
    suspend fun addFromText(text: String): Int {
        val parsed = VlessLink.parseAll(text)
        if (parsed.isEmpty()) return 0
        var added = 0
        update { list ->
            val ids = list.map { it.id }.toSet()
            val fresh = parsed.filter { it.id !in ids }
            added = fresh.size
            list + fresh
        }
        return added
    }

    suspend fun remove(id: String) = update { list -> list.filterNot { it.id == id } }

    /**
     * Downloads a subscription (plain or base64 list of vless:// links) and replaces
     * previously imported subscription servers. Returns the number of servers.
     */
    suspend fun refreshSubscription(url: String): Int = withContext(Dispatchers.IO) {
        val body = (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("User-Agent", "TulporVPN")
            try {
                if (responseCode !in 200..299) error("HTTP $responseCode")
                inputStream.bufferedReader().use { it.readText() }
            } finally {
                disconnect()
            }
        }
        val text = decodeSubscription(body)
        val servers = VlessLink.parseAll(text, Server.SOURCE_SUBSCRIPTION)
        if (servers.isEmpty()) error("empty subscription")
        update { list -> list.filterNot { it.source == Server.SOURCE_SUBSCRIPTION } + servers }
        servers.size
    }

    private suspend fun update(transform: (List<Server>) -> List<Server>) {
        context.dataStore.edit { p ->
            val current = p[key]?.let { runCatching { json.decodeFromString<List<Server>>(it) }.getOrNull() }.orEmpty()
            p[key] = json.encodeToString(transform(current))
        }
    }

    companion object {
        fun decodeSubscription(body: String): String {
            val trimmed = body.trim()
            if (trimmed.contains("://")) return trimmed
            return runCatching {
                String(Base64.decode(trimmed, Base64.DEFAULT or Base64.URL_SAFE))
            }.getOrDefault(trimmed)
        }
    }
}
