package uz.tulpor.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import uz.tulpor.vpn.ui.theme.TulporColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

enum class Glow { Default, Connected, Premium, Cool }

private data class Blob(val color: Color, val x: Float, val y: Float, val r: Float)

private fun blobsFor(glow: Glow): List<Blob> = when (glow) {
    Glow.Default -> listOf(
        Blob(TulporColors.VioletGlow.copy(alpha = .75f), .10f, .08f, .75f),
        Blob(TulporColors.Blue.copy(alpha = .60f), .98f, .32f, .60f),
        Blob(TulporColors.Amber.copy(alpha = .30f), .85f, .92f, .70f),
        Blob(TulporColors.Pink.copy(alpha = .25f), .02f, .70f, .55f),
    )
    Glow.Connected -> listOf(
        Blob(TulporColors.VioletGlow.copy(alpha = .75f), .08f, .06f, .75f),
        Blob(TulporColors.Amber.copy(alpha = .42f), .50f, .40f, .80f),
        Blob(TulporColors.Green.copy(alpha = .18f), .98f, .85f, .60f),
        Blob(TulporColors.Blue.copy(alpha = .45f), .00f, .75f, .60f),
    )
    Glow.Premium -> listOf(
        Blob(TulporColors.VioletGlow.copy(alpha = .80f), .00f, .35f, .70f),
        Blob(TulporColors.Amber.copy(alpha = .40f), .50f, .12f, .90f),
        Blob(TulporColors.Pink.copy(alpha = .32f), 1.0f, .70f, .65f),
        Blob(TulporColors.Blue.copy(alpha = .30f), .20f, .95f, .55f),
    )
    Glow.Cool -> listOf(
        Blob(TulporColors.VioletGlow.copy(alpha = .70f), .95f, .05f, .70f),
        Blob(TulporColors.Blue.copy(alpha = .55f), .00f, .55f, .65f),
        Blob(TulporColors.Amber.copy(alpha = .25f), 1.0f, .95f, .65f),
        Blob(TulporColors.Pink.copy(alpha = .16f), .30f, .25f, .45f),
    )
}

/**
 * Dark gradient with soft colored light blobs that slowly drift; glass surfaces blur
 * this layer. Switching [glow] morphs the blobs' colors and positions smoothly.
 */
@Composable
fun GlassBackground(glow: Glow, hazeState: HazeState, content: @Composable BoxScope.() -> Unit) {
    val target = blobsFor(glow)
    val drift = rememberInfiniteTransition(label = "drift")
    val t by drift.animateFloat(
        0f, (2 * PI).toFloat(), infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "t",
    )
    val animated = target.mapIndexed { i, b ->
        val spec = tween<Float>(1100, easing = FastOutSlowInEasing)
        Triple(
            animateColorAsState(b.color, tween(1100), label = "bc$i"),
            animateFloatAsState(b.x, spec, label = "bx$i") to animateFloatAsState(b.y, spec, label = "by$i"),
            animateFloatAsState(b.r, spec, label = "br$i"),
        )
    }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(TulporColors.BgTop, TulporColors.Bg)))
                    animated.forEachIndexed { i, (c, xy, r) ->
                        // Each blob orbits slowly around its anchor, out of phase with the others.
                        val phase = t + i * 1.7f
                        val cx = size.width * (xy.first.value + 0.06f * sin(phase))
                        val cy = size.height * (xy.second.value + 0.035f * cos(phase * 0.8f))
                        val radius = size.width * r.value * (1f + 0.06f * sin(phase * 1.3f))
                        drawCircle(
                            Brush.radialGradient(listOf(c.value, Color.Transparent), Offset(cx, cy), radius),
                            radius = radius, center = Offset(cx, cy),
                        )
                    }
                }
        )
        content()
    }
}

private val glassStyle = HazeStyle(
    backgroundColor = TulporColors.Bg,
    tint = HazeTint(Color.White.copy(alpha = 0.07f)),
    blurRadius = 26.dp,
    noiseFactor = 0.04f,
    fallbackTint = HazeTint(Color(0xB3251E63)),
)

private val navStyle = HazeStyle(
    backgroundColor = TulporColors.Bg,
    tint = HazeTint(Color(0x99302470)),
    blurRadius = 30.dp,
    noiseFactor = 0.05f,
    fallbackTint = HazeTint(Color(0xE6261C66)),
)

val GlassBorder = Brush.linearGradient(listOf(Color.White.copy(alpha = .30f), Color.White.copy(alpha = .06f)))
val GoldBorder = Brush.linearGradient(listOf(TulporColors.Gold.copy(alpha = .85f), TulporColors.Gold.copy(alpha = .25f)))
val GoldBrush = Brush.linearGradient(listOf(TulporColors.GoldLight, TulporColors.Amber))

/** Frosted-glass surface: blurs the background, adds a light sheen and a hairline border. */
@Composable
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(20.dp),
    selected: Boolean = false,
    nav: Boolean = false,
): Modifier {
    val state = LocalHazeState.current
    val base = this.clip(shape)
    val blurred = if (state != null) base.hazeEffect(state, if (nav) navStyle else glassStyle) else
        base.background(Color(0xB3251E63))
    val sheen = if (selected) {
        Brush.linearGradient(listOf(TulporColors.Gold.copy(alpha = .22f), TulporColors.Gold.copy(alpha = .05f)))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(alpha = .10f), Color.White.copy(alpha = .02f)))
    }
    return blurred
        .background(sheen)
        .border(1.dp, if (selected) GoldBorder else GlassBorder, shape)
}
