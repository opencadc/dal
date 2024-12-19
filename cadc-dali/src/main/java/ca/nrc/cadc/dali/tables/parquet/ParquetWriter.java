package ca.nrc.cadc.dali.tables.parquet;

import ca.nrc.cadc.dali.tables.TableData;
import ca.nrc.cadc.dali.tables.TableWriter;
import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableResource;
import ca.nrc.cadc.dali.tables.votable.VOTableWriter;
import ca.nrc.cadc.dali.util.FormatFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

import java.nio.charset.StandardCharsets;
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

    private FormatFactory formatFactory;

    public ParquetWriter() {

    }

    @Override
    public String getExtension() {
        return "parquet";
    }

    @Override
    public String getContentType() {
        return "application/vnd.apache.parquet";
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
        log.debug("ParquetWriter Write service called. MaxRec = " + maxRec);
        for (VOTableResource resource : voTableDocument.getResources()) {
            Schema schema = DynamicSchemaGenerator.generateSchema(resource.getTable().getFields());
            OutputFile outputFile = outputFileFromStream(out);

            TableData tableData = resource.getTable().getTableData();
            resource.getTable().setTableData(null);

            StringWriter stringWriter = new StringWriter();
            VOTableWriter votableWriter = new VOTableWriter();
            votableWriter.write(voTableDocument, stringWriter, maxRec);

            Map<String, String> customMetaData = new HashMap<>();
            customMetaData.put("IVOA.VOTable-Parquet.version", "1.0");
            customMetaData.put("IVOA.VOTable-Parquet.content", stringWriter.toString());

            try (org.apache.parquet.hadoop.ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(outputFile)
                    .withSchema(schema)
                    .withCompressionCodec(CompressionCodecName.SNAPPY)
                    .withRowGroupSize(Long.valueOf(org.apache.parquet.hadoop.ParquetWriter.DEFAULT_BLOCK_SIZE))
                    .withPageSize(org.apache.parquet.hadoop.ParquetWriter.DEFAULT_PAGE_SIZE)
                    .withConf(new Configuration())
                    .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                    .withValidation(false)
                    .withDictionaryEncoding(false)
                    .withExtraMetaData(customMetaData)
                    .build()) {

                Iterator<List<Object>> iterator = tableData.iterator();
                int recordCount = 1;

                while (iterator.hasNext() && recordCount <= maxRec) {
                    GenericRecord record = new GenericData.Record(schema);
                    List<Object> rowData = iterator.next();
                    int columnIndex = 0;

                    for (Schema.Field field : schema.getFields()) {
                        String columnName = field.name();
                        record.put(columnName, rowData.get(columnIndex)); // TODO: convert non-simple row data to correct format before adding to record
                        columnIndex++;
                    }

                    writer.write(record);
                    recordCount++;
                    log.debug("Total Records generated= " + (recordCount - 1));
                }
            } catch (Exception e) {
                throw new IOException("error while writing", e);
            }
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
}
