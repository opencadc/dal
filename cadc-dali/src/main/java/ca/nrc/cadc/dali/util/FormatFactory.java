/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2009.                            (c) 2009.
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
 *  $Revision: 4 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.dali.util;

import ca.nrc.cadc.dali.tables.votable.VOTableField;
import org.apache.log4j.Logger;

/**
 * This factory class implements the mapping of datatypes to VOTable according
 * to the DALI-1.0 specification.
 *
 * @author jburke
 */
public class FormatFactory {

    private static final Logger log = Logger.getLogger(FormatFactory.class);

    /**
     *
     * @param field
     * @return
     */
    public Format getFormat(VOTableField field) {
        String datatype = field.getDatatype();
        Format ret = new DefaultFormat();

        if (datatype == null) {
            ret = new DefaultFormat();
        } else if (datatype.equalsIgnoreCase("boolean")) {
            ret = new BooleanFormat();
        } else if (datatype.equalsIgnoreCase("bit")) {
            if (isArray(field)) {
                ret = new ByteArrayFormat();
            } else {
                ret = new ByteFormat();
            }
        } else if (datatype.equalsIgnoreCase("unsignedByte")) {
            if (isArray(field)) {
                ret = new ByteArrayFormat();
            } else {
                ret = new ByteFormat();
            }
        } else if (datatype.equalsIgnoreCase("short")) {
            if (isArray(field)) {
                if ("interval".equalsIgnoreCase(field.xtype)) {
                    ret = new LongIntervalFormat();
                } else {
                    ret = new ShortArrayFormat();
                }
            } else {
                ret = new ShortFormat(field.nullValue);
            }
        } else if (datatype.equalsIgnoreCase("int")) {
            if (isArray(field)) {
                if ("interval".equalsIgnoreCase(field.xtype)) {
                    ret = new LongIntervalFormat();
                } else {
                    ret = new IntArrayFormat();
                }
            } else {
                ret = new IntegerFormat(field.nullValue);
            }
        } else if (datatype.equalsIgnoreCase("long")) {
            if (isArray(field)) {
                if ("interval".equalsIgnoreCase(field.xtype)) {
                    ret = new LongIntervalFormat();
                } else {
                    ret = new LongArrayFormat();
                }
            } else {
                ret = new LongFormat(field.nullValue);
            }
        } else if (datatype.equalsIgnoreCase("unicodeChar")) {
            ret = new StringFormat();
        } else if (datatype.equalsIgnoreCase("float")) {
            if (isArray(field)) {
                if ("point".equalsIgnoreCase(field.xtype)) {
                    ret = new PointFormat();
                } else if ("circle".equalsIgnoreCase(field.xtype)) {
                    ret = new CircleFormat();
                } else if ("polygon".equalsIgnoreCase(field.xtype)) {
                    ret = new PolygonFormat();
                } else if ("interval".equalsIgnoreCase(field.xtype)) {
                    ret = new DoubleIntervalFormat();
                } else if (field.getArrayShape().length == 1) {
                    ret = new FloatArrayFormat();
                }
            } else {
                ret = new FloatFormat();
            }
        } else if (datatype.equalsIgnoreCase("double")) {
            if (isArray(field)) {
                if ("point".equalsIgnoreCase(field.xtype)) {
                    ret = new PointFormat();
                } else if ("circle".equalsIgnoreCase(field.xtype)) {
                    ret = new CircleFormat();
                } else if ("polygon".equalsIgnoreCase(field.xtype)) {
                    ret = new PolygonFormat();
                } else if ("interval".equalsIgnoreCase(field.xtype)) {
                    if (field.getArrayShape().length == 1 && field.getArrayShape()[0] == 2) {
                        ret = new DoubleIntervalFormat();
                    } else if (field.getArrayShape().length == 2 && field.getArrayShape()[0] == 2) {
                        ret = new DoubleIntervalArrayFormat();
                    }
                } else if (field.getArrayShape().length == 1) {
                    ret = new DoubleArrayFormat();
                } else if (field.getArrayShape().length == 2) {
                    ret = new DoubleArray2DFormat(field.getArrayShape());
                }
            } else {
                ret = new DoubleFormat();
            }
        } else if (datatype.equalsIgnoreCase("floatComplex")) {
            throw new UnsupportedOperationException("floatComplex datatype not supported for column " + field.getName());
        } else if (datatype.equalsIgnoreCase("doubleComplex")) {
            throw new UnsupportedOperationException("doubleComplex datatype not supported for column " + field.getName());
        } else if (datatype.equalsIgnoreCase("char")) {
            if (isArray(field)) {
                if ("timestamp".equalsIgnoreCase(field.xtype)) { // DALI-1.1
                    ret = new UTCTimestampFormat();
                } else if (field.xtype != null && field.xtype.endsWith("shape")) { // DALI-1.2 prototype, ignore prefix
                    ret = new ShapeFormat();
                } else if ("adql:timestamp".equalsIgnoreCase(field.xtype)) {
                    ret = new UTCTimestampFormat();
                } else if ("adql:point".equalsIgnoreCase(field.xtype)) {
                    ret = new STCPositionFormat();
                } else if ("adql:region".equalsIgnoreCase(field.xtype)) {
                    ret = new STCRegionFormat();
                } else if ("uuid".equalsIgnoreCase(field.xtype)) { // custom
                    ret = new UUIDFormat();
                } else if ("uri".equalsIgnoreCase(field.xtype)) { // custom
                    ret = new URIFormat();
                }
            }
        }
        log.debug(field + " formatter: " + ret.getClass().getName());
        return ret;
    }

    private static boolean isArray(VOTableField field) {
        return field.getArrayShape() != null;
    }

}
