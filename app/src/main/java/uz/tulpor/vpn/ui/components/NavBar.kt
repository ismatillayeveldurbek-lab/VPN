package uz.tulpor.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.tulpor.vpn.ui.theme.TulporColors

data class NavItem(val route: String, val label: String, val icon: ImageVector)

/**
 * Floating frosted-glass tab bar. A single gold "pill" springs between tabs and the
 * selected icon pops slightly, so switching tabs feels physical.
 */
@Composable
fun GlassNavBar(items: List<NavItem>, current: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(26.dp)
    val selectedIndex = items.indexOfFirst { it.route == current }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .padding(bottom = 12.dp)
            .shadow(24.dp, shape, ambientColor = Color(0xFF030114), spotColor = Color(0xFF030114))
            .glass(shape, nav = true)
            .height(68.dp)
            .padding(6.dp),
    ) {
        val tab = maxWidth / items.size
        val pillX by animateDpAsState(
            tab * selectedIndex,
            spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow), label = "pill",
        )
        val pill = RoundedCornerShape(20.dp)
        Box(
            Modifier
                .offset(x = pillX)
                .width(tab)
                .fillMaxHeight()
                .padding(horizontal = 3.dp)
                .clip(pill)
                .background(Brush.linearGradient(listOf(Color(0x52FBBF24), Color(0x1AF59E0B))))
                .border(1.dp, Color(0x73FBBF24), pill)
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            items.forEachIndexed { i, item ->
                val selected = i == selectedIndex
                val color by animateColorAsState(
                    if (selected) TulporColors.GoldLight else TulporColors.Text2, tween(220), label = "navc",
                )
                val iconScale by animateFloatAsState(
                    if (selected) 1.14f else 1f,
                    spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), label = "navs",
                )
                val glow by animateFloatAsState(if (selected) 1f else 0f, tween(300), label = "navg")
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(pill)
                        .selectable(
                            selected = selected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(item.route) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(contentAlignment = Alignment.Center) {
                            // soft halo behind the active icon
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .graphicsLayer { alpha = glow * 0.55f; scaleX = 0.6f + glow * 0.6f; scaleY = scaleX }
                                    .background(
                                        Brush.radialGradient(listOf(TulporColors.Gold.copy(alpha = .6f), Color.Transparent)),
                                        RoundedCornerShape(50),
                                    )
                            )
                            Icon(
                                item.icon, null, tint = color,
                                modifier = Modifier.size(22.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale },
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            item.label, color = color, fontSize = 11.sp, maxLines = 1, softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
