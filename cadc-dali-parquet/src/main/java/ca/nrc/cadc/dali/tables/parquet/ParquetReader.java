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
 *  : 5 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.dali.tables.parquet;

import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import org.apache.avro.generic.GenericRecord;
import org.apache.log4j.Logger;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.io.SeekableInputStream;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;


//TODO: Temporary implementation
public class ParquetReader {

    private static final Logger log = Logger.getLogger(ParquetReader.class);

    public TableShape read(InputStream inputStream) throws IOException {

        InputFile inputFile = getInputFile(inputStream);
        int recordCount = 0;
        int columnCount = 0;

        ParquetMetadata metadata = ParquetFileReader.open(inputFile).getFooter();
        MessageType schema = metadata.getFileMetaData().getSchema();
        columnCount = schema.getFieldCount();

        String votable = metadata.getFileMetaData().getKeyValueMetaData().get("IVOA.VOTable-Parquet.content");
        log.debug("VOTable: " + votable);

        VOTableReader voTableReader = new VOTableReader();
        VOTableDocument voTableDocument = voTableReader.read(votable);

        try (org.apache.parquet.hadoop.ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(inputFile).build()) {
            GenericRecord record;

            while ((record = reader.read()) != null) {
                log.debug("Reading Record: " + record);
                recordCount++;

                // TODO: This is a temporary implementation to read the data
                Map<String, Object> rowData = new HashMap<>();
                for (Type field : schema.getFields()) {
                    String fieldName = field.getName();
                    rowData.put(fieldName, record.get(fieldName));
                }
                log.debug("Retrieved Row Data: " + rowData);
            }
            log.debug("Record Count = " + recordCount);

        } catch (Exception e) {
            throw new IOException("failed to read parquet data", e);
        }
        if (recordCount == 0) {
            throw new RuntimeException("NO Records Read");
        }
        return new TableShape(recordCount, columnCount, voTableDocument);
    }

    private static InputFile getInputFile(InputStream inputStream) throws IOException {

        // Read the entire InputStream into a byte array
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(temp)) != -1) {
            buffer.write(temp, 0, bytesRead);
        }

        byte[] inputData = buffer.toByteArray();

        return new InputFile() {
            @Override
            public long getLength() throws IOException {
                return inputData.length;
            }

            @Override
            public SeekableInputStream newStream() {
                return new SeekableInputStream() {
                    private final InputStream delegate = new ByteArrayInputStream(inputData);
                    private long pos = 0;

                    @Override
                    public void seek(long newPos) throws IOException {
                        if (newPos < 0) {
                            throw new IllegalArgumentException("Negative positions are not supported");
                        }
                        delegate.reset();
                        delegate.skip(newPos);
                        pos = newPos;
                    }

                    @Override
                    public void readFully(byte[] bytes, int off, int len) throws IOException {
                        int bytesRead = 0;
                        while (bytesRead < len) {
                            int result = read(bytes, off + bytesRead, len - bytesRead);
                            if (result == -1) {
                                throw new EOFException("Unexpected end of stream");
                            }
                            bytesRead += result;
                        }
                    }

                    @Override
                    public void readFully(byte[] bytes) throws IOException {
                        int bytesRead = 0;
                        while (bytesRead < bytes.length) {
                            int result = read(bytes, bytesRead, bytes.length - bytesRead);
                            if (result == -1) {
                                throw new EOFException("Unexpected end of stream");
                            }
                            bytesRead += result;
                        }
                    }

                    @Override
                    public void readFully(ByteBuffer byteBuffer) throws IOException {
                        int bytesRead = 0;
                        while (byteBuffer.hasRemaining()) {
                            int result = read(byteBuffer);
                            if (result == -1) {
                                throw new EOFException("Unexpected end of stream");
                            }
                            bytesRead += result;
                        }
                    }

                    @Override
                    public int read(ByteBuffer byteBuffer) throws IOException {
                        byte[] temp = new byte[byteBuffer.remaining()];
                        int bytesRead = read(temp);
                        if (bytesRead > 0) {
                            byteBuffer.put(temp, 0, bytesRead);
                        }
                        return bytesRead;
                    }

                    @Override
                    public int read() throws IOException {
                        int result = delegate.read();
                        if (result != -1) {
                            pos++;
                        }
                        return result;
                    }

                    @Override
                    public long getPos() throws IOException {
                        return pos;
                    }

                    @Override
                    public void close() throws IOException {
                        delegate.close();
                    }
                };
            }
        };
    }

    public static class TableShape {
        int recordCount;
        int columnCount;
        VOTableDocument voTableDocument;

        public TableShape(int recordCount, int columnCount, VOTableDocument voTableDocument) {
            this.recordCount = recordCount;
            this.columnCount = columnCount;
            this.voTableDocument = voTableDocument;
        }

        public int getRecordCount() {
            return recordCount;
        }

        public int getColumnCount() {
            return columnCount;
        }

        public VOTableDocument getVoTableDocument() {
            return voTableDocument;
        }
    }

}
