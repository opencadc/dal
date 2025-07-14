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
 *  : 5 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.dali.tables.parquet.writerhelper;

import ca.nrc.cadc.dali.tables.votable.VOTableField;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.Types;

public class DynamicSchemaGenerator {

    private static final Logger log = Logger.getLogger(DynamicSchemaGenerator.class);

    public static MessageType generateSchema(List<VOTableField> voFields) {
        Types.MessageTypeBuilder builder = Types.buildMessage();
        try {
            int columnCount = voFields.size();
            log.debug("VOTable Column count = " + columnCount);
            for (VOTableField voField : voFields) {
                Type fieldType = getAvroFieldType(voField);
                builder.addField(fieldType);
            }
        } catch (Exception e) {
            log.debug("Failure while creating Parquet Schema from VOTable", e);
            throw new RuntimeException("Failure while creating Parquet Schema from VOTable : " + e.getMessage(), e);
        }

        MessageType schema = builder.named("schema");
        log.debug("Schema Generated Successfully : " + schema);
        return schema;
    }

    private static Type getAvroFieldType(VOTableField voTableField) {
        String datatype = voTableField.getDatatype();
        String arraysize = voTableField.getArraysize();
        String xtype = voTableField.xtype;

        PrimitiveType.PrimitiveTypeName typeName;
        LogicalTypeAnnotation logicalTypeAnnotation = null;

        switch (datatype) {
            case "short":
            case "int":
                typeName = PrimitiveType.PrimitiveTypeName.INT32;
                break;
            case "long":
                typeName = PrimitiveType.PrimitiveTypeName.INT64;
                break;
            case "float":
                typeName = PrimitiveType.PrimitiveTypeName.FLOAT;
                break;
            case "double":
                typeName = PrimitiveType.PrimitiveTypeName.DOUBLE;
                break;
            case "char":
                if ("timestamp".equalsIgnoreCase(xtype)) {
                    logicalTypeAnnotation = LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS);
                    typeName = PrimitiveType.PrimitiveTypeName.INT64;
                } else if ("uuid".equalsIgnoreCase(xtype)) {
                    logicalTypeAnnotation = LogicalTypeAnnotation.uuidType();
                    typeName = PrimitiveType.PrimitiveTypeName.BINARY;
                } else {
                    typeName = PrimitiveType.PrimitiveTypeName.BINARY;
                }
                break;
            case "boolean":
                typeName = PrimitiveType.PrimitiveTypeName.BOOLEAN;
                break;
            case "date":
            case "timestamp":
                typeName = PrimitiveType.PrimitiveTypeName.INT64;
                logicalTypeAnnotation = LogicalTypeAnnotation.timestampType(true, LogicalTypeAnnotation.TimeUnit.MILLIS);
                break;
            case "byte":
            default:
                typeName = PrimitiveType.PrimitiveTypeName.BINARY;
        }

        return createSchema(voTableField.getName().replaceAll("\"", "_"), typeName, logicalTypeAnnotation, arraysize);
    }

    private static Type createSchema(String fieldName, PrimitiveType.PrimitiveTypeName typeName,
                                     LogicalTypeAnnotation logicalTypeAnnotation, String arraysize) {
        if (arraysize == null
                || typeName.equals(PrimitiveType.PrimitiveTypeName.BINARY) // String = (datatype = char) + (arraysize = *)
                || logicalTypeAnnotation instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) { // timestamp = (datatype = char) + (arraysize = *)

            // primitives
            Types.PrimitiveBuilder<PrimitiveType> prim = Types.primitive(typeName, Type.Repetition.OPTIONAL);
            if (logicalTypeAnnotation != null) {
                prim = prim.as(logicalTypeAnnotation);
            }
            return prim.named(fieldName);
        } else {
            // list
            return Types.optionalGroup()
                    .as(LogicalTypeAnnotation.listType())
                    .repeated(typeName)
                    .named("element")
                    .named(fieldName);
        }
    }
}

