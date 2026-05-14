package ch.lottomatchapp.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Theme-Variante. Alle ausser Classic sind Premium-only und werden vom ViewModel gegated. */
enum class ThemeVariant { Classic, Tombola, Casino, Sunset, Midnight, Forest }

/* ----------------------- Classic ColorSchemes (alle User) ----------------------- */

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

/* ----------------------- Tombola ColorSchemes (Premium) ----------------------- */

val TombolaLightScheme: ColorScheme = lightColorScheme(
    primary = TombolaLightPrimary,
    onPrimary = TombolaLightOnPrimary,
    primaryContainer = TombolaLightPrimaryContainer,
    onPrimaryContainer = TombolaLightOnPrimaryContainer,

    secondary = TombolaLightSecondary,
    onSecondary = TombolaLightOnSecondary,

    tertiary = TombolaLightTertiary,
    onTertiary = TombolaLightOnTertiary,

    background = TombolaLightBackground,
    onBackground = TombolaLightTextPrimary,

    surface = TombolaLightSurface,
    onSurface = TombolaLightTextPrimary,
    surfaceVariant = TombolaLightSurfaceVariant,
    onSurfaceVariant = TombolaLightTextSecondary,

    outline = TombolaLightOutline
)

val TombolaDarkScheme: ColorScheme = darkColorScheme(
    primary = TombolaDarkPrimary,
    onPrimary = TombolaDarkOnPrimary,
    primaryContainer = TombolaDarkPrimaryContainer,
    onPrimaryContainer = TombolaDarkOnPrimaryContainer,

    secondary = TombolaDarkSecondary,
    onSecondary = TombolaDarkOnSecondary,

    tertiary = TombolaDarkTertiary,
    onTertiary = TombolaDarkOnTertiary,

    background = TombolaDarkBackground,
    onBackground = TombolaDarkTextPrimary,

    surface = TombolaDarkSurface,
    onSurface = TombolaDarkTextPrimary,
    surfaceVariant = TombolaDarkSurfaceVariant,
    onSurfaceVariant = TombolaDarkTextSecondary,

    outline = TombolaDarkOutline
)

/* ----------------------- Casino ColorSchemes (Premium) ----------------------- */

val CasinoLightScheme: ColorScheme = lightColorScheme(
    primary = CasinoLightPrimary, onPrimary = CasinoLightOnPrimary,
    primaryContainer = CasinoLightPrimaryContainer, onPrimaryContainer = CasinoLightOnPrimaryContainer,
    secondary = CasinoLightSecondary, onSecondary = CasinoLightOnSecondary,
    tertiary = CasinoLightTertiary, onTertiary = CasinoLightOnTertiary,
    background = CasinoLightBackground, onBackground = CasinoLightTextPrimary,
    surface = CasinoLightSurface, onSurface = CasinoLightTextPrimary,
    surfaceVariant = CasinoLightSurfaceVariant, onSurfaceVariant = CasinoLightTextSecondary,
    outline = CasinoLightOutline
)

val CasinoDarkScheme: ColorScheme = darkColorScheme(
    primary = CasinoDarkPrimary, onPrimary = CasinoDarkOnPrimary,
    primaryContainer = CasinoDarkPrimaryContainer, onPrimaryContainer = CasinoDarkOnPrimaryContainer,
    secondary = CasinoDarkSecondary, onSecondary = CasinoDarkOnSecondary,
    tertiary = CasinoDarkTertiary, onTertiary = CasinoDarkOnTertiary,
    background = CasinoDarkBackground, onBackground = CasinoDarkTextPrimary,
    surface = CasinoDarkSurface, onSurface = CasinoDarkTextPrimary,
    surfaceVariant = CasinoDarkSurfaceVariant, onSurfaceVariant = CasinoDarkTextSecondary,
    outline = CasinoDarkOutline
)

/* ----------------------- Sunset ColorSchemes (Premium) ----------------------- */

val SunsetLightScheme: ColorScheme = lightColorScheme(
    primary = SunsetLightPrimary, onPrimary = SunsetLightOnPrimary,
    primaryContainer = SunsetLightPrimaryContainer, onPrimaryContainer = SunsetLightOnPrimaryContainer,
    secondary = SunsetLightSecondary, onSecondary = SunsetLightOnSecondary,
    tertiary = SunsetLightTertiary, onTertiary = SunsetLightOnTertiary,
    background = SunsetLightBackground, onBackground = SunsetLightTextPrimary,
    surface = SunsetLightSurface, onSurface = SunsetLightTextPrimary,
    surfaceVariant = SunsetLightSurfaceVariant, onSurfaceVariant = SunsetLightTextSecondary,
    outline = SunsetLightOutline
)

val SunsetDarkScheme: ColorScheme = darkColorScheme(
    primary = SunsetDarkPrimary, onPrimary = SunsetDarkOnPrimary,
    primaryContainer = SunsetDarkPrimaryContainer, onPrimaryContainer = SunsetDarkOnPrimaryContainer,
    secondary = SunsetDarkSecondary, onSecondary = SunsetDarkOnSecondary,
    tertiary = SunsetDarkTertiary, onTertiary = SunsetDarkOnTertiary,
    background = SunsetDarkBackground, onBackground = SunsetDarkTextPrimary,
    surface = SunsetDarkSurface, onSurface = SunsetDarkTextPrimary,
    surfaceVariant = SunsetDarkSurfaceVariant, onSurfaceVariant = SunsetDarkTextSecondary,
    outline = SunsetDarkOutline
)

/* ----------------------- Midnight ColorSchemes (Premium) ----------------------- */

val MidnightLightScheme: ColorScheme = lightColorScheme(
    primary = MidnightLightPrimary, onPrimary = MidnightLightOnPrimary,
    primaryContainer = MidnightLightPrimaryContainer, onPrimaryContainer = MidnightLightOnPrimaryContainer,
    secondary = MidnightLightSecondary, onSecondary = MidnightLightOnSecondary,
    tertiary = MidnightLightTertiary, onTertiary = MidnightLightOnTertiary,
    background = MidnightLightBackground, onBackground = MidnightLightTextPrimary,
    surface = MidnightLightSurface, onSurface = MidnightLightTextPrimary,
    surfaceVariant = MidnightLightSurfaceVariant, onSurfaceVariant = MidnightLightTextSecondary,
    outline = MidnightLightOutline
)

val MidnightDarkScheme: ColorScheme = darkColorScheme(
    primary = MidnightDarkPrimary, onPrimary = MidnightDarkOnPrimary,
    primaryContainer = MidnightDarkPrimaryContainer, onPrimaryContainer = MidnightDarkOnPrimaryContainer,
    secondary = MidnightDarkSecondary, onSecondary = MidnightDarkOnSecondary,
    tertiary = MidnightDarkTertiary, onTertiary = MidnightDarkOnTertiary,
    background = MidnightDarkBackground, onBackground = MidnightDarkTextPrimary,
    surface = MidnightDarkSurface, onSurface = MidnightDarkTextPrimary,
    surfaceVariant = MidnightDarkSurfaceVariant, onSurfaceVariant = MidnightDarkTextSecondary,
    outline = MidnightDarkOutline
)

/* ----------------------- Forest ColorSchemes (Premium) ----------------------- */

val ForestLightScheme: ColorScheme = lightColorScheme(
    primary = ForestLightPrimary, onPrimary = ForestLightOnPrimary,
    primaryContainer = ForestLightPrimaryContainer, onPrimaryContainer = ForestLightOnPrimaryContainer,
    secondary = ForestLightSecondary, onSecondary = ForestLightOnSecondary,
    tertiary = ForestLightTertiary, onTertiary = ForestLightOnTertiary,
    background = ForestLightBackground, onBackground = ForestLightTextPrimary,
    surface = ForestLightSurface, onSurface = ForestLightTextPrimary,
    surfaceVariant = ForestLightSurfaceVariant, onSurfaceVariant = ForestLightTextSecondary,
    outline = ForestLightOutline
)

val ForestDarkScheme: ColorScheme = darkColorScheme(
    primary = ForestDarkPrimary, onPrimary = ForestDarkOnPrimary,
    primaryContainer = ForestDarkPrimaryContainer, onPrimaryContainer = ForestDarkOnPrimaryContainer,
    secondary = ForestDarkSecondary, onSecondary = ForestDarkOnSecondary,
    tertiary = ForestDarkTertiary, onTertiary = ForestDarkOnTertiary,
    background = ForestDarkBackground, onBackground = ForestDarkTextPrimary,
    surface = ForestDarkSurface, onSurface = ForestDarkTextPrimary,
    surfaceVariant = ForestDarkSurfaceVariant, onSurfaceVariant = ForestDarkTextSecondary,
    outline = ForestDarkOutline
)

/* -------------------------- Typography --------------------------- */

val LottomatchTypography: Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 57.sp, lineHeight = 64.sp, letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,
        fontSize = 45.sp, lineHeight = 52.sp, letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = 0.sp
    ),

    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = 0.sp
    ),

    titleLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp
    ),

    labelLarge = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,
        fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp
    )
)

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
    themeVariant: ThemeVariant = ThemeVariant.Classic,
    content: @Composable () -> Unit
) {
    val scheme = when (themeVariant) {
        ThemeVariant.Tombola  -> if (darkTheme) TombolaDarkScheme  else TombolaLightScheme
        ThemeVariant.Casino   -> if (darkTheme) CasinoDarkScheme   else CasinoLightScheme
        ThemeVariant.Sunset   -> if (darkTheme) SunsetDarkScheme   else SunsetLightScheme
        ThemeVariant.Midnight -> if (darkTheme) MidnightDarkScheme else MidnightLightScheme
        ThemeVariant.Forest   -> if (darkTheme) ForestDarkScheme   else ForestLightScheme
        ThemeVariant.Classic  -> if (darkTheme) DarkScheme         else LightScheme
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = LottomatchTypography,
        shapes = LottomatchShapes,
        content = content
    )
}
