package ca.nrc.cadc.dali.tables.parquet.readerHelper;

import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;

public class ParquetInputFile implements InputFile {

    byte[] buf;
    int length;

    public ParquetInputFile(ByteArrayInputStream byteArrayInputStream) throws IOException {
        try {
            Field bufField = ByteArrayInputStream.class.getDeclaredField("buf");
            Field countField = ByteArrayInputStream.class.getDeclaredField("count");
            bufField.setAccessible(true);
            countField.setAccessible(true);

            buf = (byte[]) bufField.get(byteArrayInputStream);
            length = countField.getInt(byteArrayInputStream);
        } catch (Exception e) {
            throw new IOException("Failed to get ByteArrayInputStream buffer length", e);
        }

    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public SeekableInputStream newStream() {
        return new SeekableInputStream() {
            // Required as footer gets read first and then the data.
            private final InputStream delegate = new ByteArrayInputStream(buf, 0, length);
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
            public long getPos() {
                return pos;
            }

            @Override
            public void close() throws IOException {
                delegate.close();
            }
        };
    }
}
