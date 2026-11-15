package uz.tulpor.vpn.ui.home

import android.os.SystemClock
import android.text.format.Formatter
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uz.tulpor.vpn.R
import uz.tulpor.vpn.data.Countries
import uz.tulpor.vpn.data.Server
import uz.tulpor.vpn.ui.components.AutoBadge
import uz.tulpor.vpn.ui.components.Flag
import uz.tulpor.vpn.ui.components.GoldBrush
import uz.tulpor.vpn.ui.components.PingBars
import uz.tulpor.vpn.ui.components.bouncyClickable
import uz.tulpor.vpn.ui.components.glass
import uz.tulpor.vpn.ui.components.shimmerBrush
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
        TopBar(onOpenPremium)
        StatusLine(connected, busy)
        Spacer(Modifier.height(4.dp))
        AnimatedContent(
            targetState = status::class,
            transitionSpec = {
                (fadeIn(tween(300)) + slideInVertically(tween(350)) { it / 2 }) togetherWith
                    (fadeOut(tween(200)) + slideOutVertically(tween(250)) { -it / 2 })
            },
            label = "title",
        ) {
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
                    style = MaterialTheme.typography.headlineMedium, maxLines = 1,
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
        AnimatedContent(hint, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(150)) }, label = "hint") { h ->
            Text(
                h,
                color = when {
                    connected -> TulporColors.Green
                    status is VpnStatus.Failed -> TulporColors.Red.copy(alpha = .9f)
                    else -> TulporColors.Text3
                },
                fontWeight = if (connected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
            )
        }
        Spacer(Modifier.weight(0.6f))

        AnimatedVisibility(
            visible = connected,
            enter = fadeIn(tween(350)) + expandVertically(tween(400)) + slideInVertically(tween(400)) { it / 3 },
            exit = fadeOut(tween(200)) + shrinkVertically(tween(300)),
        ) {
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(Modifier.weight(1f), stringResource(R.string.download), traffic.downBps, true)
                    StatTile(Modifier.weight(1f), stringResource(R.string.upload), traffic.upBps, false)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        ServerCard(server = server, ping = serverPing, hasServers = hasServers, onClick = onOpenServers)
    }
}

@Composable
private fun TopBar(onOpenPremium: () -> Unit) {
    // The logo gently floats; the Premium pill has a slow gold shimmer.
    val inf = rememberInfiniteTransition(label = "top")
    val float by inf.animateFloat(-2f, 2f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")
    val shimmer by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, delayMillis = 1400, easing = LinearEasing)), label = "s")

    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(
            painterResource(R.drawable.splash_icon), null,
            Modifier.size(40.dp).graphicsLayer { scaleX = 1.6f; scaleY = 1.6f; translationY = float.dp.toPx() },
        )
        Spacer(Modifier.width(6.dp))
        Text("Tulpor", style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp))
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .bouncyClickable(CircleShape, onClick = onOpenPremium)
                .clip(CircleShape)
                .background(GoldBrush)
                .drawWithContent {
                    drawContent()
                    drawRect(shimmerBrush(shimmer, size.width))
                }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.WorkspacePremium, null, tint = TulporColors.OnGold, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.premium), color = TulporColors.OnGold, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
        }
    }
}

@Composable
private fun StatusLine(connected: Boolean, busy: Boolean) {
    val dotColor by animateColorAsState(
        when {
            connected -> TulporColors.Green
            busy -> TulporColors.Amber
            else -> TulporColors.Red
        }, tween(400), label = "dot",
    )
    val inf = rememberInfiniteTransition(label = "dotpulse")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "dp")
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(16.dp).drawBehind {
                if (connected || busy) {
                    drawCircle(dotColor.copy(alpha = (1f - pulse) * .5f), radius = size.minDimension / 2 * (0.5f + pulse * 0.5f))
                }
                drawCircle(dotColor, radius = 4.dp.toPx())
            }
        )
        Spacer(Modifier.width(4.dp))
        AnimatedContent(connected, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "prot") { c ->
            Text(
                stringResource(if (c) R.string.status_protected else R.string.status_unprotected),
                color = TulporColors.Text2, fontSize = 14.sp, maxLines = 1,
            )
        }
    }
}

/** HH:MM:SS where each digit rolls up when it changes, like an odometer. */
@Composable
private fun ConnectedTimer(since: Long) {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(since) {
        while (true) {
            now = SystemClock.elapsedRealtime(); delay(1000)
        }
    }
    val s = ((now - since) / 1000).coerceAtLeast(0)
    val text = "%02d:%02d:%02d".format(s / 3600, (s % 3600) / 60, s % 60)
    val style = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp)
    Row {
        text.forEachIndexed { i, ch ->
            if (ch == ':') {
                Text(":", style = style)
            } else {
                AnimatedContent(
                    targetState = ch,
                    transitionSpec = {
                        (slideInVertically(tween(260)) { it } + fadeIn(tween(260))) togetherWith
                            (slideOutVertically(tween(260)) { -it } + fadeOut(tween(200)))
                    },
                    label = "digit$i",
                ) { c -> Text(c.toString(), style = style) }
            }
        }
    }
}

@Composable
private fun ConnectButton(connected: Boolean, busy: Boolean, onClick: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val inf = rememberInfiniteTransition(label = "btn")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "p")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "spin")
    val breathe by inf.animateFloat(0.985f, 1.02f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b")

    val on by animateFloatAsState(if (connected) 1f else 0f, tween(700, easing = FastOutSlowInEasing), label = "on")
    val busyAlpha by animateFloatAsState(if (busy) 1f else 0f, tween(300), label = "busy")
    val logoScale by animateFloatAsState(
        if (connected) 1f else 0.9f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessLow), label = "logo",
    )
    // One-shot ring that bursts outward the moment we connect.
    val burst = remember { Animatable(0f) }
    LaunchedEffect(connected) {
        if (connected) {
            burst.snapTo(0f); burst.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
        }
    }

    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 0.93f else 1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), label = "press",
    )
    val gold = TulporColors.Gold

    Box(
        Modifier
            .size(250.dp)
            .drawBehind {
                val r = size.minDimension / 2
                drawCircle(Color.White.copy(alpha = .06f + .14f * on), r * 0.98f, style = Stroke(1.dp.toPx()))
                drawCircle(Color.White.copy(alpha = .10f + .20f * on), r * 0.84f, style = Stroke(2.dp.toPx()))
                if (on > 0f) {
                    drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = .45f * on), Color.Transparent), radius = r), r)
                }
                if (busyAlpha > 0f) {
                    for (k in 0..1) {
                        val p = (pulse + k * 0.5f) % 1f
                        drawCircle(gold.copy(alpha = (1f - p) * .45f * busyAlpha), r * (0.62f + 0.36f * p), style = Stroke(3.dp.toPx()))
                    }
                    // Rotating comet arc on the inner ring.
                    val ringR = r * 0.84f
                    drawArc(
                        brush = Brush.sweepGradient(listOf(Color.Transparent, gold.copy(alpha = busyAlpha), Color.Transparent)),
                        startAngle = spin, sweepAngle = 110f, useCenter = false,
                        topLeft = Offset(center.x - ringR, center.y - ringR), size = Size(ringR * 2, ringR * 2),
                        style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                    )
                }
                if (burst.value in 0.001f..0.999f) {
                    val b = burst.value
                    drawCircle(gold.copy(alpha = (1f - b) * .7f), r * (0.7f + 0.35f * b), style = Stroke((6 - 4 * b).dp.toPx()))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        val shape = CircleShape
        Box(
            Modifier
                .size(176.dp)
                .graphicsLayer {
                    val s = press * (if (connected) breathe else 1f)
                    scaleX = s; scaleY = s
                }
                .glass(shape)
                .drawBehind {
                    // Gold fill fades in over the glass when connected.
                    if (on > 0f) drawCircle(
                        Brush.radialGradient(
                            listOf(Color(0xFFFFF3C4), TulporColors.Gold, TulporColors.Amber),
                            center = Offset(size.width * .35f, size.height * .3f), radius = size.width * .85f,
                        ),
                        alpha = on,
                    )
                }
                .border(1.dp, Color.White.copy(alpha = .2f + .3f * on), shape)
                .clickable(interactionSource = source, indication = null, role = Role.Button) {
                    scope.launch { burst.snapTo(0f) }
                    onClick()
                },
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(R.drawable.splash_icon), null,
                modifier = Modifier
                    .size(170.dp)
                    .graphicsLayer { alpha = 0.42f + 0.58f * on; scaleX = logoScale; scaleY = logoScale },
                colorFilter = ColorFilter.tint(Color.White),
            )
        }
    }
}

@Composable
private fun StatTile(modifier: Modifier, label: String, bps: Long, down: Boolean) {
    val ctx = LocalContext.current
    // Count smoothly towards the new rate instead of jumping every second.
    val shown by animateFloatAsState(bps.toFloat(), tween(800, easing = FastOutSlowInEasing), label = "rate")
    Column(modifier.glass(RoundedCornerShape(18.dp)).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (down) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward, null,
                tint = if (down) TulporColors.Green else TulporColors.Gold, modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(label, color = TulporColors.Text3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            Formatter.formatShortFileSize(ctx, shown.toLong()) + "/s",
            fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1,
        )
    }
}

@Composable
private fun ServerCard(server: Server?, ping: Long?, hasServers: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .bouncyClickable(RoundedCornerShape(20.dp), onClick = onClick)
            .glass(RoundedCornerShape(20.dp))
    ) {
        AnimatedContent(
            targetState = Triple(server?.id, hasServers, ping),
            transitionSpec = {
                if (initialState.first != targetState.first) {
                    (fadeIn(tween(300)) + slideInVertically(tween(320)) { it / 2 }) togetherWith
                        (fadeOut(tween(180)) + slideOutVertically(tween(220)) { -it / 2 })
                } else {
                    fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                }
            },
            label = "server",
        ) { (_, has, p) ->
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (server == null) AutoBadge() else Flag(server.country)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    val title = when {
                        !has -> stringResource(R.string.add_server)
                        server == null -> stringResource(R.string.fastest_server)
                        server.country.isNotBlank() -> Countries.displayName(server.country)
                        else -> server.name
                    }
                    val sub = when {
                        !has -> stringResource(R.string.empty_servers_title)
                        server == null -> stringResource(R.string.auto_selected)
                        else -> listOfNotNull(
                            server.city.ifBlank { null } ?: server.name.takeIf { server.country.isNotBlank() && it != title },
                            p?.takeIf { it >= 0 }?.let { stringResource(R.string.ms, it) },
                        ).joinToString(" · ")
                    }
                    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (sub.isNotBlank()) {
                        Text(sub, color = TulporColors.Text3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (server != null) {
                    PingBars(p)
                    Spacer(Modifier.width(6.dp))
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = TulporColors.Text3)
            }
        }
    }
}
