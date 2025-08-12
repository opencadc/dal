package ca.nrc.cadc.dali.tables.parquet.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpRandomAccessSource implements RandomAccessSource {
    private final URL url;
    private long position = 0;
    private final long contentLength;

    public HttpRandomAccessSource(URL url) throws IOException {
        this.url = url;
        this.contentLength = fetchContentLength(url);
    }

    @Override
    public void seek(long position) throws IOException {
        if (position < 0 || position >= contentLength) {
            throw new IOException("Seek position out of bounds");
        }
        this.position = position;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        long end = position + length - 1;

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Range", "bytes=" + position + "-" + end);
        conn.connect();

        try (InputStream in = conn.getInputStream()) {
            int bytesRead = in.read(buffer, offset, length);
            if (bytesRead > 0) {
                position += bytesRead;
            }
            return bytesRead;
        } finally {
            conn.disconnect();
        }
    }

    @Override
    public long length() throws IOException {
        return contentLength;
    }

    @Override
    public void close() {
        // No persistent connection to close
    }

    private long fetchContentLength(URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("HEAD");
        conn.connect();
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to fetch content length: HTTP " + responseCode);
        }
        long len = conn.getContentLengthLong();
        conn.disconnect();
        return len;
    }
}
