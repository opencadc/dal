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
import ca.nrc.cadc.wcs.WCSKeywords;
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
     *      null if no pixels are included
     */
    public long[] getBounds(final Interval<Number> bounds)
            throws HeaderCardException, NoSuchKeywordException, WCSLibRuntimeException {
        // compute intersection
        final Interval<Double> boundsInterval = getCutoutInterval(bounds);
        final Interval<Double> nativeMetresInterval = toInterval();
        final int energyAxis = this.fitsHeaderWCSKeywords.getSpectralAxis();

        if (energyAxis < 0) {
            LOGGER.debug("No energy axis found.");
            return null;
        } else {
            final int naxis = this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXIS.key());
            final int energyAxisIndex = energyAxis - 1;
            final String cType = this.fitsHeaderWCSKeywords.getStringValue(Standard.CTYPEn.n(energyAxis).key());
            final Interval<Double> intersectionMetres = getOverlap(nativeMetresInterval, boundsInterval);

            if (intersectionMetres == null) {
                LOGGER.warn("No overlap.");
                return null;
            } else {
                LOGGER.debug(
                        "Overlap is (" + intersectionMetres.getLower() + ", " + intersectionMetres.getUpper() + ")");

                Transform transform = new Transform(this.fitsHeaderWCSKeywords);
                final FITSHeaderWCSKeywords transWCSKeywords;
                LOGGER.debug("Transform is " + transform);

                if (!cType.startsWith(EnergyConverter.CORE_CTYPE)) {
                    transWCSKeywords = new FITSHeaderWCSKeywords(transform.translate(EnergyConverter.CORE_CTYPE + "-???"));
                    transform = new Transform(transWCSKeywords);
                    LOGGER.debug("Translate OK:\n" + transform);
                } else {
                    transWCSKeywords = this.fitsHeaderWCSKeywords;
                }

                // Lower coordinates of the range.  All zeroes (for default lower range) but for the energy axis
                // calculated value.
                final double[] lowerCoords = new double[naxis];
                lowerCoords[energyAxisIndex] = intersectionMetres.getLower();

                LOGGER.debug("Getting pixel one for lower coords " + Arrays.toString(lowerCoords));

                final Transform.Result p1 = transform.sky2pix(lowerCoords);
                LOGGER.debug(
                        "getBounds: sky2pix " + intersectionMetres.getLower() + " -> " + p1.coordinates[energyAxisIndex]
                        + " "
                        + p1.units[energyAxisIndex]);

                final double[] upperCoords = new double[naxis];
                for (int i = 0; i < naxis; i++) {
                    upperCoords[i] = transWCSKeywords.getDoubleValue(Standard.NAXISn.n(i + 1).key());
                }
                upperCoords[energyAxisIndex] = intersectionMetres.getUpper();

                LOGGER.debug("Getting pixel two for upper coords " + Arrays.toString(upperCoords));

                final Transform.Result p2 = transform.sky2pix(upperCoords);
                LOGGER.debug(
                        "getBounds: sky2pix " + intersectionMetres.getUpper() + " -> " + p2.coordinates[energyAxisIndex]
                        + " "
                        + p2.units[energyAxisIndex]);

                // values can be inverted if WCS is in freq or energy instead of wavelength
                final double low = p1.coordinates[energyAxisIndex];
                final double up = p2.coordinates[energyAxisIndex];

                return clip((long) Math.floor(Math.min(low, up) + 0.5D), (long) Math.ceil(Math.max(low, up) - 0.5D));
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

    Interval<Double> getCutoutInterval(final Interval<Number> bounds) {
        double lower = bounds.getLower().doubleValue();
        double upper = bounds.getUpper().doubleValue();

        return new Interval<>(Math.min(lower, upper), Math.max(lower, upper));
    }

    Interval<Double> toInterval() throws HeaderCardException, NoSuchKeywordException {
        final int energyAxis = this.fitsHeaderWCSKeywords.getSpectralAxis();
        if (energyAxis > 0) {
            final String cType = this.fitsHeaderWCSKeywords.getStringValue(Standard.CTYPEn.n(energyAxis).key());
            final int energyAxisIndex = energyAxis - 1;

            // Translate the NAXIS (min/max) values into sky values to be used to get an intersection
            Transform transform = new Transform(this.fitsHeaderWCSKeywords);

            final FITSHeaderWCSKeywords intervalWCSKeywords;

            if (!cType.startsWith(EnergyConverter.CORE_CTYPE)) {
                final WCSKeywords translatedKeywords = transform.translate(EnergyConverter.CORE_CTYPE + "-???");
                intervalWCSKeywords = new FITSHeaderWCSKeywords(translatedKeywords);
                transform = new Transform(intervalWCSKeywords);
            } else {
                intervalWCSKeywords = this.fitsHeaderWCSKeywords;
            }

            final int naxis = intervalWCSKeywords.getIntValue(Standard.NAXIS.key());
            final double energyAxisValue = intervalWCSKeywords.getDoubleValue(Standard.NAXISn.n(energyAxis).key());
            final double[] coords1 = new double[naxis];
            coords1[energyAxisIndex] = 0.5D;

            final double[] coords2 = new double[naxis];
            coords2[energyAxisIndex] = energyAxisValue + 0.5D;

            LOGGER.debug("Translating coords1 from " + Arrays.toString(coords1));
            LOGGER.debug("Translating coords2 from " + Arrays.toString(coords2));

            final Transform.Result start = transform.pix2sky(coords1);
            final Transform.Result end = transform.pix2sky(coords2);

            LOGGER.debug("Transformation Start: " + Arrays.toString(start.coordinates) + " - ("
                         + Arrays.toString(start.units) + ")");
            LOGGER.debug("Transformation End: " + Arrays.toString(end.coordinates) + " - (" + Arrays.toString(end.units)
                         + ")");

            // wcslib convert to WAVE-??? but units might be a multiple of EnergyConverter.CORE_UNIT
            final String cUnit = start.units[energyAxisIndex]; // Assume same as end.units[energyAxisIndex].
            final double restFreq = intervalWCSKeywords.getDoubleValue(CADCExt.RESTFRQ.key());
            final double restWav = intervalWCSKeywords.getDoubleValue(CADCExt.RESTWAV.key());

            LOGGER.debug("CUNIT: " + cUnit);
            LOGGER.debug("CTYPE: " + cType);
            LOGGER.debug("RESTFRQ: " + restFreq);
            LOGGER.debug("RESTWAV: " + restWav);

            double a = start.coordinates[energyAxisIndex];
            double b = end.coordinates[energyAxisIndex];

            final Interval<Double> result = new Interval<>(Math.min(a, b), Math.max(a, b));
            LOGGER.debug("Computed Header interval in m (" + result.getLower() + ", " + result.getUpper() + ")");
            return result;
        } else {
            return null;
        }
    }

    private long[] clip(final long lower, final long upper) {
        final long len = this.fitsHeaderWCSKeywords.getIntValue(
                Standard.NAXISn.n(fitsHeaderWCSKeywords.getSpectralAxis()).key());

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
