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

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.DoubleInterval;
import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.dali.LongInterval;
import ca.nrc.cadc.dali.MultiPolygon;
import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.dali.Shape;
import ca.nrc.cadc.dali.tables.TableData;
import ca.nrc.cadc.dali.tables.TableWriter;
import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.tables.votable.VOTableResource;
import ca.nrc.cadc.dali.tables.votable.VOTableWriter;
import ca.nrc.cadc.dali.util.FormatFactory;
import ca.nrc.cadc.dali.util.MultiPolygonFormat;
import ca.nrc.cadc.dali.util.ShapeFormat;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;

public class ParquetWriter implements TableWriter<VOTableDocument> {

    private static final Logger log = Logger.getLogger(ParquetWriter.class);

    public static final String IVOA_VOTABLE_PARQUET_VERSION_KEY = "IVOA.VOTable-Parquet.version";
    public static final String IVOA_VOTABLE_PARQUET_CONTENT_KEY = "IVOA.VOTable-Parquet.content";
    public static final String IVOA_VOTABLE_PARQUET_VERSION_VALUE = "1.0";
    public static final String PARQUET_CONTENT_TYPE = "application/vnd.apache.parquet";

    private FormatFactory formatFactory;
    private final ShapeFormat sfmt = new ShapeFormat();

    public ParquetWriter() {

    }

    @Override
    public String getExtension() {
        return "parquet";
    }

    @Override
    public String getContentType() {
        return PARQUET_CONTENT_TYPE;
    }

    @Override
    public String getErrorContentType() {
        return "text/plain";
    }

    @Override
    public void setFormatFactory(FormatFactory ff) {
        this.formatFactory = ff;
    }

    @Override
    public void write(VOTableDocument voTableDocument, OutputStream out) throws IOException {
        write(voTableDocument, out, Long.MAX_VALUE);
    }

    @Override
    public void write(VOTableDocument voTableDocument, OutputStream out, Long maxRec) throws IOException {
        OutputFile outputFile = outputFileFromStream(out);

        VOTableResource voTableResource = voTableDocument.getResources().stream().filter(obj -> "results".equals(obj.getType())).reduce((a, b) -> {
            throw new RuntimeException("Multiple objects with type = results");
        }).orElseThrow(() -> new RuntimeException("No object found with type = results"));

        Schema schema = DynamicSchemaGenerator.generateSchema(voTableResource.getTable().getFields());
        TableData tableData = voTableResource.getTable().getTableData();

        updateVOTable(voTableResource);

        try (org.apache.parquet.hadoop.ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(outputFile)
                .withSchema(schema)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .withRowGroupSize(Long.valueOf(org.apache.parquet.hadoop.ParquetWriter.DEFAULT_BLOCK_SIZE))
                .withPageSize(org.apache.parquet.hadoop.ParquetWriter.DEFAULT_PAGE_SIZE)
                .withConf(new Configuration())
                .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                .withValidation(false)
                .withDictionaryEncoding(false)
                .withExtraMetaData(prepareCustomMetaData(voTableDocument, maxRec))
                .build()) {

            int recordsCount = saveRecords(maxRec, tableData, schema, writer);
            log.debug("Total Records generated= " + recordsCount);
        } catch (Exception e) {
            throw new IOException("error while writing", e);
        }
        out.close();
    }

    @Override
    public void write(Throwable thrown, OutputStream output) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        writer.write(thrown.getMessage());
    }

    private static OutputFile outputFileFromStream(OutputStream outputStream) {
        return new OutputFile() {
            @Override
            public PositionOutputStream create(long blockSizeHint) {
                // TODO: use blockSizeHint argument probably assigning to OutputStream.
                return new PositionOutputStream() {
                    private long position = 0;

                    @Override
                    public void write(int b) {
                        try {
                            outputStream.write(b);
                            position++;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void write(byte[] b) {
                        try {
                            outputStream.write(b);
                            position += b.length;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void write(byte[] b, int off, int len) {
                        try {
                            outputStream.write(b, off, len);
                            position += len;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public long getPos() {
                        return position;
                    }

                    @Override
                    public void close() {
                        try {
                            outputStream.close();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }

            @Override
            public PositionOutputStream createOrOverwrite(long blockSizeHint) {
                return create(blockSizeHint);
            }

            @Override
            public boolean supportsBlockSize() {
                return false;
            }

            @Override
            public long defaultBlockSize() {
                return 0;
            }
        };
    }

    private static void updateVOTable(VOTableResource voTableResource) {
        List<VOTableField> fields = voTableResource.getTable().getFields();

        // Convert timestamp fields to long to ensure inconsistencies between avro schema field and votable metadata field
        for (int i = 0; i < fields.size(); i++) {
            VOTableField field = fields.get(i);

            if ("timestamp".equals(field.xtype)) {
                VOTableField timestampField = new VOTableField(field.getName(), "long");
                copyFieldValues(timestampField, field);

                fields.set(i, timestampField);
            }

            if ("short".equals(field.getDatatype())) {
                VOTableField shortField = new VOTableField(field.getName(), "int", field.getArraysize());
                copyFieldValues(shortField, field);

                fields.set(i, shortField);

            }
        }

        // Clear table data for empty VOTable
        voTableResource.getTable().setTableData(null);
    }

    private static void copyFieldValues(VOTableField targetField, VOTableField sourceField) {
        targetField.unit = sourceField.unit;
        targetField.ucd = sourceField.ucd;
        targetField.utype = sourceField.utype;
        targetField.description = sourceField.description;
        targetField.nullValue = sourceField.nullValue;

        if ("short".equals(sourceField.getDatatype())) {
            targetField.xtype = sourceField.xtype;
        }
    }

    private static Map<String, String> prepareCustomMetaData(VOTableDocument voTableDocument, Long maxRec) throws IOException {
        StringWriter stringWriter = new StringWriter();
        VOTableWriter votableWriter = new VOTableWriter();
        votableWriter.write(voTableDocument, stringWriter, maxRec);

        Map<String, String> customMetaData = new HashMap<>();
        customMetaData.put(IVOA_VOTABLE_PARQUET_VERSION_KEY, IVOA_VOTABLE_PARQUET_VERSION_VALUE);
        customMetaData.put(IVOA_VOTABLE_PARQUET_CONTENT_KEY, stringWriter.toString());

        return customMetaData;
    }

    private int saveRecords(Long maxRec, TableData tableData, Schema schema, org.apache.parquet.hadoop.ParquetWriter<GenericRecord> writer)
            throws IOException {
        List<Schema.Field> fields = schema.getFields();
        Iterator<List<Object>> iterator = tableData.iterator();
        int recordCount = 1;

        while (iterator.hasNext() && recordCount <= maxRec) {
            GenericRecord record = new GenericData.Record(schema);
            List<Object> rowData = iterator.next();

            for (int i = 0; i < rowData.size(); i++) {
                Schema.Field field = fields.get(i);
                String columnName = field.name();
                Schema unionSchema = field.schema().getTypes().get(1);
                Object data = rowData.get(i);
                
                String xtype = unionSchema.getProp("xtype");
                
                if (unionSchema.getType().equals(Schema.Type.ARRAY)) {
                    handleArrays(field, xtype, record, data);
                } else if (unionSchema.getType().equals(Schema.Type.LONG)
                        && (unionSchema.getLogicalType() != null
                        && unionSchema.getLogicalType().getName().equals("timestamp-millis"))) {
                    handleDateAndTimestamp(data, record, columnName);
                } else {
                    // handle primitives
                    // xtype and !array: object to string
                    if ("shape".equals(xtype)) {
                        Shape s = (Shape) data;
                        data = sfmt.format(s);
                    }
                    record.put(columnName, data);
                }
            }

            writer.write(record);
            recordCount++;
        }
        return recordCount;
    }

    private void handleArrays(Schema.Field field, String xtype, GenericRecord record, Object data) {
        if (xtype == null) {
            record.put(field.name(), data);
        } else {
            handleXTypeArrayData(record, field.name(), xtype, data);
        }
    }

    private void handleXTypeArrayData(GenericRecord record, String columnName, String xtype, Object data) {
        if (data == null) {
            record.put(columnName, null);
            return;
        }
        if (xtype.equals("interval")) {
            if (data instanceof Interval) {
                Interval di = (Interval) data;
                record.put(columnName, di.toArray());
            } else if (data instanceof Interval[]) {
                Interval[] da = (Interval[]) data;
                record.put(columnName, Interval.toArray(da));
            } else {
                throw new UnsupportedOperationException("unexpected value type: " + data.getClass().getName() + " with xtype: " + xtype);
            }
        } else if (xtype.equals("point") && data instanceof Point) {
            Point p = (Point) data;
            record.put(columnName, p.toArray());
        } else if (xtype.equals("circle") && data instanceof Circle) {
            Circle c = (Circle) data;
            record.put(columnName, c.toArray());
        } else if (xtype.equals("polygon") && data instanceof Polygon) {
            Polygon p = (Polygon) data;
            record.put(columnName, p.toArray());
        } else if (xtype.equals("multipolygon") && data instanceof MultiPolygon) {
            MultiPolygon p = (MultiPolygon) data;
            record.put(columnName, MultiPolygonFormat.toArray(p));
        } else {
            throw new UnsupportedOperationException("unexpected value type: " + data.getClass().getName() + " with xtype: " + xtype);
        }
    }

    private static void handleDateAndTimestamp(Object data, GenericRecord record, String columnName) {
        if (data instanceof Date) {
            long timeInMillis = ((Date) data).getTime();
            record.put(columnName, timeInMillis);

            log.debug("Date converted to milliseconds: " + timeInMillis);
        } else if (data instanceof java.time.Instant) {
            long timeInMillis = ((Instant) data).toEpochMilli();
            record.put(columnName, timeInMillis);

            log.debug("Instant converted to milliseconds: " + timeInMillis);
        } else {
            log.error("Expected types: Util Date or Instant, but found: " + data.getClass().getName());

            throw new IllegalArgumentException("Unsupported object type for timestamp conversion");
        }
    }
}
