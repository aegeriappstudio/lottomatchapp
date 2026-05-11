package ch.lottomatchapp.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/* ----------------------- ColorSchemes (M3) ----------------------- */

val LightScheme: ColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,

    secondary = LightSecondary,
    onSecondary = LightOnSecondary,

    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,

    background = LightBackground,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,

    outline = LightOutline
)

val DarkScheme: ColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,

    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,

    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,

    outline = DarkOutline
)

/* -------------------------- Typography --------------------------- */

private val BaseTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.25).sp, fontFamily = FontFamily.Default
    ),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp, fontFamily = FontFamily.Default),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp, fontFamily = FontFamily.Default),

    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontFamily = FontFamily.Default),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp, fontFamily = FontFamily.Default),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontFamily = FontFamily.Default),

    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),

    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontFamily = FontFamily.Default),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontFamily = FontFamily.Default),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontFamily = FontFamily.Default),

    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)

/* High-Contrast Varianten – mit passenden Textfarben */

val LottomatchTypographyLight: Typography = BaseTypography.copy(
    bodyLarge = BaseTypography.bodyLarge.copy(color = LightTextPrimary),
    bodyMedium = BaseTypography.bodyMedium.copy(color = LightTextPrimary),
    bodySmall = BaseTypography.bodySmall.copy(color = LightTextSecondary),

    titleLarge = BaseTypography.titleLarge.copy(color = LightTextPrimary),
    titleMedium = BaseTypography.titleMedium.copy(color = LightTextPrimary),
    titleSmall = BaseTypography.titleSmall.copy(color = LightTextPrimary),

    labelLarge = BaseTypography.labelLarge.copy(color = LightTextPrimary),
    labelMedium = BaseTypography.labelMedium.copy(color = LightTextSecondary),
    labelSmall = BaseTypography.labelSmall.copy(color = LightTextSecondary),

    headlineSmall = BaseTypography.headlineSmall.copy(color = LightTextPrimary),
    headlineMedium = BaseTypography.headlineMedium.copy(color = LightTextPrimary),
    headlineLarge = BaseTypography.headlineLarge.copy(color = LightTextPrimary)
)

val LottomatchTypographyDark: Typography = BaseTypography.copy(
    displayLarge = BaseTypography.displayLarge.copy(color = Color.White),
    displayMedium = BaseTypography.displayMedium.copy(color = Color.White),
    displaySmall = BaseTypography.displaySmall.copy(color = Color.White),

    headlineLarge = BaseTypography.headlineLarge.copy(color = Color.White),
    headlineMedium = BaseTypography.headlineMedium.copy(color = Color.White),
    headlineSmall = BaseTypography.headlineSmall.copy(color = Color.White),

    titleLarge = BaseTypography.titleLarge.copy(color = Color.White),
    titleMedium = BaseTypography.titleMedium.copy(color = Color.White),
    titleSmall = BaseTypography.titleSmall.copy(color = Color.White),

    bodyLarge = BaseTypography.bodyLarge.copy(color = Color.White),
    bodyMedium = BaseTypography.bodyMedium.copy(color = Color.White),
    bodySmall = BaseTypography.bodySmall.copy(color = Color.White),

    labelLarge = BaseTypography.labelLarge.copy(color = Color.White),
    labelMedium = BaseTypography.labelMedium.copy(color = Color.White),
    labelSmall = BaseTypography.labelSmall.copy(color = Color.White)
)

/* Öffentliche Referenz, falls du sie direkt brauchst */
val LottomatchTypography: Typography
    @Composable get() = if (isSystemInDarkTheme()) LottomatchTypographyDark else LottomatchTypographyLight

/* --------------------------- Shapes ------------------------------ */

val LottomatchShapes: Shapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small      = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium     = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large      = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

/* --------------------------- Theme API --------------------------- */

@Composable
fun LottomatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val typo  = if (darkTheme) LottomatchTypographyDark else LottomatchTypographyLight

    MaterialTheme(
        colorScheme = scheme,
        typography = typo,
        shapes = LottomatchShapes,
        content = content
    )
}
