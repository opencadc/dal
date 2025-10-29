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
import ca.nrc.cadc.xml.MaxIterations;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Element;

/**
 * Writes data in VOTable BINARY2 format.
 */
public class BinaryElementWriter {

    private final FieldProcessorFactory decoderFactory = new FieldProcessorFactory();
    private final Iterator<List<Object>> rowIter;
    private final List<VOTableField> fields;
    private final MaxIterations maxIterations;
    private final Element trailer;

    public BinaryElementWriter(Iterator<List<Object>> rowIter, List<VOTableField> fields, MaxIterations maxIterations, Element trailer) {
        this.rowIter = rowIter;
        this.fields = fields;
        this.maxIterations = maxIterations;
        this.trailer = trailer;
    }

    public void write(Writer out) throws IOException {
        out.write("<BINARY2><STREAM encoding=\"base64\">");

        try (OutputStream base64Out = Base64.getEncoder().wrap(new WriterOutputStream(out))) {
            DataOutputStream dataOut = new DataOutputStream(base64Out);

            Iterator<List<Object>> iter = rowIter;
            long rowCount = 1;
            while (iter.hasNext()) {
                List<Object> row = iter.next();

                try {
                    writeRow(row, dataOut);
                } catch (Exception ex) {
                    // DALI error
                    trailer.setAttribute("name", "QUERY_STATUS");
                    trailer.setAttribute("value", "ERROR");
                    trailer.setText(ex.toString());
                }

                if (maxIterations != null && rowCount == maxIterations.getMaxIterations()) {
                    maxIterations.maxIterationsReached(iter.hasNext());
                    break;
                }
                rowCount++;
            }
            dataOut.flush();
        }

        out.write("</STREAM></BINARY2>");
    }

    private void writeRow(List<Object> row, DataOutputStream out) throws IOException {
        // Null flags
        int numFields = fields.size();
        int numFlagBytes = (numFields + 7) / 8;
        byte[] nullFlags = new byte[numFlagBytes];
        for (int i = 0; i < numFields; i++) {
            if (row.get(i) == null) {
                nullFlags[i / 8] |= (byte) (1 << (7 - (i % 8)));
            }
        }
        out.write(nullFlags);

        // Field values
        for (int i = 0; i < numFields; i++) {
            Object value = row.get(i);
            if (value != null) {
                decoderFactory.getFieldProcessor(fields.get(i).getDatatype().toLowerCase()).serialize(out, fields.get(i), value);
            }
        }
    }

    static class WriterOutputStream extends OutputStream {
        private final Writer writer;

        WriterOutputStream(Writer writer) {
            this.writer = writer;
        }

        @Override
        public void write(int b) throws IOException {
            writer.write(b);
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            for (int i = off; i < off + len; i++) {
                writer.write(b[i] & 0xFF);
            }
        }

        @Override
        public void flush() throws IOException {
            writer.flush();
        }
    }
}
