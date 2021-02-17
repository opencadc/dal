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

import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;
import nom.tam.fits.Fits;
import nom.tam.fits.Header;
import nom.tam.util.RandomAccessDataObject;
import nom.tam.util.RandomAccessFileExt;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;


public class FITSHeaderWCSKeywordsTest {
    private static final Logger LOGGER = Logger.getLogger(FITSHeaderWCSKeywordsTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.fits", Level.DEBUG);
    }

    @Test
    public void testInvalidConstructor() {
        final long startMillis = System.currentTimeMillis();
        try {
            new FITSHeaderWCSKeywords(null);
            Assert.fail("Should throw IllegalArgumentException.");
        } catch (IllegalArgumentException illegalArgumentException) {
            // Good.
        }
        LOGGER.debug("FITSHeaderWCSKeywordsTest.testInvalidConstructor ran in "
                     + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    public void testEmptyHeaderConstructor() {
        final long startMillis = System.currentTimeMillis();
        final Header header = new Header();
        final FITSHeaderWCSKeywords testSubject = new FITSHeaderWCSKeywords(header);
        Assert.assertEquals("Should be empty.", 0, testSubject.getNumberOfKeywords());
        LOGGER.debug("FITSHeaderWCSKeywordsTest.testEmptyHeaderConstructor ran in "
                     + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    public void testGet() throws Exception {
        final long startMillis = System.currentTimeMillis();
        try (final RandomAccessDataObject randomAccessDataObject =
                     new RandomAccessFileExt(FileUtil.getFileFromResource("sample-mef.fits",
                                                                          FITSHeaderWCSKeywordsTest.class), "r");
             final Fits fits = new Fits(randomAccessDataObject)) {

            // Just to cache it up front, and ensure that it can be read.
            fits.read();

            final Header header = fits.getHDU(0).getHeader();
            final FITSHeaderWCSKeywords testSubject = new FITSHeaderWCSKeywords(header);
            final Counter counter = new Counter();

            header.iterator().forEachRemaining(headerCard -> {
                final Class<?> valueType = headerCard.valueType();
                final String headerCardKey = headerCard.getKey();
                final String headerCardValue = headerCard.getValue();

                if (valueType == Integer.class) {
                    Assert.assertEquals("Wrong integer value.", Integer.parseInt(headerCardValue),
                                        testSubject.getIntValue(headerCardKey));
                } else if (valueType == Double.class || valueType == BigDecimal.class) {
                    Assert.assertEquals("Wrong double value.", Double.parseDouble(headerCardValue),
                                        testSubject.getDoubleValue(headerCardKey), 0.0D);
                } else {
                    Assert.assertEquals("Wrong default value for " + headerCardKey + ".", headerCardValue,
                                        testSubject.getStringValue(headerCardKey));
                }

                counter.increment();
            });

            Assert.assertEquals("Should've created " + counter.count + " keywords.",
                                counter.count, testSubject.getNumberOfKeywords());
        }
        LOGGER.debug("FITSHeaderWCSKeywordsTest.testGet ran in " + (System.currentTimeMillis() - startMillis) + " ms");
    }

    @Test
    public void testIterator() throws Exception {
        final long startMillis = System.currentTimeMillis();
        try (final RandomAccessDataObject randomAccessDataObject =
                     new RandomAccessFileExt(FileUtil.getFileFromResource("sample-mef.fits",
                                                                          FITSHeaderWCSKeywordsTest.class), "r");
             final Fits fits = new Fits(randomAccessDataObject)) {

            // Just to cache it up front, and ensure that it can be read.
            fits.read();

            final Header header = fits.getHDU(0).getHeader();
            final FITSHeaderWCSKeywords testSubject = new FITSHeaderWCSKeywords(header);
            final Counter counter = new Counter();

            testSubject.iterator().forEachRemaining(stringObjectEntry -> {
                LOGGER.debug("iterator.next: " + stringObjectEntry.getKey() + " -> " + stringObjectEntry.getValue());
                counter.increment();
            });

            Assert.assertEquals("Should've created " + counter.count + " keywords.",
                                counter.count, testSubject.getNumberOfKeywords());
        }
        LOGGER.debug("FITSHeaderWCSKeywordsTest.testIterator ran in " + (System.currentTimeMillis() - startMillis)
                     + " ms");
    }

    @Test
    public void testEmptyIterator() {
        final long startMillis = System.currentTimeMillis();
        final FITSHeaderWCSKeywords testSubject = new FITSHeaderWCSKeywords();
        final Counter counter = new Counter();

        testSubject.iterator().forEachRemaining(stringObjectEntry -> {
            LOGGER.debug("iterator.next: " + stringObjectEntry.getKey() + " -> " + stringObjectEntry.getValue());
            counter.increment();
        });

        Assert.assertEquals("Should've created " + counter.count + " keywords.",
                            counter.count, testSubject.getNumberOfKeywords());
        LOGGER.debug("FITSHeaderWCSKeywordsTest.testIterator ran in " + (System.currentTimeMillis() - startMillis)
                     + " ms");
    }


    public static final class Counter {
        // Should only ever be modified with the increment method.
        private int count = 0;

        void increment() {
            count++;
        }
    }
}