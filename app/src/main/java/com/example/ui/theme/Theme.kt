package com.example.ui.theme

import android.app.Activity
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
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.data.model.ThemeMode

private val DarkColorScheme =
    darkColorScheme(
        primary = SakuraPrimaryDark,
        onPrimary = SakuraOnPrimaryDark,
        primaryContainer = SakuraPrimaryContainerDark,
        onPrimaryContainer = SakuraOnPrimaryContainerDark,
        secondary = MauveSecondaryDark,
        onSecondary = MauveOnSecondaryDark,
        secondaryContainer = MauveSecondaryContainerDark,
        onSecondaryContainer = MauveOnSecondaryContainerDark,
        tertiary = ApricotTertiaryDark,
        onTertiary = ApricotOnTertiaryDark,
        tertiaryContainer = ApricotTertiaryContainerDark,
        onTertiaryContainer = ApricotOnTertiaryContainerDark,
        background = VelvetBackgroundDark,
        onBackground = VelvetOnBackgroundDark,
        surface = VelvetSurfaceDark,
        onSurface = VelvetOnBackgroundDark,
        surfaceVariant = VelvetSurfaceVariantDark,
        onSurfaceVariant = VelvetOnSurfaceVariantDark,
        outline = VelvetOutlineDark,
        surfaceContainerLow = VelvetSurfaceContainerDark,
        surfaceContainer = VelvetSurfaceContainerDark,
        surfaceContainerHigh = VelvetSurfaceContainerHighDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = RosePrimaryLight,
        onPrimary = RoseOnPrimaryLight,
        primaryContainer = RosePrimaryContainerLight,
        onPrimaryContainer = RoseOnPrimaryContainerLight,
        secondary = PlumSecondaryLight,
        onSecondary = PlumOnSecondaryLight,
        secondaryContainer = PlumSecondaryContainerLight,
        onSecondaryContainer = PlumOnSecondaryContainerLight,
        tertiary = PeachTertiaryLight,
        onTertiary = PeachOnTertiaryLight,
        tertiaryContainer = PeachTertiaryContainerLight,
        onTertiaryContainer = PeachOnTertiaryContainerLight,
        background = BlossomBackgroundLight,
        onBackground = BlossomOnBackgroundLight,
        surface = BlossomSurfaceLight,
        onSurface = BlossomOnBackgroundLight,
        surfaceVariant = BlossomSurfaceVariantLight,
        onSurfaceVariant = BlossomOnSurfaceVariantLight,
        outline = BlossomOutlineLight,
        surfaceContainerLow = BlossomSurfaceContainerLight,
        surfaceContainer = BlossomSurfaceContainerLight,
        surfaceContainerHigh = BlossomSurfaceContainerHighLight,
    )

val PastelkaShapes =
    Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(32.dp),
    )

@Composable
fun PastelkaTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark =
        when (themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    val colorScheme =
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val context = LocalContext.current
                if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            isDark -> DarkColorScheme
            else -> LightColorScheme
        }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PastelkaShapes,
        content = content,
    )
}
