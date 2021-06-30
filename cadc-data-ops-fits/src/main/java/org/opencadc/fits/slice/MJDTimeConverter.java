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

import org.apache.log4j.Logger;

/**
 * Conversions to MJD from other formats.
 */
public class MJDTimeConverter {
    private static final Logger LOGGER = Logger.getLogger(MJDTimeConverter.class);

    public static final String DEFAULT_TIME_UNIT = "s";

    private static final double FROM_JULIAN_DATE = 2400000.5D;
    private static final double TWENTY_FOUR_HOURS_MS = 86400000.0D;


    /**
     * Convert Julian to MJD.
     * @param julianDate    The Julian date.
     * @return  MJD double
     */
    public final double fromJulianDate(final double julianDate) {
        return julianDate - FROM_JULIAN_DATE;
    }

    /**
     * Convert the given Date in ISO-8601 format (yyyy-MM-dd'T'HH:mm:ss) in the given time zone to MJD.
     * @param isoDate       The Date to convert to MJD
     * @param timeZone      The time zone the date is in to match.
     * @return      MJD double value
     */
    public final double fromISODate(final Date isoDate, final TimeZone timeZone) {
        final Calendar calendar = Calendar.getInstance(timeZone);
        calendar.setTime(isoDate);

        final double offsetHours = -(calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET))
                                   / (60.0D * 1000.0D);
        LOGGER.debug("Offset hours are " + offsetHours);
        return fromJulianDate((isoDate.getTime() / TWENTY_FOUR_HOURS_MS) - (offsetHours / 1440.0D) + 2440587.5D);
    }

    /**
     * Convert the given date string from the given time system to MJD.
     * @param timeSystem    The Time System to convert from.
     * @param dateString    The date string.
     * @return  MJD double
     * @throws ParseException   If the date string is invalid.
     */
    public final double fromISODate(final String timeSystem, final String dateString) throws ParseException {
        final TimeZone timeZone = TimeZone.getTimeZone(timeSystem);
        LOGGER.debug("fromISODate -> " + timeSystem + " (" + timeZone + ")");
        final Date iso8601Date = DateUtil.getDateFormat(DateUtil.ISO8601_DATE_FORMAT_LOCAL, timeZone).parse(dateString);
        return fromISODate(iso8601Date, timeZone);
    }
}
