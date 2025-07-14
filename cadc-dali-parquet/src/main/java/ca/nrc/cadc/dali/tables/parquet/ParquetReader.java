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

import ca.nrc.cadc.dali.tables.ListTableData;
import ca.nrc.cadc.dali.tables.parquet.readerHelper.DynamicRow;
import ca.nrc.cadc.dali.tables.parquet.readerHelper.DynamicRowMaterializer;
import ca.nrc.cadc.dali.tables.parquet.readerHelper.ParquetInputFile;
import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.tables.votable.VOTableReader;
import ca.nrc.cadc.dali.tables.votable.VOTableResource;
import ca.nrc.cadc.dali.tables.votable.VOTableTable;

import ca.nrc.cadc.dali.util.DoubleFormat;
import ca.nrc.cadc.dali.util.DoubleArrayFormat;
import ca.nrc.cadc.dali.util.Format;
import ca.nrc.cadc.dali.util.FloatArrayFormat;
import ca.nrc.cadc.dali.util.FormatFactory;
import ca.nrc.cadc.dali.util.FloatFormat;
import ca.nrc.cadc.dali.util.IntArrayFormat;
import ca.nrc.cadc.dali.util.IntegerFormat;
import ca.nrc.cadc.dali.util.LongArrayFormat;
import ca.nrc.cadc.dali.util.LongFormat;
import ca.nrc.cadc.dali.util.StringFormat;
import ca.nrc.cadc.dali.util.UUIDFormat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import ca.nrc.cadc.dali.util.UTCTimestampFormat;
import org.apache.log4j.Logger;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.LogicalTypeAnnotation;

import static ca.nrc.cadc.dali.tables.parquet.ParquetWriter.IVOA_VOTABLE_PARQUET_CONTENT_KEY;

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
            log.debug("VOTable: " + votable);

            voTableDocument = getVOTableDocument(votable, parquetSchema);
            List<VOTableField> votableFields = voTableDocument.getResourceByType("results").getTable().getFields();

            ListTableData tableData = new ListTableData();
            voTableDocument.getResourceByType("results").getTable().setTableData(tableData);

            PageReadStore rowGroup;

            log.debug("Reading Parquet file with schema: " + parquetSchema);
            while ((rowGroup = reader.readNextRowGroup()) != null) {
                readRowGroup(parquetSchema, rowGroup, votableFields, tableData);
            }
            log.debug("Finished reading Parquet file");
        } catch (Exception e) {
            throw new RuntimeException("Error reading Parquet file", e);
        }
        return voTableDocument;
    }

    private static void readRowGroup(MessageType parquetSchema, PageReadStore rowGroup, List<VOTableField> votableFields, ListTableData tableData) {
        DynamicRow dynamicRow;
        MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(parquetSchema);
        DynamicRowMaterializer materializer = new DynamicRowMaterializer(parquetSchema);
        RecordReader<DynamicRow> recordReader = columnIO.getRecordReader(rowGroup, materializer);

        for (int i = 0; i < rowGroup.getRowCount(); i++) {
            dynamicRow = recordReader.read();
            List<Object> row = new ArrayList<>();

            String value;
            for (VOTableField field : votableFields) {
                String fieldName = field.getName();
                int fieldIndex = parquetSchema.getFieldIndex(fieldName);
                Type parquetField = parquetSchema.getType(fieldIndex);

                if (dynamicRow.get(fieldName) == null) {
                    value = null;
                } else if (parquetField.isPrimitive() && parquetField.asPrimitiveType().getLogicalTypeAnnotation() instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                            .withZone(ZoneId.of("UTC"));
                    value = formatter.format(Instant.ofEpochMilli((long) dynamicRow.get(fieldName)));
                } else {
                    value = dynamicRow.get(fieldName).toString().replaceAll("[\\[\\],]", "");
                }
                row.add(field.getFormat().parse(value));
            }
            tableData.getArrayList().add(row);
        }
    }

    private VOTableDocument getVOTableDocument(String votable, MessageType parquetSchema) throws IOException {
        if (votable == null || votable.isBlank()) {
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
            VOTableReader voTableReader = new VOTableReader();
            VOTableDocument voTableDocument = voTableReader.read(votable);

            voTableDocument.getResourceByType("results").getTable().getFields().forEach(field -> field.setFormat(formatFactory.getFormat(field)));
            return voTableDocument;
        }
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
