package it.elledue.control.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ElledueNight = Color(0xFF111B32)
val ElledueGold = Color(0xFFDBBF8B)
private val LightColors = lightColorScheme(
    primary = ElledueNight, onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EDF5), onPrimaryContainer = ElledueNight,
    secondary = Color(0xFF79613A), secondaryContainer = Color(0xFFF4E7CF),
    background = Color(0xFFF6F7FA), surface = Color(0xFFF6F7FA),
    surfaceContainer = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFCFCFE),
    outlineVariant = Color(0xFFDFE2E9),
)
private val DarkColors = darkColorScheme(
    primary = ElledueGold, onPrimary = ElledueNight,
    primaryContainer = Color(0xFF263550), onPrimaryContainer = Color(0xFFE6ECF5),
    secondary = ElledueGold, secondaryContainer = Color(0xFF3C3542),
    background = ElledueNight, surface = ElledueNight,
    surfaceContainer = Color(0xFF1C2840), surfaceContainerLow = Color(0xFF182238),
    outlineVariant = Color(0xFF374258),
)
private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.6).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
)

@Composable
fun ElledueTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(22.dp)),
        content = content,
    )
}
