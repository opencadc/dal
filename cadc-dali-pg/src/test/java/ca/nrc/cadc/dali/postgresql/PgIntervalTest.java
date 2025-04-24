/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2017.                            (c) 2017.
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

package ca.nrc.cadc.dali.postgresql;

import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.util.Log4jInit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.postgresql.geometric.PGpolygon;

/**
 *
 * @author pdowler
 */
public class PgIntervalTest {

    private static final Logger log = Logger.getLogger(PgIntervalTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.dali", Level.INFO);
    }

    public PgIntervalTest() {
    }

    PgInterval gen = new PgInterval();

    @Test
    public void testNull() {
        try {
            Interval iexp = null;
            Interval<Double>[] expected = null;

            PGpolygon ip;

            ip = gen.generatePolygon2D(iexp);
            Assert.assertNull(ip);

            Interval iact = gen.getInterval(null);
            Assert.assertNull(iact);

            ip = gen.generatePolygon2D(expected);
            Assert.assertNull(ip);

            Interval<Double>[] actual = gen.getIntervalArray(null);
            Assert.assertNull(actual);

            expected = new Interval[0];
            ip = gen.generatePolygon2D(expected);
            Assert.assertNull("empty", ip);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testGetPolygonFromInterval() {
        try {
            Interval expected = new Interval(1.0, 3.0);

            PGpolygon ip = gen.generatePolygon2D(expected);
            Assert.assertNotNull(ip);
            Assert.assertEquals(4, ip.points.length);

            String sval = ip.getValue(); // equiv to db round-trip
            log.info("testGetPolygonFromInterval: " + sval);

            Interval actual = gen.getInterval(sval);
            Assert.assertNotNull(actual);
            log.info("actual: " + actual.getLower() + "," + actual.getUpper());
            Assert.assertEquals(expected.getLower(), actual.getLower());
            Assert.assertEquals(expected.getUpper(), actual.getUpper());
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testGetPolygonFromIntervalArray() {
        try {
            Interval<Double>[] expected = new Interval[]{
                new Interval<>(1.0, 1.2),
                new Interval<>(2.8, 3.0)
            };

            PGpolygon ip = gen.generatePolygon2D(expected);
            Assert.assertNotNull(ip);
            Assert.assertEquals(8, ip.points.length);

            String sval = ip.getValue(); // equiv to db round-trip
            log.info("testGetPolygonFromIntervalArray: " + sval);

            Interval<Double>[] actual = gen.getIntervalArray(sval);
            Assert.assertNotNull(actual);
            Assert.assertEquals(expected.length, actual.length);
            for (int i = 0; i < expected.length; i++) {
                Interval<Double> ie = expected[i];
                Interval<Double> ia = actual[i];
                log.info(i + " actual: " + ia.getLower() + "," + ia.getUpper());
                Assert.assertEquals(i + " lower: ", ie.getLower(), ia.getLower());
                Assert.assertEquals(i + " upper: ", ie.getUpper(), ia.getUpper());
            }
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
}
