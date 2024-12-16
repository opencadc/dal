package ca.nrc.cadc.dali.tables.parquet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.avro.generic.GenericRecord;

import org.apache.log4j.Logger;

import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;


//TODO: Temporary implementation
public class ParquetReader {

    private static final Logger log = Logger.getLogger(ParquetReader.class);

    public TableShape read(InputStream inputStream) throws IOException {

        InputFile inputFile = getInputFile(inputStream);
        int recordCount = 0;
        int columnCount = 0;

        ParquetMetadata metadata = ParquetFileReader.open(inputFile).getFooter();
        MessageType schema = metadata.getFileMetaData().getSchema();
        columnCount = schema.getFieldCount();

        try (org.apache.parquet.hadoop.ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(inputFile).build()) {
            GenericRecord record;

            while ((record = reader.read()) != null) {
                log.debug("Reading Record: " + record);
                recordCount++;

                // TODO: This is a temporary implementation to read the data
                Map<String, Object> rowData = new HashMap<>();
                for (Type field : schema.getFields()) {
                    String fieldName = field.getName();
                    rowData.put(fieldName, record.get(fieldName));
                }
                log.debug("Retrieved Row Data: " + rowData);
            }
            log.debug("Record Count = " + recordCount);

        } catch (Exception e) {
            throw new IOException("failed to read parquet data", e);
        }
        if (recordCount == 0) {
            throw new RuntimeException("NO Records Read");
        }
        return new TableShape(recordCount, columnCount);
    }

    private static InputFile getInputFile(InputStream inputStream) throws IOException {

        // Read the entire InputStream into a byte array
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(temp)) != -1) {
            buffer.write(temp, 0, bytesRead);
        }

        byte[] inputData = buffer.toByteArray();

        return new InputFile() {
            @Override
            public long getLength() throws IOException {
                return inputData.length;
            }

            @Override
            public SeekableInputStream newStream() {
                return new SeekableInputStream() {
                    private final InputStream delegate = new ByteArrayInputStream(inputData);
                    private long pos = 0;

                    @Override
                    public void seek(long newPos) throws IOException {
                        if (newPos < 0) {
                            throw new IllegalArgumentException("Negative positions are not supported");
                        }
                        delegate.reset();
                        delegate.skip(newPos);
                        pos = newPos;
                    }

                    @Override
                    public void readFully(byte[] bytes, int off, int len) throws IOException {
                        int bytesRead = 0;
                        while (bytesRead < len) {
                            int result = read(bytes, off + bytesRead, len - bytesRead);
                            if (result == -1) {
                                throw new EOFException("Unexpected end of stream");
                            }
                            bytesRead += result;
                        }
                    }

                    @Override
                    public void readFully(byte[] bytes) throws IOException {
                        int bytesRead = 0;
                        while (bytesRead < bytes.length) {
                            int result = read(bytes, bytesRead, bytes.length - bytesRead);
                            if (result == -1) {
                                throw new EOFException("Unexpected end of stream");
                            }
                            bytesRead += result;
                        }
                    }

                    @Override
                    public void readFully(ByteBuffer byteBuffer) throws IOException {
                        int bytesRead = 0;
                        while (byteBuffer.hasRemaining()) {
                            int result = read(byteBuffer);
                            if (result == -1) {
                                throw new EOFException("Unexpected end of stream");
                            }
                            bytesRead += result;
                        }
                    }

                    @Override
                    public int read(ByteBuffer byteBuffer) throws IOException {
                        byte[] temp = new byte[byteBuffer.remaining()];
                        int bytesRead = read(temp);
                        if (bytesRead > 0) {
                            byteBuffer.put(temp, 0, bytesRead);
                        }
                        return bytesRead;
                    }

                    @Override
                    public int read() throws IOException {
                        int result = delegate.read();
                        if (result != -1) {
                            pos++;
                        }
                        return result;
                    }

                    @Override
                    public long getPos() throws IOException {
                        return pos;
                    }

                    @Override
                    public void close() throws IOException {
                        delegate.close();
                    }
                };
            }
        };
    }

    public static class TableShape {
        int recordCount;
        int columnCount;

        public TableShape(int recordCount, int columnCount) {
            this.recordCount = recordCount;
            this.columnCount = columnCount;
        }

        public int getRecordCount() {
            return recordCount;
        }

        public int getColumnCount() {
            return columnCount;
        }
    }

}
