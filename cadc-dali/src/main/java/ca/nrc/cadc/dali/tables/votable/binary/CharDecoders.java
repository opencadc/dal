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
import java.io.IOException;

/**
 * Utility class with methods that can supply Decoders for reading
 * characters, strings and string arrays.
 *
 * @author Mark Taylor (Starlink)
 */
abstract class CharDecoders {

    private static final long ASSUMED_ARRAYSIZE = -2L;

    /**
     * Property which determines the default strictness state.
     * Its value may be set to "true" or "false" case-insensitively.
     * Borrowed from uk.ac.starlink.votable.VOElementFactory
     */
    public static final String STRICT_PROPERTY = "votable.strict";

    /**
     * Returns a decoder for a FIELD/PARAM with a declared
     * datatype attribute of 'char' and declared arraysize attribute
     * as per a given dimensions array.
     *
     * @param arraysize array representing value dimensions - last element
     *                  may be -1 to indicate unknown
     * @return decoder for <code>arraysize</code>-sized array of <code>char</code>s
     */
    public static Decoder makeCharDecoder(long[] arraysize) {
        CharDecoders.CharReader cread = new CharDecoders.CharReader() {
            public char readCharFromStream(DataInput strm)
                    throws IOException {
                return (char) (strm.readByte() & 0x00ff);
            }

            public int getCharSize() {
                return 1;
            }
        };
        return makeDecoder(arraysize, cread);
    }

    /**
     * Returns a decoder for a FIELD/PARAM with a declared
     * datatype attribute of 'unicodeChar' and declared arraysize attribute
     * as per a given dimensions array.
     *
     * @param arraysize array representing value dimensions - last element
     *                  may be -1 to indicate unknown
     * @return decoder for <code>arraysize</code>-sized array of
     * <code>unicodeChar</code>s
     */
    public static Decoder makeUnicodeCharDecoder(long[] arraysize) {
        CharDecoders.CharReader cread = new CharDecoders.CharReader() {
            public char readCharFromStream(DataInput strm)
                    throws IOException {
                return strm.readChar();
            }

            public int getCharSize() {
                return 2;
            }
        };
        return makeDecoder(arraysize, cread);
    }

    /**
     * Helper interface defining how to get a <code>char</code> from a stream.
     */
    private interface CharReader {

        /**
         * Reads a character from a stream.
         *
         * @param strm input stream
         * @return single character read from <code>strm</code>
         */
        char readCharFromStream(DataInput strm) throws IOException;

        /**
         * Returns the number of bytes read for a single character
         * (a single call of {@link #readCharFromStream}).
         *
         * @return byte count per character
         */
        int getCharSize();
    }


    /**
     * Returns a decoder for a character-type FIELD/PARAM with a given
     * arraysize and way of getting characters from a stream.
     *
     * @param arraysize array representing value dimensions
     * @param cread     character reader
     * @return decoder
     */
    private static Decoder makeDecoder(long[] arraysize, CharDecoders.CharReader cread) {
        int ndim = arraysize.length;

        /* Single character decoder. */
        if (ndim == 0 || ndim == 1 && arraysize[0] == 1) {
            return new CharDecoders.ScalarCharDecoder(cread);
        } else if (ndim == 1 && arraysize[0] == CharDecoders.ASSUMED_ARRAYSIZE) {
            /* If we have an assumed arraysize (non-strict VOTable parsing)
             * behave as if it's a variable-length array, except in the case
             * where we're decoding from a stream.  Attempting that would
             * probably be disastrous, since it would likely attempt to read
             * a character array a random number of bytes long, and fail wth
             * an OutOfMemoryError. */
            return new CharDecoders.ScalarStringDecoder(arraysize, cread) {
                public Object decodeStream(DataInput strm) {
                    throw new RuntimeException("Refuse to decode assumed char arraysize - try -D"
                                               + CharDecoders.STRICT_PROPERTY + "=true");
                }

                public void skipStream(DataInput strm) {
                    decodeStream(strm);
                }
            };
        } else if (ndim == 1) {
            /* Character vector (string) decoder. */
            return new CharDecoders.ScalarStringDecoder(arraysize, cread);
        } else {
            /* String array decoder. */
            return new CharDecoders.StringDecoder(arraysize, cread);
        }
    }

    /**
     * Decoder subclass for reading single character values.
     */
    private static class ScalarCharDecoder extends Decoder {
        final CharDecoders.CharReader cread;

        ScalarCharDecoder(CharDecoders.CharReader cread) {
            super(Character.class, SCALAR_SIZE);
            this.cread = cread;
        }

        public Object decodeString(String txt) {
            return txt.length() > 0 ? txt.charAt(0) : '\0';
        }

        public Object decodeStream(DataInput strm) throws IOException {
            assert getNumItems(strm) == 1;
            return cread.readCharFromStream(strm);
        }

        public void skipStream(DataInput strm) throws IOException {
            assert getNumItems(strm) == 1;
            skipBytes(strm, cread.getCharSize());
        }

        public boolean isNull(Object array, int index) {
            return false;
        }

        public void setNullValue(String txt) {
        }
    }

    /**
     * Decoder subclass for reading single string (= 1-d character array)
     * values.
     */
    private static class ScalarStringDecoder extends Decoder {
        final CharDecoders.CharReader cread;

        ScalarStringDecoder(long[] arraysize, CharDecoders.CharReader cread) {
            super(String.class, arraysize);
            this.cread = cread;
        }

        public long[] getDecodedShape() {
            return SCALAR_SIZE;
        }

        public int getElementSize() {
            return (int) arraysize[0];
        }

        public Object decodeString(String txt) {
            return txt;
        }

        public Object decodeStream(DataInput strm) throws IOException {
            int num = getNumItems(strm);
            StringBuilder data = new StringBuilder(num);
            int i = 0;
            while (i < num) {
                char c = cread.readCharFromStream(strm);
                i++;
                if (c == '\0') {
                    break;
                }
                data.append(c);
            }
            for (; i < num; i++) {
                cread.readCharFromStream(strm);
            }
            return new String(data);
        }

        public void skipStream(DataInput strm) throws IOException {
            int num = getNumItems(strm);
            skipBytes(strm, (long) num * cread.getCharSize());
        }

        public boolean isNull(Object array, int index) {
            return false;
        }

        public void setNullValue(String txt) {
        }
    }

    /**
     * Decoder subclass for reading arrays of strings (= multiple-dimensional
     * character array)l
     */
    private static class StringDecoder extends Decoder {
        final CharDecoders.CharReader cread;
        final long[] decodedShape;
        final boolean isVariable;
        int fixedSize;

        StringDecoder(long[] arraysize, CharDecoders.CharReader cread) {
            super(String[].class, arraysize);
            this.cread = cread;
            int ndim = arraysize.length;
            decodedShape = new long[ndim - 1];
            System.arraycopy(arraysize, 1, decodedShape, 0, ndim - 1);
            isVariable = arraysize[ndim - 1] < 0;
            if (!isVariable) {
                fixedSize = 1;
                for (long l : arraysize) {
                    fixedSize *= l;
                }
            }
        }

        public long[] getDecodedShape() {
            return decodedShape;
        }

        public int getElementSize() {
            return (int) arraysize[0];
        }

        public int getNumItems(DataInput strm) throws IOException {
            return isVariable ? super.getNumItems(strm) : fixedSize;
        }

        public Object decodeString(String txt) {
            return makeStrings(txt);
        }

        public Object decodeStream(DataInput strm) throws IOException {
            int num = getNumItems(strm);
            StringBuffer sbuf = new StringBuffer(num);
            for (int i = 0; i < num; i++) {
                sbuf.append(cread.readCharFromStream(strm));
            }
            return makeStrings(sbuf);
        }

        public void skipStream(DataInput strm) throws IOException {
            int num = getNumItems(strm);
            skipBytes(strm, (long) cread.getCharSize() * num);
        }

        public String[] makeStrings(CharSequence txt) {
            int ntok = txt.length();
            int ncell = numCells(ntok);
            int sleng = (int) arraysize[0];
            int nstr = ncell / sleng;
            String[] result = new String[nstr];
            int k = 0;
            char[] buf = new char[sleng];
            for (int i = 0; i < nstr && k < ntok; i++) {
                int leng = 0;
                while (leng < sleng && k < ntok) {
                    char c = txt.charAt(k++);
                    if (c == '\0') {
                        break;
                    }
                    buf[leng++] = c;
                }
                while (leng > 0 && buf[leng - 1] == ' ') {
                    leng--;
                }
                if (leng > 0) {
                    result[i] = new String(buf, 0, leng);
                }
                k = (i + 1) * sleng;
            }
            return result;
        }

        public boolean isNull(Object array, int index) {
            return false;
        }

        public void setNullValue(String txt) {
        }
    }
}
