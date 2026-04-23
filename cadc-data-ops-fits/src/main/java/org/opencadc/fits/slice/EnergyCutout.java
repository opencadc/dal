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

import ca.nrc.cadc.dali.EnergyConverter;
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


/**
 * Provide the cutout bounds for the given Header.  This class is executed after the inputs are parsed into a
 * representative Interval object that can be used to obtain an array of bounding pixels.
 */
public class EnergyCutout extends FITSCutout<Interval<Number>> {
    private static final Logger LOGGER = Logger.getLogger(EnergyCutout.class);


    public EnergyCutout(final Header header) throws HeaderCardException {
        super(header);
    }

    /**
     * Implementors can override this to further process the Header to accommodate different cutout types.  Leave empty
     * if no further processing needs to be done.
     * This class overrides to remove the PV matrix as it produces erroneous results.
     *
     * @param header The Header to modify.
     */
    @Override
    protected void postProcess(final Header header) {
        final int naxis = header.getIntValue(Standard.NAXIS);

        for (int x = 1; x <= naxis; x++) {
            for (int y = 1; y <= naxis; y++) {
                final String pvMatrixKey = String.format("PV%d_%d", x, y);
                header.deleteKey(pvMatrixKey);
            }
        }
    }

    /**
     * Compute a pixel cutout for the specified bounds. The bounds are assumed to be
     * barycentric wavelength in meters.
     *
     * @param bounds The bounds of the cutout.
     * @return long[NAXIS] with the pixel bounds, long[0] if all pixels are included, or
     *          null if no pixels are included
     */
    @Override
    public long[] getBounds(final Interval<Number> bounds) throws NoSuchKeywordException, WCSLibRuntimeException {
        // compute intersection
        final int energyAxis = this.fitsHeaderWCSKeywords.getSpectralAxis();
        final FITSHeaderWCSKeywords spectralWCSKeywords = this.fitsHeaderWCSKeywords;

        if (energyAxis < 0) {
            LOGGER.debug("No energy axis found.");
            return null;
        } else {
            final int naxis = spectralWCSKeywords.getIntValue(Standard.NAXIS.key());
            final String ctype = spectralWCSKeywords.getStringValue(Standard.CTYPEn.n(energyAxis).key());
            final boolean isVelocity = CoordTypeCode.fromCType(ctype).isVelocity();
            // Intersect user-requested wavelength (m) with the spectral range covered by the data at pixels 1..N
            // so that extended / infinite physical bounds (e.g. -Inf) map to a finite WCS call instead of
            // sky2pix endpoints that miss the field of view in pixel space.
            final Interval<Number> requestMetres = isVelocity ? bounds : clampWavelengthToSpectralFieldOfView(bounds, energyAxis);
            if (requestMetres == null) {
                LOGGER.warn("No overlap with spectral extent on data.");
                return null;
            }
            final int nchan = spectralWCSKeywords.getIntValue(Standard.NAXISn.n(energyAxis).key());
            final Interval<Double> boundsIntervalPixel = getCutoutPixelInterval(requestMetres, energyAxis, naxis);
            // FITS pixel indices 1..N; clip() in FITSCutout is 1-based to len inclusive.
            final Interval<Double> nativePixelsInterval = new Interval<>(1.0D, (double) nchan);
            final Interval<Double> intersectionPixels = getOverlap(nativePixelsInterval, boundsIntervalPixel);

            if (intersectionPixels == null) {
                LOGGER.warn("No overlap.");
                return null;
            } else {
                final double low = intersectionPixels.getLower();
                final double up = intersectionPixels.getUpper();
                final long maxSpectralLength =
                        this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXISn.n(energyAxis).key());

                final long[] clippedSpectralBounds =
                        clip(maxSpectralLength, (long) Math.floor(Math.min(low, up) + 0.5D),
                             (long) Math.ceil(Math.max(low, up) - 0.5D));

                final int axes = clippedSpectralBounds == null ? 0 : naxis * 2;
                return AxisBoundsFiller.fill(axes, clippedSpectralBounds, energyAxis,
                                             AxisBoundsFiller.naxisSizes(spectralWCSKeywords, naxis));
            }
        }
    }

    private Interval<Double> getOverlap(final Interval<Double> a, final Interval<Double> b) {
        final double lo = Math.max(a.getLower(), b.getLower());
        final double hi = Math.min(a.getUpper(), b.getUpper());
        LOGGER.debug("Pixel interval intersection (" + a.getLower() + ", " + a.getUpper() + ") with ("
                     + b.getLower() + ", " + b.getUpper() + ") -> (" + lo + ", " + hi + ")");
        if (lo > hi) {
            return null;
        }
        return new Interval<>(lo, hi);
    }

    /**
     * Wavelength in metres (barycentric) for channels 1 and nchan, using a linear WCS in the native
     * spectral unit (CUNIT) at the reference pixel (same approximation as a simple 1D grid).
     * Returns the ordered pair [min, max] in metres.
     */
    private double[] spectralWavelengthMetresAtBandEdges(final int energyAxis) {
        final int nchan = this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXISn.n(energyAxis).key());
        final double crval = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CRVALn.n(energyAxis).key());
        final double cdelt = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CDELTn.n(energyAxis).key(), 0.0D);
        final double crpix = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CRPIXn.n(energyAxis).key());
        final String cunit = this.fitsHeaderWCSKeywords.getStringValue(CADCExt.CUNITn.n(energyAxis).key());
        final double world1 = crval + cdelt * (1.0D - crpix);
        final double worldN = crval + cdelt * ((double) nchan - crpix);
        final EnergyConverter energyConverter = new EnergyConverter();
        final double m1 = energyConverter.toMeters(world1, cunit);
        final double m2 = energyConverter.toMeters(worldN, cunit);
        return new double[]{Math.min(m1, m2), Math.max(m1, m2)};
    }

    /**
     * Intersect the requested barycentric wavelength range (m) with the [min,max] in metres of the
     * spectral band on the data (linear in native unit at pixels 1 and nchan). Handles ±Infinity on
     * the request via {@code max}/{@code min} with the band; returns null if there is no intersection.
     */
    private Interval<Number> clampWavelengthToSpectralFieldOfView(final Interval<Number> bounds, final int energyAxis) {
        final double wmin = bounds.getLower().doubleValue();
        final double wmax = bounds.getUpper().doubleValue();
        final double rlo = Math.min(wmin, wmax);
        final double rhi = Math.max(wmin, wmax);
        final double[] m = spectralWavelengthMetresAtBandEdges(energyAxis);
        final double mmin = m[0];
        final double mmax = m[1];
        // max(rlo, mmin) lets -Inf select the in-band start; min(rhi, mmax) clips a request that runs past the band.
        final double cLo = Math.max(rlo, mmin);
        final double cHi = Math.min(rhi, mmax);
        LOGGER.debug("Spectral (m) request [" + rlo + ", " + rhi + "] with header band ~[" + mmin + ", " + mmax
                     + "] -> [" + cLo + ", " + cHi + "]");
        if (cLo > cHi) {
            return null;
        }
        return new Interval<>(cLo, cHi);
    }

    /**
     * Produce an Interval of pixel values as converted from the given bounds.
     * @param bounds            The user-input bounds in metres.
     * @param energyAxis        The energy axis.
     * @param naxis             The number of axes.
     * @return  Interval of double pixels.  Never null.
     * @throws NoSuchKeywordException   If there is no NAXIS keyword.
     */
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

        LOGGER.debug("Bounds in metres (" + lower + ", " + upper + ")");

        if (isVelocity) {
            throw new IllegalArgumentException("Unable to cutout from velocity type (" + ctype + ") using provided "
                                               + "wavelength metres.");
        } else {
            lowerCoords[energyAxisIndex] = energyConverter.fromMetres(lower, unit);
            upperCoords[energyAxisIndex] = energyConverter.fromMetres(upper, unit);
        }

        // To convert from Sky coordinate values to pixel values, using just 0.0 for the longitude and latitude
        // is invalid.  We need to set them to the CRVALn values.
        final int longitudeAxis = this.fitsHeaderWCSKeywords.getSpatialLongitudeAxis();
        final int latitudeAxis = this.fitsHeaderWCSKeywords.getSpatialLatitudeAxis();

        if (longitudeAxis > 0 && latitudeAxis > 0) {
            final double longitudeReferenceVal =
                    this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CRVALn.n(longitudeAxis).key());
            if (longitudeReferenceVal > 0.0D) {
                lowerCoords[longitudeAxis - 1] = longitudeReferenceVal;
                upperCoords[longitudeAxis - 1] = longitudeReferenceVal;
            }

            final double latitudeReferenceVal =
                    this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CRVALn.n(latitudeAxis).key());
            if (latitudeReferenceVal > 0.0D) {
                lowerCoords[latitudeAxis - 1] = latitudeReferenceVal;
                upperCoords[latitudeAxis - 1] = latitudeReferenceVal;
            }
        }

        LOGGER.debug("Getting pixel value for lower coords " + Arrays.toString(lowerCoords));
        final Transform.Result p1 = transform.sky2pix(lowerCoords);

        LOGGER.debug("Getting pixel value for upper coords " + Arrays.toString(upperCoords));
        final Transform.Result p2 = transform.sky2pix(upperCoords);

        final double low = p1.coordinates[energyAxisIndex];
        final double hi = p2.coordinates[energyAxisIndex];

        LOGGER.debug("Calculated pixel values (" + low + "," + hi + ")");

        return new Interval<>(Math.min(low, hi), Math.max(low, hi));
    }
}
