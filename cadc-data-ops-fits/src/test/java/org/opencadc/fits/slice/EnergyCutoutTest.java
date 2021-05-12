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
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import nom.tam.util.RandomAccessDataObject;
import nom.tam.util.RandomAccessFileExt;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;

public class EnergyCutoutTest extends BaseCutoutTest {
    private static final Logger LOGGER = Logger.getLogger(EnergyCutoutTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.fits.slice", Level.DEBUG);
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
            final long[] expectedBounds = new long[]{1L, 13L};

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
            final long[] expectedBounds = new long[]{1L, 18L};

            assertFuzzyPixelArrayEquals("Wrong energy bounds for ALMA Cube.", expectedBounds, resultBounds);
        }
        LOGGER.debug("EnergyCutoutTest.testALMA OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    @Ignore("Until SIP distortions are understood.")
    public void testSITELLE() throws Exception {
        final long startMillis = System.currentTimeMillis();

        final String headerFileName = "test-sitelle-cube-header.txt";
        final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);

        try (final InputStream inputStream = new FileInputStream(testFile);
             final ArrayDataInput arrayDataInput = new BufferedDataInputStream(inputStream)) {

            final Header testHeader = Header.readHeader(arrayDataInput);
            final EnergyCutout energyCutout = new EnergyCutout(testHeader);
            final Interval<Number> energyCutoutBounds = new Interval<>(0.00000051D, 0.00000052D);

            final long[] resultBounds = energyCutout.getBounds(energyCutoutBounds);

            Assert.assertEquals("Wrong bounds.", 252, resultBounds[0]);
        }
        LOGGER.debug("EnergyCutoutTest.testSITELLE OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }
}
