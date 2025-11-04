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
import ca.nrc.cadc.dali.tables.votable.VOTableUtil;
import ca.nrc.cadc.dali.util.FormatFactory;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
 * Reads a single row from a VOTable binary stream.
 * */
public class BinaryRowReader {
    private final List<VOTableField> fields;
    private final FieldProcessorFactory decoderFactory = new FieldProcessorFactory();
    private final FormatFactory formatFactory;
    private final boolean isBinary2;

    public BinaryRowReader(List<VOTableField> fields, FormatFactory formatFactory, boolean isBinary2) {
        this.fields = fields;
        this.formatFactory = formatFactory == null ? new FormatFactory() : formatFactory;
        this.isBinary2 = isBinary2;
    }

    public List<Object> readRow(DataInputStream in) throws IOException {
        int numFields = fields.size();
        byte[] nullMask = null;

        // read null mask if binary2
        if (isBinary2) {
            int numMaskBytes = (numFields + 7) / 8;
            nullMask = new byte[numMaskBytes];
            int read = in.read(nullMask);
            if (read < numMaskBytes) {
                return null; // End of stream
            }
        }

        List<Object> row = new ArrayList<>(numFields);
        for (int i = 0; i < numFields; i++) {
            if (isBinary2) {
                boolean isNull = (nullMask[i / 8] & (1 << (7 - (i % 8)))) != 0;
                if (isNull) {
                    row.add(null);
                    continue;
                }
            }

            VOTableField field = fields.get(i);
            int length = computeLength(in, field);

            FieldProcessor fieldProcessor = decoderFactory.getFieldProcessor(field.getDatatype().toLowerCase());
            Object rawData = fieldProcessor.deSerialize(in, field, length);

            String stringValue = fieldProcessor.toStringValue(length, rawData);
            row.add(parseAndResolveNull(field, stringValue));
        }
        return row;
    }

    private static int computeLength(DataInputStream in, VOTableField field) throws IOException {
        int[] shape = VOTableUtil.parseArraySize(field.getArraysize());
        int length = 1;

        if (shape != null) {
            int variableDim = 1;
            if (shape[shape.length - 1] == -1) { // -1 is for variable dimension
                variableDim = in.readInt();
            }
            for (int dim : shape) {
                if (dim == -1) {
                    dim = variableDim;
                }
                length *= dim;
            }
        }
        return length;
    }

    private Object parseAndResolveNull(VOTableField field, String stringValue) {
        Object parsedData = formatFactory.getFormat(field).parse(stringValue);

        if (field.nullValue != null && !field.nullValue.isEmpty()) {
            Object nullValue = formatFactory.getFormat(field).parse(field.nullValue);
            if (Objects.equals(parsedData, nullValue)) {
                parsedData = null;
            }
        } else if (parsedData instanceof Float && Float.isNaN((Float) parsedData)) {
            parsedData = null;
        } else if (parsedData instanceof Double && Double.isNaN((Double) parsedData)) {
            parsedData = null;
        }
        return parsedData;
    }

}
