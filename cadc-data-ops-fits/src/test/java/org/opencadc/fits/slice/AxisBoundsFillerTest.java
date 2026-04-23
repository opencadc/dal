/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2026.                            (c) 2026.
*  Government of Canada                 Gouvernement du Canada
*  National Research Council            Conseil national de recherches
*  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
*  All rights reserved                  Tous droits réservés
*
************************************************************************
*/

package org.opencadc.fits.slice;

import nom.tam.fits.Header;
import nom.tam.fits.header.Standard;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.fits.CADCExt;

public class AxisBoundsFillerTest {

    @Test
    public void fillPutsClipOnRequestedAxisAndFullExtentOnOthers() {
        final int naxis = 4;
        final int axes = naxis * 2;
        final int clipAxis = 3;
        final long[] clippedSpectral = new long[]{5L, 18L};
        final int[] nAxisPerAxis = new int[]{400, 400, 60, 1};

        final long[] out = AxisBoundsFiller.fill(axes, clippedSpectral, clipAxis, nAxisPerAxis);

        final long[] expected = new long[]{
                1L, 400L, 1L, 400L, 5L, 18L, 1L, 1L
        };
        Assert.assertArrayEquals("RA/DEC and Stokes full, spectral axis from clip", expected, out);
    }

    @Test
    public void fillUsesClippedRangeOnFirstAxis() {
        final int naxis = 2;
        final long[] out = AxisBoundsFiller.fill(
                naxis * 2, new long[]{3L, 9L}, 1, new int[]{128, 256});
        Assert.assertArrayEquals(new long[]{3L, 9L, 1L, 256L}, out);
    }

    @Test
    public void nAxisSizesReadsNaxis1ThroughN() throws Exception {
        final Header header = new Header();
        header.setSimple(true);
        header.setNaxes(3);
        header.setBitpix(8);
        header.addValue(Standard.NAXISn.n(1), 10);
        header.addValue(Standard.NAXISn.n(2), 20);
        header.addValue(Standard.NAXISn.n(3), 4);
        for (int i = 1; i <= 3; i++) {
            header.addValue(Standard.CTYPEn.n(i), "TEMP");
            header.addValue(CADCExt.CUNITn.n(i), "m");
        }

        final FITSHeaderWCSKeywords wcs = new FITSHeaderWCSKeywords(header);
        final int[] sizes = AxisBoundsFiller.nAxisSizes(wcs, 3);
        Assert.assertArrayEquals(new int[]{10, 20, 4}, sizes);
    }
}
