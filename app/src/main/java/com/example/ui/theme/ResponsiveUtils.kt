package com.example.ui.theme

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

enum class ScreenSizeClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Composable
fun rememberScreenSizeClass(): ScreenSizeClass {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp
    val height = configuration.screenHeightDp
    return when {
        width < 360 || height < 580 -> ScreenSizeClass.COMPACT
        width >= 600 -> ScreenSizeClass.EXPANDED
        else -> ScreenSizeClass.MEDIUM
    }
}

@Composable
fun getResponsiveButtonShape(): Shape {
    return when (rememberScreenSizeClass()) {
        ScreenSizeClass.COMPACT -> RoundedCornerShape(10.dp)
        ScreenSizeClass.MEDIUM -> RoundedCornerShape(16.dp)
        ScreenSizeClass.EXPANDED -> RoundedCornerShape(24.dp)
    }
}

@Composable
fun getResponsiveButtonHeight(): Dp {
    return when (rememberScreenSizeClass()) {
        ScreenSizeClass.COMPACT -> 44.dp
        ScreenSizeClass.MEDIUM -> 50.dp
        ScreenSizeClass.EXPANDED -> 56.dp
    }
}

@Composable
fun getResponsiveButtonFontSize(): TextUnit {
    return when (rememberScreenSizeClass()) {
        ScreenSizeClass.COMPACT -> 14.sp
        ScreenSizeClass.MEDIUM -> 16.sp
        ScreenSizeClass.EXPANDED -> 18.sp
    }
}

@Composable
fun Modifier.responsiveButton(tag: String? = null): Modifier {
    val heightVal = getResponsiveButtonHeight()
    val base = this
        .height(heightVal)
        .widthIn(max = 400.dp)
        .fillMaxWidth()
    return if (tag != null) base.testTag(tag) else base
}

@Composable
fun Modifier.responsiveContainer(): Modifier {
    return this
        .widthIn(max = 500.dp)
        .fillMaxWidth()
}
