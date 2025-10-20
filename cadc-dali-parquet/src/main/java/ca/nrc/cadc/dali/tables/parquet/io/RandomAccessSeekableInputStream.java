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
 *  $Revision: 5 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.dali.tables.parquet.io;

import ca.nrc.cadc.io.RandomAccessSource;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.parquet.io.SeekableInputStream;

/**
 * A {@link SeekableInputStream} implementation that wraps a {@link RandomAccessSource}
 * to provide random access read operations for Parquet files.
 * <p>
 * This stream maintains its own position and delegates seek and read operations
 * to the underlying {@code RandomAccessSource}.
 * </p>
 */
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
        // Note: Do not close the stream here, as it gets reused until the whole stream is read.
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
