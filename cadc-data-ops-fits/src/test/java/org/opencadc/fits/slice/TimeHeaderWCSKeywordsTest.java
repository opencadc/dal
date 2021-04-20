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
import nom.tam.fits.Header;
import nom.tam.fits.header.Standard;
import nom.tam.fits.header.extra.NOAOExt;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.fits.CADCExt;

import java.util.Calendar;
import java.util.Date;


public class TimeHeaderWCSKeywordsTest {
    @Test
    public void testMJDRef() throws Exception {
        final Header testHeader = new Header();
        final Calendar calendar = Calendar.getInstance(DateUtil.UTC);
        calendar.set(2007, Calendar.SEPTEMBER, 18, 1, 15, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        testHeader.addValue(Standard.NAXIS, 1);
        testHeader.addValue(Standard.NAXISn.n(1), 350);
        testHeader.addValue(CADCExt.CUNITn.n(1), "s");
        testHeader.addValue(Standard.CRVALn.n(1), 40.0D); // Forty seconds long exposure
        testHeader.addValue(Standard.CRPIXn.n(1), 1.0);
        testHeader.addValue(Standard.CDELTn.n(1), 0.369);
        testHeader.addValue(Standard.CTYPEn.n(1), "UTC");
        testHeader.addValue(CADCExt.DATEREF, DateUtil.getDateFormat(DateUtil.ISO8601_DATE_FORMAT_LOCAL, DateUtil.UTC)
                                                     .format(calendar.getTime()));

        final FITSHeaderWCSKeywords fitsHeaderWCSKeywords = new FITSHeaderWCSKeywords(testHeader);
        final TimeHeaderWCSKeywords testSubject = new TimeHeaderWCSKeywords(fitsHeaderWCSKeywords);

        final double result = testSubject.getMJDRef();
        final double expected = 54360.7604D;

        Assert.assertEquals("Wrong MJD Ref value.", expected, result, 3.0E-5D);
    }

    @Test
    public void testMJDStart() throws Exception {
        final MJDTimeConverter mjdTimeConverter = new MJDTimeConverter();
        final Header testHeader = new Header();
        final Calendar calendar = Calendar.getInstance(DateUtil.UTC);
        calendar.set(2012, Calendar.NOVEMBER, 17, 1, 21, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        final Date startDate = calendar.getTime();

        calendar.add(Calendar.HOUR, 2);
        final Date stopDate = calendar.getTime();

        final double mjdValue = mjdTimeConverter.fromISODate(calendar.getTime());
        final int mjdValueI = (int) Math.floor(mjdValue);

        testHeader.addValue(Standard.NAXIS, 1);
        testHeader.addValue(Standard.NAXISn.n(1), 350);
        testHeader.addValue(CADCExt.CUNITn.n(1), "s");
        testHeader.addValue(Standard.CRVALn.n(1), 40.0D); // Forty seconds long exposure
        testHeader.addValue(Standard.CRPIXn.n(1), 1.0);
        testHeader.addValue(Standard.CDELTn.n(1), 0.369);
        testHeader.addValue(CADCExt.DATEBEG, DateUtil.getDateFormat(DateUtil.ISO8601_DATE_FORMAT_LOCAL, DateUtil.UTC)
                                                     .format(startDate));
        testHeader.addValue(CADCExt.DATEEND, DateUtil.getDateFormat(DateUtil.ISO8601_DATE_FORMAT_LOCAL, DateUtil.UTC)
                                                     .format(stopDate));
        testHeader.addValue(NOAOExt.TIMESYS, "UTC");
        testHeader.addValue(CADCExt.MJDREFI, mjdValueI);
        testHeader.addValue(CADCExt.MJDREFF, (mjdValue - mjdValueI));

        final TimeHeaderWCSKeywords testSubject = new TimeHeaderWCSKeywords(testHeader);
        final double mjdStart = testSubject.getMJDStart();

        Assert.assertEquals("Wrong MJD Start", 56247.7229D, mjdStart, 3.0E-5D);
    }

    @Test
    public void testMJDUnit() throws Exception {
        final Header testHeader = new Header();

        testHeader.addValue(CADCExt.TIMEUNIT, "m");
        final TimeHeaderWCSKeywords testSubject = new TimeHeaderWCSKeywords(testHeader);
        Assert.assertEquals("Wrong unit.", "m", testSubject.getUnit());

        Assert.assertEquals("Wrong default unit.", "s",
                            new TimeHeaderWCSKeywords(new Header()).getUnit());
    }
}
