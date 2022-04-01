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

package ca.nrc.cadc.dali.tables;

import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.tables.votable.binary.BinaryRowSequence;
import ca.nrc.cadc.dali.tables.votable.binary.Decoder;
import ca.nrc.cadc.dali.util.FormatFactory;
import ca.nrc.cadc.util.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;


/**
 * Handle incoming BINARY stream of data.
 */
public class BinaryTableData implements TableData {
    private static final Logger LOGGER = Logger.getLogger(BinaryTableData.class);

    private final List<VOTableField> fields = new ArrayList<>();
    private final String encoding;
    private final boolean isBinary2;
    private final InputStream inputStream;

    /**
     * Constructor.
     *
     * @param fields        Used to prime a formatter and setup decoders to properly get values from the stream.
     * @param inputStream   The stream to pull data from.
     * @param encoding      The binary encoding used.
     */
    public BinaryTableData(final List<VOTableField> fields, final InputStream inputStream, final String encoding,
                           final boolean isBinary2) {
        if (fields != null) {
            this.fields.addAll(fields);
        }

        this.encoding = encoding;
        this.isBinary2 = isBinary2;
        this.inputStream = inputStream;
    }

    /**
     * An iterator over the rows in the table. Each row is returned by one call
     * to Iterator.next() as a List of Object(s).
     *
     * @return iterator over the table rows
     */
    @Override
    public Iterator<List<Object>> iterator() {
        return new BinaryStreamIterator();
    }

    private final class BinaryStreamIterator implements Iterator<List<Object>> {
        private final FormatFactory formatFactory = new FormatFactory();
        private final HasNext hasNext = new HasNext();
        private final BinaryRowSequence rowSequence;

        public BinaryStreamIterator() {
            final Decoder[] decoders = new Decoder[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                final VOTableField field = fields.get(i);
                final int[] fieldArrayShape;
                if (field.getArrayShape() == null) {
                    fieldArrayShape = new int[0];
                } else {
                    fieldArrayShape = field.getArrayShape();
                }

                final long[] longArrayShape = new long[fieldArrayShape.length];
                for (int j = 0; j < fieldArrayShape.length; j++) {
                    longArrayShape[j] = fieldArrayShape[j];
                }
                decoders[i] = Decoder.makeDecoder(field.getDatatype(), longArrayShape, field.nullValue);
            }

            try {
                rowSequence = new BinaryRowSequence(decoders, inputStream, encoding, isBinary2);
            } catch (IOException ioException) {
                throw new IllegalStateException(ioException.getMessage(), ioException);
            }
        }

        /**
         * Returns {@code true} if the iteration has more elements.
         * (In other words, returns {@code true} if {@link #next} would
         * return an element rather than throwing an exception.)
         *
         * <p>The rowSequence.next() method will always try to advance, even if getRow() was not called</p>
         *
         * @return {@code true} if the iteration has more elements
         */
        @Override
        public boolean hasNext() {
            if (!this.hasNext.hasCheckedForNext) {
                try {
                    this.hasNext.hasNext = this.rowSequence.next();
                } catch (IOException ioException) {
                    throw new IllegalStateException(ioException.getMessage(), ioException);
                }
            }
            this.hasNext.hasCheckedForNext = true;

            return this.hasNext.hasNext;
        }

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration
         * @throws NoSuchElementException if the iteration has no more elements
         */
        @Override
        public List<Object> next() {
            try {
                final List<Object> row = getRow();
                for (int i = 0; i < fields.size(); i++) {
                    final VOTableField field = fields.get(i);
                    final Object o = row.get(i);
                    if (o != null) {
                        final Object formattedValue = formatFactory.getFormat(field).parse(o.toString());
                        final String arraysize = field.getArraysize();

                        // Where an arraysize="1" (or absent) and the datatype is char, then the value set must be of
                        // char.class.
                        if ((formattedValue instanceof String)
                            && ("1".equals(arraysize) || !StringUtil.hasText(arraysize))) {
                            final char[] charArray = ((String) formattedValue).toCharArray();
                            if (charArray.length == 1) {
                                row.set(i, Character.toString(charArray[0]));
                            } else if (charArray.length == 0) {
                                row.set(i, null);
                            } else {
                                throw new IllegalStateException("Expected char but got char array.");
                            }
                        } else {
                            row.set(i, formattedValue);
                        }
                    }
                }

                LOGGER.debug("Read row " + row);

                // Allow for another check.
                hasNext.hasCheckedForNext = false;
                return row;
            } catch (IOException ex) {
                throw new IllegalStateException(ex.getMessage(), ex);
            }
        }

        private List<Object> getRow() throws IOException {
            return Arrays.asList(rowSequence.getRow());
        }

        /**
         * Preserve the state of hasNext() in the Iterator.  This is required so that repeated checks of hasNext()
         * always returns the same value as when it was last called.  Only obtaining the next value from the iterator
         * will alter the state of this object.
         */
        private final class HasNext {
            boolean hasNext;
            boolean hasCheckedForNext;
        }
    }
}
