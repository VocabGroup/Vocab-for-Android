package com.example.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Optical rim only: backdrop capture belongs to the host, never to every list row. */
fun Modifier.glassRim(shape: Shape): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val edge = Brush.linearGradient(listOf(Color.White.copy(alpha = .8f), Color.White.copy(alpha = .12f), Color.Black.copy(alpha = .16f), Color.White.copy(alpha = .42f)))
    onDrawWithContent {
        drawContent()
        drawOutline(outline, edge, style = Stroke(1.2.dp.toPx()))
    }
}
