package uz.tulpor.vpn.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.SystemClock
import android.text.format.Formatter
import kotlinx.coroutines.delay
import uz.tulpor.vpn.R
import uz.tulpor.vpn.data.Countries
import uz.tulpor.vpn.data.Server
import uz.tulpor.vpn.ui.components.AutoBadge
import uz.tulpor.vpn.ui.components.Flag
import uz.tulpor.vpn.ui.components.GoldBrush
import uz.tulpor.vpn.ui.components.PingBars
import uz.tulpor.vpn.ui.components.glass
import uz.tulpor.vpn.ui.theme.TulporColors
import uz.tulpor.vpn.vpn.Traffic
import uz.tulpor.vpn.vpn.VpnStatus

@Composable
fun HomeScreen(
    status: VpnStatus,
    traffic: Traffic,
    server: Server?,
    serverPing: Long?,
    hasServers: Boolean,
    onToggle: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenPremium: () -> Unit,
) {
    val connected = status is VpnStatus.Connected
    val busy = status is VpnStatus.Connecting || status is VpnStatus.Disconnecting

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(bottom = 96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.splash_icon), null, Modifier.size(40.dp).scale(1.6f))
            Spacer(Modifier.width(6.dp))
            Text("Tulpor", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp))
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .clip(CircleShape)
                    .background(GoldBrush)
                    .clickable(onClick = onOpenPremium)
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.WorkspacePremium, null, tint = TulporColors.OnGold, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.premium), color = TulporColors.OnGold, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Status
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) TulporColors.Green else if (busy) TulporColors.Amber else TulporColors.Red))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (connected) R.string.status_protected else R.string.status_unprotected),
                color = TulporColors.Text2, fontSize = 14.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
        AnimatedContent(status::class, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "title") {
            if (status is VpnStatus.Connected) {
                ConnectedTimer(status.sinceElapsedMs)
            } else {
                Text(
                    stringResource(
                        when (status) {
                            VpnStatus.Connecting -> R.string.status_connecting
                            VpnStatus.Disconnecting -> R.string.status_disconnecting
                            is VpnStatus.Failed -> R.string.status_failed
                            else -> R.string.status_disconnected
                        }
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        ConnectButton(connected = connected, busy = busy, onClick = onToggle)
        Spacer(Modifier.height(18.dp))

        val hint = when {
            status is VpnStatus.Failed -> status.message
            connected -> stringResource(R.string.status_connected)
            busy -> ""
            else -> stringResource(R.string.tap_to_connect)
        }
        Text(
            hint,
            color = when {
                connected -> TulporColors.Green
                status is VpnStatus.Failed -> TulporColors.Red.copy(alpha = .9f)
                else -> TulporColors.Text3
            },
            fontWeight = if (connected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.height(40.dp),
        )
        Spacer(Modifier.weight(0.6f))

        if (connected) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Modifier.weight(1f), stringResource(R.string.download), traffic.downBps, true)
                StatTile(Modifier.weight(1f), stringResource(R.string.upload), traffic.upBps, false)
            }
            Spacer(Modifier.height(12.dp))
        }
        ServerCard(server = server, ping = serverPing, hasServers = hasServers, onClick = onOpenServers)
    }
}

@Composable
private fun ConnectedTimer(since: Long) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(since) {
        while (true) {
            now = SystemClock.elapsedRealtime(); delay(1000)
        }
    }
    val s = ((now - since) / 1000).coerceAtLeast(0)
    Text(
        "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60),
        style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
    )
}

@Composable
private fun ConnectButton(connected: Boolean, busy: Boolean, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "pulse")
    val pulse by inf.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart), label = "p",
    )
    val breathe by inf.animateFloat(
        0.92f, 1.04f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b",
    )
    val on by animateFloatAsState(if (connected) 1f else 0f, tween(600), label = "on")
    val gold = TulporColors.Gold

    Box(
        Modifier
            .size(250.dp)
            .drawBehind {
                val r = size.minDimension / 2
                // static rings
                drawCircle(Color.White.copy(alpha = .06f + .14f * on), r * 0.98f, style = Stroke(1.dp.toPx()))
                drawCircle(Color.White.copy(alpha = .10f + .20f * on), r * 0.84f, style = Stroke(2.dp.toPx()))
                if (on > 0f) {
                    drawCircle(
                        Brush.radialGradient(listOf(gold.copy(alpha = .45f * on), Color.Transparent), radius = r),
                        r,
                    )
                }
                if (busy) {
                    for (k in 0..1) {
                        val p = (pulse + k * 0.5f) % 1f
                        drawCircle(gold.copy(alpha = (1f - p) * .5f), r * (0.62f + 0.38f * p), style = Stroke(3.dp.toPx()))
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val shape = CircleShape
        Box(
            Modifier
                .size(176.dp)
                .scale(if (connected) breathe.coerceIn(0.97f, 1.02f) else 1f)
                .then(
                    if (connected) Modifier
                        .clip(shape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFFFFF3C4), TulporColors.Gold, TulporColors.Amber),
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = .5f), shape)
                    else Modifier.glass(shape)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, role = Role.Button, enabled = true, onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(R.drawable.splash_icon), null,
                modifier = Modifier.size(170.dp).alpha(if (connected) 1f else 0.42f),
                colorFilter = ColorFilter.tint(Color.White),
            )
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier, label: String, bps: Long, down: Boolean) {
    val ctx = LocalContext.current
    Column(modifier.glass(RoundedCornerShape(18.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (down) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, null,
                tint = if (down) TulporColors.Green else TulporColors.Gold, modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(label, color = TulporColors.Text3, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(Formatter.formatShortFileSize(ctx, bps) + "/s", fontWeight = FontWeight.Bold, fontSize = 17.sp)
    }
}

@Composable
private fun ServerCard(server: Server?, ping: Long?, hasServers: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (server == null) AutoBadge() else Flag(server.country)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val title = when {
                !hasServers -> stringResource(R.string.add_server)
                server == null -> stringResource(R.string.fastest_server)
                server.country.isNotBlank() -> Countries.displayName(server.country)
                else -> server.name
            }
            val sub = when {
                !hasServers -> stringResource(R.string.empty_servers_title)
                server == null -> stringResource(R.string.auto_selected)
                else -> listOfNotNull(
                    server.city.ifBlank { null } ?: server.name.takeIf { server.country.isNotBlank() && it != title },
                    ping?.takeIf { it >= 0 }?.let { stringResource(R.string.ms, it) },
                ).joinToString(" · ")
            }
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1)
            if (sub.isNotBlank()) Text(sub, color = TulporColors.Text3, fontSize = 12.sp, maxLines = 1)
        }
        if (server != null) {
            PingBars(ping)
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = TulporColors.Text3)
    }
}
