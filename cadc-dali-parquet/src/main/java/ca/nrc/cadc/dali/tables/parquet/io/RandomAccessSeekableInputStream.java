package ca.nrc.cadc.dali.tables.parquet.io;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.parquet.io.SeekableInputStream;

public class RandomAccessSeekableInputStream extends SeekableInputStream {

    private final RandomAccessSource source;
    private long position = 0;

    public RandomAccessSeekableInputStream(RandomAccessSource source) {
        this.source = source;
    }

    @Override
    public int read() throws IOException {
        byte[] b = new byte[1];
        int read = read(b, 0, 1);
        return (read == -1) ? -1 : (b[0] & 0xFF);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        source.seek(position);
        int bytesRead = source.read(b, off, len);
        if (bytesRead > 0) {
            position += bytesRead;
        }
        return bytesRead;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        byte[] buffer = new byte[dst.remaining()];
        int bytesRead = read(buffer, 0, buffer.length);
        if (bytesRead > 0) {
            dst.put(buffer, 0, bytesRead);
        }
        return bytesRead;
    }

    @Override
    public void seek(long newPos) throws IOException {
        position = newPos;
    }

    @Override
    public void readFully(byte[] b) throws IOException {
        readFully(b, 0, b.length);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws IOException {
        int totalRead = 0;
        while (totalRead < len) {
            int bytesRead = read(b, off + totalRead, len - totalRead);
            if (bytesRead == -1) {
                throw new IOException("Unexpected end of stream");
            }
            totalRead += bytesRead;
        }
    }

    @Override
    public void readFully(ByteBuffer dst) throws IOException {
        while (dst.hasRemaining()) {
            int bytesRead = read(dst);
            if (bytesRead == -1) {
                throw new IOException("Unexpected end of stream while reading ByteBuffer");
            }
        }
    }

    @Override
    public long getPos() throws IOException {
        return position;
    }

    @Override
    public void close() throws IOException {
        source.close();
    }

    @Override
    public long skip(long n) throws IOException {
        long newPos = position + n;
        if (newPos > source.length()) {
            newPos = source.length();
        }
        long skipped = newPos - position;
        position = newPos;
        return skipped;
    }

    @Override
    public int available() throws IOException {
        long available = source.length() - position;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, available));
    }
}
