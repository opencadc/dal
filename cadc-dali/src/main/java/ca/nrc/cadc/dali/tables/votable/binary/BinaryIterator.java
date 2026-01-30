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

package ca.nrc.cadc.dali.tables.votable.binary;

import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.util.FormatFactory;
import ca.nrc.cadc.io.ResourceIterator;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.log4j.Logger;

/**
 * Iterator for reading rows from a VOTable BINARY2 encoded stream.
 * <p>
 * This class decodes an input stream containing BINARY or BINARY2 data,
 * reads each row using a {@link BinaryRowReader}, and returns the row as a list of objects.
 * </p>
 */
public class BinaryIterator implements ResourceIterator<List<Object>> {

    private static final Logger log = Logger.getLogger(BinaryIterator.class);

    private final DataInputStream in;
    private final BinaryRowReader binaryRowReader;
    private List<Object> nextRow;
    private boolean finished = false;

    public BinaryIterator(InputStream input, List<VOTableField> fields, String encoding, FormatFactory formatFactory, boolean isBinary2) {
        if ("gzip".equalsIgnoreCase(encoding)) {
            try {
                this.in = new DataInputStream(new GZIPInputStream(input));
            } catch (IOException e) {
                throw new RuntimeException("Failed to create GZIPInputStream");
            }
        } else if ("base64".equalsIgnoreCase(encoding)) {
            this.in = new DataInputStream(new Base64InputStream(input));
        } else {
            throw new IllegalArgumentException("Unsupported encoding: " + encoding);
        }

        this.binaryRowReader = new BinaryRowReader(fields, formatFactory, isBinary2);
        fetchNext();
    }

    @Override
    public boolean hasNext() {
        return nextRow != null && !finished;
    }

    @Override
    public List<Object> next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No more rows");
        }
        List<Object> current = nextRow;
        fetchNext();
        return current;
    }

    private void fetchNext() {
        try {
            nextRow = binaryRowReader.readRow(in);
            if (nextRow == null) {
                finished = true;
            }
        } catch (EOFException eof) { // This can be a loophole. But no other way for binary reading.
            finished = true;
            nextRow = null;
        } catch (Exception e) {
            log.error("Error while reading next row", e);
            finished = true;
            nextRow = null;
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
