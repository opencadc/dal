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

package ca.nrc.cadc.dali.tables.parquet.readerhelper;

import ca.nrc.cadc.dali.tables.votable.VOTableField;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.io.api.RecordMaterializer;

import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;

public class ParquetRowIterator implements Iterator<List<Object>> {
    private final ParquetFileReader reader;
    private final MessageType schema;
    private final List<VOTableField> fields;
    private PageReadStore currentRowGroup;
    private RecordReader<DynamicRow> recordReader;
    private long rowsInGroup;
    private long rowIndex;

    public ParquetRowIterator(ParquetFileReader reader, MessageType schema, List<VOTableField> fields) {
        this.reader = reader;
        this.schema = schema;
        this.fields = fields;
        this.currentRowGroup = null;
        this.recordReader = null;
        this.rowsInGroup = 0;
        this.rowIndex = 0;
        advanceRowGroup();
    }

    private void advanceRowGroup() {
        try {
            currentRowGroup = reader.readNextRowGroup();
            if (currentRowGroup != null) {
                rowsInGroup = currentRowGroup.getRowCount();
                rowIndex = 0;
                recordReader = createRecordReader(currentRowGroup, schema);
            } else {
                rowsInGroup = 0;
                recordReader = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read next row group", e);
        }
    }

    @Override
    public boolean hasNext() {
        if (recordReader == null) {
            return false;
        }
        if (rowIndex < rowsInGroup) {
            return true;
        }
        advanceRowGroup();
        return recordReader != null && rowIndex < rowsInGroup;
    }

    @Override
    public List<Object> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        rowIndex++;

        DynamicRow dynamicRow = recordReader.read();
        List<Object> row = new ArrayList<>();
        for (VOTableField field : fields) {
            String fieldName = field.getName();
            Type parquetField = schema.getType(fieldName);
            Object valueObj = dynamicRow.get(fieldName);
            String value;
            if (valueObj == null) {
                value = null;
            } else if (parquetField.isPrimitive()
                    && parquetField.asPrimitiveType().getLogicalTypeAnnotation() instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {
                if (valueObj instanceof Long) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                            .withZone(ZoneId.of("UTC"));
                    value = formatter.format(Instant.ofEpochMilli((long) valueObj));
                } else {
                    value = valueObj.toString();
                }

            } else {
                value = valueObj.toString().replaceAll("[\\[\\],]", "");
            }
            row.add(field.getFormat().parse(value));
        }

        return row;
    }

    private RecordReader<DynamicRow> createRecordReader(PageReadStore rowGroup, MessageType schema) {
        MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);
        RecordMaterializer<DynamicRow> materializer = new DynamicRowMaterializer(schema);
        return columnIO.getRecordReader(rowGroup, materializer);
    }
}
