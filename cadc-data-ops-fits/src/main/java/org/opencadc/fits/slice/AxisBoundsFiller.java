package org.opencadc.fits.slice;

import nom.tam.fits.header.Standard;
import org.apache.log4j.Logger;

import java.util.Arrays;

/**
 * Fills a flat {@code long[naxis*2]} bounds array: the cut axis uses clipped interval coordinates;
 * every other axis spans {@code [1, NAXISn]} for that axis (not a single global length).
 */
public class AxisBoundsFiller {
    private static final Logger LOGGER = Logger.getLogger(AxisBoundsFiller.class);

    static long[] fill(final int axes, final long[] clippedBounds, final int clipAxis, final int[] nAxisPerAxis) {
        LOGGER.debug("Filling bounds for " + axes + " axes, clip axis " + clipAxis + ", clipped bounds "
                     + Arrays.toString(clippedBounds));
        final long[] bounds = new long[axes];
        for (int i = 0; i < axes; i += 2) {
            final int axis = (i + 2) / 2;
            if (axis == clipAxis) {
                bounds[i] = clippedBounds.length > 0 ? clippedBounds[0] : 1L;
                bounds[i + 1] = clippedBounds.length > 1 ? clippedBounds[1] : nAxisPerAxis[axis - 1];
            } else {
                bounds[i] = 1L;
                bounds[i + 1] = nAxisPerAxis[axis - 1];
            }
        }
        LOGGER.debug("Filled bounds: " + Arrays.toString(bounds));

        return bounds;
    }

    static int[] nAxisSizes(final FITSHeaderWCSKeywords wcs, final int nAxis) {
        final int[] sizes = new int[nAxis];
        for (int a = 1; a <= nAxis; a++) {
            sizes[a - 1] = wcs.getIntValue(Standard.NAXISn.n(a).key());
        }
        return sizes;
    }
}
