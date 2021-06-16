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

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.dali.Range;
import ca.nrc.cadc.dali.Shape;
import ca.nrc.cadc.wcs.exceptions.NoSuchKeywordException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import org.apache.log4j.Logger;
import org.opencadc.soda.PixelRange;
import org.opencadc.soda.server.Cutout;


/**
 * Utility class to provide bounds for the given WCS Cutout.  In the unlikely event that multiple cutouts are specified,
 * only one will be honoured, with priority given to POSITION, then BAND, then TIME, and finally POLARIZATION.
 * Pixel cutouts are handled outside of this class.
 */
public class WCSCutoutUtil {
    private static final Logger LOGGER = Logger.getLogger(WCSCutoutUtil.class);

    /**
     * Calculate the pixel ranges that the set of WCS cutouts will produce for the HDU that the given Header represents.
     * @param header        The Header to check.
     * @param cutout        The Cutout specifications.
     * @return  Array of PixelRange objects, or empty if no overlap.  Never null.
     * @throws NoSuchKeywordException Unknown keyword found in Header.
     * @throws HeaderCardException    If a FITS Header card couldn't be read.
     */
    public static PixelRange[] getBounds(final Header header, final Cutout cutout)
            throws HeaderCardException, NoSuchKeywordException {
        final List<PixelRange> allPixelRanges = new ArrayList<>();

        if (cutout.pos != null) {
            final PixelRange[] spatialPixelRanges = WCSCutoutUtil.getSpatialBounds(header, cutout.pos);
            if (spatialPixelRanges != null) {
                WCSCutoutUtil.merge(spatialPixelRanges, allPixelRanges);
            }
        }

        if (cutout.band != null) {
            final PixelRange[] spectralPixelRanges = WCSCutoutUtil.getSpectralBounds(header, cutout.band);
            if (spectralPixelRanges != null) {
                WCSCutoutUtil.merge(spectralPixelRanges, allPixelRanges);
            }
        }

        if (cutout.time != null) {
            final PixelRange[] temporalPixelRanges = WCSCutoutUtil.getTemporalBounds(header, cutout.time);
            if (temporalPixelRanges != null) {
                WCSCutoutUtil.merge(temporalPixelRanges, allPixelRanges);
            }
        }

        if (cutout.pol != null) {
            final PixelRange[] polarizationPixelRanges = WCSCutoutUtil.getPolarizationBounds(header, cutout.pol);
            if (polarizationPixelRanges != null) {
                WCSCutoutUtil.merge(polarizationPixelRanges, allPixelRanges);
            }
        }

        return allPixelRanges.toArray(new PixelRange[0]);
    }

    static PixelRange[] getSpatialBounds(final Header header, final Shape shape)
            throws HeaderCardException, NoSuchKeywordException {
        final long[] bounds;
        if (shape instanceof Circle) {
            bounds = new CircleCutout(header).getBounds((Circle) shape);
        } else if (shape instanceof Polygon) {
            bounds = new PolygonCutout(header).getBounds((Polygon) shape);
        } else if (shape instanceof Range) {
            bounds = new RangeCutout(header).getBounds((Range) shape);
        } else {
            bounds = null;
        }

        return WCSCutoutUtil.toPixelRanges(bounds);
    }

    static PixelRange[] getSpectralBounds(final Header header, final Interval<Number> spectralInterval)
            throws HeaderCardException, NoSuchKeywordException {
        return WCSCutoutUtil.toPixelRanges(new EnergyCutout(header).getBounds(spectralInterval));
    }

    static PixelRange[] getTemporalBounds(final Header header, final Interval<Number> temporalInterval)
            throws HeaderCardException {
        throw new UnsupportedOperationException("Temporal not yet implemented.");
    }

    static PixelRange[] getPolarizationBounds(final Header header, final List<String> polarizationStates)
            throws HeaderCardException {
        throw new UnsupportedOperationException("Polarization not yet implemented.");
    }

    /**
     * Merge in the given pixelCutoutRanges array into the entire list of PixelRange objects.  This is to support
     * multiple cutouts in a single HDU (Header), but along different axes.
     * @param pixelCutoutRanges         The NAXIS-length array of PixelRange objects.
     * @param completePixelRangeList    The List of PixelRange objects so far.  Can be empty but not null.
     */
    static void merge(final PixelRange[] pixelCutoutRanges, final List<PixelRange> completePixelRangeList) {
        if (completePixelRangeList.isEmpty()) {
            completePixelRangeList.addAll(Arrays.asList(pixelCutoutRanges));
        } else {
            final int pixelCutoutRangesLength = pixelCutoutRanges.length;
            for (int i = 0; i < pixelCutoutRangesLength; i++) {
                final PixelRange pixelCutoutRange = pixelCutoutRanges[i];
                final PixelRange pixelRange = completePixelRangeList.get(i);

                if (!pixelCutoutRange.equals(pixelRange)) {
                    final PixelRange mergedPixelRange =
                            new PixelRange(Math.max(pixelCutoutRange.lowerBound, pixelRange.lowerBound),
                                           Math.min(pixelCutoutRange.upperBound, pixelRange.upperBound));
                    completePixelRangeList.set(i, mergedPixelRange);
                }
            }
        }
    }

    static PixelRange[] toPixelRanges(final long[] pixelBounds) {
        LOGGER.debug("toPixelRanges from bounds " + Arrays.toString(pixelBounds));
        if (pixelBounds == null) {
            return null;
        } else {
            final PixelRange[] pixelRanges = new PixelRange[pixelBounds.length / 2];
            for (int i = 0; i < pixelBounds.length; i += 2) {
                pixelRanges[i / 2] = new PixelRange((int) pixelBounds[i], (int) pixelBounds[i + 1]);
            }
            return pixelRanges;
        }
    }
}
