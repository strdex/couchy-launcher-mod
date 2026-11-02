package com.conreo.couchytv.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * High-performance hardware-accelerated rounded shape for TV GPUs.
 * Uses native Skia round-rect primitives instead of CPU-heavy polygon path calculation.
 */
class SmoothCornerShape(
    private val radius: Dp,
    @Suppress("UNUSED_PARAMETER") private val smoothing: Float = 0.6f,
) : Shape {
    private val delegate = RoundedCornerShape(radius)

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline = delegate.createOutline(size, layoutDirection, density)
}
