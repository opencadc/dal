/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2011.                            (c) 2011.
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
*  $Revision: 5 $
*
************************************************************************
 */

package ca.nrc.cadc.dali.tables.votable;

import ca.nrc.cadc.dali.util.Format;
import java.util.ArrayList;
import java.util.List;

/**
 * VOTable-specific extension of TableColumn. This adds the XML ID/IDREF attributes
 * and a list of string values as permitted by the VOTable schema.
 *
 * @author pdowler
 */
public class VOTableField {

    private String name;
    private String datatype;

    protected String arraysize;
    protected int[] arrayShape;
    protected Format<Object> format;

    public String ucd;
    public String unit;
    public String utype;
    public String xtype;
    public String description;
    public String nullValue;

    // TODO: add precision support and use it to configure numeric format objects
    public String id;
    public String ref;

    private final List<String> values = new ArrayList<String>();

    protected VOTableField() {
    }

    public VOTableField(String name, String datatype) {
        this(name, datatype, null);
    }

    public VOTableField(String name, String datatype, String arraysize) {
        this(name, datatype, arraysize, null);
    }

    public VOTableField(String name, String datatype, String arraysize, Format<Object> format) {
        this.name = name;
        this.datatype = datatype;
        this.arraysize = arraysize;
        this.format = format;
        validateArraysize();
    }

    private void validateArraysize() {
        this.arrayShape = VOTableUtil.getArrayShape(arraysize);
    }

    public String getName() {
        return name;
    }

    public String getDatatype() {
        return datatype;
    }

    public String getArraysize() {
        return arraysize;
    }

    public Format<Object> getFormat() {
        return format;
    }

    public int[] getArrayShape() {
        return arrayShape;
    }

    public List<String> getValues() {
        return values;
    }

    public void setFormat(Format<Object> format) {
        this.format = format;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName()).append("[");
        sb.append(name).append(",");
        sb.append(datatype);
        if (arraysize != null) {
            sb.append(",").append(arraysize);
        }
        if (xtype != null) {
            sb.append(",").append(xtype);
        }
        sb.append("]");
        return sb.toString();
    }
}
