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

import ca.nrc.cadc.dali.PolarizationState;
import ca.nrc.cadc.util.Log4jInit;
import nom.tam.fits.Header;
import nom.tam.fits.header.Standard;
import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.opencadc.fits.CADCExt;

public class PolarizationCutoutTest extends BaseCutoutTest {

    static {
        Log4jInit.setLevel("org.opencadc.fits.slice", Level.DEBUG);
    }

    @Test
    public void testStokesI() throws Exception {
        final Header testHeader = new Header();

        testHeader.addValue(Standard.NAXIS, 4);
        testHeader.addValue(Standard.NAXISn.n(1), 300);
        testHeader.addValue(Standard.CTYPEn.n(1), CoordTypeCode.RA.name() + "---SIN");
        testHeader.addValue(Standard.CRVALn.n(1), 2.465333333333E+02D);
        testHeader.addValue(Standard.CDELTn.n(1), -1.111111111111E-04D);
        testHeader.addValue(Standard.CRPIXn.n(1), 1.510000000000E+02D);
        testHeader.addValue(CADCExt.CUNITn.n(1), "deg");

        testHeader.addValue(Standard.NAXISn.n(2), 300);
        testHeader.addValue(Standard.CTYPEn.n(2), CoordTypeCode.DEC.name() + "--SIN");
        testHeader.addValue(Standard.CRVALn.n(2), 2.434013888889E+01D);
        testHeader.addValue(Standard.CDELTn.n(2), 1.111111111111E-04D);
        testHeader.addValue(Standard.CRPIXn.n(2), 1.510000000000E+02D);
        testHeader.addValue(CADCExt.CUNITn.n(2), "deg");

        testHeader.addValue(Standard.NAXISn.n(3), 151);
        testHeader.addValue(Standard.CTYPEn.n(3), CoordTypeCode.FREQ.name());
        testHeader.addValue(Standard.CRVALn.n(3), 1.152750450330E+11D);
        testHeader.addValue(Standard.CDELTn.n(3), -7.690066705322E+04D);
        testHeader.addValue(Standard.CRPIXn.n(3), 1.000000000000E+00D);
        testHeader.addValue(CADCExt.CUNITn.n(3), "Hz");

        testHeader.addValue(Standard.NAXISn.n(4), 1);
        testHeader.addValue(Standard.CTYPEn.n(4), CoordTypeCode.STOKES.name());
        testHeader.addValue(Standard.CRVALn.n(4), 1.0D);
        testHeader.addValue(Standard.CDELTn.n(4), 1.0D);
        testHeader.addValue(Standard.CRPIXn.n(4), 1.0D);
        testHeader.addValue(CADCExt.CUNITn.n(4), "");

        final PolarizationState[] states = new PolarizationState[] {
                PolarizationState.I
        };

        final PolarizationCutout testSubject = new PolarizationCutout(testHeader);

        final long[] results = testSubject.getBounds(states);
        final long[] expected = new long[]{1L, 300L, 1L, 300L, 1L, 151L, 1L, 1L};

        assertFuzzyPixelArrayEquals("Wrong output.", expected, results);
    }

    @Test
    public void testCircularRRLL() throws Exception {
        final Header testHeader = new Header();

        testHeader.addValue(Standard.NAXIS, 4);
        testHeader.addValue(Standard.NAXISn.n(1), 300);
        testHeader.addValue(Standard.CTYPEn.n(1), CoordTypeCode.RA.name() + "---SIN");
        testHeader.addValue(Standard.CRVALn.n(1), 2.465333333333E+02D);
        testHeader.addValue(Standard.CDELTn.n(1), -1.111111111111E-04D);
        testHeader.addValue(Standard.CRPIXn.n(1), 1.510000000000E+02D);
        testHeader.addValue(CADCExt.CUNITn.n(1), "deg");

        testHeader.addValue(Standard.NAXISn.n(2), 300);
        testHeader.addValue(Standard.CTYPEn.n(2), CoordTypeCode.DEC.name() + "--SIN");
        testHeader.addValue(Standard.CRVALn.n(2), 2.434013888889E+01D);
        testHeader.addValue(Standard.CDELTn.n(2), 1.111111111111E-04D);
        testHeader.addValue(Standard.CRPIXn.n(2), 1.510000000000E+02D);
        testHeader.addValue(CADCExt.CUNITn.n(2), "deg");

        testHeader.addValue(Standard.NAXISn.n(3), 151);
        testHeader.addValue(Standard.CTYPEn.n(3), CoordTypeCode.FREQ.name());
        testHeader.addValue(Standard.CRVALn.n(3), 1.152750450330E+11D);
        testHeader.addValue(Standard.CDELTn.n(3), -7.690066705322E+04D);
        testHeader.addValue(Standard.CRPIXn.n(3), 1.000000000000E+00D);
        testHeader.addValue(CADCExt.CUNITn.n(3), "Hz");

        testHeader.addValue(Standard.NAXISn.n(4), 2);
        testHeader.addValue(Standard.CTYPEn.n(4), CoordTypeCode.STOKES.name());
        testHeader.addValue(Standard.CRVALn.n(4), -3.0D);
        testHeader.addValue(Standard.CDELTn.n(4), 1.0D);
        testHeader.addValue(Standard.CRPIXn.n(4), 1.0D);
        testHeader.addValue(CADCExt.CUNITn.n(4), "");

        final PolarizationState[] states = new PolarizationState[]{
                PolarizationState.RR,
                PolarizationState.LL
        };

        final PolarizationCutout testSubject = new PolarizationCutout(testHeader);

        final long[] results = testSubject.getBounds(states);
        final long[] expected = new long[]{1L, 300L, 1L, 300L, 1L, 151L, 3L, 4L};

        assertFuzzyPixelArrayEquals("Wrong output.", expected, results);
    }

    @Test
    public void testLinear() throws Exception {
        final Header testHeader = new Header();

        testHeader.addValue(Standard.NAXIS, 4);
        testHeader.addValue(Standard.NAXISn.n(1), 300);
        testHeader.addValue(Standard.CTYPEn.n(1), CoordTypeCode.RA.name() + "---SIN");
        testHeader.addValue(Standard.CRVALn.n(1), 2.465333333333E+02D);
        testHeader.addValue(Standard.CDELTn.n(1), -1.111111111111E-04D);
        testHeader.addValue(Standard.CRPIXn.n(1), 1.510000000000E+02D);
        testHeader.addValue(CADCExt.CUNITn.n(1), "deg");

        testHeader.addValue(Standard.NAXISn.n(2), 300);
        testHeader.addValue(Standard.CTYPEn.n(2), CoordTypeCode.DEC.name() + "--SIN");
        testHeader.addValue(Standard.CRVALn.n(2), 2.434013888889E+01D);
        testHeader.addValue(Standard.CDELTn.n(2), 1.111111111111E-04D);
        testHeader.addValue(Standard.CRPIXn.n(2), 1.510000000000E+02D);
        testHeader.addValue(CADCExt.CUNITn.n(2), "deg");

        testHeader.addValue(Standard.NAXISn.n(3), 151);
        testHeader.addValue(Standard.CTYPEn.n(3), CoordTypeCode.FREQ.name());
        testHeader.addValue(Standard.CRVALn.n(3), 1.152750450330E+11D);
        testHeader.addValue(Standard.CDELTn.n(3), -7.690066705322E+04D);
        testHeader.addValue(Standard.CRPIXn.n(3), 1.000000000000E+00D);
        testHeader.addValue(CADCExt.CUNITn.n(3), "Hz");

        testHeader.addValue(Standard.NAXISn.n(4), 1);
        testHeader.addValue(Standard.CTYPEn.n(4), CoordTypeCode.STOKES.name());
        testHeader.addValue(Standard.CRVALn.n(4), -5.0D);
        testHeader.addValue(Standard.CDELTn.n(4), 1.0D);
        testHeader.addValue(Standard.CRPIXn.n(4), 1.0D);
        testHeader.addValue(CADCExt.CUNITn.n(4), "");

        final PolarizationState[] states = new PolarizationState[] {
                PolarizationState.XX
        };

        final PolarizationCutout testSubject = new PolarizationCutout(testHeader);

        final long[] results = testSubject.getBounds(states);
        final long[] expected = new long[]{1L, 300L, 1L, 300L, 1L, 151L, 1L, 1L};

        assertFuzzyPixelArrayEquals("Wrong output.", expected, results);
    }

    @Test
    public void testNoOverlap() throws Exception {
        final Header testHeader = new Header();

        testHeader.addValue(Standard.NAXIS, 4);
        testHeader.addValue(Standard.NAXISn.n(1), 300);
        testHeader.addValue(Standard.CTYPEn.n(1), CoordTypeCode.RA.name() + "---SIN");
        testHeader.addValue(Standard.CRVALn.n(1), 2.465333333333E+02D);
        testHeader.addValue(Standard.CDELTn.n(1), -1.111111111111E-04D);
        testHeader.addValue(Standard.CRPIXn.n(1), 1.510000000000E+02D);
        testHeader.addValue(CADCExt.CUNITn.n(1), "deg");

        testHeader.addValue(Standard.NAXISn.n(2), 300);
        testHeader.addValue(Standard.CTYPEn.n(2), CoordTypeCode.DEC.name() + "--SIN");
        testHeader.addValue(Standard.CRVALn.n(2), 2.434013888889E+01D);
        testHeader.addValue(Standard.CDELTn.n(2), 1.111111111111E-04D);
        testHeader.addValue(Standard.CRPIXn.n(2), 1.510000000000E+02D);
        testHeader.addValue(CADCExt.CUNITn.n(2), "deg");

        testHeader.addValue(Standard.NAXISn.n(3), 151);
        testHeader.addValue(Standard.CTYPEn.n(3), CoordTypeCode.FREQ.name());
        testHeader.addValue(Standard.CRVALn.n(3), 1.152750450330E+11D);
        testHeader.addValue(Standard.CDELTn.n(3), -7.690066705322E+04D);
        testHeader.addValue(Standard.CRPIXn.n(3), 1.000000000000E+00D);
        testHeader.addValue(CADCExt.CUNITn.n(3), "Hz");

        testHeader.addValue(Standard.NAXISn.n(4), 1);
        testHeader.addValue(Standard.CTYPEn.n(4), CoordTypeCode.STOKES.name());
        testHeader.addValue(Standard.CRVALn.n(4), 3.0D);
        testHeader.addValue(Standard.CDELTn.n(4), 1.0D);
        testHeader.addValue(Standard.CRPIXn.n(4), 1.0D);
        testHeader.addValue(CADCExt.CUNITn.n(4), "");

        final PolarizationState[] states = new PolarizationState[] {
                PolarizationState.LL
        };

        final PolarizationCutout testSubject = new PolarizationCutout(testHeader);

        Assert.assertNull("Should be no overlap.", testSubject.getBounds(states));
    }
}
