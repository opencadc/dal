package ca.nrc.cadc.dali.tables.parquet.io;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileRandomAccessSource implements RandomAccessSource {

    private final RandomAccessFile raf;

    public FileRandomAccessSource(File file) throws IOException {
        this.raf = new RandomAccessFile(file, "r");
    }

    @Override
    public void seek(long position) throws IOException {
        raf.seek(position);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        return raf.read(buffer, offset, length);
    }

    @Override
    public long length() throws IOException {
        return raf.length();
    }

    @Override
    public void close() throws IOException {
        raf.close();
    }
}
