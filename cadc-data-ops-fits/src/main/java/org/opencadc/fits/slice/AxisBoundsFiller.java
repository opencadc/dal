package org.opencadc.fits.slice;

import java.util.Arrays;

import nom.tam.fits.header.Standard;
import org.apache.log4j.Logger;


/**
 * Fills a flat {@code long[naxis*2]} bounds array: the cut axis uses clipped interval coordinates
 * ({@code clippedBounds[0]},{@code clippedBounds[1]} when present); every other axis spans
 * {@code [1, NAXISn]} for that axis. {@code naxisPerAxis.length} must equal {@code naxis} (FITS
 * NAXIS1..NAXISn sizes in axis order).
 */
public class AxisBoundsFiller {
    private static final Logger LOGGER = Logger.getLogger(AxisBoundsFiller.class);

    /**
     * @param naxis          FITS NAXIS (number of axes); array length in elements is {@code 2 * naxis}
     * @param clippedBounds  pixel bounds on {@code clipAxis} (up to two values), or {@code null} only
     *                      when {@code naxis} is 0
     * @param clipAxis       1-based axis index receiving {@code clippedBounds}
     * @param naxisPerAxis   per-axis NAXISn lengths, length {@code naxis}
     */
    static long[] fill(final int naxis, final long[] clippedBounds, final int clipAxis, final int[] naxisPerAxis) {
        LOGGER.debug("Filling bounds for naxis=" + naxis + ", clip axis " + clipAxis + ", clipped bounds "
                     + Arrays.toString(clippedBounds));
        final int flatLen = 2 * naxis;
        final long[] bounds = new long[flatLen];
        for (int i = 0; i < flatLen; i += 2) {
            final int axis = (i + 2) / 2;
            if (axis == clipAxis) {
                bounds[i] = clippedBounds != null && clippedBounds.length > 0 ? clippedBounds[0] : 1L;
                bounds[i + 1] = clippedBounds != null && clippedBounds.length > 1
                        ? clippedBounds[1] : naxisPerAxis[axis - 1];
            } else {
                bounds[i] = 1L;
                bounds[i + 1] = naxisPerAxis[axis - 1];
            }
        }
        LOGGER.debug("Filled bounds: " + Arrays.toString(bounds));

        return bounds;
    }

    static int[] naxisSizes(final FITSHeaderWCSKeywords wcs, final int naxis) {
        final int[] sizes = new int[naxis];
        for (int a = 1; a <= naxis; a++) {
            sizes[a - 1] = wcs.getIntValue(Standard.NAXISn.n(a).key());
        }
        return sizes;
    }
}
