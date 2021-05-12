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
import ca.nrc.cadc.wcs.exceptions.WCSLibRuntimeException;

import java.text.ParseException;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.header.Standard;
import org.apache.log4j.Logger;



/**
 * Time cutout.  The Temporal Axis is determined from the WCSKeywords, and CRPIXn, CRVALn, and CDELTn are required
 * for that axis.
 */
public class TimeCutout extends FITSCutout<Interval<Number>> {
    private static final Logger LOGGER = Logger.getLogger(TimeCutout.class);

    private final TimeHeaderWCSKeywords timeHeaderWCSKeywords;


    public TimeCutout(final Header header) throws HeaderCardException {
        super(header);
        this.timeHeaderWCSKeywords = new TimeHeaderWCSKeywords(header);
    }

    public TimeCutout(final FITSHeaderWCSKeywords fitsHeaderWCSKeywords) {
        super(fitsHeaderWCSKeywords);
        this.timeHeaderWCSKeywords = new TimeHeaderWCSKeywords(fitsHeaderWCSKeywords);
    }


    /**
     * Obtain the bounds of the given cutout.
     *
     * @param cutoutBound The interval bounds of the cutout in MJD.
     * @return long[] array of overlapping bounds, or long[0] if all pixels are included.
     * @throws WCSLibRuntimeException WCSLib (C) error.
     */
    @Override
    public long[] getBounds(final Interval<Number> cutoutBound) throws WCSLibRuntimeException {
        final int timeAxis = this.fitsHeaderWCSKeywords.getTemporalAxis();
        try {
            final Interval<Double> headerSecondsInterval = toSecondsInterval();
            final Interval<Double> cutoutSecondsInterval = getCutoutSecondsInterval(cutoutBound);
            final Interval<Double> overlapSeconds = getOverlap(headerSecondsInterval, cutoutSecondsInterval);

            if (overlapSeconds == null) {
                LOGGER.debug("No overlap.");
                return null;
            }

            LOGGER.debug("Found overlap (" + overlapSeconds.getLower() + ", " + overlapSeconds.getUpper() + ")");

            final double d1 = val2pix(timeAxis, overlapSeconds.getLower());
            final double d2 = val2pix(timeAxis, overlapSeconds.getUpper());

            final double padding = 0.0D;
            final long x1 = (long) Math.floor(Math.min(d1, d2) + padding);
            final long x2 = (long) Math.ceil(Math.max(d1, d2) - padding);

            LOGGER.debug("Pixel overlap: (" + x1 + ", " + x2 + ")");

            return clip(x1, x2);
        } catch (ParseException parseException) {
            throw new IllegalArgumentException(parseException.getMessage(), parseException);
        }
    }

    Interval<Double> getCutoutSecondsInterval(final Interval<Number> bounds) throws ParseException {
        final SecondsConverter secondsConverter = new SecondsConverter();
        final double mjdRef = this.timeHeaderWCSKeywords.getMJDRef();
        final double daysLower = bounds.getLower().doubleValue() - mjdRef;
        final double daysUpper = bounds.getUpper().doubleValue() - mjdRef;
        final double secondsLower = secondsConverter.from(daysLower, "d");
        final double secondsUpper = secondsConverter.from(daysUpper, "d");

        return new Interval<>(Math.min(secondsLower, secondsUpper), Math.max(secondsLower, secondsUpper));
    }

    private Interval<Double> getOverlap(final Interval<Double> headerPixelInterval,
                                        final Interval<Double> cutoutPixelInterval) {
        LOGGER.debug("Checking if (" + cutoutPixelInterval.getLower() + ","
                     + cutoutPixelInterval.getUpper() + ") overlaps in header (" + headerPixelInterval.getLower() + ","
                     + headerPixelInterval.getUpper() + ")");
        if (headerPixelInterval.getLower() > cutoutPixelInterval.getUpper()
            || headerPixelInterval.getUpper() < cutoutPixelInterval.getLower()) {
            return null;
        } else {
            return new Interval<>(Math.max(headerPixelInterval.getLower(), cutoutPixelInterval.getLower()),
                                  Math.min(headerPixelInterval.getUpper(), cutoutPixelInterval.getUpper()));
        }
    }


    /**
     * Turn the Temporal Axis's into an Interval.  Expect an IllegalArgumentException if the Header is not properly
     * setup for a cutout.
     * @return  Interval&lt;Double&gt;.  Never null.
     */
    Interval<Double> toSecondsInterval() throws ParseException {
        final int timeAxis = this.fitsHeaderWCSKeywords.getTemporalAxis();

        if (!this.fitsHeaderWCSKeywords.containsKey(Standard.CDELTn.n(timeAxis).key()) || timeAxis < 0L) {
            throw new IllegalArgumentException("Invalid Time WCS: No time axis or delta = 0.0");
        }

        final double refVal = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CRVALn.n(timeAxis).key());
        final double refCDelt = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CDELTn.n(timeAxis).key());
        final String refUnit = this.timeHeaderWCSKeywords.getUnit();
        final SecondsConverter secondsConverter = new SecondsConverter();
        final double upperSeconds = secondsConverter.from(refVal + refCDelt, refUnit);

        final Interval<Double> returnInterval = new Interval<>(0.0D, upperSeconds);
        LOGGER.debug("Header interval seconds is (" + returnInterval.getLower() + "," + returnInterval.getUpper()
                     + ")");

        return returnInterval;
    }

    private double val2pix(final int timeAxis, final double secondsValue) throws ParseException {
        final double refPix = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CRPIXn.n(timeAxis).key());
        final double refVal = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CRVALn.n(timeAxis).key());
        final double refCDelt = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CDELTn.n(timeAxis).key());
        final String refUnit = this.timeHeaderWCSKeywords.getUnit();

        final SecondsConverter secondsConverter = new SecondsConverter();
        final double headerSeconds = secondsConverter.from(refVal + refCDelt, refUnit);

        return refPix + (secondsValue - headerSeconds) / refCDelt;
    }

    private long[] clip(final long lower, final long upper) {
        final long len = this.fitsHeaderWCSKeywords.getIntValue(
                Standard.NAXISn.n(fitsHeaderWCSKeywords.getTemporalAxis()).key());

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
