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

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;
import nom.tam.fits.Header;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.BufferedDataInputStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class CircleCutoutTest extends BaseCutoutTest {
    private static final Logger LOGGER = Logger.getLogger(CircleCutoutTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.fits.slice", Level.DEBUG);
    }

    /**
     * 4D cube cutout.
     *
     * @throws Exception Any badness.
     */
    @Test
    public void testALMA() throws Exception {
        final long startMillis = System.currentTimeMillis();

        final String headerFileName = "test-alma-cube-header.txt";
        final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);

        try (final InputStream inputStream = new FileInputStream(testFile);
             final ArrayDataInput arrayDataInput = new BufferedDataInputStream(inputStream)) {

            final Header testHeader = Header.readHeader(arrayDataInput);
            final Circle circle = new Circle(new Point(246.52D, -24.33D), 0.01D);
            final CircleCutout circleCutout = new CircleCutout(testHeader);

            final long[] expected = new long[]{169, 300, 151, 300, 1, 151, 1, 1};
            final long[] result = circleCutout.getBounds(circle);

            assertFuzzyPixelArrayEquals("Wrong ALMA circle cutout.", expected, result);
        }
        LOGGER.debug("CircleCutoutTest.testALMA OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    public void testBLAST() throws Exception {
        final long startMillis = System.currentTimeMillis();
        final String headerFileNamePrefix = "test-blast-header";
        final Map<Integer, long[]> headerExpectations = new HashMap<>();

        headerExpectations.put(0, new long[]{1, 118, 1, 140});
        headerExpectations.put(1, new long[]{1, 118, 1, 140});
        headerExpectations.put(2, null);

        for (final Map.Entry<Integer, long[]> entry : headerExpectations.entrySet()) {
            final int hduIndex = entry.getKey();
            final String headerFileName = headerFileNamePrefix + "-" + hduIndex + ".txt";
            final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);

            LOGGER.debug("Reading header file " + headerFileName);

            try (final InputStream inputStream = new FileInputStream(testFile);
                 final ArrayDataInput arrayDataInput = new BufferedDataInputStream(inputStream)) {
                final Header testHeader = Header.readHeader(arrayDataInput);
                final Circle circle = new Circle(new Point(309.8D, 42.7D), 0.3D);
                final CircleCutout circleCutout = new CircleCutout(testHeader);

                final long[] expected = entry.getValue();
                final long[] result = circleCutout.getBounds(circle);

                assertFuzzyPixelArrayEquals("Wrong BLAST circle cutout at HDU " + hduIndex, expected, result);
            }
        }
        LOGGER.debug("CircleCutoutTest.testComputeBLAST OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    @Ignore("Bad header information?  Pixels are off by ~5.")
    public void testIRIS() throws Exception {
        final long startMillis = System.currentTimeMillis();

        final String headerFileName = "test-iris-header.txt";
        final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);

        try (final InputStream inputStream = new FileInputStream(testFile);
             final ArrayDataInput arrayDataInput = new BufferedDataInputStream(inputStream)) {

            final Header testHeader = Header.readHeader(arrayDataInput);
            final Circle circle = new Circle(new Point(250.0D, 75.2D), 0.3D);
            final CircleCutout circleCutout = new CircleCutout(testHeader);

            final long[] expected = new long[]{431, 463, 79, 110};
            final long[] result = circleCutout.getBounds(circle);
            assertFuzzyPixelArrayEquals("Wrong IRIS circle bounds.", expected, result);
        }
        LOGGER.debug("CircleCutoutTest.testComputeIRIS OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    public void testOMM() throws Exception {
        final long startMillis = System.currentTimeMillis();

        final String headerFileName = "test-omm-header.txt";
        final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);

        try (final InputStream inputStream = new FileInputStream(testFile);
             final ArrayDataInput arrayDataInput = new BufferedDataInputStream(inputStream)) {

            final Header testHeader = Header.readHeader(arrayDataInput);
            final Circle circle = new Circle(new Point(20.89D, -59.47D), 0.1D);
            final CircleCutout circleCutout = new CircleCutout(testHeader);

            final long[] expected = new long[]{642, 1381, 603, 1342};
            final long[] result = circleCutout.getBounds(circle);

            assertFuzzyPixelArrayEquals("Wrong OMM circle cutout.", expected, result);
        }
        LOGGER.debug("CircleCutoutTest.testComputeOMM OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    public void testHSTMEF() throws Exception {
        final long startMillis = System.currentTimeMillis();
        final String headerFileNamePrefix = "test-hst-mef-header";
        final Map<Integer, long[]> headerExpectations = new HashMap<>();
        final int hduCount = 131;

        final int[] expectedExtensionIndexes = new int[] {
                1, 101, 106, 11, 111, 116, 121, 126, 16, 21, 26, 31, 36, 41, 46, 51, 56, 6, 61, 66, 71, 76, 81, 86,
                91, 96
        };

        for (int i = 0; i < hduCount; i++) {
            final int hduIndex = i;
            headerExpectations.put(hduIndex,
                                   Arrays.stream(expectedExtensionIndexes).anyMatch(index -> hduIndex == index)
                                   ? new long[0] : null);
        }

        final Map<Integer, long[]> extensionRanges = new HashMap<>();
        final Circle circle = new Circle(new Point(189.1726880000002D, 62.17111899999974D), 0.01D);

        for (final Map.Entry<Integer, long[]> entry : headerExpectations.entrySet()) {
            final int hduIndex = entry.getKey();
            final String headerFileName = headerFileNamePrefix + "-" + hduIndex + ".txt";
            final File testFile = FileUtil.getFileFromResource("test-hst-mef/" + headerFileName,
                                                               CircleCutoutTest.class);

            try (final InputStream inputStream = new FileInputStream(testFile);
                 final ArrayDataInput arrayDataInput = new BufferedDataInputStream(inputStream)) {
                final Header testHeader = Header.readHeader(arrayDataInput);

                LOGGER.debug("Looking at HDU " + hduIndex);

                final CircleCutout circleCutout = new CircleCutout(testHeader);
                final long[] bounds = circleCutout.getBounds(circle);
                if (bounds != null) {
                    extensionRanges.put(hduIndex, bounds);
                }
            }
        }

        extensionRanges.forEach((i, bounds) -> LOGGER.debug("Extension " + i + " > " + Arrays.toString(bounds)));

        Assert.assertEquals("Should have 26 matched extensions.", expectedExtensionIndexes.length,
                            extensionRanges.size());
        Assert.assertEquals("Wrong extensions.", 0,
                            Arrays.stream(expectedExtensionIndexes).filter(i -> !extensionRanges.containsKey(i)).sum());

        LOGGER.debug("CircleCutoutTest.testComputeCircleMEFCutout OK: " + (System.currentTimeMillis() - startMillis)
                     + " ms");
    }

    @Test
    public void testNoOverlap() throws Exception {
        final long startMillis = System.currentTimeMillis();
        final String headerFileNamePrefix = "test-sample-mef-header";
        final Map<Integer, long[]> headerExpectations = new HashMap<>();

        headerExpectations.put(0, new long[]{1, 118, 1, 140});
        headerExpectations.put(1, new long[]{1, 118, 1, 140});
        headerExpectations.put(2, null);

        final Circle circle = new Circle(new Point(240.0D, 50.2D), 0.2D);

        for (final Map.Entry<Integer, long[]> entry : headerExpectations.entrySet()) {
            final int hduIndex = entry.getKey();
            final String headerFileName = headerFileNamePrefix + "-" + hduIndex + ".txt";
            final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);

            LOGGER.debug("Reading header file " + headerFileName);

            try (final InputStream inputStream = new FileInputStream(testFile);
                 final ArrayDataInput arrayDataInput = new BufferedDataInputStream(inputStream)) {
                final Header testHeader = Header.readHeader(arrayDataInput);
                final CircleCutout circleCutout = new CircleCutout(testHeader);
                assertFuzzyPixelArrayEquals("Should be empty.", null, circleCutout.getBounds(circle));
            }
        }
        LOGGER.debug("CircleCutoutTest.testNoOverlap OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }
}
