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

import ca.nrc.cadc.date.DateUtil;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.header.DateTime;
import org.apache.log4j.Logger;
import org.opencadc.fits.CADCExt;


/**
 * Class to sanitize and obtain time specific values.
 */
public class TimeHeaderWCSKeywords {
    private static final Logger LOGGER = Logger.getLogger(TimeHeaderWCSKeywords.class);
    private static final String DEFAULT_TIME_SYSTEM = "UTC";
    private static final String DEFAULT_TIME_UNIT = "s";

    private final FITSHeaderWCSKeywords fitsHeaderWCSKeywords;


    public TimeHeaderWCSKeywords(final FITSHeaderWCSKeywords fitsHeaderWCSKeywords) {
        this.fitsHeaderWCSKeywords = fitsHeaderWCSKeywords;
    }

    public TimeHeaderWCSKeywords(final Header header) throws HeaderCardException {
        this.fitsHeaderWCSKeywords = new FITSHeaderWCSKeywords(header);
    }


    /**
     * Parse out the MJD Reference value from the header.
     *
     * @return double MJD ref value.  Returns 0.0 if none exists.
     * @throws ParseException If the DATEREF card is set and is not in ISO-8601 format.
     */
    public double getMJDRef() throws ParseException {
        final double mjdRef;
        if (this.fitsHeaderWCSKeywords.containsKey(CADCExt.MJDREFI.key())
            && this.fitsHeaderWCSKeywords.containsKey(CADCExt.MJDREFF.key())) {
            mjdRef = this.fitsHeaderWCSKeywords.getIntValue(CADCExt.MJDREFI.key())
                     + this.fitsHeaderWCSKeywords.getDoubleValue(CADCExt.MJDREFF.key());
        } else if (this.fitsHeaderWCSKeywords.containsKey(DateTime.MJDREF.key())) {
            mjdRef = this.fitsHeaderWCSKeywords.getDoubleValue(DateTime.MJDREF.key());
        } else if (this.fitsHeaderWCSKeywords.containsKey(CADCExt.JDREFI.key())
                   && this.fitsHeaderWCSKeywords.containsKey(CADCExt.JDREFF.key())) {
            final double jdRef = this.fitsHeaderWCSKeywords.getIntValue(CADCExt.JDREFI.key())
                                 + this.fitsHeaderWCSKeywords.getDoubleValue(CADCExt.JDREFF.key());
            mjdRef = DateUtil.toModifiedJulianDate(jdRef);
        } else if (this.fitsHeaderWCSKeywords.containsKey(DateTime.JDREF.key())) {
            mjdRef = DateUtil.toModifiedJulianDate(this.fitsHeaderWCSKeywords.getDoubleValue(DateTime.JDREF.key()));
        } else if (this.fitsHeaderWCSKeywords.containsKey(DateTime.DATEREF.key())) {
            LOGGER.debug("Using DATEREF: " + this.fitsHeaderWCSKeywords.getStringValue(DateTime.DATEREF.key()));
            final String timeSystem = getSystem();
            final TimeZone timeZone = TimeZone.getTimeZone(timeSystem);
            final Date date = DateUtil.getDateFormat(DateUtil.ISO8601_DATE_FORMAT_LOCAL, timeZone).parse(
                    this.fitsHeaderWCSKeywords.getStringValue(DateTime.DATEREF.key()));
            mjdRef = DateUtil.toModifiedJulianDate(date, timeZone);
        } else {
            mjdRef = 0.0D;
        }

        LOGGER.debug("MJD Reference: " + mjdRef);

        return mjdRef;
    }

    public double getMJDStart() throws ParseException {
        final double startMJD;

        if (this.fitsHeaderWCSKeywords.containsKey(DateTime.MJD_BEG.key())) {
            startMJD = this.fitsHeaderWCSKeywords.getDoubleValue(DateTime.MJD_BEG.key());
        } else if (this.fitsHeaderWCSKeywords.containsKey(DateTime.MJD_OBS.key())) {
            startMJD = this.fitsHeaderWCSKeywords.getDoubleValue(DateTime.MJD_OBS.key());
        } else if (this.fitsHeaderWCSKeywords.containsKey(CADCExt.JDBEG.key())) {
            startMJD = DateUtil.toModifiedJulianDate(
                    this.fitsHeaderWCSKeywords.getDoubleValue(CADCExt.JDBEG.key()));
        } else if (this.fitsHeaderWCSKeywords.containsKey(CADCExt.JDOBS.key())) {
            startMJD = DateUtil.toModifiedJulianDate(
                    this.fitsHeaderWCSKeywords.getDoubleValue(CADCExt.JDOBS.key()));
        } else if (this.fitsHeaderWCSKeywords.containsKey(DateTime.DATE_OBS.key())) {
            final String timeSystem = getSystem();
            final TimeZone timeZone = TimeZone.getTimeZone(timeSystem);
            final Date date = DateUtil.getDateFormat(DateUtil.ISO8601_DATE_FORMAT_LOCAL, timeZone).parse(
                    this.fitsHeaderWCSKeywords.getStringValue(DateTime.DATE_OBS.key()));
            startMJD = DateUtil.toModifiedJulianDate(date, timeZone);
        } else if (this.fitsHeaderWCSKeywords.containsKey(DateTime.DATE_BEG.key())) {
            final String timeSystem = getSystem();
            final TimeZone timeZone = TimeZone.getTimeZone(timeSystem);
            final Date date = DateUtil.getDateFormat(DateUtil.ISO8601_DATE_FORMAT_LOCAL, timeZone).parse(
                    this.fitsHeaderWCSKeywords.getStringValue(DateTime.DATE_BEG.key()));
            startMJD = DateUtil.toModifiedJulianDate(date, timeZone);
        } else if (this.fitsHeaderWCSKeywords.containsKey(CADCExt.TSTART.key())) {
            final String unit = getUnit();
            final double mjdRef = getMJDRef();
            startMJD = addToMJD(mjdRef, this.fitsHeaderWCSKeywords.getDoubleValue(CADCExt.TSTART.key()), unit);
        } else {
            throw new IllegalStateException("No start time available in Header.");
        }

        LOGGER.debug("Start MJD: " + startMJD);

        return startMJD;
    }

    private double addToMJD(final double mjdValue, final double crval, final String unit) {
        final double seconds = DateUtil.toSeconds(crval, unit);
        final Date date = DateUtil.fromModifiedJulianDate(mjdValue);

        final Calendar calendar = Calendar.getInstance(DateUtil.UTC);
        calendar.setTime(date);
        calendar.add(Calendar.MILLISECOND, (int) Math.round(seconds * 1000.0D));

        return DateUtil.toModifiedJulianDate(calendar.getTime(), DateUtil.UTC);
    }

    private String getSystem() {
        return this.fitsHeaderWCSKeywords.containsKey(DateTime.TIMESYS.key())
               ? this.fitsHeaderWCSKeywords.getStringValue(DateTime.TIMESYS.key()) : DEFAULT_TIME_SYSTEM;
    }

    public String getUnit() {
        final String unit;
        final int temporalAxis = this.fitsHeaderWCSKeywords.getTemporalAxis();

        final String cUnitKey = temporalAxis < 0 ? null : CADCExt.CUNITn.n(temporalAxis).key();

        if (this.fitsHeaderWCSKeywords.containsKey(DateTime.TIMEUNIT.key())) {
            unit = this.fitsHeaderWCSKeywords.getStringValue(DateTime.TIMEUNIT.key());
        } else if (cUnitKey != null && this.fitsHeaderWCSKeywords.containsKey(cUnitKey)) {
            unit = this.fitsHeaderWCSKeywords.getStringValue(cUnitKey);
        } else {
            unit = DEFAULT_TIME_UNIT;
        }

        return unit;
    }
}
