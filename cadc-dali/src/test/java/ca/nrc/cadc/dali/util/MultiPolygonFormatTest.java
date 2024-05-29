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
************************************************************************
 */

package ca.nrc.cadc.dali.util;

import ca.nrc.cadc.dali.MultiPolygon;
import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.util.Log4jInit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author pdowler
 */
public class MultiPolygonFormatTest {

    private static final Logger log = Logger.getLogger(MultiPolygonFormatTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.dali", Level.INFO);
    }
    
    public MultiPolygonFormatTest() {
    }

    @Test
    public void testSingleValue() {
        log.debug("testSingleValue");
        try {
            MultiPolygonFormat format = new MultiPolygonFormat();
            MultiPolygon expected = new MultiPolygon();

            Polygon p = new Polygon();
            p.getVertices().add(new Point(10.0, 10.0));
            p.getVertices().add(new Point(12.0, 10.0));
            p.getVertices().add(new Point(11.0, 11.0));

            expected.getPolygons().add(p);
            
            String result = format.format(expected);
            Assert.assertEquals("no extra whitespace", result.trim(), result);
            MultiPolygon actual = format.parse(result);

            Assert.assertEquals("p1", expected.getPolygons().get(0), actual.getPolygons().get(0));
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testMultiValue() {
        log.debug("testMultiValue");
        try {
            MultiPolygonFormat format = new MultiPolygonFormat();
            MultiPolygon expected = new MultiPolygon();

            Polygon p1 = new Polygon();
            p1.getVertices().add(new Point(10.0, 10.0));
            p1.getVertices().add(new Point(12.0, 10.0));
            p1.getVertices().add(new Point(11.0, 11.0));
            
            Polygon p2 = new Polygon();
            p2.getVertices().add(new Point(11.0, 11.0));
            p2.getVertices().add(new Point(13.0, 11.0));
            p2.getVertices().add(new Point(12.0, 12.0));
            
            expected.getPolygons().add(p1);
            expected.getPolygons().add(p2);

            String result = format.format(expected);
            Assert.assertEquals("no extra whitespace", result.trim(), result);
            MultiPolygon actual = format.parse(result);

            Assert.assertEquals("p1", expected.getPolygons().get(0), actual.getPolygons().get(0));
            Assert.assertEquals("p2", expected.getPolygons().get(1), actual.getPolygons().get(1));
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    //@Test
    public void testParseSingleNaN() throws Exception {
        log.debug("testParseSingleNaN");

        MultiPolygonFormat format = new MultiPolygonFormat();

        String noSep = "10.0 10.0 12.0 10.0 11.0 11.0";
        String singleSep = "10.0 10.0 12.0 10.0 11.0 11.0 NaN 11.0 11.0 13.0 11.0 12.0 12.0";
        String singleSepExtraWhitespace = " 10.0 10.0 12.0 10.0 11.0  11.0  NaN  11.0 11.0 13.0 11.0 12.0 12.0 ";
        String shortBeforeSep = "10.0 10.0 12.0 10.0 11.0 NaN 11.0 11.0 13.0 11.0 12.0 12.0";
        String shortAfterSep = "10.0 10.0 12.0 10.0 11.0 11.0 NaN 11.0 11.0 13.0 11.0 12.0";
        String doubleNaN = "10.0 10.0 12.0 10.0 11.0 11.0 NaN NaN 11.0 11.0 13.0 11.0 12.0 12.0";
        
        // OK
        try {
            MultiPolygon mp1 = format.parseSingleNaN(noSep);
            Assert.assertNotNull(mp1);
            Assert.assertEquals(1, mp1.getPolygons().size());
            
            MultiPolygon mp2 = format.parseSingleNaN(singleSep);
            Assert.assertNotNull(mp2);
            Assert.assertEquals(2, mp2.getPolygons().size());
            
            MultiPolygon mp3 = format.parseSingleNaN(singleSepExtraWhitespace);
            Assert.assertNotNull(mp3);
            Assert.assertEquals(2, mp3.getPolygons().size());
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
        
        // invalid
        try {
            MultiPolygon mp = format.parseSingleNaN(shortBeforeSep);
            Assert.fail("expected IllegalArgumentException, got: " + mp);
        } catch (IllegalArgumentException expected) {
            log.info("caught expected fail: shortBeforeSep " + expected);
        }

        try {
            MultiPolygon mp = format.parseSingleNaN(shortAfterSep);
            Assert.fail("expected IllegalArgumentException, got: " + mp);
        } catch (IllegalArgumentException expected) {
            log.info("caught expected fail: shortAfterSep " + expected);
        }

        try {
            MultiPolygon mp = format.parseSingleNaN(doubleNaN);
            Assert.fail("expected IllegalArgumentException, got: " + mp);
        } catch (IllegalArgumentException expected) {
            log.info("caught expected fail: doubleNaN " + expected);
        }
    }
    
    @Test
    public void testParseDoubleNaN() throws Exception {
        log.debug("testParseDoubleNaN");

        MultiPolygonFormat format = new MultiPolygonFormat();

        String noSep = "10.0 10.0 12.0 10.0 11.0 11.0";
        String singleSep = "10.0 10.0 12.0 10.0 11.0 11.0 NaN NaN 11.0 11.0 13.0 11.0 12.0 12.0";
        String shortBeforeSep = "10.0 10.0 12.0 10.0 11.0 NaN NaN 11.0 11.0 13.0 11.0 12.0 12.0";
        String shortAfterSep = "10.0 10.0 12.0 10.0 11.0 11.0 NaN NaN 11.0 11.0 13.0 11.0 12.0";
        String singleNaN = "10.0 10.0 12.0 10.0 11.0 11.0 NaN 11.0 11.0 13.0 11.0 12.0 12.0";
        
        // OK
        try {
            MultiPolygon mp1 = format.parse(noSep);
            Assert.assertNotNull(mp1);
            Assert.assertEquals(1, mp1.getPolygons().size());
            
            MultiPolygon mp2 = format.parse(singleSep);
            Assert.assertNotNull(mp2);
            Assert.assertEquals(2, mp2.getPolygons().size());
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
        
        // invalid
        try {
            MultiPolygon mp = format.parse(shortBeforeSep);
            Assert.fail("expected IllegalArgumentException, got: " + mp);
        } catch (IllegalArgumentException expected) {
            log.info("caught expected fail: shortBeforeSep " + expected);
        }

        try {
            MultiPolygon mp = format.parse(shortAfterSep);
            Assert.fail("expected IllegalArgumentException, got: " + mp);
        } catch (IllegalArgumentException expected) {
            log.info("caught expected fail: shortAfterSep " + expected);
        }

        try {
            MultiPolygon mp = format.parse(singleNaN);
            Assert.fail("expected IllegalArgumentException, got: " + mp);
        } catch (IllegalArgumentException expected) {
            log.info("caught expected fail: doubleNaN " + expected);
        }
    }

    @Test
    public void testNull() throws Exception {
        log.debug("testNull");

        MultiPolygonFormat format = new MultiPolygonFormat();

        String s = format.format(null);
        Assert.assertEquals("", s);

        MultiPolygon object = format.parse(null);
        Assert.assertNull(object);
    }
}
