package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.core.ThemeMode

private val CityLoopLightColorScheme = lightColorScheme(
    primary = TealPrimaryLight,
    onPrimary = OnTealPrimaryLight,
    primaryContainer = TealContainerLight,
    onPrimaryContainer = OnTealContainerLight,
    secondary = SaffronSecondaryLight,
    onSecondary = OnSaffronSecondaryLight,
    secondaryContainer = SaffronContainerLight,
    onSecondaryContainer = OnSaffronContainerLight,
    tertiary = TerracottaTertiaryLight,
    onTertiary = OnTerracottaTertiaryLight,
    tertiaryContainer = TerracottaContainerLight,
    onTertiaryContainer = OnTerracottaContainerLight,
    background = SandBackgroundLight,
    onBackground = OnWarmSurfaceLight,
    surface = WarmSurfaceLight,
    onSurface = OnWarmSurfaceLight,
    surfaceVariant = WarmSurfaceVariantLight,
    onSurfaceVariant = OnWarmSurfaceVariantLight,
    outline = OutlineLight,
    error = ErrorRedLight
)

private val CityLoopDarkColorScheme = darkColorScheme(
    primary = TealPrimaryDark,
    onPrimary = OnTealPrimaryDark,
    primaryContainer = TealContainerDark,
    onPrimaryContainer = OnTealContainerDark,
    secondary = SaffronSecondaryDark,
    onSecondary = OnSaffronSecondaryDark,
    secondaryContainer = SaffronContainerDark,
    onSecondaryContainer = OnSaffronContainerDark,
    tertiary = TerracottaTertiaryDark,
    onTertiary = OnTerracottaTertiaryDark,
    tertiaryContainer = TerracottaContainerDark,
    onTertiaryContainer = OnTerracottaContainerDark,
    background = SlateBackgroundDark,
    onBackground = OnSlateSurfaceDark,
    surface = SlateSurfaceDark,
    onSurface = OnSlateSurfaceDark,
    surfaceVariant = SlateSurfaceVariantDark,
    onSurfaceVariant = OnSlateSurfaceVariantDark,
    outline = OutlineDark,
    error = ErrorRedDark
)

val CityLoopShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun CityLoopTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CityLoopDarkColorScheme
        else -> CityLoopLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CityLoopTypography,
        shapes = CityLoopShapes,
        content = content
    )
}
