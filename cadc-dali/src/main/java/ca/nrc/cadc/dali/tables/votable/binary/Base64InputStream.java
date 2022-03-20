/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2022.                            (c) 2022.
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
 *
 ************************************************************************
 */

package ca.nrc.cadc.dali.tables.votable.binary;

import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads in base-64 encoded input and spits out the raw binary decoding.
 *
 * @author Mozilla project
 */
public class Base64InputStream extends FilterInputStream {

    private static final int WOULD_BLOCK = -2;

    //
    // decoding table
    //
    private static final int[] table = new int[256];

    // one-time initialization of decoding table
    static {
        int i;
        for (i = 0; i < 256; ++i) {
            table[i] = -1;
        }
        int c;
        for (c = 'A', i = 0; c <= 'Z'; ++c, ++i) {
            table[c] = i;
        }
        for (c = 'a'; c <= 'z'; ++c, ++i) {
            table[c] = i;
        }
        for (c = '0'; c <= '9'; ++c, ++i) {
            table[c] = i;
        }
        table['+'] = 62;
        table['/'] = 63;
    }

    // prev is the previous significant character read from the in stream.
    // Significant characters are those that are part of the encoded data,
    // as opposed to whitespace.
    private int prev;
    private int savedPrev;

    // state is the current state of our state machine. The states are 1-4,
    // indicating which character of the current 4-character block we
    // are looking for. After state 4 we wrap back to state 1. The state
    // is not advanced when we read an insignificant character (such as
    // whitespace).
    private int state = 1;
    private int savedState;

    public Base64InputStream(InputStream in) {
        super(in);
    }

    public long skip(long n) throws IOException {
        long count = 0;
        while ((count < n) && (read() != -1)) {
            ++count;
        }
        return count;
    }

    /**
     * param block Whether or not to block waiting for input.
     */
    private int read(boolean block) throws IOException {
        int cur = 0;
        int ret = 0;
        boolean done = false;
        while (!done) {
            if (in.available() < 1 && !block) {
                return WOULD_BLOCK;
            }
            cur = in.read();
            switch (state) {
                case 1:
                    if (cur == -1) {
                        // end of file
                        return -1;
                    }
                    if (cur == '=') {
                        throw new IOException("Invalid pad character");
                    }
                    if (table[cur] != -1) {
                        prev = cur;
                        state = 2;
                    }
                    break;
                case 2:
                    if (cur == -1) {
                        throw new EOFException("Unexpected end-of-file");
                    }
                    if (cur == '=') {
                        throw new IOException("Invalid pad character");
                    }
                    if (table[cur] != -1) {
                        ret = (table[prev] << 2) | ((table[cur] & 0x30) >> 4);
                        prev = cur;
                        state = 3;
                        done = true;
                    }
                    break;
                case 3:
                    if (cur == -1) {
                        throw new EOFException("Unexpected end-of-file");
                    }
                    if (cur == '=') {
                        // pad character
                        state = 4;
                        return -1;
                    }
                    if (table[cur] != -1) {
                        ret = ((table[prev] & 0x0f) << 4) | ((table[cur] & 0x3c) >> 2);
                        prev = cur;
                        state = 4;
                        done = true;
                    }
                    break;
                case 4:
                    if (cur == -1) {
                        throw new EOFException("Unexpected end-of-file");
                    }
                    if (cur == '=') {
                        // pad character
                        state = 1;
                        return -1;
                    }
                    if (table[cur] != -1) {
                        ret = ((table[prev] & 0x03) << 6) | table[cur];
                        state = 1;
                        done = true;
                    }
                    break;
                default:
                    assert false;
                    break;
            }
        }
        return ret;
    }

    public int read() throws IOException {
        return read(true);
    }

    public int read(byte[] b, int off, int len) throws IOException {
        int count = 0;

        if (len < 0) {
            throw new IndexOutOfBoundsException("len is negative");
        }
        if (off < 0) {
            throw new IndexOutOfBoundsException("off is negative");
        }

        while (count < len) {
            int cur = read(count == 0);
            if (cur == -1) {
                // end-of-file
                if (count == 0) {
                    return -1;
                } else {
                    return count;
                }
            }
            if (cur == WOULD_BLOCK) {
                assert count > 0;
                return count;
            }
            assert cur >= 0 && cur <= 255;
            b[off + (count++)] = (byte) cur;
        }
        return count;
    }

    public int available() {
        // We really don't know how much is left. in.available() could all
        // be whitespace.
        return 0;
    }

    public boolean markSupported() {
        return in.markSupported();
    }

    public void mark(int readlimit) {
        in.mark(readlimit);
        savedPrev = prev;
        savedState = state;
    }

    public void close() throws IOException {
        in.close();
    }

    public void reset() throws IOException {
        in.reset();
        prev = savedPrev;
        state = savedState;
    }
}


