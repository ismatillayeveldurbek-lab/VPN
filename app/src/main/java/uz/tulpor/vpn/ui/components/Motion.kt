package uz.tulpor.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import uz.tulpor.vpn.ui.theme.TulporColors

/**
 * False when entrance animations should be skipped: the user disabled system animations
 * (accessibility) or a screenshot test needs the final frame.
 */
val LocalMotionEnabled = staticCompositionLocalOf { true }

/** Spring used for most UI motion: quick, with a little life. */
fun <T> tulporSpring() = spring<T>(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)

/**
 * Clickable that gently shrinks while pressed and springs back, with a soft white ripple.
 * Use instead of `clickable` on glass cards and buttons.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    pressedScale: Float = 0.965f,
    role: Role? = Role.Button,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) pressedScale else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "press",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = source,
            indication = ripple(color = Color.White),
            enabled = enabled,
            role = role,
            onLongClick = onLongClick,
            onClick = onClick,
        )
}

/**
 * Fade + rise entrance, staggered by [index]. Items composed after the screen has
 * settled (e.g. scrolled into view later) appear without delay so scrolling stays snappy.
 */
fun Modifier.appear(index: Int, enabled: Boolean = true): Modifier = composed {
    if (!enabled || !LocalMotionEnabled.current) return@composed this
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay((index.coerceAtMost(10) * 45L))
        anim.animateTo(1f, tween(420))
    }
    this.graphicsLayer {
        val p = anim.value
        alpha = p
        translationY = (1f - p) * 22.dp.toPx()
    }
}

/**
 * Glass segmented control with a gold indicator that slides between options.
 * Labels stay on one line and shrink to fit on narrow screens instead of wrapping.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .glass(CircleShape)
            .padding(4.dp)
    ) {
        val segment = maxWidth / options.size
        val offset by animateDpAsState(segment * selected, tulporSpring(), label = "seg")
        Box(
            Modifier
                .offset(x = offset)
                .width(segment)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(GoldBrush)
                .border(1.dp, Color.White.copy(alpha = .35f), CircleShape)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { i, label ->
                val color by animateColorAsState(
                    if (i == selected) TulporColors.OnGold else TulporColors.Text2, tween(250), label = "segc",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .bouncyClickable(role = Role.Tab, pressedScale = 0.94f) { onSelect(i) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        label,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        color = { color },
                        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 13.sp, stepSize = 0.5.sp),
                        style = TextStyle(
                            fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                        ),
                    )
                }
            }
        }
    }
}

/** Soft light sweep across a surface at [progress] (0..1); call from a draw lambda. */
fun shimmerBrush(progress: Float, widthPx: Float): Brush = Brush.linearGradient(
    colors = listOf(Color.Transparent, Color.White.copy(alpha = .35f), Color.Transparent),
    start = androidx.compose.ui.geometry.Offset(widthPx * (progress * 2f - 1f), 0f),
    end = androidx.compose.ui.geometry.Offset(widthPx * (progress * 2f - 0.6f), 0f),
)
