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
private val DarkColors = darkColorScheme(
    primary = ElledueGold, onPrimary = ElledueNight,
    primaryContainer = Color(0xFF162742), onPrimaryContainer = Color(0xFFF6F2EA),
    secondary = ElledueGold, secondaryContainer = Color(0xFF162742),
    background = ConceptBackground, surface = ConceptBackground,
    onBackground = Color(0xFFF6F2EA), onSurface = Color(0xFFF6F2EA), onSurfaceVariant = ConceptMuted,
    surfaceContainer = ConceptCard, surfaceContainerLow = Color(0xFF101D30), surfaceContainerHigh = Color(0xFF162742),
    outline = ConceptBorder, outlineVariant = ConceptBorder,
    error = Color(0xFFD96B6B),
)
private val Serif = FontFamily(androidx.compose.ui.text.font.Font(it.elledue.control.R.font.dm_serif_display))
private val Sans = FontFamily(androidx.compose.ui.text.font.Font(it.elledue.control.R.font.inter))
private val AppTypography = Typography(
    headlineLarge = TextStyle(fontFamily = Serif, fontSize = 30.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = Serif, fontSize = 27.sp, lineHeight = 31.sp),
    titleLarge = TextStyle(fontFamily = Serif, fontSize = 23.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontSize = 14.sp, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontSize = 11.sp, lineHeight = 15.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontSize = 10.sp, lineHeight = 13.sp),
)

@Composable
fun ElledueTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        shapes = Shapes(small = RoundedCornerShape(9.dp), medium = RoundedCornerShape(13.dp), large = RoundedCornerShape(16.dp)),
        content = content,
    )
}
