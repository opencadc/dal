/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2023.                            (c) 2023.
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

package org.opencadc.datalink.server;

import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableReader;
import ca.nrc.cadc.dali.tables.votable.VOTableResource;
import ca.nrc.cadc.dali.tables.votable.VOTableTable;
import ca.nrc.cadc.dali.tables.votable.VOTableWriter;
import ca.nrc.cadc.util.Log4jInit;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.opencadc.datalink.DataLink;
import org.opencadc.datalink.ServiceDescriptor;

/**
 *
 * @author pdowler
 */
public class DataLinkUtilTest {
    private static final Logger log = Logger.getLogger(DataLinkUtilTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.datalink", Level.INFO);
    }
    
    public DataLinkUtilTest() { 
    }
    
    @Test
    public void testEmptyIterator() throws Exception {
        List<DataLink> links = new ArrayList<>();
        
        VOTableDocument vot = DataLinkUtil.createVOTable();
        VOTableTable tab = vot.getResourceByType("results").getTable();
        tab.setTableData(DataLinkUtil.getTableDataWrapper(links.iterator()));
        
        VOTableWriter writer = new VOTableWriter();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writer.write(vot, bos);
        
        String xml = bos.toString("UTF-8");
        log.info("datalink doc:\n" + xml);
        
        VOTableReader r = new VOTableReader(true);
        VOTableDocument doc = r.read(xml);
        Assert.assertNotNull(doc);
        VOTableTable vtt = doc.getResourceByType("results").getTable();
        Assert.assertFalse(vtt.getTableData().iterator().hasNext());
    }
    
    @Test
    public void testMinimalLinks() throws Exception {
        
        DataLink a = new DataLink("a", DataLink.Term.THIS);
        a.accessURL = new URL("https://opencadc.org/example/a");
        DataLink b = new DataLink("b", DataLink.Term.PREVIEW);
        b.serviceDef = "prev-gen";
        DataLink c = new DataLink("c", DataLink.Term.AUXILIARY);
        c.errorMessage = "oops";
        
        final List<DataLink> links = new ArrayList<>();
        links.add(a);
        links.add(b);
        links.add(c);
        final ServiceDescriptor prev = new ServiceDescriptor(new URL("https://opencadc.oreg/example/prev-gen?id=xyz"));
        prev.id = b.serviceDef;
        
        VOTableDocument vot = DataLinkUtil.createVOTable();
        VOTableTable tab = vot.getResourceByType("results").getTable();
        tab.setTableData(DataLinkUtil.getTableDataWrapper(links.iterator()));
        
        VOTableResource metaResource = DataLinkUtil.convert(prev);
        vot.getResources().add(metaResource);
        
        VOTableWriter writer = new VOTableWriter();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writer.write(vot, bos);
        
        String xml = bos.toString("UTF-8");
        log.info("datalink doc:\n" + xml);
        
        VOTableReader r = new VOTableReader(true);
        VOTableDocument doc = r.read(xml);
        Assert.assertNotNull(doc);
        VOTableTable vtt = doc.getResourceByType("results").getTable();
        Assert.assertTrue(vtt.getTableData().iterator().hasNext());
        
        VOTableResource srv = doc.getResourceByID(b.serviceDef);
        Assert.assertNotNull(srv);
        
    }
    
    @Test
    public void testFullLinks() throws Exception {
        
        DataLink a = new DataLink("a", DataLink.Term.THIS);
        a.accessURL = new URL("https://opencadc.org/example/a");
        a.contentLength = 1024L * 1024L;
        a.contentType = "application/fits";
        a.contentQualifier = "image";
        a.description = "a fits image of M31";
        a.linkAuth = DataLink.LinkAuthTerm.OPTIONAL;
        a.linkAuthorized = true;
        a.localSemantics = "foo";

        DataLink b = new DataLink("b", DataLink.Term.PREVIEW);
        b.serviceDef = "prev-gen";
        b.contentLength = 1024L;
        b.contentType = "image/png";
        b.contentQualifier = "image";
        b.description = "a pretty picture of M31";
        b.linkAuth = DataLink.LinkAuthTerm.OPTIONAL;
        b.linkAuthorized = true;
        b.localSemantics = "bar";
        
        DataLink c = new DataLink("c", DataLink.Term.AUXILIARY);
        c.errorMessage = "oops";
        
        final List<DataLink> links = new ArrayList<>();
        links.add(a);
        links.add(b);
        links.add(c);
        
        final ServiceDescriptor prev = new ServiceDescriptor(new URL("https://opencadc.oreg/example/prev-gen?id=xyz"));
        prev.id = b.serviceDef;
        
        VOTableDocument vot = DataLinkUtil.createVOTable();
        VOTableTable tab = vot.getResourceByType("results").getTable();
        tab.setTableData(DataLinkUtil.getTableDataWrapper(links.iterator()));
        
        VOTableResource metaResource = DataLinkUtil.convert(prev);
        vot.getResources().add(metaResource);
        
        VOTableWriter writer = new VOTableWriter();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writer.write(vot, bos);
        
        String xml = bos.toString("UTF-8");
        log.info("datalink doc:\n" + xml);
        
        VOTableReader r = new VOTableReader(true);
        VOTableDocument doc = r.read(xml);
        Assert.assertNotNull(doc);
        VOTableTable vtt = doc.getResourceByType("results").getTable();
        Assert.assertTrue(vtt.getTableData().iterator().hasNext());
        
        VOTableResource srv = doc.getResourceByID(b.serviceDef);
        Assert.assertNotNull(srv);
    }
    
    // TODO: fully test ServiceDescriptor output
}
