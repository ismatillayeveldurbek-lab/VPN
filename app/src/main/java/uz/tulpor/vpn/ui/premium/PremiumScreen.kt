package uz.tulpor.vpn.ui.premium

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.tulpor.vpn.R
import uz.tulpor.vpn.ui.components.GoldButton
import uz.tulpor.vpn.ui.components.appear
import uz.tulpor.vpn.ui.components.bouncyClickable
import uz.tulpor.vpn.ui.components.glass
import uz.tulpor.vpn.ui.components.shimmerBrush
import uz.tulpor.vpn.ui.theme.TulporColors

@Composable
fun PremiumScreen(onClose: () -> Unit) {
    var plan by rememberSaveable { mutableIntStateOf(1) }
    val inf = rememberInfiniteTransition(label = "premium")
    val float by inf.animateFloat(-6f, 6f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "f")
    val tilt by inf.animateFloat(-2.5f, 2.5f, infiniteRepeatable(tween(3400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "t")
    val glow by inf.animateFloat(0.35f, 0.7f, infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "g")
    val shimmer by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, delayMillis = 900, easing = LinearEasing)), label = "s")
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.offset(x = (-10).dp)) {
            Icon(Icons.Rounded.Close, null, tint = TulporColors.Text2)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth().appear(0), contentAlignment = Alignment.Center) {
                Box(
                    Modifier.size(150.dp).graphicsLayer { alpha = glow }
                        .background(Brush.radialGradient(listOf(TulporColors.Amber.copy(alpha = .55f), Color.Transparent)), CircleShape)
                )
                Image(
                    painterResource(R.drawable.splash_icon), null,
                    Modifier.size(170.dp).graphicsLayer { translationY = float.dp.toPx(); rotationZ = tilt },
                )
            }
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.premium_title) + " ")
                    withStyle(SpanStyle(color = TulporColors.Gold)) { append(stringResource(R.string.premium)) }
                },
                style = MaterialTheme.typography.headlineMedium, maxLines = 1,
                modifier = Modifier.align(Alignment.CenterHorizontally).appear(1),
            )
            Text(stringResource(R.string.premium_subtitle), color = TulporColors.Text2, fontSize = 14.sp, maxLines = 2,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally).appear(2).padding(top = 4.dp))
            Spacer(Modifier.height(20.dp))
            listOf(R.string.premium_f1, R.string.premium_f2, R.string.premium_f3, R.string.premium_f4).forEachIndexed { i, it ->
                Row(Modifier.appear(3 + i).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, tint = TulporColors.Gold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(it), fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(Modifier.appear(7)) {
                PlanRow(stringResource(R.string.plan_month), stringResource(R.string.plan_month_sub), "25 000", plan == 0, null) { plan = 0 }
            }
            Spacer(Modifier.height(12.dp))
            Box(Modifier.appear(8)) {
                PlanRow(stringResource(R.string.plan_year), stringResource(R.string.plan_year_sub), "15 000", plan == 1, "−40%") { plan = 1 }
            }
        }
        Box(
            Modifier.clip(RoundedCornerShape(16.dp)).drawWithContent {
                drawContent()
                drawRect(shimmerBrush(shimmer, size.width))
            }
        ) { GoldButton(stringResource(R.string.premium_cta), enabled = false) {} }
        Text(stringResource(R.string.premium_note), color = TulporColors.Text3, fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp))
    }
}

@Composable
private fun PlanRow(title: String, sub: String, price: String, selected: Boolean, badge: String?, onClick: () -> Unit) {
    Box {
        Row(
            Modifier.fillMaxWidth().bouncyClickable(RoundedCornerShape(16.dp), onClick = onClick).glass(RoundedCornerShape(16.dp), selected = selected).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = TulporColors.Gold))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                Text(sub, color = TulporColors.Text3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(price, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(stringResource(R.string.per_month), color = TulporColors.Text3, fontSize = 12.sp)
            }
        }
        if (badge != null) {
            Text(
                badge, color = Color(0xFF03250F), fontWeight = FontWeight.Bold, fontSize = 11.sp,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-14).dp, y = (-9).dp)
                    .shadow(4.dp, CircleShape).clip(CircleShape).background(TulporColors.Green)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            )
        }
    }
}
