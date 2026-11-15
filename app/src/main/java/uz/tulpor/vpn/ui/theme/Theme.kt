package uz.tulpor.vpn.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object TulporColors {
    val Bg = Color(0xFF0E0B2A)
    val BgTop = Color(0xFF1A1250)
    val Indigo = Color(0xFF2B1F8C)
    val Violet = Color(0xFF741BEB)
    val VioletGlow = Color(0xFF7C3AED)
    val Blue = Color(0xFF4F46E5)
    val Pink = Color(0xFFDB2777)
    val GoldLight = Color(0xFFFDE68A)
    val Gold = Color(0xFFFBBF24)
    val Amber = Color(0xFFF59E0B)
    val OnGold = Color(0xFF2A1A00)
    val Green = Color(0xFF22C55E)
    val Red = Color(0xFFEF4444)
    val Text1 = Color(0xFFF5F3FF)
    val Text2 = Color(0xFFB7B1E3)
    val Text3 = Color(0xFF8A84C0)
}

private val scheme = darkColorScheme(
    primary = TulporColors.Gold,
    onPrimary = TulporColors.OnGold,
    secondary = TulporColors.Violet,
    background = TulporColors.Bg,
    onBackground = TulporColors.Text1,
    surface = Color(0xFF1D1750),
    onSurface = TulporColors.Text1,
    surfaceVariant = Color(0xFF251E63),
    onSurfaceVariant = TulporColors.Text2,
    outline = Color(0x33FFFFFF),
    error = TulporColors.Red,
)

private val typography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp),
    bodyMedium = TextStyle(fontSize = 14.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
)

@Composable
fun TulporTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = typography) {
        // Screens draw on our own gradient (no Surface), so set the default text color here.
        CompositionLocalProvider(LocalContentColor provides TulporColors.Text1, content = content)
    }
}
