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

package ca.nrc.cadc.dali.tables.parquet;

import static ca.nrc.cadc.dali.tables.parquet.ParquetWriter.IVOA_VOTABLE_PARQUET_CONTENT_KEY;

import ca.nrc.cadc.dali.tables.parquet.readerhelper.ParquetInputFile;
import ca.nrc.cadc.dali.tables.parquet.readerhelper.ParquetTableData;
import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.tables.votable.VOTableReader;
import ca.nrc.cadc.dali.tables.votable.VOTableResource;
import ca.nrc.cadc.dali.tables.votable.VOTableTable;

import ca.nrc.cadc.dali.util.DoubleArrayFormat;
import ca.nrc.cadc.dali.util.DoubleFormat;

import ca.nrc.cadc.dali.util.FloatArrayFormat;
import ca.nrc.cadc.dali.util.FloatFormat;
import ca.nrc.cadc.dali.util.Format;
import ca.nrc.cadc.dali.util.FormatFactory;
import ca.nrc.cadc.dali.util.IntArrayFormat;
import ca.nrc.cadc.dali.util.IntegerFormat;
import ca.nrc.cadc.dali.util.LongArrayFormat;
import ca.nrc.cadc.dali.util.LongFormat;
import ca.nrc.cadc.dali.util.StringFormat;
import ca.nrc.cadc.dali.util.UTCTimestampFormat;
import ca.nrc.cadc.dali.util.UUIDFormat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;

public class ParquetReader {

    private static final Logger log = Logger.getLogger(ParquetReader.class);

    private static final FormatFactory formatFactory = new FormatFactory();

    public VOTableDocument read(InputStream inputStream) throws IOException {

        InputFile inputFile = new ParquetInputFile((ByteArrayInputStream) inputStream);

        VOTableDocument voTableDocument;

        try (ParquetFileReader reader = ParquetFileReader.open(inputFile)) {
            ParquetMetadata metadata = reader.getFooter();
            MessageType parquetSchema = metadata.getFileMetaData().getSchema();

            String votable = metadata.getFileMetaData().getKeyValueMetaData().get(IVOA_VOTABLE_PARQUET_CONTENT_KEY);

            voTableDocument = getVOTableDocument(votable, parquetSchema);
            List<VOTableField> votableFields = voTableDocument.getResourceByType("results").getTable().getFields();

            ParquetTableData tableData = new ParquetTableData(reader, parquetSchema, votableFields);
            voTableDocument.getResourceByType("results").getTable().setTableData(tableData);
        } catch (Exception e) {
            throw new RuntimeException("Error reading Parquet file", e);
        }
        return voTableDocument;
    }

    private VOTableDocument getVOTableDocument(String votable, MessageType parquetSchema) throws IOException {
        if (votable == null || votable.isBlank()) {
            log.debug("Creating VOTable from scratch as no VOTable content found in Parquet metadata.");
            List<VOTableField> fields = new ArrayList<>();
            VOTableDocument voTableDocument = new VOTableDocument();

            VOTableResource voTableResource = new VOTableResource("results");
            voTableResource.setTable(new VOTableTable());
            voTableDocument.getResources().add(voTableResource);

            parquetSchema.getFields().forEach(field -> {
                String name = field.getName();
                Type actualField = extractActualElementType(field);
                PrimitiveType.PrimitiveTypeName physicalType = actualField.asPrimitiveType().getPrimitiveTypeName();

                String type;
                Format format;
                switch (physicalType) {
                    case INT32:
                        type = "int";
                        format = actualField.isRepetition(Type.Repetition.REPEATED) ? new IntArrayFormat() : new IntegerFormat(null);
                        break;
                    case INT64:
                        LogicalTypeAnnotation logicalType = actualField.asPrimitiveType().getLogicalTypeAnnotation();
                        if (logicalType instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {
                            type = "timestamp";
                            format = new UTCTimestampFormat();
                        } else {
                            type = "long";
                            format = actualField.isRepetition(Type.Repetition.REPEATED) ? new LongArrayFormat() : new LongFormat(null);
                        }
                        break;
                    case FLOAT:
                        type = "float";

                        format = actualField.isRepetition(Type.Repetition.REPEATED) ? new FloatArrayFormat() : new FloatFormat();
                        break;
                    case DOUBLE:
                        type = "double";

                        format = actualField.isRepetition(Type.Repetition.REPEATED) ? new DoubleArrayFormat() : new DoubleFormat();
                        break;
                    case BINARY:
                        type = "char";
                        format = (actualField.getLogicalTypeAnnotation() instanceof LogicalTypeAnnotation.UUIDLogicalTypeAnnotation)
                                ? new UUIDFormat() : new StringFormat();
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported parquet physical type: " + physicalType);
                }

                VOTableField voTableField = new VOTableField(name, type);
                voTableField.setFormat(format);
                fields.add(voTableField);
            });
            voTableResource.getTable().getFields().addAll(fields);
            return voTableDocument;
        } else {
            log.debug("Reading VOTable content from Parquet metadata.");
            VOTableReader voTableReader = new VOTableReader();
            VOTableDocument voTableDocument = voTableReader.read(votable);

            List<VOTableField> fields = voTableDocument.getResourceByType("results").getTable().getFields();
            for (int i = 0; i < fields.size(); i++) {
                VOTableField field = fields.get(i);
                Type parquetField = parquetSchema.getType(field.getName());
                if (parquetField != null && parquetField.isPrimitive()) {
                    PrimitiveType.PrimitiveTypeName physicalType = parquetField.asPrimitiveType().getPrimitiveTypeName();
                    if (physicalType == PrimitiveType.PrimitiveTypeName.INT64
                            && parquetField.asPrimitiveType().getLogicalTypeAnnotation() instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {

                        VOTableField timestampField = new VOTableField(field.getName(), "char", "*");
                        copyFieldValues(timestampField, field);
                        timestampField.xtype = "timestamp";
                        field = timestampField; // Update timestamp field
                    }
                }
                field.setFormat(formatFactory.getFormat(field));
                fields.set(i, field); // Update the field in the list
            }

            return voTableDocument;
        }
    }

    private static void copyFieldValues(VOTableField targetField, VOTableField sourceField) {
        targetField.unit = sourceField.unit;
        targetField.ucd = sourceField.ucd;
        targetField.utype = sourceField.utype;
        targetField.description = sourceField.description;
        targetField.nullValue = sourceField.nullValue;
    }

    public static Type extractActualElementType(Type field) {
        if (field.isPrimitive()) {
            return field;
        }

        GroupType group = field.asGroupType();

        // 2-level list: LIST group with repeated primitive directly
        if (group.getFieldCount() == 1 && group.getType(0).isPrimitive() && group.getType(0).isRepetition(Type.Repetition.REPEATED)) {
            return group.getType(0); // ex: repeated int32 element
        }

        // 3-level list: LIST group -> repeated group -> primitive
        if (group.getFieldCount() == 1 && !group.getType(0).isPrimitive()) {
            GroupType repeatedGroup = group.getType(0).asGroupType();

            if (repeatedGroup.getFieldCount() == 1 && repeatedGroup.getType(0).isPrimitive()) {
                return repeatedGroup.getType(0); // ex: required int32 element
            }
        }

        throw new IllegalArgumentException("Unsupported nested structure: " + field);
    }

}
