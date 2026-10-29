package com.gemid.photostudio.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Blue60,
    onPrimary = Color.White,
    primaryContainer = Blue30,
    onPrimaryContainer = Blue90,
    secondary = AccentCyan,
    onSecondary = Navy10,
    secondaryContainer = Navy35,
    onSecondaryContainer = Blue80,
    tertiary = AccentTeal,
    onTertiary = Navy10,
    tertiaryContainer = Navy30,
    onTertiaryContainer = Blue90,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorRedDark,
    onErrorContainer = Color(0xFFFFDAD6),
    background = Navy10,
    onBackground = Neutral90,
    surface = Navy15,
    onSurface = Neutral90,
    surfaceVariant = Navy25,
    onSurfaceVariant = Neutral70,
    outline = Neutral40,
    outlineVariant = Neutral30,
    inverseSurface = Neutral90,
    inverseOnSurface = Navy10,
    inversePrimary = Blue40,
    surfaceTint = Blue60
)

@Composable
fun GemIDPhotoStudioTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GemIDTypography,
        shapes = GemIDShapes,
        content = content
    )
}
