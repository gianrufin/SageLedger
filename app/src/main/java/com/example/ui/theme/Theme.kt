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
    val heroGradientTop: Color,
    val heroGradientMid: Color,
    val heroGradientBottom: Color,
    val contrastPillBg: Color,
    val contrastPillText: Color,
    val hairlineBorder: Color,
    val positiveGreen: Color,
    val positiveGreenContainer: Color,
    val onPositiveGreenContainer: Color,
    val expenseCoral: Color
)

private val LightExtraColors = LedgerExtraColors(
    heroGradientTop = IrisHeroTop,
    heroGradientMid = IrisHeroMid,
    heroGradientBottom = IrisHeroBottom,
    contrastPillBg = ObsidianBannerSurface,
    contrastPillText = ObsidianBannerText,
    hairlineBorder = HairlineBorderLight,
    positiveGreen = VividMintSecondary,
    positiveGreenContainer = MintSecondaryContainer,
    onPositiveGreenContainer = OnMintSecondaryContainer,
    expenseCoral = CrispCoralError
)

private val DarkExtraColors = LedgerExtraColors(
    heroGradientTop = IrisDarkHeroTop,
    heroGradientMid = IrisDarkHeroMid,
    heroGradientBottom = IrisDarkHeroBottom,
    contrastPillBg = Color(0xFF222938),
    contrastPillText = CrispFrostInk,
    hairlineBorder = HairlineBorderDark,
    positiveGreen = LuminousMintDark,
    positiveGreenContainer = DarkMintContainer,
    onPositiveGreenContainer = OnDarkMintContainer,
    expenseCoral = LuminousCoralDark
)

val LocalLedgerExtraColors = staticCompositionLocalOf { LightExtraColors }

private val SageLightColorScheme = lightColorScheme(
    primary = ElectricIrisPrimary,
    onPrimary = Color.White,
    primaryContainer = IrisPrimaryContainer,
    onPrimaryContainer = OnIrisPrimaryContainer,
    secondary = VividMintSecondary,
    onSecondary = Color.White,
    secondaryContainer = MintSecondaryContainer,
    onSecondaryContainer = OnMintSecondaryContainer,
    tertiary = SunnyTangerineTertiary,
    onTertiary = Color.White,
    tertiaryContainer = TangerineContainer,
    onTertiaryContainer = OnTangerineContainer,
    error = CrispCoralError,
    onError = Color.White,
    errorContainer = CoralContainer,
    onErrorContainer = OnCoralContainer,
    background = StudioCanvasLight,
    onBackground = InkCharcoal,
    surface = PureCardWhite,
    onSurface = InkCharcoal,
    surfaceVariant = SoftPillContainer,
    onSurfaceVariant = MutedSlateText,
    surfaceContainerLowest = PureCardWhite,
    surfaceContainerLow = Color(0xFFF9FAFC),
    surfaceContainer = SoftPillContainer,
    surfaceContainerHigh = SoftTrackHigh,
    surfaceContainerHighest = Color(0xFFD9DDE8),
    outline = OutlineLight,
    outlineVariant = HairlineBorderLight
)

private val SageDarkColorScheme = darkColorScheme(
    primary = LuminousIrisDark,
    onPrimary = Color(0xFF150D48),
    primaryContainer = DarkIrisContainer,
    onPrimaryContainer = OnDarkIrisContainer,
    secondary = LuminousMintDark,
    onSecondary = Color(0xFF062C17),
    secondaryContainer = DarkMintContainer,
    onSecondaryContainer = OnDarkMintContainer,
    tertiary = LuminousTangerineDark,
    onTertiary = Color(0xFF391900),
    tertiaryContainer = DarkTangerineContainer,
    onTertiaryContainer = OnDarkTangerineContainer,
    error = LuminousCoralDark,
    onError = Color(0xFF420A07),
    errorContainer = DarkCoralContainer,
    onErrorContainer = OnDarkCoralContainer,
    background = ObsidianCanvasDark,
    onBackground = CrispFrostInk,
    surface = SlateCardDark,
    onSurface = CrispFrostInk,
    surfaceVariant = SlateContainerDark,
    onSurfaceVariant = MutedFrostText,
    surfaceContainerLowest = Color(0xFF090B10),
    surfaceContainerLow = Color(0xFF12161E),
    surfaceContainer = SlateContainerDark,
    surfaceContainerHigh = SlateTrackDark,
    surfaceContainerHighest = Color(0xFF333E52),
    outline = OutlineDark,
    outlineVariant = HairlineBorderDark
)

val SageShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
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
