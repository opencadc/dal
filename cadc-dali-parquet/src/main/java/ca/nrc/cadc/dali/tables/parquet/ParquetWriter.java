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
import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.dali.MultiPolygon;
import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.dali.Shape;
import ca.nrc.cadc.dali.tables.TableData;
import ca.nrc.cadc.dali.tables.TableWriter;
import ca.nrc.cadc.dali.tables.parquet.writerhelper.DynamicParquetWriterBuilder;
import ca.nrc.cadc.dali.tables.parquet.writerhelper.DynamicSchemaGenerator;
import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.tables.votable.VOTableResource;
import ca.nrc.cadc.dali.tables.votable.VOTableWriter;
import ca.nrc.cadc.dali.util.FormatFactory;
import ca.nrc.cadc.dali.util.MultiPolygonFormat;
import ca.nrc.cadc.dali.util.ShapeFormat;
import ca.nrc.cadc.dali.util.URIFormat;
import ca.nrc.cadc.dali.util.UUIDFormat;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.io.PositionOutputStream;
import org.apache.parquet.schema.MessageType;

/**
 * Parquet Writer - Writes out votable in parquet format.
 * */
public class ParquetWriter implements TableWriter<VOTableDocument> {

    private static final Logger log = Logger.getLogger(ParquetWriter.class);

    public static final String IVOA_VOTABLE_PARQUET_VERSION_KEY = "IVOA.VOTable-Parquet.version";
    public static final String IVOA_VOTABLE_PARQUET_CONTENT_KEY = "IVOA.VOTable-Parquet.content";
    public static final String IVOA_VOTABLE_PARQUET_VERSION_VALUE = "1.0";
    public static final String PARQUET_CONTENT_TYPE = "application/vnd.apache.parquet";

    private FormatFactory formatFactory;
    private List<VOTableField> voTableFields = new ArrayList<>();
    private final boolean addMetadata;

    public ParquetWriter() {
        this(true);
    }

    public ParquetWriter(boolean addMetadata) {
        this.addMetadata = addMetadata;
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

    /**
     * Write the VOTable in Parquet format to the specified OutputStream.
     *
     * @param voTableDocument VOTable object to write.
     * @param out OutputStream to write to.
     * @throws IOException if problem writing to OutputStream.
     */
    @Override
    public void write(VOTableDocument voTableDocument, OutputStream out) throws IOException {
        write(voTableDocument, out, Long.MAX_VALUE);
    }

    /**
     * Write the VOTable in Parquet format to the specified OutputStream
     * It ignores the records after maxrec rows.
     *
     * @param voTableDocument VOTable object to write.
     * @param out OutputStream to write to.
     * @param maxRec maximum number of rows to write.
     * @throws IOException if problem writing to OutputStream.
     */
    @Override
    public void write(VOTableDocument voTableDocument, OutputStream out, Long maxRec) throws IOException {
        log.debug("Writing VOTable to Parquet.");
        OutputFile outputFile = outputFileFromStream(out);

        VOTableResource voTableResource = voTableDocument.getResources().stream().filter(obj -> "results".equals(obj.getType())).reduce((a, b) -> {
            throw new RuntimeException("Multiple objects with type = results");
        }).orElseThrow(() -> new RuntimeException("No object found with type = results"));

        voTableFields.addAll(voTableResource.getTable().getFields());

        TableData tableData = voTableResource.getTable().getTableData();

        MessageType schema = DynamicSchemaGenerator.generateSchema(voTableResource.getTable().getFields());
        log.debug("Parquet Schema prepared successfully");

        updateVOTable(voTableResource);

        try (org.apache.parquet.hadoop.ParquetWriter<List<Object>> writer =
                     new DynamicParquetWriterBuilder(outputFile, schema, voTableResource.getTable().getFields(), prepareCustomMetaData(voTableDocument, maxRec))
                             .withCompressionCodec(CompressionCodecName.SNAPPY)
                             .withRowGroupSize(Long.valueOf(org.apache.parquet.hadoop.ParquetWriter.DEFAULT_BLOCK_SIZE))
                             .withPageSize(org.apache.parquet.hadoop.ParquetWriter.DEFAULT_PAGE_SIZE)
                             .withConf(new Configuration())
                             .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                             .withValidation(false)
                             .withDictionaryEncoding(false)
                             .build()) {

            int recordsCount = writeRecords(maxRec, tableData, writer);
            writer.close();
            log.debug("Total Records written= " + recordsCount);
        } catch (Exception e) {
            log.debug("error while writing: " + e.getMessage());
            throw new IOException("error while writing : " + e.getMessage(), e);
        }
        out.close();
    }

    /**
     * Write the Throwable to the specified OutputStream.
     *
     * @param thrown Throwable to write.
     * @param output OutputStream to write to.
     * @throws IOException if problem writing to the stream.
     */
    @Override
    public void write(Throwable thrown, OutputStream output) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        writer.write(thrown.getMessage());
    }

    /**
     * Creates an {@link OutputFile} implementation that writes to the provided {@link OutputStream}.
     * This allows Parquet to write data directly to an {@code OutputStream}.
     *
     * @param outputStream the {@code OutputStream} to write Parquet data to
     * @return an {@code OutputFile} that writes to the given {@code OutputStream}
     */
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

    private Map<String, String> prepareCustomMetaData(VOTableDocument voTableDocument, Long maxRec) throws IOException {
        if (!addMetadata) {
            return new HashMap<>();
        }

        StringWriter stringWriter = new StringWriter();
        VOTableWriter votableWriter = new VOTableWriter();
        votableWriter.write(voTableDocument, stringWriter, maxRec);

        Map<String, String> customMetaData = new HashMap<>();
        customMetaData.put(IVOA_VOTABLE_PARQUET_VERSION_KEY, IVOA_VOTABLE_PARQUET_VERSION_VALUE);
        customMetaData.put(IVOA_VOTABLE_PARQUET_CONTENT_KEY, stringWriter.toString());

        return customMetaData;
    }

    private int writeRecords(Long maxRec, TableData tableData, org.apache.parquet.hadoop.ParquetWriter<List<Object>> writer)
            throws IOException {
        Iterator<List<Object>> iterator = tableData.iterator();
        int recordCount = 1;

        while (iterator.hasNext() && recordCount <= maxRec) {
            List<Object> rowData = iterator.next();
            List<Object> formattedRowData = new ArrayList<>();

            for (int i = 0; i < rowData.size(); i++) {
                VOTableField voTableField = voTableFields.get(i);
                Object data = rowData.get(i);
                String xtype = voTableField.xtype;

                try {
                    if (data == null) {
                        formattedRowData.add(null);
                    } else if (xtype != null) {
                        // handle complex types
                        formattedRowData.add(handleXtypeConversion(xtype, data));
                    } else {
                        // handle primitives and primitive arrays
                        formattedRowData.add(data);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failure while preparing data for field : " + voTableField, e);
                }
            }

            writer.write(formattedRowData);
            recordCount++;
        }
        return recordCount;
    }

    private Object handleXtypeConversion(String xtype, Object data) {
        Object dataToStore;
        if (xtype.equals("interval")) {
            if (data instanceof Interval) {
                Interval di = (Interval) data;
                dataToStore = di.toArray();
            } else if (data instanceof Interval[]) {
                Interval[] da = (Interval[]) data;
                dataToStore = Interval.toArray(da);
            } else {
                throw new UnsupportedOperationException("unexpected Interval type: " + data.getClass().getName());
            }
        } else if (xtype.equals("point") && data instanceof Point) {
            Point p = (Point) data;
            dataToStore = p.toArray();
        } else if (xtype.equals("circle") && data instanceof Circle) {
            Circle c = (Circle) data;
            dataToStore = c.toArray();
        } else if (xtype.equals("polygon") && data instanceof Polygon) {
            Polygon p = (Polygon) data;
            dataToStore = p.toArray();
        } else if (xtype.equals("multipolygon") && data instanceof MultiPolygon) {
            MultiPolygon p = (MultiPolygon) data;
            dataToStore = MultiPolygonFormat.toArray(p);
        } else if (xtype.equals("shape") && data instanceof Shape) {
            Shape s = (Shape) data;
            ShapeFormat shapeFormat = new ShapeFormat();
            dataToStore = shapeFormat.format(s);
        } else if (data instanceof Date) {
            dataToStore = ((Date) data).getTime();
        } else if (data instanceof Instant) {
            dataToStore = ((Instant) data).toEpochMilli();
        } else if (xtype.equals("uuid")) {
            UUIDFormat uuidFormat = new UUIDFormat();
            dataToStore = uuidFormat.uuidToBytes((UUID) data);
        } else if (xtype.equals("uri")) {
            URIFormat uriFormat = new URIFormat();
            dataToStore = uriFormat.format((URI) data);
        } else {
            throw new UnsupportedOperationException("unexpected value type: " + data.getClass().getName() + " with xtype: " + xtype);
        }
        return dataToStore;
    }

}
