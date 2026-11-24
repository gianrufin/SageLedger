package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class LedgerExtraColors(
    val isDark: Boolean,
    val contrastPillBg: Color,
    val contrastPillText: Color,
    val hairlineBorder: Color,
    val bentoSheenBorder: Color,
    val iconWellBg: Color,
    val heroCardBg: Color,
    val heroCardChipBg: Color,
    val heroCardBorder: Color,
    val heroCardTrack: Color,
    val heroCardInk: Color,
    val heroCardMuted: Color,
    val positiveGreen: Color,
    val positiveGreenContainer: Color,
    val onPositiveGreenContainer: Color,
    val expenseCoral: Color,
    val expenseCoralContainer: Color,
    val onExpenseCoralContainer: Color,
    val cobaltAccent: Color,
    val cobaltContainer: Color,
    val onCobaltContainer: Color,
    val eWalletTint: Color,
    val amberAccent: Color
)

private val LightExtraColors = LedgerExtraColors(
    isDark = false,
    contrastPillBg = AppLightButtonBg,
    contrastPillText = Color.White,
    hairlineBorder = AppLightHairline,
    bentoSheenBorder = AppLightHairline,
    iconWellBg = AppLightIconWell,
    heroCardBg = HeroCharcoalBg,
    heroCardChipBg = HeroCharcoalChip,
    heroCardBorder = HeroCharcoalBorder,
    heroCardTrack = HeroCharcoalTrack,
    heroCardInk = HeroCharcoalInk,
    heroCardMuted = HeroCharcoalMuted,
    positiveGreen = EmeraldAccent,
    positiveGreenContainer = EmeraldContainerLight,
    onPositiveGreenContainer = EmeraldOnContainerLight,
    expenseCoral = CoralAlertLight,
    expenseCoralContainer = CoralContainerLight,
    onExpenseCoralContainer = CoralOnContainerLight,
    cobaltAccent = SkyCobaltLight,
    cobaltContainer = SkyContainerLight,
    onCobaltContainer = SkyOnContainerLight,
    eWalletTint = SkyCobaltLight,
    amberAccent = Color(0xFFD97706)
)

private val DarkExtraColors = LedgerExtraColors(
    isDark = true,
    contrastPillBg = AppDarkHighest,
    contrastPillText = AppDarkInkPrimary,
    hairlineBorder = AppDarkHairline,
    bentoSheenBorder = AppDarkHairline,
    iconWellBg = AppDarkIconWell,
    heroCardBg = HeroCharcoalBgDark,
    heroCardChipBg = HeroCharcoalChip,
    heroCardBorder = Color(0xFF323942),
    heroCardTrack = HeroCharcoalTrack,
    heroCardInk = HeroCharcoalInk,
    heroCardMuted = HeroCharcoalMuted,
    positiveGreen = EmeraldVibrant,
    positiveGreenContainer = EmeraldContainerDark,
    onPositiveGreenContainer = EmeraldOnContainerDark,
    expenseCoral = CoralAlertDark,
    expenseCoralContainer = CoralContainerDark,
    onExpenseCoralContainer = CoralOnContainerDark,
    cobaltAccent = SkyCobaltDark,
    cobaltContainer = SkyContainerDark,
    onCobaltContainer = SkyOnContainerDark,
    eWalletTint = SkyCobaltDark,
    amberAccent = Color(0xFFF59E0B)
)

val LocalLedgerExtraColors = staticCompositionLocalOf { LightExtraColors }

private val SageLightColorScheme = lightColorScheme(
    primary = EmeraldAccent,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = EmeraldOnContainerLight,
    secondary = SkyCobaltLight,
    onSecondary = Color.White,
    secondaryContainer = SkyContainerLight,
    onSecondaryContainer = SkyOnContainerLight,
    tertiary = CoralAlertLight,
    onTertiary = Color.White,
    tertiaryContainer = CoralContainerLight,
    onTertiaryContainer = CoralOnContainerLight,
    error = CoralAlertLight,
    onError = Color.White,
    errorContainer = CoralContainerLight,
    onErrorContainer = CoralOnContainerLight,
    background = AppLightCanvas,
    onBackground = AppLightInkPrimary,
    surface = AppLightSurface,
    onSurface = AppLightInkPrimary,
    surfaceVariant = AppLightIconWell,
    onSurfaceVariant = AppLightInkMuted,
    surfaceContainerLowest = AppLightSurface,
    surfaceContainerLow = Color(0xFFF8F9FB),
    surfaceContainer = AppLightIconWell,
    surfaceContainerHigh = AppLightSurfaceTrack,
    surfaceContainerHighest = Color(0xFFD1D5DB),
    outline = Color(0xFF9CA3AF),
    outlineVariant = AppLightHairline
)

private val SageDarkColorScheme = darkColorScheme(
    primary = EmeraldVibrant,
    onPrimary = Color(0xFF052E16),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = EmeraldOnContainerDark,
    secondary = SkyCobaltDark,
    onSecondary = Color(0xFF052033),
    secondaryContainer = SkyContainerDark,
    onSecondaryContainer = SkyOnContainerDark,
    tertiary = CoralAlertDark,
    onTertiary = Color(0xFF3A0712),
    tertiaryContainer = CoralContainerDark,
    onTertiaryContainer = CoralOnContainerDark,
    error = CoralAlertDark,
    onError = Color(0xFF3A0712),
    errorContainer = CoralContainerDark,
    onErrorContainer = CoralOnContainerDark,
    background = AppDarkCanvas,
    onBackground = AppDarkInkPrimary,
    surface = AppDarkSurface,
    onSurface = AppDarkInkPrimary,
    surfaceVariant = AppDarkIconWell,
    onSurfaceVariant = AppDarkInkMuted,
    surfaceContainerLowest = Color(0xFF0B0D0F),
    surfaceContainerLow = AppDarkIconWell,
    surfaceContainer = AppDarkIconWell,
    surfaceContainerHigh = AppDarkSurfaceTrack,
    surfaceContainerHighest = AppDarkHighest,
    outline = Color(0xFF4B5563),
    outlineVariant = AppDarkHairline
)

val SageShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun SageLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SageDarkColorScheme else SageLightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors
    CompositionLocalProvider(LocalLedgerExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SageTypography,
            shapes = SageShapes,
            content = content
        )
    }
}
