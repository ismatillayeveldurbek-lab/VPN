package uz.tulpor.vpn.ui.servers

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.tulpor.vpn.R
import uz.tulpor.vpn.data.AppSettings
import uz.tulpor.vpn.data.Countries
import uz.tulpor.vpn.data.Server
import uz.tulpor.vpn.ui.components.AutoBadge
import uz.tulpor.vpn.ui.components.Flag
import uz.tulpor.vpn.ui.components.GoldBrush
import uz.tulpor.vpn.ui.components.PingBars
import uz.tulpor.vpn.ui.components.SegmentedControl
import uz.tulpor.vpn.ui.components.appear
import uz.tulpor.vpn.ui.components.bouncyClickable
import uz.tulpor.vpn.ui.components.glass
import uz.tulpor.vpn.ui.theme.TulporColors

private enum class Tab { ALL, FREE, PREMIUM }

@Composable
fun ServersScreen(
    servers: List<Server>,
    selectedId: String,
    pings: Map<String, Long>,
    pinging: Boolean,
    isPremium: Boolean,
    onSelect: (String) -> Unit,
    onPingAll: () -> Unit,
    onAdd: () -> Unit,
    onDelete: (Server) -> Unit,
    onPremium: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf(Tab.ALL) }
    var toDelete by remember { mutableStateOf<Server?>(null) }

    val filtered = servers.filter { s ->
        val q = query.trim().lowercase()
        val matches = q.isEmpty() || listOf(s.name, s.city, Countries.displayName(s.country), s.country)
            .any { it.lowercase().contains(q) }
        val inTab = when (tab) {
            Tab.ALL -> true
            Tab.FREE -> !s.premium
            Tab.PREMIUM -> s.premium
        }
        matches && inTab
    }
    val mine = filtered.filter { it.source != Server.SOURCE_BUILTIN }
    val free = filtered.filter { it.source == Server.SOURCE_BUILTIN && !it.premium }
    val premium = filtered.filter { it.source == Server.SOURCE_BUILTIN && it.premium }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 14.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.servers_title), style = MaterialTheme.typography.titleLarge, maxLines = 1,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            val inf = rememberInfiniteTransition(label = "spin")
            val angle by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "a")
            RoundIcon(onClick = onPingAll) {
                Icon(Icons.Rounded.Refresh, stringResource(R.string.ping_all), tint = TulporColors.Text1,
                    modifier = Modifier.rotate(if (pinging) angle else 0f))
            }
            Spacer(Modifier.width(8.dp))
            RoundIcon(gold = true, onClick = onAdd) {
                Icon(Icons.Rounded.Add, stringResource(R.string.add_server), tint = TulporColors.OnGold)
            }
        }

        // Search
        Row(
            Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .glass(RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = TulporColors.Text3, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text(stringResource(R.string.search_hint), color = TulporColors.Text3, fontSize = 14.sp)
                BasicTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    textStyle = TextStyle(color = TulporColors.Text1, fontSize = 14.sp),
                    cursorBrush = SolidColor(TulporColors.Gold), modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        SegmentedControl(
            options = listOf(stringResource(R.string.tab_all), stringResource(R.string.tab_free), stringResource(R.string.tab_premium)),
            selected = tab.ordinal,
            onSelect = { tab = Tab.entries[it] },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )

        if (servers.isEmpty()) {
            EmptyState(onAdd)
            return@Column
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (tab != Tab.PREMIUM) item("auto") {
                Row(
                    Modifier
                        .animateItem()
                        .appear(0)
                        .fillMaxWidth()
                        .bouncyClickable(RoundedCornerShape(16.dp)) { onSelect(AppSettings.AUTO_SERVER) }
                        .glass(RoundedCornerShape(16.dp), selected = selectedId == AppSettings.AUTO_SERVER)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AutoBadge()
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.fastest_server), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(stringResource(R.string.auto), color = TulporColors.Text3, fontSize = 12.sp)
                    }
                    SelectedCheck(selectedId == AppSettings.AUTO_SERVER)
                }
            }
            var idx = 1
            section(R.string.section_my, mine, idx, selectedId, pings, isPremium, onSelect, onPremium) { toDelete = it }
            idx += mine.size + 1
            section(R.string.section_free, free, idx, selectedId, pings, isPremium, onSelect, onPremium, null)
            idx += free.size + 1
            section(R.string.section_premium, premium, idx, selectedId, pings, isPremium, onSelect, onPremium, null)
        }
    }

    toDelete?.let { s ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(s.name) },
            text = { Text("${s.address}:${s.port}") },
            confirmButton = {
                TextButton(onClick = { onDelete(s); toDelete = null }) {
                    Text(stringResource(R.string.delete), color = TulporColors.Red)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) } },
            containerColor = Color(0xFF1D1750),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(
    title: Int,
    list: List<Server>,
    startIndex: Int,
    selectedId: String,
    pings: Map<String, Long>,
    isPremium: Boolean,
    onSelect: (String) -> Unit,
    onPremium: () -> Unit,
    onLongPress: ((Server) -> Unit)?,
) {
    if (list.isEmpty()) return
    item("h$title") {
        Text(
            stringResource(title).uppercase(), style = MaterialTheme.typography.labelMedium,
            color = TulporColors.Text3, maxLines = 1,
            modifier = Modifier.animateItem().appear(startIndex).padding(start = 4.dp, top = 10.dp, bottom = 2.dp),
        )
    }
    itemsIndexed(list, key = { _, it -> it.id }) { i, s ->
        ServerRow(
            Modifier.animateItem().appear(startIndex + 1 + i),
            s, selectedId == s.id, pings[s.id], locked = s.premium && !isPremium,
            onClick = { if (s.premium && !isPremium) onPremium() else onSelect(s.id) },
            onLongClick = onLongPress?.let { { it(s) } },
        )
    }
}

@Composable
private fun ServerRow(
    modifier: Modifier,
    s: Server,
    selected: Boolean,
    ping: Long?,
    locked: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
) {
    Row(
        modifier
            .fillMaxWidth()
            .bouncyClickable(RoundedCornerShape(16.dp), onLongClick = onLongClick, onClick = onClick)
            .glass(RoundedCornerShape(16.dp), selected = selected)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Flag(s.country)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val title = if (s.country.isNotBlank()) Countries.displayName(s.country) else s.name
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = s.city.ifBlank { if (title != s.name) s.name else s.address }
            Text(sub, color = TulporColors.Text3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        AnimatedContent(ping, transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) }, label = "ping") { p ->
            Text(
                when {
                    p == null -> "—"
                    p < 0 -> stringResource(R.string.unreachable)
                    else -> stringResource(R.string.ms, p)
                },
                color = if (p != null && p < 0) TulporColors.Red.copy(alpha = .8f) else TulporColors.Text2,
                fontSize = 12.sp, maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(contentAlignment = Alignment.Center) {
            when {
                locked -> Icon(Icons.Rounded.WorkspacePremium, null, tint = TulporColors.Gold, modifier = Modifier.size(18.dp))
                else -> {
                    androidx.compose.animation.AnimatedVisibility(!selected, enter = fadeIn(), exit = fadeOut()) { PingBars(ping) }
                    SelectedCheck(selected)
                }
            }
        }
    }
}

/** Gold check that pops in with a spring when an item becomes selected. */
@Composable
private fun SelectedCheck(visible: Boolean) {
    androidx.compose.animation.AnimatedVisibility(
        visible,
        enter = scaleIn(spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium)) + fadeIn(),
        exit = scaleOut(tween(150)) + fadeOut(tween(150)),
    ) {
        Icon(Icons.Rounded.Check, null, tint = TulporColors.Gold, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun RoundIcon(gold: Boolean = false, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .bouncyClickable(CircleShape, pressedScale = 0.9f, onClick = onClick)
            .then(if (gold) Modifier.clip(CircleShape).background(GoldBrush) else Modifier.glass(CircleShape)),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(20.dp).appear(0).glass(RoundedCornerShape(22.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.empty_servers_title), style = MaterialTheme.typography.titleMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.empty_servers_text), color = TulporColors.Text2, fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        uz.tulpor.vpn.ui.components.GoldButton(stringResource(R.string.add_server), onClick = onAdd)
    }
}
