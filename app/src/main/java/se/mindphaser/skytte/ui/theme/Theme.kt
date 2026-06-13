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
    primary = Color(0xFF355E3B),            // hunter green
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC4D5B0),   // dry sage
    onPrimaryContainer = Color(0xFF12260F),
    secondary = Color(0xFF6F5839),          // saddle brown
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DCC3), // tan
    onSecondaryContainer = Color(0xFF2A1F0E),
    tertiary = Color(0xFFC2571B),           // blaze orange
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBC8),
    onTertiaryContainer = Color(0xFF3A1500),
    background = Color(0xFFF6F3EA),         // warm parchment
    onBackground = Color(0xFF1B1C16),
    surface = Color(0xFFF6F3EA),
    onSurface = Color(0xFF1B1C16),
    surfaceVariant = Color(0xFFE3E0CF),
    onSurfaceVariant = Color(0xFF46483D),
    surfaceContainerLowest = Color(0xFFFFFDF4),
    surfaceContainerLow = Color(0xFFF0EDE3),
    surfaceContainer = Color(0xFFEAE7DC),
    surfaceContainerHigh = Color(0xFFE4E1D6),
    surfaceContainerHighest = Color(0xFFDEDBD0),
    outline = Color(0xFF77796A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA3C293),            // moss green
    onPrimary = Color(0xFF11290D),
    primaryContainer = Color(0xFF324A2C),   // forest floor
    onPrimaryContainer = Color(0xFFC4D5B0),
    secondary = Color(0xFFD2BD97),          // light tan
    onSecondary = Color(0xFF3B2D14),
    secondaryContainer = Color(0xFF544226),
    onSecondaryContainer = Color(0xFFE8DCC3),
    tertiary = Color(0xFFFF8A4C),           // blaze orange
    onTertiary = Color(0xFF4A1B00),
    tertiaryContainer = Color(0xFF7A3A12),
    onTertiaryContainer = Color(0xFFFFDBC8),
    background = Color(0xFF14160F),         // dark woodland
    onBackground = Color(0xFFE4E2D7),
    surface = Color(0xFF14160F),
    onSurface = Color(0xFFE4E2D7),
    surfaceVariant = Color(0xFF46483D),
    onSurfaceVariant = Color(0xFFC7C8B4),
    surfaceContainerLowest = Color(0xFF0E100A),
    surfaceContainerLow = Color(0xFF1C1E16),
    surfaceContainer = Color(0xFF20221A),
    surfaceContainerHigh = Color(0xFF2A2D24),
    surfaceContainerHighest = Color(0xFF35382E),
    outline = Color(0xFF90927F)
)

@Composable
fun SkytteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
