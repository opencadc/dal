/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2020.                            (c) 2020.
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

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.ImageHDU;
import nom.tam.util.RandomAccessDataObject;
import nom.tam.util.RandomAccessFileExt;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.fits.FitsTest;
import org.opencadc.soda.ExtensionSlice;
import org.opencadc.soda.ExtensionSliceFormat;
import org.opencadc.soda.server.Cutout;


public class NDimensionalSlicerTest {
    private static final Logger LOGGER = Logger.getLogger(NDimensionalSlicer.class);

    static {
        Log4jInit.setLevel("org.opencadc", Level.DEBUG);
    }

    @Test
    public void testMEFFileSlice() throws Exception {
        ExtensionSliceFormat fmt = new ExtensionSliceFormat();
        List<ExtensionSlice> slices = new ArrayList<>();
        slices.add(fmt.parse("[SCI,10][80:220,100:150]"));
        slices.add(fmt.parse("[1][10:16,70:90]"));
        slices.add(fmt.parse("[106][8:32,88:112]"));
        slices.add(fmt.parse("[126]"));
        final Cutout cutout = new Cutout();
        cutout.pixelCutouts = slices;

        final NDimensionalSlicer slicer = new NDimensionalSlicer();
        final File file = FileUtil.getFileFromResource("test-hst-mef.fits",
                                                       NDimensionalSlicerTest.class);

        final String configuredTestWriteDir = System.getenv("TEST_WRITE_DIR");
        final String testWriteDir = configuredTestWriteDir == null ? "/tmp" : configuredTestWriteDir;
        final File expectedFile = FileUtil.getFileFromResource("test-hst-mef-cutout.fits",
                                                               NDimensionalSlicerTest.class);
        final Path outputPath = Files.createTempFile(new File(testWriteDir).toPath(), "test-hst-mef-cutout", ".fits");
        LOGGER.debug("Writing out to " + outputPath);

        try (final OutputStream outputStream = new FileOutputStream(outputPath.toFile())) {
            slicer.slice(file, cutout, outputStream);
        }

        final Fits expectedFits = new Fits(expectedFile);
        final Fits resultFits = new Fits(outputPath.toFile());

        FitsTest.assertFitsEqual(expectedFits, resultFits);
        Files.deleteIfExists(outputPath);
    }

    @Test
    public void testMEFRandomAccessSlice() throws Exception {
        ExtensionSliceFormat fmt = new ExtensionSliceFormat();
        List<ExtensionSlice> slices = new ArrayList<>();
        slices.add(fmt.parse("[SCI,10][80:220,100:150]"));
        slices.add(fmt.parse("[1][10:16,70:90]"));
        slices.add(fmt.parse("[106][8:32,88:112]"));
        slices.add(fmt.parse("[126]"));
        final Cutout cutout = new Cutout();
        cutout.pixelCutouts = slices;

        final NDimensionalSlicer slicer = new NDimensionalSlicer();
        final File file = FileUtil.getFileFromResource("test-hst-mef.fits",
                                                       NDimensionalSlicerTest.class);
        final String configuredTestWriteDir = System.getenv("TEST_WRITE_DIR");
        final String testWriteDir = configuredTestWriteDir == null ? "/tmp" : configuredTestWriteDir;
        final File expectedFile = FileUtil.getFileFromResource("test-hst-mef-cutout.fits",
                                                               NDimensionalSlicerTest.class);
        final Path outputPath = Files.createTempFile(new File(testWriteDir).toPath(), "test-hst-mef-cutout", ".fits");
        LOGGER.debug("Writing out to " + outputPath);

        try (final RandomAccessDataObject randomAccessDataObject = new RandomAccessFileExt(file, "r");
             final OutputStream outputStream = new FileOutputStream(outputPath.toFile());
             final OutputStream hstFileCutoutStream = new DataOutputStream(outputStream)) {
            slicer.slice(randomAccessDataObject, cutout, hstFileCutoutStream);
            hstFileCutoutStream.flush();
        }

        final Fits expectedFits = new Fits(expectedFile);
        final Fits resultFits = new Fits(outputPath.toFile());

        FitsTest.assertFitsEqual(expectedFits, resultFits);
        Files.deleteIfExists(outputPath);
    }

    /**
     * Two slices from a simple FITS file.
     */
    @Test
    public void testSimpleToMEF() throws Exception {
        ExtensionSliceFormat fmt = new ExtensionSliceFormat();
        List<ExtensionSlice> slices = new ArrayList<>();
        slices.add(fmt.parse("[0][25:125]"));
        slices.add(fmt.parse("[0][300:375]"));
        final Cutout cutout = new Cutout();
        cutout.pixelCutouts = slices;

        final NDimensionalSlicer slicer = new NDimensionalSlicer();
        final File file = FileUtil.getFileFromResource("test-simple-iris.fits",
                                                       NDimensionalSlicerTest.class);
        final File expectedFile = FileUtil.getFileFromResource("test-mef-iris-cutout.fits",
                                                               NDimensionalSlicerTest.class);
        final String configuredTestWriteDir = System.getenv("TEST_WRITE_DIR");
        final String testWriteDir = configuredTestWriteDir == null ? "/tmp" : configuredTestWriteDir;
        final Path outputPath = Files.createTempFile(new File(testWriteDir).toPath(), "test-simple-iris-cutout", ".fits");

        try (final RandomAccessDataObject randomAccessDataObject = new RandomAccessFileExt(file, "r");
             final OutputStream outputStream = new FileOutputStream(outputPath.toFile())) {
            slicer.slice(randomAccessDataObject, cutout, outputStream);
            outputStream.flush();
        }

        final Fits expectedFits = new Fits(expectedFile);
        final Fits resultFits = new Fits(outputPath.toFile());

        FitsTest.assertFitsEqual(expectedFits, resultFits);
        Files.deleteIfExists(outputPath);
    }

    @Test
    public void testMEFToSimple() throws Exception {
        List<ExtensionSlice> slices = new ArrayList<>();
        slices.add(new ExtensionSlice("SCI", 13));
        final Cutout cutout = new Cutout();
        cutout.pixelCutouts = slices;

        final NDimensionalSlicer slicer = new NDimensionalSlicer();
        final File file = FileUtil.getFileFromResource("test-hst-mef.fits",
                                                       NDimensionalSlicerTest.class);
        final String configuredTestWriteDir = System.getenv("TEST_WRITE_DIR");
        final String testWriteDir = configuredTestWriteDir == null ? "/tmp" : configuredTestWriteDir;
        final File expectedFile = FileUtil.getFileFromResource("test-simple-hst-cutout.fits",
                                                               NDimensionalSlicerTest.class);
        final Path outputPath = Files.createTempFile(new File(testWriteDir).toPath(), "test-simple-hst-cutout", ".fits");
        LOGGER.debug("Writing out to " + outputPath);

        try (final RandomAccessDataObject randomAccessDataObject = new RandomAccessFileExt(file, "r");
             final OutputStream hstFileCutoutStream = new FileOutputStream(outputPath.toFile())) {
            slicer.slice(randomAccessDataObject, cutout, hstFileCutoutStream);
            hstFileCutoutStream.flush();
        }

        final Fits expectedFits = new Fits(expectedFile);
        final Fits resultFits = new Fits(outputPath.toFile());

        FitsTest.assertFitsEqual(expectedFits, resultFits);
        Files.deleteIfExists(outputPath);
    }

    @Test
    public void testNoSuchExtension() throws Exception {
        List<ExtensionSlice> slices = new ArrayList<>();
        slices.add(new ExtensionSlice("BOGUS", 367));
        final Cutout cutout = new Cutout();
        cutout.pixelCutouts = slices;

        final NDimensionalSlicer slicer = new NDimensionalSlicer();
        final File file = FileUtil.getFileFromResource("test-hst-mef.fits",
                                                       NDimensionalSlicerTest.class);
        final String configuredTestWriteDir = System.getenv("TEST_WRITE_DIR");
        final String testWriteDir = configuredTestWriteDir == null ? "/tmp" : configuredTestWriteDir;
        final Path outputPath = Files.createTempFile(new File(testWriteDir).toPath(), "test-simple-hst-cutout", ".fits");
        LOGGER.debug("Writing out to " + outputPath);

        try (final RandomAccessDataObject randomAccessDataObject = new RandomAccessFileExt(file, "r");
             final OutputStream hstFileCutoutStream = new FileOutputStream(outputPath.toFile())) {
            slicer.slice(randomAccessDataObject, cutout, hstFileCutoutStream);
            hstFileCutoutStream.flush();
        } catch (IllegalArgumentException illegalArgumentException) {
            Assert.assertTrue("Wrong message", illegalArgumentException.getMessage().contains(
                    "One or more requested slices could not be found"));
        }

        Files.deleteIfExists(outputPath);
    }

    @Test
    public void testDataInPrimaryHeader() throws Exception {
        final List<ExtensionSlice> slices = new ArrayList<>();
        final ExtensionSliceFormat format = new ExtensionSliceFormat();
        slices.add(format.parse("[1][*,1:100]"));
        slices.add(format.parse("[2][50:90,*]"));
        final Cutout cutout = new Cutout();
        cutout.pixelCutouts = slices;

        final int[][] data0 = new int[101][101];
        final int[][] data1 = new int[250][250];
        final int[][] data2 = new int[95][95];

        final NDimensionalSlicer slicer = new NDimensionalSlicer();
        final File file = Files.createTempFile("test-data-primary-", ".fits").toFile();
        try (final DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(file));
             final Fits fits = new Fits()) {
            fits.setStreamWrite(true);
            for (int i = 0; i < 101; i += 1) {
                for (int j = 0; j < 101; j += 1) {
                    data0[i][j] = i * j;
                }
            }
            fits.addHDU(Fits.makeHDU(data0));

            for (int i = 0; i < 250; i += 1) {
                for (int j = 0; j < 250; j += 1) {
                    data1[i][j] = i * j;
                }
            }
            fits.addHDU(Fits.makeHDU(data1));

            for (int i = 0; i < 95; i += 1) {
                for (int j = 0; j < 95; j += 1) {
                    data2[i][j] = i * j;
                }
            }
            fits.addHDU(Fits.makeHDU(data2));

            fits.write(dataOutputStream);
        }

        final String configuredTestWriteDir = System.getenv("TEST_WRITE_DIR");
        final String tempFileExtension = ".fits";
        final String tempFilename = "test-data-primary";
        final Path outputPath = (configuredTestWriteDir == null)
                                ? Files.createTempFile(tempFilename, tempFileExtension)
                                : Files.createTempFile(new File(configuredTestWriteDir).toPath(), tempFilename,
                                                       tempFileExtension);

        LOGGER.debug("Writing out to " + outputPath);

        try (final RandomAccessDataObject randomAccessDataObject = new RandomAccessFileExt(file, "r");
             final OutputStream outputStream = new FileOutputStream(outputPath.toFile())) {
            slicer.slice(randomAccessDataObject, cutout, outputStream);
            outputStream.flush();
        } catch (IllegalArgumentException illegalArgumentException) {
            Assert.assertTrue("Wrong message", illegalArgumentException.getMessage().contains(
                    "One or more requested slices could not be found"));
        }

        try (final Fits checkFits = new Fits(outputPath.toFile())) {
            checkFits.setStreamWrite(true);
            checkFits.read();

            int index = 0;

            final BasicHDU<?> firstImageHDU = checkFits.getHDU(index++);
            Assert.assertArrayEquals("HDU 1 data dimensions do not match.",
                                     new int[]{100, 250}, firstImageHDU.getAxes());

            final BasicHDU<?> secondImageHDU = checkFits.getHDU(index);
            Assert.assertArrayEquals("HDU 2 data dimensions do not match.",
                                     new int[]{95, 41}, secondImageHDU.getAxes());
        }

        Files.deleteIfExists(outputPath);
    }
}
