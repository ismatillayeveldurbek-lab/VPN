package uz.tulpor.vpn.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "tulpor")

enum class SplitMode { ALL, ONLY_SELECTED, EXCEPT_SELECTED }

data class AppSettings(
    val selectedServerId: String = AUTO_SERVER,
    val autoConnect: Boolean = false,
    val bypassUz: Boolean = true,
    val splitMode: SplitMode = SplitMode.ALL,
    val splitApps: Set<String> = emptySet(),
    val subscriptionUrl: String = "",
    val isPremium: Boolean = false,
) {
    companion object {
        const val AUTO_SERVER = "auto"
    }
}

class SettingsRepository(private val context: Context) {
    private object K {
        val server = stringPreferencesKey("selected_server")
        val autoConnect = booleanPreferencesKey("auto_connect")
        val bypassUz = booleanPreferencesKey("bypass_uz")
        val splitMode = stringPreferencesKey("split_mode")
        val splitApps = stringSetPreferencesKey("split_apps")
        val subscription = stringPreferencesKey("subscription_url")
        val premium = booleanPreferencesKey("premium")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            selectedServerId = p[K.server] ?: AppSettings.AUTO_SERVER,
            autoConnect = p[K.autoConnect] ?: false,
            bypassUz = p[K.bypassUz] ?: true,
            splitMode = p[K.splitMode]?.let { runCatching { SplitMode.valueOf(it) }.getOrNull() } ?: SplitMode.ALL,
            splitApps = p[K.splitApps] ?: emptySet(),
            subscriptionUrl = p[K.subscription] ?: "",
            isPremium = p[K.premium] ?: false,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setSelectedServer(id: String) = context.dataStore.edit { it[K.server] = id }
    suspend fun setAutoConnect(v: Boolean) = context.dataStore.edit { it[K.autoConnect] = v }
    suspend fun setBypassUz(v: Boolean) = context.dataStore.edit { it[K.bypassUz] = v }
    suspend fun setSplitMode(v: SplitMode) = context.dataStore.edit { it[K.splitMode] = v.name }
    suspend fun setSplitApps(v: Set<String>) = context.dataStore.edit { it[K.splitApps] = v }
    suspend fun setSubscriptionUrl(v: String) = context.dataStore.edit { it[K.subscription] = v }
}
