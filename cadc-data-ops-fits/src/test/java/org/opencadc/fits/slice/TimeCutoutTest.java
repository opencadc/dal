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
import ca.nrc.cadc.date.DateUtil;
import ca.nrc.cadc.util.Log4jInit;
import nom.tam.fits.Header;
import nom.tam.fits.header.DateTime;
import nom.tam.fits.header.Standard;
import nom.tam.fits.header.extra.NOAOExt;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.fits.CADCExt;

import java.util.Calendar;


public class TimeCutoutTest extends BaseCutoutTest {

    static {
        Log4jInit.setLevel("org.opencadc.fits.slice", Level.DEBUG);
    }

    @Test
    public void testSimpleOverlap() {
        final Header testHeader = new Header();
        final Calendar calendar = Calendar.getInstance(DateUtil.UTC);
        calendar.set(2007, Calendar.SEPTEMBER, 18, 1, 15, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        testHeader.addValue(Standard.NAXIS, 1);
        testHeader.addValue(Standard.NAXISn.n(1), 11);
        testHeader.addValue(CADCExt.CUNITn.n(1), "s");
        testHeader.addValue(Standard.CRVALn.n(1), 2375.341D);
        testHeader.addValue(Standard.CRPIXn.n(1), 10.0);
        testHeader.addValue(Standard.CDELTn.n(1), 13.3629);
        testHeader.addValue(Standard.CTYPEn.n(1), "UTC");
        testHeader.addValue(DateTime.DATE_OBS, "2008-10-07T00:39:35.3342");
        testHeader.addValue(DateTime.MJD_OBS, 54746.02749237);
        testHeader.addValue(DateTime.MJDREF, 54746.0);

        final Interval<Number> testInterval = new Interval<>(54746.013D, 54746.058D);
        final TimeCutout testSubject = new TimeCutout(testHeader);

        final long[] results = testSubject.getBounds(testInterval);
        final long[] expected = new long[]{1L, 10L};

        assertFuzzyPixelArrayEquals("Wrong output.", expected, results);
    }

    @Test
    public void testSimpleMJDOverlap() {
        final Header testHeader = new Header();

        testHeader.setNaxes(3);
        testHeader.addValue(Standard.NAXISn.n(1), 350);
        testHeader.addValue(Standard.NAXISn.n(2), 550);
        testHeader.addValue(Standard.NAXISn.n(3), 550);
        testHeader.addValue(CADCExt.CUNITn.n(1), "s");
        testHeader.addValue(Standard.CRVALn.n(1), 40.0D); // Forty seconds long exposure
        testHeader.addValue(Standard.CRPIXn.n(1), 102.0);
        testHeader.addValue(Standard.CDELTn.n(1), 0.369);
        testHeader.addValue(Standard.CTYPEn.n(1), "UTC");
        testHeader.addValue(DateTime.MJD_BEG, 54533.0112D);
        testHeader.addValue(DateTime.MJD_END, 54565.0112D);
        testHeader.addValue(CADCExt.MJDREFI, 54468);
        testHeader.addValue(CADCExt.MJDREFF, 0.2489D);

        testHeader.addValue(Standard.CTYPEn.n(2), "RA---SIN");
        testHeader.addValue(Standard.CRVALn.n(2), 2.465333333333E+02D);
        testHeader.addValue(Standard.CDELTn.n(2), -1.111111111111E-04D);
        testHeader.addValue(Standard.CRPIXn.n(2), 1.510000000000E+02D);
        testHeader.addValue(CADCExt.CUNITn.n(2), "deg");

        testHeader.addValue(Standard.CTYPEn.n(3), "DEC--SIN");
        testHeader.addValue(Standard.CRVALn.n(3), -2.434013888889E+01D);
        testHeader.addValue(Standard.CDELTn.n(3), 1.111111111111E-04D);
        testHeader.addValue(Standard.CRPIXn.n(3), 1.510000000000E+02D);
        testHeader.addValue(CADCExt.CUNITn.n(3), "deg");

        final Interval<Number> testInterval = new Interval<>(54468.2489864D, 54468.24901D);
        final TimeCutout testSubject = new TimeCutout(testHeader);

        final long[] results = testSubject.getBounds(testInterval);
        final long[] expected = new long[]{12L, 19L, 1L, 550L, 1L, 550L};

        assertFuzzyPixelArrayEquals("Wrong output.", expected, results);
    }

    @Test
    public void testNoOverlap() {
        final Header testHeader = new Header();

        testHeader.setNaxes(1);
        testHeader.addValue(Standard.NAXISn.n(1), 1600);
        testHeader.addValue(CADCExt.CUNITn.n(1), "d");
        testHeader.addValue(Standard.CRVALn.n(1), 3.23D);  // 3.23 days exposure
        testHeader.addValue(Standard.CRPIXn.n(1), 1.0D);
        testHeader.addValue(Standard.CDELTn.n(1), 7.0856D);
        testHeader.addValue(Standard.CTYPEn.n(1), "TIME");
        testHeader.addValue(DateTime.TIMESYS, "UTC");
        testHeader.addValue(DateTime.DATE_BEG, "1977-11-25T01:21:13.0");
        testHeader.addValue(DateTime.DATE_END, "1977-11-25T03:11:00.0");
        testHeader.addValue(DateTime.MJDREF, 30005.3321D);

        final Interval<Number> testInterval = new Interval<>(52644.1D, 52830.33D);
        final TimeCutout testSubject = new TimeCutout(testHeader);

        final long[] results = testSubject.getBounds(testInterval);

        Assert.assertNull("Should be no overlap (NULL).", results);
    }
}
