package com.thanhng224.androidxmlbase.core.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [ShapeUtils.buildDrawable] itself needs the Android framework ([android.graphics.drawable.GradientDrawable])
 * and can't run on the plain JVM unit test runner, so it isn't covered here. These tests target
 * the pure value-resolution helpers it delegates to, which encode real edge-case decisions
 * (negative radii, sub-pixel stroke widths) independent of any Android class.
 */
class ShapeUtilsTest {
    @Test
    fun `resolveCornerRadiusPx clamps negative radius to zero`() {
        assertEquals(0f, ShapeUtils.resolveCornerRadiusPx(-4f))
    }

    @Test
    fun `resolveCornerRadiusPx passes through a positive radius unchanged`() {
        assertEquals(12f, ShapeUtils.resolveCornerRadiusPx(12f))
    }

    @Test
    fun `resolveStrokeWidthPx returns zero for non-positive input`() {
        assertEquals(0, ShapeUtils.resolveStrokeWidthPx(0f))
        assertEquals(0, ShapeUtils.resolveStrokeWidthPx(-2f))
    }

    @Test
    fun `resolveStrokeWidthPx rounds a sub-pixel positive width up to at least one pixel`() {
        assertEquals(1, ShapeUtils.resolveStrokeWidthPx(0.3f))
    }

    @Test
    fun `resolveStrokeWidthPx rounds a fractional width to the nearest pixel`() {
        assertEquals(3, ShapeUtils.resolveStrokeWidthPx(2.6f))
    }
}
