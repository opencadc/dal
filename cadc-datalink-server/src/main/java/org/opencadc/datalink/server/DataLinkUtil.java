/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2019.                            (c) 2019.
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

import ca.nrc.cadc.dali.tables.TableData;
import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.tables.votable.VOTableGroup;
import ca.nrc.cadc.dali.tables.votable.VOTableParam;
import ca.nrc.cadc.dali.tables.votable.VOTableResource;
import ca.nrc.cadc.dali.tables.votable.VOTableTable;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.opencadc.datalink.DataLink;
import org.opencadc.datalink.ServiceDescriptor;
import org.opencadc.datalink.ServiceParameter;

/**
 *
 * @author pdowler
 */
public abstract class DataLinkUtil {

    private static final Logger log = Logger.getLogger(DataLinkUtil.class);

    private DataLinkUtil() {
    }

    /**
     * Get list of table fields that matches the iteration order of the DataLink.
     *
     * @return List of VOTabel FIELD objects for links table
     */
    public static List<VOTableField> getFields() {
        List<VOTableField> fields = new ArrayList<VOTableField>();
        VOTableField f;

        f = new VOTableField("ID", "char", "*");
        f.ucd = "meta.id;meta.main";
        fields.add(f);

        f = new VOTableField("access_url", "char", "*");
        f.ucd = "meta.ref.url";
        fields.add(f);

        f = new VOTableField("service_def", "char", "*");
        f.ucd = "meta.ref";
        fields.add(f);

        f = new VOTableField("error_message", "char", "*");
        f.ucd = "meta.code.error";
        fields.add(f);

        f = new VOTableField("semantics", "char", "*");
        f.ucd = "meta.code";
        fields.add(f);

        f = new VOTableField("description", "char", "*");
        f.ucd = "meta.note";
        fields.add(f);

        f = new VOTableField("content_type", "char", "*");
        f.ucd = "meta.code.mime";
        fields.add(f);

        f = new VOTableField("content_length", "long");
        f.unit = "byte";
        f.ucd = "phys.size;meta.file";
        fields.add(f);
        
        f = new VOTableField("content_qualifier", "char", "*");
        f.ucd = null; // TODO: some indication that this is a vocabulary term??
        f.description = "the nature of the thing the link will deliver (DataLink-1.1)";
        fields.add(f);

        f = new VOTableField("link_auth", "char", "*");
        f.ucd = "meta.code";
        f.description = "link supports authentication (DataLink-1.1)";
        fields.add(f);
        
        f = new VOTableField("link_authorized", "boolean");
        f.ucd = "meta.code";
        f.description = "the current authenticated identity is authorized (DataLink-1.1)";
        fields.add(f);
        
        // custom
        f = new VOTableField("readable", "boolean");
        f.description = "equivalent to link_authorized; backwards compat support";
        f.ucd = "meta.code";
        fields.add(f);

        return fields;
    }

    public static VOTableDocument createVOTable() {
        VOTableDocument vot = new VOTableDocument();
        VOTableResource vr = new VOTableResource("results");
        vot.getResources().add(vr);
        VOTableTable tab = new VOTableTable();
        vr.setTable(tab);
        tab.getFields().addAll(getFields());
        return vot;
    }

    private static class TableDataWrapper implements TableData, Iterator<List<Object>> {

        Iterator<DataLink> iter;

        TableDataWrapper(Iterator<DataLink> iter) {
            this.iter = iter;
        }

        @Override
        public Iterator<List<Object>> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            return iter.hasNext();
        }

        @Override
        public List<Object> next() {
            DataLink dl = iter.next();
            return linkToRow(dl);
        }
        
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static TableData getTableDataWrapper(Iterator<DataLink> iter) {
        return new TableDataWrapper(iter);
    }

    public static List<Object> linkToRow(DataLink dl) {
        List vals = new ArrayList();
        vals.add(dl.getID());
        vals.add(safeToString(dl.accessURL));
        vals.add(dl.serviceDef);
        vals.add(dl.errorMessage);
        vals.add(dl.getSemantics().getValue());
        vals.add(dl.description);
        vals.add(dl.contentType);
        vals.add(dl.contentLength);
        vals.add(dl.contentQualifier);
        vals.add(safeToString(dl.linkAuth));
        vals.add(dl.linkAuthorized);
        vals.add(dl.readable);
        
        return vals;
    }

    public static VOTableResource convert(ServiceDescriptor sd) {
        VOTableResource metaResource = new VOTableResource("meta");
        metaResource.id = sd.id;
        metaResource.utype = "adhoc:service";
        String val = null;
        String len = null;
        if (sd.resourceIdentifier != null) {
            val = sd.resourceIdentifier.toASCIIString();
            len = Integer.toString(val.length());
            metaResource.getParams().add(new VOTableParam("resourceIdentifier", "char", len, val));
        }
        if (sd.standardID != null) {
            val = sd.standardID.toASCIIString();
            len = Integer.toString(val.length());
            metaResource.getParams().add(new VOTableParam("standardID", "char", len, val));
        }
        if (sd.contentType != null) {
            val = sd.contentType;
            len = Integer.toString(val.length());
            metaResource.getParams().add(new VOTableParam("contentType", "char", len, val));
        }
        if (sd.exampleURL != null) {
            val = sd.exampleURL.toExternalForm();
            len = Integer.toString(val.length());
            VOTableParam ex = new VOTableParam("exampleURL", "char", len, val);
            ex.description = sd.exampleDescription;
            metaResource.getParams().add(ex);
        }
        
        val = sd.getAccessURL().toExternalForm();
        len = Integer.toString(val.length());
        metaResource.getParams().add(new VOTableParam("accessURL", "char", len, val));

        VOTableGroup inputParams = new VOTableGroup("inputParams");
        for (ServiceParameter p : sd.getInputParams()) {
            VOTableParam vp = new VOTableParam(p.getName(), p.getDatatype(), p.getArraysize(), p.getValue());
            vp.ref = p.getRef();
            vp.ucd = p.getUcd();
            vp.unit = p.unit;
            vp.utype = p.utype;
            vp.xtype = p.xtype;
            vp.description = p.description;
            if (p.getMin() != null || p.getMax() != null || !p.getOptions().isEmpty()) {
                vp.setMin(p.getMin());
                vp.setMax(p.getMax());
                vp.getOptions().addAll(p.getOptions());
            }
            inputParams.getParams().add(vp);
        }
        metaResource.getGroups().add(inputParams);

        return metaResource;
    }

    private static String safeToString(URI uri) {
        if (uri == null) {
            return null;
        }
        return uri.toASCIIString();
    }

    private static String safeToString(URL url) {
        if (url == null) {
            return null;
        }
        return url.toExternalForm();
    }

    private static String safeToString(DataLink.LinkAuthTerm t) {
        if (t == null) {
            return null;
        }
        return t.getValue();
    }
}
