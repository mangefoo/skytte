package se.mindphaser.skytte.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Color(0xFF3B6B4A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBCEFC6),
    secondary = Color(0xFF506352),
    background = Color(0xFFF6FBF4),
    surface = Color(0xFFF6FBF4)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA1D4AD),
    onPrimary = Color(0xFF0E3B1F),
    primaryContainer = Color(0xFF225335),
    secondary = Color(0xFFB8CCBA),
    background = Color(0xFF111712),
    surface = Color(0xFF111712)
)

@Composable
fun SkytteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, content = content)
}
