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

import java.util.Calendar;
import java.util.Date;

public class DateConverter {
    private static final double FROM_JULIAN_DATE = 2400000.5D;

    public final Date fromMJD(final double modifiedJulianDate) {
        // Julian day
        final double julianDate = Math.floor(modifiedJulianDate) + FROM_JULIAN_DATE;

        // Integer Julian day
        int julianDateInt = (int) Math.floor(julianDate);

        // Fractional part of day
        final double jdf = julianDate - julianDateInt + 0.5D;

        // Really the next calendar day?
        if (jdf >= 1.0) {
            julianDateInt++;
        }

        double fraction = modifiedJulianDate - Math.floor(modifiedJulianDate);
        final double hours = Math.floor(fraction * 24.0D);

        fraction = fraction * 24.0D - hours;

        final double minutes = Math.floor(fraction * 60.0D);
        fraction = fraction * 60.0 - minutes;

        final double seconds = Math.floor(fraction * 60.0D);
        fraction = fraction * 60.0 - seconds;

        final double milliseconds = fraction * 1000.0D;
        double l = julianDateInt + 68569.0D;
        final double n = Math.floor((4 * l) / 146097);

        l = Math.floor(l) - Math.floor((146097 * n + 3) / 4);

        double year = Math.floor((4000 * (l + 1)) / 1461001);

        l = l - Math.floor((1461 * year) / 4) + 31;
        double month = Math.floor((80 * l) / 2447);
        final double day = l - Math.floor((2447 * month) / 80);

        l = Math.floor(month / 11);

        month = Math.floor(month + 2 - 12 * l);
        year = Math.floor(100 * (n - 49) + year + l);

        // Verification step.  Month needs to be zero-based.
        final Calendar calendar = Calendar.getInstance(DateUtil.UTC);
        calendar.set((int) year, (int) (month - 1.0D), (int) day, (int) hours, (int) minutes, (int) seconds);
        calendar.set(Calendar.MILLISECOND, (int) milliseconds);

        return calendar.getTime();
    }
}
