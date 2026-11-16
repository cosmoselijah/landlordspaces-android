package com.landlordspaces.app

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat

/** LandlordSpaces brand: deep forest green with a mint accent and a warm gold highlight. */
object Brand {
    val Deep = Color(0xFF05342B)
    val Forest = Color(0xFF0A4D3C)
    val Primary = Color(0xFF0E7C56)
    val PrimaryDark = Color(0xFF0A5E42)
    val Mint = Color(0xFF22D3A0)
    val MintSoft = Color(0xFFE3F6EE)
    val Gold = Color(0xFFF5B82E)
    val Bg = Color(0xFFF4F7F6)
    val Card = Color.White
    val Ink = Color(0xFF10231D)
    val Muted = Color(0xFF6B7C75)
    val Line = Color(0xFFE3EAE7)
    val Danger = Color(0xFFD64545)
    val Violet = Color(0xFF7C5CE0)

    val Hero = Brush.verticalGradient(listOf(Color(0xFF063B30), Color(0xFF0A5B45), Color(0xFF05342B)))
    val Header = Brush.horizontalGradient(listOf(Color(0xFF05342B), Color(0xFF0A4D3C)))
    val Cta = Brush.horizontalGradient(listOf(Color(0xFF0E7C56), Color(0xFF16A06E)))
    val Scrim = Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC000000)))
}

private val LsTypography = Typography(
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
)

private val LsShapes = Shapes(
    small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(14.dp), large = RoundedCornerShape(20.dp),
)

@Composable
fun LsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Brand.Primary, onPrimary = Color.White, secondary = Brand.Mint, onSecondary = Brand.Deep,
            background = Brand.Bg, onBackground = Brand.Ink, surface = Brand.Card, onSurface = Brand.Ink,
            surfaceVariant = Brand.MintSoft, outline = Brand.Line, error = Brand.Danger,
        ),
        typography = LsTypography, shapes = LsShapes, content = content,
    )
}

/** Light icons on green screens, dark icons on white ones. */
@Composable
fun StatusBar(darkBackground: Boolean) {
    val view = LocalView.current
    SideEffect {
        val w = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(w, view).isAppearanceLightStatusBars = !darkBackground
    }
}
