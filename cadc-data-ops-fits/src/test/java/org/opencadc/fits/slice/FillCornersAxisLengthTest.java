/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 ************************************************************************
 *
 * (c) 2025. Government of Canada / Gouvernement du Canada
 *
 ************************************************************************
 */

package org.opencadc.fits.slice;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link NDimensionalSlicer#computeClippedAxisLengthForTests} (1-based inclusive pixel ranges
 * clipped to image width).
 */
public class FillCornersAxisLengthTest {

    @Test
    public void overshootPastHighEdgeUsesIntersectionNotCappedDifference() {
        // N=100, 1-based [50, 150]: intersection is pixels 50–100 => length 51.
        // Old bug: min(150-49, 100) = 100.
        Assert.assertEquals(51, NDimensionalSlicer.computeClippedAxisLengthForTests(100, 50, 150));
    }

    @Test
    public void fullWidth() {
        Assert.assertEquals(100, NDimensionalSlicer.computeClippedAxisLengthForTests(100, 1, 100));
    }

    @Test
    public void partialWidth() {
        Assert.assertEquals(50, NDimensionalSlicer.computeClippedAxisLengthForTests(100, 1, 50));
        Assert.assertEquals(51, NDimensionalSlicer.computeClippedAxisLengthForTests(100, 50, 100));
    }

    @Test
    public void zeroLengthWhenCompletelyPastImage() {
        Assert.assertEquals(0, NDimensionalSlicer.computeClippedAxisLengthForTests(100, 150, 200));
    }

    @Test
    public void lowEdgeClampedToZeroIn0Based() {
        // 1-based [0, 50] gives lowerBound 0, end 50 => 50 pixels if N>=50
        Assert.assertEquals(50, NDimensionalSlicer.computeClippedAxisLengthForTests(100, 0, 50));
    }
}
