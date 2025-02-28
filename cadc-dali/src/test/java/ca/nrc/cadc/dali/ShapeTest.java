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
 *
 * @author pdowler
 */
public class ShapeTest {
    private static final Logger log = Logger.getLogger(ShapeTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.dali", Level.INFO);
    }

    public ShapeTest() { 
    }

    @Test
    public void testCircle() throws Exception {
        Circle c = new Circle(new Point(10.0, 20.0), 1.0);
        Assert.assertEquals(10.0, c.getCenter().getLongitude(), 1.0e-6);
        Assert.assertEquals(20.0, c.getCenter().getLatitude(), 1.0e-6);
        Assert.assertEquals(1.0, c.getRadius(), 1.0e-6);
        
        Assert.assertEquals(Math.PI, c.getArea(), 1.0e-6);
        
        try {
            Circle rad = new Circle(new Point(10.0, 20.0), -1.0);
            Assert.fail("expected IllegalArgumentException, got: " + rad);
        } catch (IllegalArgumentException expected) {
            log.info("caught expected: " + expected);
        }
    }

    @Test
    public void testRange() throws Exception {
        Range r = new Range(new DoubleInterval(10.0, 11.0), new DoubleInterval(-0.5, 0.5));
        Assert.assertEquals(10.5, r.getCenter().getLongitude(), 1.0e-6);
        Assert.assertEquals(0.0, r.getCenter().getLatitude(), 1.0e-6);

        // area at equator
        Assert.assertEquals(1.0, r.getArea(), 1.0e-2);
        
        // area at high lat
        r = new Range(new DoubleInterval(10.0, 11.0), new DoubleInterval(60.0, 61.0));
        Assert.assertEquals(0.5, r.getArea(), 1.0e-2);
        
        r = new Range(new DoubleInterval(10.0, 11.0), new DoubleInterval(-61.0, -60.0));
        Assert.assertEquals(0.5, r.getArea(), 1.0e-2);
        
        try {
            Range ir = new Range(new DoubleInterval(-1.0, 1.0), new DoubleInterval(12.0, 13.0));
            Assert.fail("expected IllegalArgumentException, got: " + ir);
        } catch (IllegalArgumentException expected) {
            log.info("caught expected: " + expected);
        }
        
        try {
            Range ir = new Range(new DoubleInterval(1.0, 2.0), new DoubleInterval(91.0, 93.0));
            Assert.fail("expected IllegalArgumentException, got: " + ir);
        } catch (IllegalArgumentException expected) {
            log.info("caught expected: " + expected);
        }
        
        try {
            Range ir = new Range(new DoubleInterval(1.0, 2.0), new DoubleInterval(-91.0, -90.0));
            Assert.fail("expected IllegalArgumentException, got: " + ir);
        } catch (IllegalArgumentException expected) {
            log.info("caught expected: " + expected);
        }
    }
    
    @Test
    public void testCircleToPolygonApproximatiom() {
        try {
            Circle c = new Circle(new Point(12.0, 34.0), 1.0);
            double ca = c.getArea();

            for (int i = 4; i < 21; i++) {
                Polygon poly = Circle.generatePolygonApproximation(c, i);
                double pa = poly.getArea();
                double da = pa / ca;
                log.info("n=" + i + " poly: " + pa + " (" + da + ")");
            }
            log.info("circle: " + ca);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    // PolygonTest in separate class
}
