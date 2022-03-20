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


import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.zip.GZIPInputStream;

/**
 * RowSequence implementation which reads streamed data in VOTable BINARY
 * format.
 *
 * @author Mark Taylor
 * @since 31 Jul 2006
 */
public class BinaryRowSequence {

    private final PushbackInputStream pIn_;
    private final DataInput dataIn_;
    private final int ncol_;
    private final BinaryRowSequence.RowReader rowReader_;
    private Object[] row_;

    /**
     * Constructs a new row sequence from a set of decoders and a
     * possibly encoded input stream.
     *
     * @param decoders  n-element array of decoders for decoding n-column data
     * @param in        input stream containing binary data
     * @param encoding  encoding string as per <tt>encoding</tt> attribute
     *                  of STREAM element ("gzip" or "base64", else assumed none)
     * @param isBinary2 true for BINARY2 format, false for BINARY
     */
    public BinaryRowSequence(final Decoder[] decoders, InputStream in, String encoding, boolean isBinary2)
            throws IOException {
        ncol_ = decoders.length;
        if ("gzip".equals(encoding)) {
            in = new GZIPInputStream(in);
        } else if ("base64".equals(encoding)) {
            in = new Base64InputStream(in);
        }
        pIn_ = new PushbackInputStream(in);
        dataIn_ = new DataInputStream(pIn_);
        rowReader_ = isBinary2
                     ? new BinaryRowSequence.RowReader() {
                            final boolean[] nullFlags = new boolean[ncol_];

                            public void readRow(Object[] row) throws IOException {
                                FlagIO.readFlags(dataIn_, nullFlags);
                                for (int icol = 0; icol < ncol_; icol++) {
                                    Decoder decoder = decoders[icol];
                                    final Object cell;
                                    if (nullFlags[icol]) {
                                        decoder.skipStream(dataIn_);
                                        cell = null;
                                    } else {
                                        cell = decoder.decodeStream(dataIn_);
                                    }
                                    row[icol] = cell;
                                }
                            }
                        }
                     : row -> {
                         for (int icol = 0; icol < ncol_; icol++) {
                             row[icol] = decoders[icol].decodeStream(dataIn_);
                         }
                     };
    }

    public boolean next() throws IOException {
        final int b;
        try {
            b = pIn_.read();
        } catch (EOFException e) {
            return false;
        }
        if (b < 0) {
            return false;
        } else {
            pIn_.unread(b);
            Object[] row = new Object[ncol_];
            rowReader_.readRow(row);
            row_ = row;
            return true;
        }
    }

    public Object[] getRow() {
        if (row_ != null) {
            return row_;
        } else {
            throw new IllegalStateException("No next() yet");
        }
    }

    /**
     * Interface for an object that can read a row from a binary stream.
     */
    private interface RowReader {
        /**
         * Populates a given row array with cell values for the next
         * data row available.
         *
         * @param row array of objects to be filled
         */
        void readRow(Object[] row) throws IOException;
    }
}

