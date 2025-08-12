package ca.nrc.cadc.dali.tables.parquet.readerhelper;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;

public class ParquetInputFile implements InputFile {

    private final int length;
    private final RandomAccessFile randomAccessFile;

    public ParquetInputFile(RandomAccessFile randomAccessFile) throws IOException {
        this.randomAccessFile = randomAccessFile;
        this.length = (int) randomAccessFile.length();
    }

    @Override
    public long getLength() {
        return length;
    }

    @Override
    public SeekableInputStream newStream() {
        return new SeekableInputStream() {
            private long pos = 0;

            @Override
            public void seek(long newPos) throws IOException {
                if (newPos < 0) {
                    throw new IllegalArgumentException("Negative positions are not supported");
                }
                randomAccessFile.seek(newPos);
                pos = newPos;
            }

            @Override
            public void readFully(byte[] bytes, int off, int len) throws IOException {
                int bytesRead = 0;
                while (bytesRead < len) {
                    int result = randomAccessFile.read(bytes, off + bytesRead, len - bytesRead);
                    if (result == -1) {
                        throw new EOFException("Unexpected end of stream");
                    }
                    bytesRead += result;
                }
            }

            @Override
            public void readFully(byte[] bytes) throws IOException {
                readFully(bytes, 0, bytes.length);
            }

            @Override
            public void readFully(ByteBuffer byteBuffer) throws IOException {
                byte[] temp = new byte[byteBuffer.remaining()];
                readFully(temp);
                byteBuffer.put(temp);
            }

            @Override
            public int read(ByteBuffer byteBuffer) throws IOException {
                byte[] temp = new byte[byteBuffer.remaining()];
                int bytesRead = randomAccessFile.read(temp);
                if (bytesRead > 0) {
                    byteBuffer.put(temp, 0, bytesRead);
                }
                return bytesRead;
            }

            @Override
            public int read() throws IOException {
                int result = randomAccessFile.read();
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
                // Note: Do not close RandomAccessFile here, as it gets reused until whole stream is read.
            }
        };
    }
}
