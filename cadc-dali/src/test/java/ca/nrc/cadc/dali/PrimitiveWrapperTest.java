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
import java.net.URI;
import java.security.MessageDigest;
import java.util.UUID;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.persist.Entity;

/**
 * Single test to verify that classes implementing PrimitiveWrapper return
 * acceptable wrapped value types.
 * 
 * @author pdowler
 */
public class PrimitiveWrapperTest {
    private static final Logger log = Logger.getLogger(PrimitiveWrapperTest.class);

    static {
        Log4jInit.setLevel(PrimitiveWrapperTest.class.getPackageName(), Level.INFO);
    }

    public PrimitiveWrapperTest() { 
    }
    
    @Test
    public void testDataTypes() throws Exception {
        // static UUID because it is included in metaChecksum
        final UUID id = UUID.fromString("15c271c5-c8f9-4484-a87f-8545f57e5f0e");
        // previously computed value to make sure behaviour is stable
        final URI expected = URI.create("md5:748bd39388a2bb672019c62acb0841b9");
        
        TestEntity e = new TestEntity(id);
        log.info("Entity.id: " + e.getID());
        URI mcs = e.computeMetaChecksum(MessageDigest.getInstance("md5"));
        log.info("metaChecksum: " + mcs);
        Assert.assertNotNull(mcs);
        Assert.assertEquals("md5", mcs.getScheme());
        Assert.assertEquals(expected, mcs);
    }
    
    private class TestEntity extends Entity {
        public Interval<Double> doubleInterval = new Interval<>(1.0, 2.0);
        public Interval<Long> longInterval = new Interval<>(3L, 4L);
        public Point point = new Point(10.0, 11.0);
        public Circle circle = new Circle(point, 0.5);
        public Polygon poly = new Polygon();
        public MultiShape multiShape = new MultiShape();
        
        TestEntity(UUID id) {
            super(id, false, true, true, true);
            poly.getVertices().add(new Point(2.0, 2.0));
            poly.getVertices().add(new Point(1.0, 3.0));
            poly.getVertices().add(new Point(2.0, 4.0));
            poly.getVertices().add(new Point(3.0, 3.0));
            
            multiShape.getShapes().add(circle);
            multiShape.getShapes().add(poly);
        }
    }
}
