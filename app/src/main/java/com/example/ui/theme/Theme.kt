package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADBFC), // Soft purple highlight container
    onPrimaryContainer = PurplePrimary,

    secondary = PurpleAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1EDFD), 
    onSecondaryContainer = PurpleDarkText,

    background = PurpleLightBg,
    onBackground = PurpleDarkText,

    surface = PurpleCardBg,
    onSurface = PurpleDarkText,

    surfaceVariant = Color(0xFFEDECF6), 
    onSurfaceVariant = PurpleDarkText,

    outline = Color(0xFFD4D2E5), 
    outlineVariant = Color(0xFFE2E0F0), 

    error = RoseAccent,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE), 
    onErrorContainer = RoseAccent
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit,
) {
    // We enforce light Elegant theme to match mockup images
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Renders the beautiful light lavender atmospheric gradient
 * matching the mockup backgrounds.
 */
fun Modifier.meshBackground(): Modifier = this.drawBehind {
    val width = size.width
    val height = size.height

    // 1. Draw base soft lilac off-white
    drawRect(color = PurpleLightBg)

    // 2. Draw soft glowing lavender circle at the top center
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFE5DFFF), Color.Transparent),
            center = androidx.compose.ui.geometry.Offset(width * 0.5f, 0f),
            radius = width * 1.2f
        ),
        center = androidx.compose.ui.geometry.Offset(width * 0.5f, 0f),
        radius = width * 1.2f
    )

    // 3. Draw a subtle pinkish-peach accent glow at the bottom left
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF0F5), Color.Transparent),
            center = androidx.compose.ui.geometry.Offset(0f, height),
            radius = width * 0.8f
        ),
        center = androidx.compose.ui.geometry.Offset(0f, height),
        radius = width * 0.8f
    )
}

