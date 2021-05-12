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

import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.wcs.Transform;
import ca.nrc.cadc.wcs.exceptions.NoSuchKeywordException;
import ca.nrc.cadc.wcs.exceptions.WCSLibRuntimeException;

import java.util.Arrays;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.header.Standard;
import org.apache.log4j.Logger;
import org.opencadc.fits.CADCExt;


public class EnergyCutout extends FITSCutout<Interval<Number>> {
    private static final Logger LOGGER = Logger.getLogger(EnergyCutout.class);


    public EnergyCutout(final Header header) throws HeaderCardException {
        super(header);
    }

    /**
     * Compute a pixel cutout for the specified bounds. The bounds are assumed to be
     * barycentric wavelength in meters.
     *
     * @param bounds The bounds of the cutout.
     * @return int[2] with the pixel bounds, int[0] if all pixels are included, or
     *          null if no pixels are included
     */
    public long[] getBounds(final Interval<Number> bounds)
            throws NoSuchKeywordException, WCSLibRuntimeException {
        // compute intersection
        final int energyAxis = this.fitsHeaderWCSKeywords.getSpectralAxis();
        final FITSHeaderWCSKeywords spectralWCSKeywords = this.fitsHeaderWCSKeywords;

        if (energyAxis < 0) {
            LOGGER.debug("No energy axis found.");
            return null;
        } else {
            final int naxis = spectralWCSKeywords.getIntValue(Standard.NAXIS.key());
            final Interval<Double> boundsIntervalPixel = getCutoutPixelInterval(bounds, energyAxis, naxis);
            final Interval<Double> nativePixelsInterval =
                    new Interval<>(0.0D, (double) spectralWCSKeywords.getIntValue(
                            Standard.NAXISn.n(energyAxis).key()));
            final Interval<Double> intersectionPixels = getOverlap(nativePixelsInterval, boundsIntervalPixel);

            if (intersectionPixels == null) {
                LOGGER.warn("No overlap.");
                return null;
            } else {
                LOGGER.debug("Overlap is (" + intersectionPixels.getLower() + ", " + intersectionPixels.getUpper()
                             + ")");

                final double low = intersectionPixels.getLower();
                final double up = intersectionPixels.getUpper();

                return clip(spectralWCSKeywords, (long) Math.floor(Math.min(low, up) + 0.5D), (long) Math.ceil(
                        Math.max(low, up) - 0.5D));
            }
        }
    }

    private Interval<Double> getOverlap(final Interval<Double> headerWCSInterval, final Interval<Double> cutoutBounds) {
        if (headerWCSInterval.getLower() > cutoutBounds.getUpper()
            || headerWCSInterval.getUpper() < cutoutBounds.getLower()) {
            return null;
        } else {
            return new Interval<>(Math.max(headerWCSInterval.getLower(), cutoutBounds.getLower()),
                                  Math.min(headerWCSInterval.getUpper(), cutoutBounds.getUpper()));
        }
    }

    Interval<Double> getCutoutPixelInterval(final Interval<Number> bounds, final int energyAxis, final int naxis)
            throws NoSuchKeywordException {
        final double lower = bounds.getLower().doubleValue();
        final double upper = bounds.getUpper().doubleValue();
        final int energyAxisIndex = energyAxis - 1;
        final String unitKey = CADCExt.CUNITn.n(energyAxis).key();
        final EnergyConverter energyConverter = new EnergyConverter();
        final Transform transform = new Transform(this.fitsHeaderWCSKeywords);

        final String unit = this.fitsHeaderWCSKeywords.getStringValue(unitKey);
        final String ctype = this.fitsHeaderWCSKeywords.getStringValue(Standard.CTYPEn.n(energyAxis).key());
        final boolean isVelocity = CoordTypeCode.fromCType(ctype).isVelocity();

        // Lower coordinates of the range.  All zeroes (for default lower range) but for the energy axis
        // calculated value.
        final double[] lowerCoords = new double[naxis];

        // Upper coordinates of the range.
        final double[] upperCoords = new double[naxis];

        if (isVelocity) {
            throw new IllegalArgumentException("Unable to cutout from velocity type (" + ctype + ") using provided "
                                               + "wavelength metres.");
        } else {
            lowerCoords[energyAxisIndex] = energyConverter.fromMetres(lower, unit);
            upperCoords[energyAxisIndex] = energyConverter.fromMetres(upper, unit);
        }

        LOGGER.debug("Getting pixel one for lower coords " + Arrays.toString(lowerCoords));
        final Transform.Result p1 = transform.sky2pix(lowerCoords);

        LOGGER.debug("Getting pixel one for upper coords " + Arrays.toString(upperCoords));
        final Transform.Result p2 = transform.sky2pix(upperCoords);

        final double low = p1.coordinates[energyAxisIndex];
        final double hi = p2.coordinates[energyAxisIndex];

        LOGGER.debug("Calculated pixel values (" + low + "," + hi + ")");

        return new Interval<>(Math.min(low, hi), Math.max(low, hi));
    }

    private long[] clip(final FITSHeaderWCSKeywords energyFITSKeywords, final long lower, final long upper) {
        final long len = energyFITSKeywords.getIntValue(Standard.NAXISn.n(1).key());

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
