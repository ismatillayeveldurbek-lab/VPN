package uz.tulpor.vpn.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.tulpor.vpn.ui.theme.TulporColors

/** Round country flag drawn with simple shapes (no emoji font needed). */
@Composable
fun Flag(code: String, size: Dp = 36.dp, modifier: Modifier = Modifier) {
    val c = code.uppercase()
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, Color.White.copy(alpha = .18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (c in drawable) {
            Canvas(Modifier.size(size)) { drawFlag(c) }
        } else {
            Box(
                Modifier
                    .size(size)
                    .background(Brush.linearGradient(listOf(TulporColors.Violet, TulporColors.Blue))),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    c.ifBlank { "?" }, color = Color.White, fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.36f).sp,
                )
            }
        }
    }
}

@Composable
fun AutoBadge(size: Dp = 36.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(GoldBrush),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Bolt, null, tint = TulporColors.OnGold, modifier = Modifier.size(size * 0.55f))
    }
}

private val drawable = setOf(
    "DE", "NL", "FR", "PL", "EE", "AT", "IT", "BE", "IE", "LT", "LV", "HU", "BG", "RO", "LU", "RU", "UA",
    "FI", "SE", "NO", "DK", "CH", "JP", "TR", "US", "GB", "KZ", "UZ", "ES", "AE", "SG", "CZ", "GE", "AM", "AZ", "CA",
)

private fun DrawScope.h(vararg colors: Long) {
    val band = size.height / colors.size
    colors.forEachIndexed { i, col -> drawRect(Color(col), Offset(0f, band * i), Size(size.width, band + 1)) }
}

private fun DrawScope.v(vararg colors: Long) {
    val band = size.width / colors.size
    colors.forEachIndexed { i, col -> drawRect(Color(col), Offset(band * i, 0f), Size(band + 1, size.height)) }
}

private fun DrawScope.nordic(bg: Long, cross: Long, inner: Long? = null) {
    drawRect(Color(bg))
    val w = size.width; val hgt = size.height
    val t = hgt * 0.2f
    val x = w * 0.32f
    drawRect(Color(cross), Offset(x, 0f), Size(t, hgt))
    drawRect(Color(cross), Offset(0f, hgt / 2 - t / 2), Size(w, t))
    if (inner != null) {
        val ti = t * 0.5f
        drawRect(Color(inner), Offset(x + (t - ti) / 2, 0f), Size(ti, hgt))
        drawRect(Color(inner), Offset(0f, hgt / 2 - ti / 2), Size(w, ti))
    }
}

private fun DrawScope.drawFlag(c: String) {
    val w = size.width; val hgt = size.height
    when (c) {
        "DE" -> h(0xFF000000, 0xFFDD0000, 0xFFFFCE00)
        "NL" -> h(0xFFAE1C28, 0xFFFFFFFF, 0xFF21468B)
        "FR" -> v(0xFF0055A4, 0xFFFFFFFF, 0xFFEF4135)
        "PL" -> h(0xFFFFFFFF, 0xFFDC143C)
        "EE" -> h(0xFF0072CE, 0xFF000000, 0xFFFFFFFF)
        "AT" -> h(0xFFED2939, 0xFFFFFFFF, 0xFFED2939)
        "IT" -> v(0xFF009246, 0xFFFFFFFF, 0xFFCE2B37)
        "BE" -> v(0xFF000000, 0xFFFDDA24, 0xFFEF3340)
        "IE" -> v(0xFF169B62, 0xFFFFFFFF, 0xFFFF883E)
        "LT" -> h(0xFFFDB913, 0xFF006A44, 0xFFC1272D)
        "LV" -> h(0xFF9E3039, 0xFFFFFFFF, 0xFF9E3039)
        "HU" -> h(0xFFCD2A3E, 0xFFFFFFFF, 0xFF436F4D)
        "BG" -> h(0xFFFFFFFF, 0xFF00966E, 0xFFD62612)
        "RO" -> v(0xFF002B7F, 0xFFFCD116, 0xFFCE1126)
        "LU" -> h(0xFFEF3340, 0xFFFFFFFF, 0xFF00A3E0)
        "RU" -> h(0xFFFFFFFF, 0xFF0039A6, 0xFFD52B1E)
        "UA" -> h(0xFF0057B7, 0xFFFFD700)
        "AM" -> h(0xFFD90012, 0xFF0033A0, 0xFFF2A800)
        "AZ" -> h(0xFF0092BC, 0xFFE4002B, 0xFF00AF66)
        "FI" -> nordic(0xFFFFFFFF, 0xFF003580)
        "SE" -> nordic(0xFF006AA7, 0xFFFECC00)
        "NO" -> nordic(0xFFBA0C2F, 0xFFFFFFFF, 0xFF00205B)
        "DK" -> nordic(0xFFC8102E, 0xFFFFFFFF)
        "CH" -> {
            drawRect(Color(0xFFDA291C))
            val t = w * 0.18f; val l = w * 0.58f
            drawRect(Color.White, Offset(w / 2 - t / 2, hgt / 2 - l / 2), Size(t, l))
            drawRect(Color.White, Offset(w / 2 - l / 2, hgt / 2 - t / 2), Size(l, t))
        }
        "GE" -> {
            drawRect(Color.White)
            val t = w * 0.2f
            drawRect(Color(0xFFFF0000), Offset(w / 2 - t / 2, 0f), Size(t, hgt))
            drawRect(Color(0xFFFF0000), Offset(0f, hgt / 2 - t / 2), Size(w, t))
        }
        "JP" -> { drawRect(Color.White); drawCircle(Color(0xFFBC002D), w * 0.28f) }
        "TR" -> {
            drawRect(Color(0xFFE30A17))
            drawCircle(Color.White, w * 0.25f, Offset(w * 0.42f, hgt / 2))
            drawCircle(Color(0xFFE30A17), w * 0.20f, Offset(w * 0.48f, hgt / 2))
            drawCircle(Color.White, w * 0.07f, Offset(w * 0.70f, hgt / 2))
        }
        "US" -> {
            val stripes = 7
            for (i in 0 until stripes) {
                drawRect(Color(if (i % 2 == 0) 0xFFB22234 else 0xFFFFFFFF), Offset(0f, hgt / stripes * i), Size(w, hgt / stripes + 1))
            }
            drawRect(Color(0xFF3C3B6E), Offset.Zero, Size(w * 0.5f, hgt * 0.55f))
        }
        "GB" -> {
            drawRect(Color(0xFF012169))
            drawLine(Color.White, Offset(0f, 0f), Offset(w, hgt), w * 0.16f)
            drawLine(Color.White, Offset(w, 0f), Offset(0f, hgt), w * 0.16f)
            drawLine(Color(0xFFC8102E), Offset(0f, 0f), Offset(w, hgt), w * 0.06f)
            drawLine(Color(0xFFC8102E), Offset(w, 0f), Offset(0f, hgt), w * 0.06f)
            drawRect(Color.White, Offset(w * 0.38f, 0f), Size(w * 0.24f, hgt))
            drawRect(Color.White, Offset(0f, hgt * 0.38f), Size(w, hgt * 0.24f))
            drawRect(Color(0xFFC8102E), Offset(w * 0.43f, 0f), Size(w * 0.14f, hgt))
            drawRect(Color(0xFFC8102E), Offset(0f, hgt * 0.43f), Size(w, hgt * 0.14f))
        }
        "KZ" -> { drawRect(Color(0xFF00AFCA)); drawCircle(Color(0xFFFEC50C), w * 0.2f) }
        "UZ" -> {
            h(0xFF0099B5, 0xFFFFFFFF, 0xFF1EB53A)
            drawRect(Color(0xFFCE1126), Offset(0f, hgt / 3 - hgt * 0.02f), Size(w, hgt * 0.04f))
            drawRect(Color(0xFFCE1126), Offset(0f, hgt * 2 / 3 - hgt * 0.02f), Size(w, hgt * 0.04f))
        }
        "ES" -> {
            drawRect(Color(0xFFAA151B))
            drawRect(Color(0xFFF1BF00), Offset(0f, hgt * 0.25f), Size(w, hgt * 0.5f))
        }
        "AE" -> {
            h(0xFF00732F, 0xFFFFFFFF, 0xFF000000)
            drawRect(Color(0xFFFF0000), Offset.Zero, Size(w * 0.3f, hgt))
        }
        "SG" -> h(0xFFEF3340, 0xFFFFFFFF)
        "CZ" -> {
            h(0xFFFFFFFF, 0xFFD7141A)
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(0f, 0f); lineTo(w * 0.5f, hgt / 2); lineTo(0f, hgt); close()
            }
            drawPath(p, Color(0xFF11457E))
        }
        "CA" -> {
            v(0xFFD80621, 0xFFFFFFFF, 0xFFD80621)
            drawCircle(Color(0xFFD80621), w * 0.13f)
        }
    }
}

/** Three signal bars colored by latency. */
@Composable
fun PingBars(ms: Long?, modifier: Modifier = Modifier) {
    val (level, color) = when {
        ms == null -> 0 to Color.White.copy(alpha = .25f)
        ms < 0 -> 0 to TulporColors.Red
        ms < 150 -> 3 to TulporColors.Green
        ms < 350 -> 2 to TulporColors.Green
        else -> 1 to TulporColors.Amber
    }
    Row(modifier.height(14.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        listOf(5.dp, 9.dp, 14.dp).forEachIndexed { i, hgt ->
            Box(
                Modifier
                    .width(4.dp)
                    .height(hgt)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < level) color else Color.White.copy(alpha = .2f))
            )
        }
    }
}

@Composable
fun GoldButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) GoldBrush else Brush.linearGradient(listOf(Color(0x66FDE68A), Color(0x66F59E0B))))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = TulporColors.OnGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}
