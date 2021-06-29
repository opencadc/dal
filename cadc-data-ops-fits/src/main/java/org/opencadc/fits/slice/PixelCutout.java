/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2021.                            (c) 2021.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *
 ************************************************************************
 */

package org.opencadc.fits.slice;

import ca.nrc.cadc.wcs.exceptions.WCSLibRuntimeException;
import java.util.Arrays;
import java.util.List;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.header.Standard;
import org.apache.log4j.Logger;
import org.opencadc.soda.ExtensionSlice;
import org.opencadc.soda.PixelRange;


/**
 * Simple class to cut pixels out.  Exists to maintain the same format as the WCS cutouts.
 */
public class PixelCutout extends FITSCutout<ExtensionSlice> {
    private static final Logger LOGGER = Logger.getLogger(PixelCutout.class);

    public PixelCutout(final Header header) throws HeaderCardException {
        super(header);
    }


    /**
     * Obtain the bounds of the given cutout.
     *
     * @param cutoutBound The bounds (shape, interval etc.) of the cutout.
     * @return long[] array of overlapping bounds, or long[0] if all pixels are included.
     * @throws WCSLibRuntimeException WCSLib (C) error.
     */
    @Override
    public long[] getBounds(final ExtensionSlice cutoutBound) throws WCSLibRuntimeException {

        // Entire extension requested.
        if (cutoutBound.getPixelRanges().isEmpty()) {
            return new long[0];
        } else {

            // The HDU matches a requested one, now check if pixels overlap
            final int naxis = this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXIS.key());

            final List<PixelRange> pixelRanges = cutoutBound.getPixelRanges();
            long[] pixelCutoutBounds = new long[0];
            for (int i = 0; i < naxis; i++) {
                final int axisKey = i + 1;
                final int maxUpperBound = this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXISn.n(axisKey).key());

                // TODO: this does not take into account flipped axis (upperBound < lowerBound
                // so upperbound is really the smallest pixel index)
                if (i < pixelRanges.size()) {
                    final int lowBound = pixelRanges.get(i).lowerBound;
                    final int hiBound = pixelRanges.get(i).upperBound;
                    if (lowBound < maxUpperBound) {
                        final long[] clip = clip(axisKey, lowBound, Math.min(maxUpperBound, hiBound));
                        if (clip != null) {
                            final long[] overlap = clip.length == 0 ? new long[] {lowBound, hiBound} : clip;
                            final int oldLength = pixelCutoutBounds.length;
                            final int newLength = oldLength + overlap.length;
                            pixelCutoutBounds = Arrays.copyOf(pixelCutoutBounds, newLength);
                            System.arraycopy(overlap, 0, pixelCutoutBounds, oldLength, overlap.length);
                        }
                    }
                }
            }

            return pixelCutoutBounds.length == 0 ? null : pixelCutoutBounds;
        }
    }

    private long[] clip(final int axis, final long lower, final long upper) {
        final long len = this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXISn.n(axis).key());

        long x1 = lower;
        long x2 = upper;

        if (x1 < 1) {
            x1 = 1;
        }

        if (x2 > len) {
            x2 = len;
        }

        LOGGER.debug("clip: " + len + " (" + x1 + ":" + x2 + ")");

        // all pixels includes
        if (x1 == 1 && x2 == len) {
            LOGGER.warn("clip: all");
            return new long[0];
        }

        // no pixels included
        if (x1 > len || x2 < 1) {
            LOGGER.warn("clip: none");
            return null;
        }

        // an actual cutout
        return new long[]{x1, x2};
    }
}
