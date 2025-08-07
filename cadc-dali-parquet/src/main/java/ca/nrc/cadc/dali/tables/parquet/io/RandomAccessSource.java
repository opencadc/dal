package ca.nrc.cadc.dali.tables.parquet.io;

import java.io.IOException;

public interface RandomAccessSource {
    void seek(long position) throws IOException;

    int read(byte[] buffer, int offset, int length) throws IOException;

    long length() throws IOException;

    void close() throws IOException;
}
