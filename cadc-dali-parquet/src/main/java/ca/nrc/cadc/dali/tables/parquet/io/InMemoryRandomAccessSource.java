package ca.nrc.cadc.dali.tables.parquet.io;

import java.io.IOException;

public class InMemoryRandomAccessSource implements RandomAccessSource {

    private final byte[] data;
    private long position = 0;

    public InMemoryRandomAccessSource(byte[] data) {
        this.data = data;
    }

    @Override
    public void seek(long position) throws IOException {
        if (position < 0 || position > data.length) {
            throw new IOException("Invalid seek position");
        }
        this.position = position;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (position >= data.length) {
            return -1;
        }
        int bytesToRead = (int) Math.min(length, data.length - position);
        System.arraycopy(data, (int) position, buffer, offset, bytesToRead);
        position += bytesToRead;
        return bytesToRead;
    }

    @Override
    public long length() {
        return data.length;
    }

    @Override
    public void close() {
        // Nothing to close
    }
}
