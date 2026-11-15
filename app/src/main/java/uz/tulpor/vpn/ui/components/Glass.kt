package uz.tulpor.vpn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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

val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

enum class Glow { Default, Connected, Premium, Cool }

/** Dark gradient with soft colored light blobs; glass surfaces blur this layer. */
@Composable
fun GlassBackground(glow: Glow, hazeState: HazeState, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .hazeSource(hazeState)
                .drawBehind {
                    drawRect(Brush.verticalGradient(listOf(TulporColors.BgTop, TulporColors.Bg)))
                    fun blob(c: Color, x: Float, y: Float, r: Float) = drawCircle(
                        Brush.radialGradient(listOf(c, Color.Transparent), Offset(size.width * x, size.height * y), size.width * r),
                        radius = size.width * r, center = Offset(size.width * x, size.height * y),
                    )
                    when (glow) {
                        Glow.Default -> {
                            blob(TulporColors.VioletGlow.copy(alpha = .75f), .1f, .08f, .75f)
                            blob(TulporColors.Blue.copy(alpha = .6f), .98f, .32f, .6f)
                            blob(TulporColors.Amber.copy(alpha = .30f), .85f, .92f, .7f)
                            blob(TulporColors.Pink.copy(alpha = .25f), .02f, .7f, .55f)
                        }
                        Glow.Connected -> {
                            blob(TulporColors.Amber.copy(alpha = .40f), .5f, .40f, .8f)
                            blob(TulporColors.VioletGlow.copy(alpha = .75f), .08f, .06f, .75f)
                            blob(TulporColors.Green.copy(alpha = .18f), .98f, .85f, .6f)
                            blob(TulporColors.Blue.copy(alpha = .45f), .0f, .75f, .6f)
                        }
                        Glow.Premium -> {
                            blob(TulporColors.Amber.copy(alpha = .40f), .5f, .12f, .9f)
                            blob(TulporColors.VioletGlow.copy(alpha = .8f), .0f, .35f, .7f)
                            blob(TulporColors.Pink.copy(alpha = .32f), 1f, .7f, .65f)
                        }
                        Glow.Cool -> {
                            blob(TulporColors.VioletGlow.copy(alpha = .7f), .95f, .05f, .7f)
                            blob(TulporColors.Blue.copy(alpha = .55f), .0f, .55f, .65f)
                            blob(TulporColors.Amber.copy(alpha = .25f), 1f, .95f, .65f)
                        }
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
