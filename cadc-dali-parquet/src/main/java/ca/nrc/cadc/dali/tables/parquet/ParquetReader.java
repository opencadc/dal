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

import ca.nrc.cadc.dali.tables.parquet.io.RandomAccessSeekableInputStream;
import ca.nrc.cadc.dali.tables.parquet.io.RandomAccessSource;
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
import ca.nrc.cadc.io.ByteCountInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ca.nrc.cadc.io.MultiBufferIO;
import org.apache.log4j.Logger;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;

/**
 * Parquet Reader - Reads parquet content and produces a VOTableDocument representation.
 * */
public class ParquetReader {

    private static final Logger log = Logger.getLogger(ParquetReader.class);

    private static final FormatFactory formatFactory = new FormatFactory();

    /**
     * Read a Parquet data source using a RandomAccessFile and produce a VOTableDocument representation.
     *
     * @param file Random-access handle to the Parquet file content.
     * @return VOTableDocument built from the Parquet input;
     * @throws IOException if reading, seeking, or parsing the Parquet content fails.
     */

    public VOTableDocument read(RandomAccessFile file) throws IOException {
        log.debug("Reading RandomAccessFile");
        ParquetInputFile parquetInputFile = new ParquetInputFile(file);
        return read(parquetInputFile);
    }

    /**
     * Read a Parquet data source via a RandomAccessSource abstraction and produce a VOTableDocument.
     *
     * @param randomAccessSource Random-access capable source of Parquet bytes; expected to support seek and bounded reads.
     * @return VOTableDocument representation of the Parquet content;
     * @throws IOException if byte access or Parquet parsing fails.
     */

    public VOTableDocument read(RandomAccessSource randomAccessSource) throws IOException {
        log.debug("Reading RandomAccessSource");
        RandomAccessSeekableInputStream randomAccessStream = new RandomAccessSeekableInputStream(randomAccessSource);
        return read(randomAccessStream);
    }

    /**
     * Read a Parquet stream and produce a VOTableDocument.
     * It reads the stream fully and stores it in a local temporary file before returning.
     *
     * @param inputStream stream of Parquet content.
     * @return VOTableDocument representation of the Parquet content;
     * @throws IOException if reading from the stream or parsing the Parquet content fails.
     */
    public VOTableDocument read(InputStream inputStream) throws IOException {
        log.debug("Reading InputStream");
        File tempFile = null;

        try {
            String fileName = "parquet-" + UUID.randomUUID();
            tempFile = File.createTempFile(fileName, ".parquet");
            log.debug("File " + fileName + " created successfully");

            try (OutputStream out = new FileOutputStream(tempFile)) {
                MultiBufferIO copier = new MultiBufferIO();
                copier.copy(inputStream, out);
            } catch (Exception e) {
                log.error("Error reading from InputStream", e);
                throw new RuntimeException(e);
            }

            log.debug("File" + fileName + " prepared successfully for reading");

            RandomAccessFile raf = new RandomAccessFile(tempFile, "r");
            ParquetInputFile parquetInputFile = new ParquetInputFile(raf);
            return read(parquetInputFile);
        } catch (IOException e) {
            log.error("Failed to read Parquet from InputStream", e);
            throw new RuntimeException("Error reading Parquet file", e);
        } finally {
            try {
                if (tempFile != null && tempFile.exists()) {
                    if (!tempFile.delete()) {
                        log.error("Failed to delete temp file: " + tempFile.getAbsolutePath());
                        throw new IOException("Failed to delete temp file: " + tempFile.getAbsolutePath());
                    }
                }
                log.debug("File " + tempFile + " deleted successfully");
            } catch (Exception e) {
                log.error("Exception while deleting temp file: " + tempFile.getAbsolutePath(), e);
                throw new IOException("Exception while deleting temp file: " + tempFile.getAbsolutePath(), e);
            }
        }
    }

    private VOTableDocument read(InputFile inputFile) {
        log.debug("Reading parquet Input File.");
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
            throw new RuntimeException("Error reading Parquet file : " + e.getMessage(), e);
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
                    case FIXED_LEN_BYTE_ARRAY:
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
            log.debug("Reading empty VOTable from Parquet metadata.");
            VOTableReader voTableReader = new VOTableReader();
            VOTableDocument voTableDocument = voTableReader.read(votable);

            List<VOTableField> fields = voTableDocument.getResourceByType("results").getTable().getFields();
            for (int i = 0; i < fields.size(); i++) {
                VOTableField field = fields.get(i);
                Type parquetField = parquetSchema.getType(field.getName().replaceAll("\"", "_"));
                if (parquetField != null && parquetField.isPrimitive()) {
                    PrimitiveType.PrimitiveTypeName physicalType = parquetField.asPrimitiveType().getPrimitiveTypeName();
                    if (physicalType == PrimitiveType.PrimitiveTypeName.INT64
                            && parquetField.asPrimitiveType().getLogicalTypeAnnotation() instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {

                        VOTableField timestampField = new VOTableField(field.getName().replaceAll("\"", "_"), "char", "*");
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
