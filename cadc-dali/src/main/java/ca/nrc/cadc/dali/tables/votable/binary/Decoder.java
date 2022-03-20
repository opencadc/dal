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
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Decoder object associated with a Field.
 * Instances of this class know about the size and shape of fields
 * as well as their numeric type, and can decode various data sources
 * into the objects they represent.  To construct a decoder use the
 * static {@link #makeDecoder} method.
 *
 * <p>The various <tt>decode</tt> methods turn some kind of representation of
 * the given object into a standard representation.  The standard
 * representation is in accordance with the recommendations made in
 * the the {@link uk.ac.starlink.table} package.
 *
 * @author Mark Taylor (Starlink)
 */
public abstract class Decoder {

    private static final Logger logger = Logger.getLogger("uk.ac.starlink.votable");
    static final long[] SCALAR_SIZE = new long[0];

    protected String blankString;
    protected boolean isVariable;
    protected int sliceSize;
    protected long[] arraysize;
    private final Class<?> clazz;

    /**
     * Sets the null (bad) value to be used by the decoder from a string.
     *
     * @param txt a string representation of the bad value
     * @throws IllegalArgumentException if the string cannot be parsed
     */
    abstract void setNullValue(String txt);

    /**
     * Returns an object array based on the given text (space-separated
     * values for numeric types, normal string values for characters).
     *
     * @param txt a string encoding one or many values
     * @return an object containing the decoded values
     */
    public abstract Object decodeString(String txt);

    /**
     * Returns an object array read from the next bit of a given input
     * stream as raw bytes.  The VOTable BINARY/BINARY2 format is used.
     *
     * @param strm a DataInput object from which bytes are to be read
     */
    public abstract Object decodeStream(DataInput strm) throws IOException;

    /**
     * Skips over the bytes in a stream corresponding to a single cell.
     * The effect is the same as calling {@link #decodeStream}, but no
     * data is returned.
     *
     * @param strm a DataInput object from which bytes are to be read
     */
    public abstract void skipStream(DataInput strm) throws IOException;

    /**
     * Indicates whether an element of a given array matches the Null value
     * used by this decoder.
     *
     * @param array the array in which the element to check is
     * @param index the index into <tt>array</tt> at which the element to
     *              check is
     * @return <tt>true</tt> iff the <tt>index</tt>'th element of <tt>array</tt>
     *          matches the Null value for this decoder
     */
    public abstract boolean isNull(Object array, int index);

    /**
     * Does required setup for a decoder given its shape.
     *
     * @param clazz     the class to which all objects returned by the
     *                  <tt>decode*</tt> methods will belong
     * @param arraysize the dimensions of objects with this type -
     *                  the last element of the array may be negative to
     *                  indicate unknown slowest-varying dimension
     */
    protected Decoder(Class<?> clazz, long[] arraysize) {
        this.clazz = clazz;
        this.arraysize = arraysize;
        int ndim = arraysize.length;
        if (ndim == 0) {
            isVariable = false;
            sliceSize = 1;
        } else if (arraysize[ndim - 1] < 0) {
            isVariable = true;
            long ss = 1;
            for (int i = 0; i < ndim - 1; i++) {
                ss *= arraysize[i];
            }
            sliceSize = (int) ss;
        } else {
            isVariable = false;
            long ss = 1;
            for (long l : arraysize) {
                ss *= l;
            }
            sliceSize = (int) ss;
        }
    }

    /**
     * Returns the class for objects returned by this decoder.
     * Objects returned by the <tt>decode*</tt> methods of this decoder
     * will be instances of the class returned by this method, or <tt>null</tt>.
     *
     * @return object class
     */
    public Class<?> getContentClass() {
        return clazz;
    }

    /**
     * Returns the number of cells to use for this Decoder given a
     * certain number of available tokens.  This is just the arraysize
     * if it is fixed, or some large-enough multiple of the slice size
     * if it is variable.
     *
     * @param ntok the number of available tokens
     * @return the number of cells to fill
     */
    int numCells(int ntok) {
        if (isVariable) {
            return ((ntok + sliceSize - 1) / sliceSize) * sliceSize;
        } else {
            return sliceSize;
        }
    }

    /**
     * Work out how many items are to be read from the supplied input stream.
     * This may be a fixed number for this Decoder, or it may require
     * reading a count from the stream.
     */
    int getNumItems(DataInput strm) throws IOException {
        return isVariable ? strm.readInt() : sliceSize;
    }

    /**
     * Gets the shape of items returned by this decoder.  By default this
     * is the same as the <tt>arraysize</tt>, but decoders may
     * change the shape from that defined by the <tt>arraysize</tt> attribute
     * of the FIELD element.  In particular, the <tt>char</tt> and
     * <tt>unicodeChar</tt> decoders package an array of characters as
     * a String.
     *
     * @return the shape of objects returned by this decoder.  The last element might be negative to indicate variable
     *          size.
     */
    public long[] getDecodedShape() {
        return arraysize;
    }

    /**
     * Gets the 'element size' of items returned by this decoder.
     * This has the same meaning as
     * the Decoder implementation returns -1, but character-type decoders
     * override this.
     *
     * @return notional size of each element an array of values decoded by this object, or -1 if unknown
     */
    public int getElementSize() {
        return -1;
    }

    /**
     * Skips over a given number of bytes in a stream without reading them.
     *
     * @param strm stream
     */
    static void skipBytes(DataInput strm, long nskip) throws IOException {
        if (nskip < 0L) {
            throw new IllegalArgumentException("Can't skip backwards");
        }

        /* Try to skip. */
        boolean hasSkipped = false;
        while (nskip > 0L) {

            /* Attempt to skip a chunk. */
            int jskip = (int) Math.min(nskip, Integer.MAX_VALUE);
            int iskip;
            try {

                /* This switch on stream implementation type is a hack;
                 * really I just want to call DataInput.skipBytes here.
                 * The DataInput.skipBytes javadoc explicitly says that
                 * the method never throws EOFException.  However,
                 * nom.tam.util.BufferedDataInputStream sometimes does
                 * throw EOFException.  I am wary about fixing the bug in
                 * nom.tam because I don't know what else it might break.
                 * So work around it here in what should be a harmless way. */
                iskip = strm instanceof InputStream
                        ? (int) ((InputStream) strm).skip(jskip)
                        : strm.skipBytes(jskip);
                hasSkipped = true;
            } catch (IOException e) {
                /* Annoyingly, skipBytes can throw an "Illegal Seek" exception if
                 * the underlying stream does not support seek (e.g. stdin on
                 * Linux).  This behaviour has not always been documented in
                 * the InputStream javadocs (see Sun bug ID 6222822).
                 * If it looks like we've tripped over this here, log a suggested
                 * explanation. */
                if (!hasSkipped && !(e instanceof EOFException)) {
                    logger.warning("Input stream does not support seeks??");
                }
                throw e;
            }

            /* If no bytes were skipped, attempt to read a byte.  This will
             * either advance 1, or throw an EOFException. */
            if (iskip == 0) {
                strm.readByte();
                iskip = 1;
            }

            /* Decrease remaining bytes and continue. */
            nskip -= iskip;
        }
    }

    /**
     * Create a decoder given its datatype, shape and blank (bad) value.
     * The shape is specified by the <tt>arraysize</tt> parameter,
     * which gives array dimensions.   The last element of this array
     * may be negative to indicate an unknown last (slowest varying)
     * dimension.
     *
     * <p>All the decoders named in the VOTable 1.0 document are supported:
     * <ul>
     * <li>boolean
     * <li>bit
     * <li>unsignedByte
     * <li>short
     * <li>int
     * <li>long
     * <li>char
     * <li>unicodeChar
     * <li>float
     * <li>double
     * <li>floatComplex
     * <li>doubleComplex
     * </ul>
     *
     * @param datatype  the datatype name, that is the value of the
     *                  VOTable "datatype" attribute
     * @param arraysize shape of the array
     * @param blank     a string giving the bad value
     * @return Decoder object capable of decoding values according to its name and shape
     */
    public static Decoder makeDecoder(String datatype, long[] arraysize,
                                      String blank) {

        /* Work out if we have an effectively scalar quantity (either an
         * actual scalar or an array with one element. */
        boolean isScalar;
        int ndim = arraysize.length;
        if (ndim == 0) {
            isScalar = true;
        } else if (arraysize[ndim - 1] > 0) {
            int nel = 1;
            for (long l : arraysize) {
                nel *= l;
            }
            isScalar = nel == 1;
        } else {
            isScalar = false;
        }

        /* Construct a decoder for the arraysize and datatype. */
        Decoder dec;
        switch (datatype) {
            case "boolean":
                dec = isScalar ? BooleanDecoder.createScalarBooleanDecoder()
                               : new BooleanDecoder(arraysize);
                break;
            case "bit":
                dec = isScalar ? BitDecoder.createScalarBitDecoder()
                               : new BitDecoder(arraysize);
                break;
            case "unsignedByte":
                dec = isScalar ? new NumericDecoder.ScalarUnsignedByteDecoder()
                               : new NumericDecoder.UnsignedByteDecoder(arraysize);
                break;
            case "short":
                dec = isScalar ? new NumericDecoder.ScalarShortDecoder()
                               : new NumericDecoder.ShortDecoder(arraysize);
                break;
            case "int":
                dec = isScalar ? new NumericDecoder.ScalarIntDecoder()
                               : new NumericDecoder.IntDecoder(arraysize);
                break;
            case "long":
                dec = isScalar ? new NumericDecoder.ScalarLongDecoder()
                               : new NumericDecoder.LongDecoder(arraysize);
                break;
            case "char":
                dec = CharDecoders.makeCharDecoder(arraysize);
                break;
            case "unicodeChar":
                dec = CharDecoders.makeUnicodeCharDecoder(arraysize);
                break;
            case "float":
                dec = isScalar ? new NumericDecoder.ScalarFloatDecoder()
                               : new NumericDecoder.FloatDecoder(arraysize);
                break;
            case "double":
                dec = isScalar ? new NumericDecoder.ScalarDoubleDecoder()
                               : new NumericDecoder.DoubleDecoder(arraysize);
                break;
            case "floatComplex": {
                long[] arraysize2 = new long[arraysize.length + 1];
                arraysize2[0] = 2L;
                System.arraycopy(arraysize, 0, arraysize2, 1, arraysize.length);
                dec = new NumericDecoder.FloatDecoder(arraysize2);
                break;
            }
            case "doubleComplex": {
                long[] arraysize2 = new long[arraysize.length + 1];
                arraysize2[0] = 2L;
                System.arraycopy(arraysize, 0, arraysize2, 1, arraysize.length);
                dec = new NumericDecoder.DoubleDecoder(arraysize2);
                break;
            }
            default:
                logger.warning("Unknown data type " + datatype + " - treat as string"
                               + ", but may cause problems");
                dec = new UnknownDecoder();
                break;
        }

        /* Set the blank value. */
        if (blank != null && blank.trim().length() > 0) {
            try {
                dec.setNullValue(blank);
            } catch (IllegalArgumentException e) {
                logger.warning("Bad null value " + blank);
            }
        }

        /* Return the new Decoder object. */
        return dec;
    }


    private static class UnknownDecoder extends Decoder {
        public UnknownDecoder() {
            super(String.class, new long[0]);
        }

        public Object decodeString(String txt) {
            return txt;
        }

        public Object decodeStream(DataInput strm) {
            throw new UnsupportedOperationException(
                    "Can't do STREAM decode of unknown data type " + this);
        }

        public void skipStream(DataInput strm) {
            decodeStream(strm);
        }

        void setNullValue(String txt) {
        }

        public boolean isNull(Object array, int index) {
            return false;
        }
    }
}
