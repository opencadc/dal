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

import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import nom.tam.fits.Header;
import nom.tam.fits.header.Standard;
import nom.tam.fits.header.extra.NOAOExt;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.opencadc.fits.CADCExt;


public class PolygonCutoutTest extends BaseCutoutTest {
    private static final Logger LOGGER = Logger.getLogger(PolygonCutoutTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.fits.slice", Level.DEBUG);
    }

    @Test
    public void testMegapipeCutout() throws Exception {
        final long startMillis = System.currentTimeMillis();

        final String headerFileName = "test-megapipe-header.txt";
        final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);

        try (final InputStream inputStream = new FileInputStream(testFile);
             final ArrayDataInput arrayDataInput = new BufferedDataInputStream(inputStream)) {

            final Header testHeader = Header.readHeader(arrayDataInput);
            final PolygonCutout polygonCutout = new PolygonCutout(testHeader);

            final Polygon polygon = new Polygon();

            polygon.getVertices().add(new Point(51.291219363105000D, -21.737249735369637D));
            polygon.getVertices().add(new Point(51.291193816346876D, -21.721717813306441D));
            polygon.getVertices().add(new Point(51.307912919582414D, -21.721693011490995D));
            polygon.getVertices().add(new Point(51.307940254544761D, -21.737224914051101D));

            final long[] result = polygonCutout.getBounds(polygon);
            final long[] expected = new long[]{400, 700, 400, 700};
            assertFuzzyPixelArrayEquals("Wrong bounds.", expected, result);
        }

        LOGGER.debug("Util.testMegapipeCutout OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    /**
     * Test a Polygon slice where the FITS file's longitude and latitude axes are reversed.
     * @throws Exception    For any errors.
     */
    @Test
    public void testSwappedRADec() throws Exception {
        final long startMillis = System.currentTimeMillis();

        final Header testHeader = new Header();
        final int naxis = 2;

        testHeader.setSimple(true);
        testHeader.setBitpix(-32);
        testHeader.addValue(Standard.EXTEND, true);
        testHeader.setNaxes(naxis);
        testHeader.setNaxis(1, 10000);
        testHeader.setNaxis(2, 10000);

        // The CD matrix needs to also be reversed in the file.
        testHeader.addValue(NOAOExt.CD1_1, 5.160234650248E-05D);
        testHeader.addValue(NOAOExt.CD1_2, 0.0D);
        testHeader.addValue(NOAOExt.CD2_1, 0.0D);
        testHeader.addValue(NOAOExt.CD2_2, -5.160234650248E-05D);

        testHeader.addValue(Standard.CTYPEn.n(1), "DEC--TAN");
        testHeader.addValue(CADCExt.CUNITn.n(1), "deg");
        testHeader.addValue(Standard.CRVALn.n(1), -2.150000000000E+01D);
        testHeader.addValue(Standard.CRPIXn.n(1), 5.000000000000E+03D);

        testHeader.addValue(Standard.CTYPEn.n(2), "RA---TAN");
        testHeader.addValue(CADCExt.CUNITn.n(2), "deg");
        testHeader.addValue(Standard.CRVALn.n(2), 5.105234642000E+01D);
        testHeader.addValue(Standard.CRPIXn.n(2), 5.000000000000E+03D);

        testHeader.addValue(Standard.EQUINOX, 2.000000000000E+03D);
        testHeader.addValue(Standard.RADESYS, "ICRS");
        testHeader.addValue("MJD-OBS", 5.479849048670E+04D, "");
        testHeader.addValue("FILTER", "R.MP9601", "");

        final PolygonCutout polygonCutout = new PolygonCutout(testHeader);
        final Polygon polygon = new Polygon();

        polygon.getVertices().add(new Point(51.291219363105000D, -21.737249735369637D));
        polygon.getVertices().add(new Point(51.291193816346876D, -21.721717813306441D));
        polygon.getVertices().add(new Point(51.307912919582414D, -21.721693011490995D));
        polygon.getVertices().add(new Point(51.307940254544761D, -21.737224914051101D));

        final long[] result = polygonCutout.getBounds(polygon);
        final long[] expected = new long[]{400, 700, 400, 700};
        assertFuzzyPixelArrayEquals("Wrong bounds.", expected, result);

        LOGGER.debug("Util.testSwappedRADec OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }
}
