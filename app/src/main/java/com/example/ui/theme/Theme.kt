package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SageLightColorScheme = lightColorScheme(
    primary = MutedSagePrimary,
    onPrimary = Color.White,
    primaryContainer = SagePrimaryContainer,
    onPrimaryContainer = OnSagePrimaryContainer,
    secondary = MossSecondary,
    onSecondary = Color.White,
    secondaryContainer = MossSecondaryContainer,
    onSecondaryContainer = OnMossSecondaryContainer,
    tertiary = WarmAmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = WarmAmberContainer,
    onTertiaryContainer = OnWarmAmberContainer,
    error = TerracottaError,
    onError = Color.White,
    errorContainer = TerracottaContainer,
    onErrorContainer = OnTerracottaContainer,
    background = AlabasterCanvas,
    onBackground = ForestCharcoal,
    surface = SoftStoneSurface,
    onSurface = ForestCharcoal,
    surfaceVariant = MutedStoneContainer,
    onSurfaceVariant = SlateSageText,
    surfaceContainerLowest = SoftStoneSurface,
    surfaceContainerLow = Color(0xFFF3F4F0),
    surfaceContainer = MutedStoneContainer,
    surfaceContainerHigh = SubtleStoneHigh,
    surfaceContainerHighest = Color(0xFFDADFD6),
    outline = SageOutline,
    outlineVariant = SageOutlineVariant
)

private val SageDarkColorScheme = darkColorScheme(
    primary = LuminousSagePrimary,
    onPrimary = Color(0xFF0E251B),
    primaryContainer = DarkSagePrimaryContainer,
    onPrimaryContainer = OnDarkSagePrimaryContainer,
    secondary = DarkMossSecondary,
    onSecondary = Color(0xFF13271E),
    secondaryContainer = DarkMossContainer,
    onSecondaryContainer = OnDarkMossContainer,
    tertiary = DarkAmberTertiary,
    onTertiary = Color(0xFF2D1D09),
    tertiaryContainer = DarkAmberContainer,
    onTertiaryContainer = OnDarkAmberContainer,
    error = DarkTerracottaError,
    onError = Color(0xFF39110D),
    errorContainer = DarkTerracottaContainer,
    onErrorContainer = OnDarkTerracottaContainer,
    background = PineSlateDarkCanvas,
    onBackground = MistInk,
    surface = CharcoalSurface,
    onSurface = MistInk,
    surfaceVariant = CharcoalSurfaceHigh,
    onSurfaceVariant = MutedMistText,
    surfaceContainerLowest = Color(0xFF0D1210),
    surfaceContainerLow = Color(0xFF151C19),
    surfaceContainer = CharcoalSurface,
    surfaceContainerHigh = CharcoalSurfaceHigh,
    surfaceContainerHighest = CharcoalSurfaceHighest,
    outline = DarkSageOutline,
    outlineVariant = DarkSageOutlineVariant
)

val SageShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SageLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SageDarkColorScheme else SageLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SageTypography,
        shapes = SageShapes,
        content = content
    )
}
