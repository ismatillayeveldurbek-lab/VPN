package uz.tulpor.vpn.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import uz.tulpor.vpn.BuildConfig
import uz.tulpor.vpn.R
import uz.tulpor.vpn.data.AppSettings
import uz.tulpor.vpn.data.SplitMode
import uz.tulpor.vpn.ui.components.glass
import uz.tulpor.vpn.ui.theme.TulporColors
import uz.tulpor.tulpor.Tulpor

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onAutoConnect: (Boolean) -> Unit,
    onBypassUz: (Boolean) -> Unit,
    onOpenApps: () -> Unit,
    onRefreshSubscription: () -> Unit,
) {
    val ctx = LocalContext.current
    var killDialog by remember { mutableStateOf(false) }
    var langDialog by remember { mutableStateOf(false) }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 6.dp)) }
        item { Section(R.string.section_connection) }
        item {
            SettingRow(Icons.Rounded.Shield, stringResource(R.string.kill_switch), stringResource(R.string.kill_switch_sub),
                onClick = { killDialog = true }) { Chevron() }
        }
        item {
            SettingRow(Icons.Rounded.Bolt, stringResource(R.string.auto_connect), stringResource(R.string.auto_connect_sub),
                onClick = { onAutoConnect(!settings.autoConnect) }) { GoldSwitch(settings.autoConnect, onAutoConnect) }
        }
        item {
            SettingRow(Icons.Rounded.SwapHoriz, stringResource(R.string.bypass_uz), stringResource(R.string.bypass_uz_sub),
                onClick = { onBypassUz(!settings.bypassUz) }) { GoldSwitch(settings.bypassUz, onBypassUz) }
        }
        item {
            val value = when (settings.splitMode) {
                SplitMode.ALL -> stringResource(R.string.split_all)
                else -> stringResource(R.string.split_n_apps, settings.splitApps.size)
            }
            SettingRow(Icons.Rounded.Apps, stringResource(R.string.split_tunneling), stringResource(R.string.split_tunneling_sub),
                onClick = onOpenApps) { Value(value) }
        }
        item {
            SettingRow(Icons.Rounded.Sync, stringResource(R.string.protocol), null, onClick = null) { Value("VLESS · Reality") }
        }
        if (settings.subscriptionUrl.isNotBlank()) item {
            SettingRow(Icons.Rounded.Sync, stringResource(R.string.subscription_refresh), settings.subscriptionUrl,
                onClick = onRefreshSubscription) { Chevron() }
        }
        item { Section(R.string.section_app) }
        item {
            val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            val current = when {
                tag.startsWith("ru") -> "Русский"
                tag.startsWith("en") -> "English"
                tag.startsWith("uz") -> "O'zbekcha"
                else -> stringResource(R.string.language_system)
            }
            SettingRow(Icons.Rounded.Language, stringResource(R.string.language), null, onClick = { langDialog = true }) { Value(current) }
        }
        item {
            SettingRow(Icons.Rounded.Info, stringResource(R.string.about),
                stringResource(R.string.version, BuildConfig.VERSION_NAME, remember { runCatching { Tulpor.version() }.getOrDefault("?") }),
                onClick = null) {}
        }
    }

    if (killDialog) {
        AlertDialog(
            onDismissRequest = { killDialog = false },
            title = { Text(stringResource(R.string.kill_switch)) },
            text = { Text(stringResource(R.string.kill_switch_dialog)) },
            confirmButton = {
                TextButton(onClick = {
                    killDialog = false
                    runCatching { ctx.startActivity(Intent(Settings.ACTION_VPN_SETTINGS)) }
                }) { Text(stringResource(R.string.open_settings), color = TulporColors.Gold) }
            },
            dismissButton = { TextButton(onClick = { killDialog = false }) { Text(stringResource(R.string.cancel)) } },
            containerColor = Color(0xFF1D1750),
        )
    }
    if (langDialog) {
        val options = listOf("" to stringResource(R.string.language_system), "uz" to "O'zbekcha", "ru" to "Русский", "en" to "English")
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        AlertDialog(
            onDismissRequest = { langDialog = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    options.forEach { (code, label) ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable {
                                langDialog = false
                                AppCompatDelegate.setApplicationLocales(
                                    if (code.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(code)
                                )
                            }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = if (code.isEmpty()) tag.isEmpty() else tag.startsWith(code), onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = TulporColors.Gold),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = Color(0xFF1D1750),
        )
    }
}

@Composable
private fun Section(res: Int) {
    Text(
        stringResource(res).uppercase(), style = MaterialTheme.typography.labelMedium,
        color = TulporColors.Text3, modifier = Modifier.padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
    )
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(16.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.White.copy(alpha = .10f))
                .border(1.dp, Color.White.copy(alpha = .12f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = TulporColors.Gold, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = TulporColors.Text3, fontSize = 12.sp, maxLines = 2)
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun Chevron() = Icon(Icons.Rounded.ChevronRight, null, tint = TulporColors.Text3)

@Composable
private fun Value(text: String) = Text(text, color = TulporColors.Text2, fontSize = 13.sp)

@Composable
private fun GoldSwitch(checked: Boolean, onChange: (Boolean) -> Unit) = Switch(
    checked = checked, onCheckedChange = onChange,
    colors = SwitchDefaults.colors(
        checkedTrackColor = TulporColors.Amber, checkedThumbColor = Color.White,
        uncheckedTrackColor = Color.White.copy(alpha = .15f), uncheckedThumbColor = Color.White,
        uncheckedBorderColor = Color.Transparent,
    ),
)
