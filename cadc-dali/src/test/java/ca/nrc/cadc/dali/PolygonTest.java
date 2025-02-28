/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2025.                            (c) 2025.
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

package ca.nrc.cadc.dali;

import ca.nrc.cadc.util.Log4jInit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * Port of CAOM-2.4 polygon validation tests.
 *
 * @author pdowler
 */
public class PolygonTest {

    private static final Logger log = Logger.getLogger(PolygonTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.dali", Level.INFO);
    }

    public PolygonTest() {
    }

    @Test
    public void testValidPolygon() {
        log.info("no transform...");
        try {
            Polygon p = new Polygon();
            p.getVertices().add(new Point(180.0, 0.0));
            p.getVertices().add(new Point(181.0, 1.0));
            p.getVertices().add(new Point(181.0, 0.0));
            log.info("poly: " + p);
            
            Assert.assertTrue("CCW", p.getCounterClockwise());
            Assert.assertEquals(0.5, p.getArea(), 0.01);
            
            p.validate();
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
        
        log.info("transform away from meridian...");
        try {
            Polygon p = new Polygon();
            p.getVertices().add(new Point(0.0, 0.0));
            p.getVertices().add(new Point(1.0, 1.0));
            p.getVertices().add(new Point(2.0, 0.0));
            log.info("poly: " + p);
            
            Assert.assertTrue("CCW", p.getCounterClockwise());
            Assert.assertEquals(1.0, p.getArea(), 0.01);
            
            Point c = p.getCenter();
            Assert.assertNotNull(c);
            Assert.assertEquals(1.0, c.getLongitude(), 0.01);
            Assert.assertEquals(0.33, c.getLatitude(), 0.01);

            Assert.assertNotNull(p.getSize());
            Assert.assertEquals(2.0, p.getSize(), 0.01);
            
            p.validate();
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
        
        log.info("north pole...");
        try {
            Polygon p = new Polygon();
            p.getVertices().add(new Point(180.0, 89.0));
            p.getVertices().add(new Point(181.0, 90.0));
            p.getVertices().add(new Point(182.0, 89.0));
            log.info("poly: " + p);
            
            Assert.assertTrue("CCW", p.getCounterClockwise());
            Assert.assertEquals(1.0 * Math.cos(Math.toRadians(89.0)), p.getArea(), 0.001); // very small
            
            Point c = p.getCenter();
            Assert.assertNotNull(c);
            Assert.assertEquals(181.0, c.getLongitude(), 0.01);
            Assert.assertEquals(89.33, c.getLatitude(), 0.01);

            Assert.assertNotNull(p.getSize());
            Assert.assertEquals(1.0, p.getSize(), 0.01); // dDEC
            
            p.validate();
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }

        log.info("south pole...");
        try {
            Polygon p = new Polygon();
            p.getVertices().add(new Point(180.0, -89.0));
            p.getVertices().add(new Point(182.0, -89.0));
            p.getVertices().add(new Point(181.0, -90.0));
            log.info("poly: " + p);
            
            Assert.assertTrue("CCW", p.getCounterClockwise());
            Assert.assertEquals(1.0 * Math.cos(Math.toRadians(-89.0)), p.getArea(), 0.001); // very small
            
            Point c = p.getCenter();
            Assert.assertNotNull(c);
            Assert.assertEquals(181.0, c.getLongitude(), 0.01);
            Assert.assertEquals(-89.33, c.getLatitude(), 0.01);

            Assert.assertNotNull(p.getSize());
            Assert.assertEquals(1.0, p.getSize(), 0.01); // dDEC
            
            p.validate();
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidPolygonFromFootprintPy() {
        // footprint.py output
        // old: Polygon ICRS 259.006152 60.047132 259.087308 60.087963 259.089760 60.087730 259.132216 60.068435
        //                   259.131770 60.067704 259.133299 60.067895 259.174702 60.049127 259.093400 60.010342 
        //                   259.078635 60.015734
        String[] oldS = ("259.006152 60.047132 259.087308 60.087963 259.089760 60.087730 259.132216 60.068435"
                + " 259.131770 60.067704 259.133299 60.067895 259.174702 60.049127 259.093400 60.010342"
                + " 259.078635 60.015734").split(" ");

        // cur: Polygon ICRS 259.006152 60.047132 259.078635 60.015734 259.093400 60.010342 259.174702 60.049127
        //                   259.133299 60.067895 259.131770 60.067704 259.132216 60.068435 259.089760 60.087730
        //                   259.087308 60.087963
        String[] curS = ("259.006152 60.047132 259.078635 60.015734 259.093400 60.010342 259.174702 60.049127"
                + " 259.133299 60.067895 259.131770 60.067704 259.132216 60.068435 259.089760 60.087730"
                + " 259.087308 60.087963").split(" ");

        String[] test = oldS;
        try {
            Polygon p = new Polygon();
            for (int i = 0; i < test.length; i += 2) {
                double x = Double.parseDouble(test[i]);
                double y = Double.parseDouble(test[i + 1]);
                p.getVertices().add(new Point(x, y));
            }

            p.validate();
            
            log.info("testValidPolygonFromFootprintPy: " + p);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testInvalidLongitude() {
        try {
            Polygon p = new Polygon();
            p.getVertices().add(new Point(360.0, 2.0));
            p.getVertices().add(new Point(359.0, 4.0));
            p.getVertices().add(new Point(361.0, 3.0));

            p.validate();
            Assert.fail("expected IllegalArgumentException, created: " + p);
        } catch (InvalidPolygonException expected) {
            log.info("caught expected: " + expected);
            Assert.assertTrue(expected.getMessage().contains("longitude"));
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testInvalidLatitude() {
        try {
            Polygon p = new Polygon();
            p.getVertices().add(new Point(2.0, 89.0));
            p.getVertices().add(new Point(1.0, 91.0));
            p.getVertices().add(new Point(3.0, 90.0));

            p.validate();
            Assert.fail("expected IllegalArgumentException, created: " + p);
        } catch (InvalidPolygonException expected) {
            log.info("caught expected: " + expected);
            Assert.assertTrue(expected.getMessage().contains("latitude"));
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testInvalidPolygonCW() {
        try {
            Polygon p = new Polygon();
            p.getVertices().add(new Point(0.0, 0.0));
            p.getVertices().add(new Point(1.0, 1.0));
            p.getVertices().add(new Point(2.0, 0.0));
            Assert.assertTrue("ccw", p.getCounterClockwise());
            p.validate();
            double a = p.getArea();
            Point c = p.getCenter();
            log.info("ccw: " + p + " a:" + p.getArea() + " c:" + p.getCenter());
            
            Polygon cw = Polygon.flip(p);
            Assert.assertFalse("cw", cw.getCounterClockwise());
            cw.validate();
            
            // TBD: instead of clockwise being invalid, inside-on-the-left can
            // be interpretted as the other large part of the unit sphere
            //Assert.fail("expected InvalidPolygonException, created: " + p);
            log.info("cw poly: " + cw + " a:" + cw.getArea() + " c:" + cw.getCenter());
            Assert.assertTrue(cw.getArea() > 40000.0);
            Assert.assertEquals(181.0, cw.getCenter().getLongitude(), 0.01);
            Assert.assertEquals(-0.33, cw.getCenter().getLatitude(), 0.01);
            
        } catch (InvalidPolygonException expected) {
            log.info("caught expected: " + expected);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testInvalidPolygonSegmentIntersect() {
        try {
            Polygon poly = new Polygon();
            poly.getVertices().add(new Point(2.0, 2.0));
            poly.getVertices().add(new Point(2.0, 4.0));
            poly.getVertices().add(new Point(4.0, 2.0));
            poly.getVertices().add(new Point(4.0, 4.0));

            try {
                poly.validate();
                Assert.fail("expected InvalidPolygonException - got: " + poly);
            } catch (InvalidPolygonException expected) {
                log.info("testValidateSegments: butterfly " + expected);
                Assert.assertTrue(expected.getMessage().startsWith("invalid Polygon: segment intersect "));
            }

            poly.getVertices().clear();
            poly.getVertices().add(new Point(2.0, 2.0));
            poly.getVertices().add(new Point(2.0, 4.0));
            poly.getVertices().add(new Point(5.0, 4.0)); // extra small loop
            poly.getVertices().add(new Point(4.0, 5.0));
            poly.getVertices().add(new Point(4.0, 2.0));

            try {
                poly.validate();
                Assert.fail("expected InvalidPolygonException - got: " + poly);
            } catch (InvalidPolygonException expected) {
                log.info("testValidateSegments: small loop " + expected);
                Assert.assertTrue(expected.getMessage().contains("invalid Polygon: segment intersect "));
            }
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
}
