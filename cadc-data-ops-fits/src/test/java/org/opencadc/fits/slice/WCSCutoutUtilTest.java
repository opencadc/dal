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
import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.header.Standard;
import nom.tam.fits.header.extra.NOAOExt;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.soda.PixelRange;
import org.opencadc.soda.server.Cutout;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;


public class WCSCutoutUtilTest extends BaseCutoutTest {
    private static final Logger LOGGER = Logger.getLogger(WCSCutoutUtilTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.wcs", Level.DEBUG);
        Log4jInit.setLevel("org.opencadc.fits.slice", Level.DEBUG);
    }

    @Test
    public void testMultipleWCS() throws Exception {
        final long startMillis = System.currentTimeMillis();

        final String headerFileName = "test-alma-cube-band-header.txt";
        final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);
        final Cutout cutout = new Cutout();
        cutout.pos = new Circle(new Point(128.638D, 17.33D), 0.01D);
        cutout.band = new Interval<>(1.3606E-3D, 1.3616E-3D);

        try (final InputStream inputStream = Files.newInputStream(testFile.toPath());
             final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            final Header testHeader = new Header();
            final char[] buff = new char[80];
            while (bufferedReader.read(buff) >= 0) {
                testHeader.addLine(HeaderCard.create(new String(buff)));
            }

            final PixelRange[] resultRanges = WCSCutoutUtil.getBounds(testHeader, cutout);

            // Combined Circle cutout (axes 1 & 2), as well as Band cutout on axis 3.
            final PixelRange[] expectedRanges = new PixelRange[] {
                    new PixelRange(17, 400),
                    new PixelRange(1, 52),
                    new PixelRange(1, 18),
                    new PixelRange(1, 1)
            };

            Assert.assertArrayEquals("Wrong cutout bounds for ALMA Cube.", expectedRanges, resultRanges);
        }
        LOGGER.debug("WCSCutoutUtilTest.testMultipleWCS OK: " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    public void testWCSAdjustment() throws Exception {
        final String headerFileName = "test-blast-header-1.txt";
        final File testFile = FileUtil.getFileFromResource(headerFileName, CircleCutoutTest.class);
        final int[] corners = new int[]{0, 0};
        final int[] stridingValues = new int[]{1, 5};

        try (final InputStream inputStream = Files.newInputStream(testFile.toPath());
             final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            final Header testHeader = new Header();
            final char[] buff = new char[80];
            while (bufferedReader.read(buff) >= 0) {
                testHeader.addLine(HeaderCard.create(new String(buff)));
            }

            final double originalCD11 = testHeader.getDoubleValue(NOAOExt.CD1_1);
            final double originalCD21 = testHeader.getDoubleValue(NOAOExt.CD2_1);

            WCSCutoutUtil.adjustHeaders(testHeader, testHeader.getIntValue(Standard.NAXIS), corners, stridingValues);

            Assert.assertEquals("Should remain unchanged as only applies to second axis.", originalCD11, testHeader.getDoubleValue(NOAOExt.CD1_1),
                                0.0D);
            Assert.assertEquals("Should be adjusted.", originalCD21 * ((double) stridingValues[1]), testHeader.getDoubleValue(NOAOExt.CD2_1),
                                0.0D);
        }
    }
}
