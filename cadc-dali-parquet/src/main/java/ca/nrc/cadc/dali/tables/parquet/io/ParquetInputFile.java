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

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;

/**
 * An implementation of Parquet's InputFile interface that wraps a RandomAccessFile.
 * This allows Parquet to read from any file that supports random access.
 */
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
                // Note: Do not close RandomAccessFile here, as it gets reused until the whole stream is read.
            }
        };
    }
}
