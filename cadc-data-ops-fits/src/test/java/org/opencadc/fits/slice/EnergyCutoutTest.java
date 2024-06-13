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
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;

import nom.tam.fits.Header;
import nom.tam.fits.header.Standard;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.opencadc.fits.CADCExt;

public class EnergyCutoutTest extends BaseCutoutTest {
    private static final Logger LOGGER = Logger.getLogger(EnergyCutoutTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.fits.slice", Level.DEBUG);
        Log4jInit.setLevel("ca.nrc.cadc.dali", Level.DEBUG);
    }

    @Test
    public void testCGPS() throws Exception {
        final long startMillis = System.currentTimeMillis();

        final String headerFileName = "test-cgps-cube-header.txt";
        final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);

        try (final InputStream inputStream = new FileInputStream(testFile);
             final ArrayDataInput arrayDataInput = new BufferedDataInputStream(inputStream)) {

            final Header testHeader = Header.readHeader(arrayDataInput);
            final EnergyCutout energyCutout = new EnergyCutout(testHeader);
            final Interval<Number> energyCutoutBounds = new Interval<>(0.00023606D, 0.00024616D);
            energyCutout.getBounds(energyCutoutBounds);
            Assert.fail("Should throw IllegalArgumentException for incompatible conversion.");
        } catch (IllegalArgumentException illegalArgumentException) {
            // Good!
            Assert.assertEquals("Wrong message.", "Unable to cutout from velocity type (VELO-LSR) "
                                                  + "using provided wavelength metres.",
                                illegalArgumentException.getMessage());
        }
        LOGGER.debug("EnergyCutoutTest.testCGPS OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    public void testVLASS() throws Exception {
        final long startMillis = System.currentTimeMillis();

        final String headerFileName = "test-vlass-cube-header.txt";
        final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);

        try (final InputStream inputStream = new FileInputStream(testFile);
             final ArrayDataInput arrayDataInput = new BufferedDataInputStream(inputStream)) {

            final Header testHeader = Header.readHeader(arrayDataInput);
            final EnergyCutout energyCutout = new EnergyCutout(testHeader);
            final Interval<Number> energyCutoutBounds = new Interval<>(1.10328E-2D, 9.99308E-2D);

            final long[] resultBounds = energyCutout.getBounds(energyCutoutBounds);
            final long[] expectedBounds = new long[]{1L, 12150L, 1L, 12150L, 1L, 13L, 1L, 4L};

            assertFuzzyPixelArrayEquals("Wrong energy bounds for VLASS Cube.", expectedBounds, resultBounds);

            LOGGER.debug("Results: " + Arrays.toString(resultBounds));
        }
        LOGGER.debug("EnergyCutoutTest.testVLASS OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    public void testALMA() throws Exception {
        final long startMillis = System.currentTimeMillis();

        final String headerFileName = "test-alma-cube-band-header.txt";
        final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);

        try (final InputStream inputStream = new FileInputStream(testFile);
             final ArrayDataInput arrayDataInput = new BufferedDataInputStream(inputStream)) {

            final Header testHeader = Header.readHeader(arrayDataInput);
            final EnergyCutout energyCutout = new EnergyCutout(testHeader);
            final Interval<Number> energyCutoutBounds = new Interval<>(1.3606E-3D, 1.3616E-3D);
            final long[] resultBounds = energyCutout.getBounds(energyCutoutBounds);
            final long[] expectedBounds = new long[]{1L, 400L, 1L, 400L, 1L, 18L, 1L, 1L};

            assertFuzzyPixelArrayEquals("Wrong energy bounds for ALMA Cube.", expectedBounds, resultBounds);
        }
        LOGGER.debug("EnergyCutoutTest.testALMA OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    /**
     * This test was added as even thought the testALMA test was passing, this cube could not be cutout from.
     * @throws Exception    For anything that was unexpected
     */
    @Test
    public void testALMA2() throws Exception {
        final long startMillis = System.currentTimeMillis();

        final String headerFileName = "test-alma-cube-2-header.txt";
        final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);

        try (final InputStream inputStream = new FileInputStream(testFile);
             final ArrayDataInput arrayDataInput = new BufferedDataInputStream(inputStream)) {

            final Header testHeader = Header.readHeader(arrayDataInput);
            final EnergyCutout energyCutout = new EnergyCutout(testHeader);
            final Interval<Number> energyCutoutBounds = new Interval<>(2.208594862199E-3, 2.212996759406E-3);
            final long[] resultBounds = energyCutout.getBounds(energyCutoutBounds);
            final long[] expectedBounds = new long[]{1L, 108L, 1L, 108L, 184L, 253L, 1L, 1L};

            assertFuzzyPixelArrayEquals("Wrong energy bounds for ALMA 2 Cube.", expectedBounds, resultBounds);
        }
        LOGGER.debug("EnergyCutoutTest.testALMA2 OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    public void test1DEnergy() throws Exception {
        final long startMillis = System.currentTimeMillis();

        final Header testHeader = new Header();

        testHeader.setSimple(true);
        testHeader.setBitpix(-32);
        testHeader.setNaxes(1);
        testHeader.addValue(Standard.NAXIS1, 151);
        testHeader.addValue(Standard.EXTEND, true);
        testHeader.addValue(Standard.CTYPEn.n(1), CoordTypeCode.FREQ.name());
        testHeader.addValue(Standard.CRVALn.n(1), 1.152750450330E+11D);
        testHeader.addValue(Standard.CDELTn.n(1), -7.690066705322E+04D);
        testHeader.addValue(Standard.CRPIXn.n(1), 1.000000000000E+00D);
        testHeader.addValue(CADCExt.CUNITn.n(1), "Hz");
        testHeader.addValue(CADCExt.PC1_1, 1.000000000000E+00D);
        testHeader.addValue(Standard.RESTFRQ, 1.152712000000E+11D);
        testHeader.addValue(CADCExt.SPECSYS, "LSRK");
        testHeader.addValue(Standard.BSCALE, 1.000000000000E+00D);
        testHeader.addValue(Standard.BZERO, 0.000000000000E+00D);
        testHeader.addValue(Standard.BUNIT, "JY/BEAM");
        testHeader.addValue("VELREF", 257, "");
        testHeader.addValue("LATPOLE", -2.434013888889E+01D, "");
        testHeader.addValue("LONPOLE", 1.800000000000E+02D, "");
        testHeader.addValue(Standard.EQUINOX, 2.000000000000E+03D);
        testHeader.addValue(Standard.RADESYS, "FK5");

        final EnergyCutout energyCutout = new EnergyCutout(testHeader);
        final Interval<Number> energyCutoutBounds = new Interval<>(2.600708E-3D, 2.6008209E-3D);
        energyCutout.fitsHeaderWCSKeywords.iterator().forEachRemaining(keyVal -> LOGGER.debug(keyVal.getKey() + " = "
                                                                                              + keyVal.getValue()));
        final long[] resultBounds = energyCutout.getBounds(energyCutoutBounds);
        final long[] expectedBounds = new long[]{22L, 87L};

        assertFuzzyPixelArrayEquals("Wrong energy bounds 1D file.", expectedBounds, resultBounds);
        LOGGER.debug("EnergyCutoutTest.test1DEnergy OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    public void testCubeEnergyNAXIS1() throws Exception {
        final long startMillis = System.currentTimeMillis();
        final Header testHeader = new Header();
        final int naxis = 3;

        testHeader.setSimple(true);
        testHeader.addValue(Standard.EXTEND, true);
        testHeader.setBitpix(-32);
        testHeader.setNaxes(naxis);
        testHeader.addValue(Standard.NAXISn.n(1), 151);
        testHeader.addValue(Standard.NAXISn.n(2), 300);
        testHeader.addValue(Standard.NAXISn.n(3), 300);
        testHeader.addValue(Standard.CTYPEn.n(1), CoordTypeCode.FREQ.name());
        testHeader.addValue(Standard.CRVALn.n(1), 1.152750450330E+11D);
        testHeader.addValue(Standard.CDELTn.n(1), -7.690066705322E+04D);
        testHeader.addValue(Standard.CRPIXn.n(1), 1.000000000000E+00D);
        testHeader.addValue(CADCExt.CUNITn.n(1), "Hz");

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

        testHeader.addValue("PV3_1", 0.000000000000E+00D, "");
        testHeader.addValue("PV3_2", 0.000000000000E+00D, "");

        for (int i = 1; i <= naxis; i++) {
            for (int j = 1; j <= naxis; j++) {
                testHeader.addValue(String.format("PC0%d_0%d", i, j), (i == j) ? 1.0D : 0.0D, "");
            }
        }

        testHeader.addValue(CADCExt.RESTFRQ, 1.152712000000E+11D);
        testHeader.addValue(CADCExt.SPECSYS, "LSRK");
        testHeader.addValue(Standard.BSCALE, 1.000000000000E+00D);
        testHeader.addValue(Standard.BZERO, 0.000000000000E+00D);
        testHeader.addValue(Standard.BUNIT, "JY/BEAM");
        testHeader.addValue("VELREF", 257, "");
        testHeader.addValue("LATPOLE", -2.434013888889E+01D, "");
        testHeader.addValue("LONPOLE", 1.800000000000E+02D, "");
        testHeader.addValue(Standard.EQUINOX, 2.000000000000E+03D);
        testHeader.addValue(Standard.RADESYS, "FK5");

        final EnergyCutout energyCutout = new EnergyCutout(testHeader);
        final Interval<Number> energyCutoutBounds = new Interval<>(2.600708E-3D, 2.6008209E-3D);
        final long[] resultBounds = energyCutout.getBounds(energyCutoutBounds);
        final long[] expectedBounds = new long[]{22L, 87L, 1L, 300L, 1L, 300L};

        assertFuzzyPixelArrayEquals("Wrong energy bounds cube file.", expectedBounds, resultBounds);
        LOGGER.debug("EnergyCutoutTest.testCubeEnergyNAXIS1 OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    @Ignore("Until SIP distortions are better understood.")
    public void testSITELLE() throws Exception {
        final long startMillis = System.currentTimeMillis();

        final String headerFileName = "test-sitelle-cube-header.txt";
        final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);

        try (final InputStream inputStream = new FileInputStream(testFile);
             final ArrayDataInput arrayDataInput = new BufferedDataInputStream(inputStream)) {

            final Header testHeader = Header.readHeader(arrayDataInput);
            final EnergyCutout energyCutout = new EnergyCutout(testHeader);
            final Interval<Number> energyCutoutBounds = new Interval<>(5.368180E-9D, 5.3810E-9D);
            final long[] resultBounds = energyCutout.getBounds(energyCutoutBounds);
            final long[] expectedBounds = new long[]{1L, 2048, 1L, 2064, 60L, 105L};

            assertFuzzyPixelArrayEquals("Wrong energy bounds cube file.", expectedBounds, resultBounds);
        }
        LOGGER.debug("EnergyCutoutTest.testSITELLE OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }
}
