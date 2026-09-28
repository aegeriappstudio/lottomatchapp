package ch.lottomatchapp.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Theme-Variante. Alle ausser Classic sind Premium-only und werden vom ViewModel gegated. */
enum class ThemeVariant { Classic, Tombola, Casino, Sunset, Midnight, Forest }

/* ----------------------- ColorScheme-Builder ----------------------- */

/**
 * Baut ein vollständiges ColorScheme aus der Theme-Palette.
 *
 * Rollen, die die Palette nicht explizit definiert (Secondary-/Tertiary-Container,
 * Surface-Container-Stufen), werden aus der Palette abgeleitet. Sonst fallen sie auf
 * die M3-Baseline (Lila/Lavendel) zurück und tauchen in Tonal-Buttons, Bottom-Sheets
 * und Dialogen auf.
 */
private fun lottoScheme(
    dark: Boolean,
    primary: Color, onPrimary: Color,
    primaryContainer: Color, onPrimaryContainer: Color,
    secondary: Color, onSecondary: Color,
    tertiary: Color, onTertiary: Color,
    background: Color, surface: Color, surfaceVariant: Color,
    textPrimary: Color, textSecondary: Color,
    outline: Color
): ColorScheme {
    val accentMix = if (dark) 0.35f else 0.22f
    val secondaryContainer = lerp(surface, secondary, accentMix)
    val tertiaryContainer = lerp(surface, tertiary, accentMix)
    val base = if (dark) {
        darkColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondary = secondary, onSecondary = onSecondary,
            secondaryContainer = secondaryContainer, onSecondaryContainer = textPrimary,
            tertiary = tertiary, onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = textPrimary,
            background = background, onBackground = textPrimary,
            surface = surface, onSurface = textPrimary,
            surfaceVariant = surfaceVariant, onSurfaceVariant = textSecondary,
            surfaceTint = primary,
            outline = outline
        )
    } else {
        lightColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondary = secondary, onSecondary = onSecondary,
            secondaryContainer = secondaryContainer, onSecondaryContainer = textPrimary,
            tertiary = tertiary, onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = textPrimary,
            background = background, onBackground = textPrimary,
            surface = surface, onSurface = textPrimary,
            surfaceVariant = surfaceVariant, onSurfaceVariant = textSecondary,
            surfaceTint = primary,
            outline = outline
        )
    }
    // Surface-Container-Stufen: von surface Richtung surfaceVariant.
    return base.copy(
        surfaceContainerLowest = if (dark) background else surface,
        surfaceContainerLow = lerp(surface, surfaceVariant, 0.2f),
        surfaceContainer = lerp(surface, surfaceVariant, 0.4f),
        surfaceContainerHigh = lerp(surface, surfaceVariant, 0.6f),
        surfaceContainerHighest = lerp(surface, surfaceVariant, 0.8f),
        surfaceBright = if (dark) lerp(surface, surfaceVariant, 0.8f) else surface,
        surfaceDim = if (dark) background else lerp(background, surfaceVariant, 0.5f)
    )
}

/* ----------------------- Classic ColorSchemes (alle User) ----------------------- */

val LightScheme: ColorScheme = lottoScheme(
    dark = false,
    primary = LightPrimary, onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer, onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary, onSecondary = LightOnSecondary,
    tertiary = LightTertiary, onTertiary = LightOnTertiary,
    background = LightBackground, surface = LightSurface, surfaceVariant = LightSurfaceVariant,
    textPrimary = LightTextPrimary, textSecondary = LightTextSecondary,
    outline = LightOutline
)

val DarkScheme: ColorScheme = lottoScheme(
    dark = true,
    primary = DarkPrimary, onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer, onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary, onSecondary = DarkOnSecondary,
    tertiary = DarkTertiary, onTertiary = DarkOnTertiary,
    background = DarkBackground, surface = DarkSurface, surfaceVariant = DarkSurfaceVariant,
    textPrimary = DarkTextPrimary, textSecondary = DarkTextSecondary,
    outline = DarkOutline
)

/* ----------------------- Tombola ColorSchemes (Premium) ----------------------- */

val TombolaLightScheme: ColorScheme = lottoScheme(
    dark = false,
    primary = TombolaLightPrimary, onPrimary = TombolaLightOnPrimary,
    primaryContainer = TombolaLightPrimaryContainer, onPrimaryContainer = TombolaLightOnPrimaryContainer,
    secondary = TombolaLightSecondary, onSecondary = TombolaLightOnSecondary,
    tertiary = TombolaLightTertiary, onTertiary = TombolaLightOnTertiary,
    background = TombolaLightBackground, surface = TombolaLightSurface, surfaceVariant = TombolaLightSurfaceVariant,
    textPrimary = TombolaLightTextPrimary, textSecondary = TombolaLightTextSecondary,
    outline = TombolaLightOutline
)

val TombolaDarkScheme: ColorScheme = lottoScheme(
    dark = true,
    primary = TombolaDarkPrimary, onPrimary = TombolaDarkOnPrimary,
    primaryContainer = TombolaDarkPrimaryContainer, onPrimaryContainer = TombolaDarkOnPrimaryContainer,
    secondary = TombolaDarkSecondary, onSecondary = TombolaDarkOnSecondary,
    tertiary = TombolaDarkTertiary, onTertiary = TombolaDarkOnTertiary,
    background = TombolaDarkBackground, surface = TombolaDarkSurface, surfaceVariant = TombolaDarkSurfaceVariant,
    textPrimary = TombolaDarkTextPrimary, textSecondary = TombolaDarkTextSecondary,
    outline = TombolaDarkOutline
)

/* ----------------------- Casino ColorSchemes (Premium) ----------------------- */

val CasinoLightScheme: ColorScheme = lottoScheme(
    dark = false,
    primary = CasinoLightPrimary, onPrimary = CasinoLightOnPrimary,
    primaryContainer = CasinoLightPrimaryContainer, onPrimaryContainer = CasinoLightOnPrimaryContainer,
    secondary = CasinoLightSecondary, onSecondary = CasinoLightOnSecondary,
    tertiary = CasinoLightTertiary, onTertiary = CasinoLightOnTertiary,
    background = CasinoLightBackground, surface = CasinoLightSurface, surfaceVariant = CasinoLightSurfaceVariant,
    textPrimary = CasinoLightTextPrimary, textSecondary = CasinoLightTextSecondary,
    outline = CasinoLightOutline
)

val CasinoDarkScheme: ColorScheme = lottoScheme(
    dark = true,
    primary = CasinoDarkPrimary, onPrimary = CasinoDarkOnPrimary,
    primaryContainer = CasinoDarkPrimaryContainer, onPrimaryContainer = CasinoDarkOnPrimaryContainer,
    secondary = CasinoDarkSecondary, onSecondary = CasinoDarkOnSecondary,
    tertiary = CasinoDarkTertiary, onTertiary = CasinoDarkOnTertiary,
    background = CasinoDarkBackground, surface = CasinoDarkSurface, surfaceVariant = CasinoDarkSurfaceVariant,
    textPrimary = CasinoDarkTextPrimary, textSecondary = CasinoDarkTextSecondary,
    outline = CasinoDarkOutline
)

/* ----------------------- Sunset ColorSchemes (Premium) ----------------------- */

val SunsetLightScheme: ColorScheme = lottoScheme(
    dark = false,
    primary = SunsetLightPrimary, onPrimary = SunsetLightOnPrimary,
    primaryContainer = SunsetLightPrimaryContainer, onPrimaryContainer = SunsetLightOnPrimaryContainer,
    secondary = SunsetLightSecondary, onSecondary = SunsetLightOnSecondary,
    tertiary = SunsetLightTertiary, onTertiary = SunsetLightOnTertiary,
    background = SunsetLightBackground, surface = SunsetLightSurface, surfaceVariant = SunsetLightSurfaceVariant,
    textPrimary = SunsetLightTextPrimary, textSecondary = SunsetLightTextSecondary,
    outline = SunsetLightOutline
)

val SunsetDarkScheme: ColorScheme = lottoScheme(
    dark = true,
    primary = SunsetDarkPrimary, onPrimary = SunsetDarkOnPrimary,
    primaryContainer = SunsetDarkPrimaryContainer, onPrimaryContainer = SunsetDarkOnPrimaryContainer,
    secondary = SunsetDarkSecondary, onSecondary = SunsetDarkOnSecondary,
    tertiary = SunsetDarkTertiary, onTertiary = SunsetDarkOnTertiary,
    background = SunsetDarkBackground, surface = SunsetDarkSurface, surfaceVariant = SunsetDarkSurfaceVariant,
    textPrimary = SunsetDarkTextPrimary, textSecondary = SunsetDarkTextSecondary,
    outline = SunsetDarkOutline
)

/* ----------------------- Midnight ColorSchemes (Premium) ----------------------- */

val MidnightLightScheme: ColorScheme = lottoScheme(
    dark = false,
    primary = MidnightLightPrimary, onPrimary = MidnightLightOnPrimary,
    primaryContainer = MidnightLightPrimaryContainer, onPrimaryContainer = MidnightLightOnPrimaryContainer,
    secondary = MidnightLightSecondary, onSecondary = MidnightLightOnSecondary,
    tertiary = MidnightLightTertiary, onTertiary = MidnightLightOnTertiary,
    background = MidnightLightBackground, surface = MidnightLightSurface, surfaceVariant = MidnightLightSurfaceVariant,
    textPrimary = MidnightLightTextPrimary, textSecondary = MidnightLightTextSecondary,
    outline = MidnightLightOutline
)

val MidnightDarkScheme: ColorScheme = lottoScheme(
    dark = true,
    primary = MidnightDarkPrimary, onPrimary = MidnightDarkOnPrimary,
    primaryContainer = MidnightDarkPrimaryContainer, onPrimaryContainer = MidnightDarkOnPrimaryContainer,
    secondary = MidnightDarkSecondary, onSecondary = MidnightDarkOnSecondary,
    tertiary = MidnightDarkTertiary, onTertiary = MidnightDarkOnTertiary,
    background = MidnightDarkBackground, surface = MidnightDarkSurface, surfaceVariant = MidnightDarkSurfaceVariant,
    textPrimary = MidnightDarkTextPrimary, textSecondary = MidnightDarkTextSecondary,
    outline = MidnightDarkOutline
)

/* ----------------------- Forest ColorSchemes (Premium) ----------------------- */

val ForestLightScheme: ColorScheme = lottoScheme(
    dark = false,
    primary = ForestLightPrimary, onPrimary = ForestLightOnPrimary,
    primaryContainer = ForestLightPrimaryContainer, onPrimaryContainer = ForestLightOnPrimaryContainer,
    secondary = ForestLightSecondary, onSecondary = ForestLightOnSecondary,
    tertiary = ForestLightTertiary, onTertiary = ForestLightOnTertiary,
    background = ForestLightBackground, surface = ForestLightSurface, surfaceVariant = ForestLightSurfaceVariant,
    textPrimary = ForestLightTextPrimary, textSecondary = ForestLightTextSecondary,
    outline = ForestLightOutline
)

val ForestDarkScheme: ColorScheme = lottoScheme(
    dark = true,
    primary = ForestDarkPrimary, onPrimary = ForestDarkOnPrimary,
    primaryContainer = ForestDarkPrimaryContainer, onPrimaryContainer = ForestDarkOnPrimaryContainer,
    secondary = ForestDarkSecondary, onSecondary = ForestDarkOnSecondary,
    tertiary = ForestDarkTertiary, onTertiary = ForestDarkOnTertiary,
    background = ForestDarkBackground, surface = ForestDarkSurface, surfaceVariant = ForestDarkSurfaceVariant,
    textPrimary = ForestDarkTextPrimary, textSecondary = ForestDarkTextSecondary,
    outline = ForestDarkOutline
)

/** ColorScheme dieser Variante für Hell/Dunkel. Wird vom Theme und vom Theme-Picker genutzt. */
fun ThemeVariant.colorScheme(dark: Boolean): ColorScheme = when (this) {
    ThemeVariant.Classic  -> if (dark) DarkScheme         else LightScheme
    ThemeVariant.Tombola  -> if (dark) TombolaDarkScheme  else TombolaLightScheme
    ThemeVariant.Casino   -> if (dark) CasinoDarkScheme   else CasinoLightScheme
    ThemeVariant.Sunset   -> if (dark) SunsetDarkScheme   else SunsetLightScheme
    ThemeVariant.Midnight -> if (dark) MidnightDarkScheme else MidnightLightScheme
    ThemeVariant.Forest   -> if (dark) ForestDarkScheme   else ForestLightScheme
}

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
    MaterialTheme(
        colorScheme = themeVariant.colorScheme(darkTheme),
        typography = LottomatchTypography,
        shapes = LottomatchShapes,
        content = content
    )
}
