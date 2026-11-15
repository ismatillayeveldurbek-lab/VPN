package uz.tulpor.vpn.ui.premium

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.tulpor.vpn.R
import uz.tulpor.vpn.ui.components.GoldButton
import uz.tulpor.vpn.ui.components.glass
import uz.tulpor.vpn.ui.theme.TulporColors

@Composable
fun PremiumScreen(onClose: () -> Unit) {
    var plan by rememberSaveable { mutableIntStateOf(1) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 22.dp),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.offset(x = (-10).dp)) {
            Icon(Icons.Rounded.Close, null, tint = TulporColors.Text2)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.splash_icon), null, Modifier.size(170.dp))
            }
            Text(
                buildAnnotatedString {
                    append(stringResource(R.string.premium_title) + " ")
                    withStyle(SpanStyle(color = TulporColors.Gold)) { append(stringResource(R.string.premium)) }
                },
                style = MaterialTheme.typography.headlineMedium, modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(stringResource(R.string.premium_subtitle), color = TulporColors.Text2, fontSize = 14.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp))
            Spacer(Modifier.height(20.dp))
            listOf(R.string.premium_f1, R.string.premium_f2, R.string.premium_f3, R.string.premium_f4).forEach {
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Check, null, tint = TulporColors.Gold, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(it), fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            PlanRow(stringResource(R.string.plan_month), stringResource(R.string.plan_month_sub), "25 000", plan == 0, null) { plan = 0 }
            Spacer(Modifier.height(12.dp))
            PlanRow(stringResource(R.string.plan_year), stringResource(R.string.plan_year_sub), "15 000", plan == 1, "−40%") { plan = 1 }
        }
        GoldButton(stringResource(R.string.premium_cta), enabled = false) {}
        Text(stringResource(R.string.premium_note), color = TulporColors.Text3, fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp))
    }
}

@Composable
private fun PlanRow(title: String, sub: String, price: String, selected: Boolean, badge: String?, onClick: () -> Unit) {
    Box {
        Row(
            Modifier.fillMaxWidth().glass(RoundedCornerShape(16.dp), selected = selected).clickable(onClick = onClick).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = TulporColors.Gold))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(sub, color = TulporColors.Text3, fontSize = 12.sp)
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
