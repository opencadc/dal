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
 *  $Revision: 4 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.dali.tables.parquet;

import ca.nrc.cadc.dali.tables.TableData;
import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.tables.votable.VOTableResource;
import ca.nrc.cadc.io.ResourceIterator;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.List;

public class ParquetReaderWriterTest extends TestUtil {

    private static final Logger log = Logger.getLogger(ParquetReaderWriterTest.class);

    @Test
    public void testReadWriteParquet() throws Exception {
        log.debug("testReadWriteParquet");
        VOTableDocument originalVOTableDoc = prepareVOTable();

        ParquetWriter writer = new ParquetWriter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(prepareVOTable(), out);

        ParquetReader reader = new ParquetReader();
        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        VOTableDocument actualVOTableDoc = reader.read(inputStream);

        compareVOTable(originalVOTableDoc, actualVOTableDoc, null);
    }

    @Test
    public void readArbitrarySampleFile() throws Exception {
        log.debug("readArbitrarySampleFile");

        //File sample = new File(System.getProperty("user.home") + "/Downloads/sample.parquet"); // example path to a sample parquet file. You can change it to point to your own sample parquet file.
        File sample = new File("sample.parquet");

        if (!sample.exists()) {
            log.debug("not found: sample.parquet -- Skipping test...");
            return;
        }

        log.info("found: sample.parquet -- Reading...");
        ParquetReader reader = new ParquetReader();
        try (InputStream in = new FileInputStream(sample)) {

            VOTableDocument doc = reader.read(in);

            VOTableResource results = doc.getResourceByType("results");
            if (results != null) {
                log.info("Found results resource -- Reading...");

                if (results.getTable() != null) {
                    log.info("Found table in results resource");
                    for (VOTableField field : results.getTable().getFields()) {
                        log.info("field: " + field.getName() + " & type: " + field.getDatatype()
                            + "," + field.getArraysize() + "," + field.xtype);
                    }

                    if (results.getTable().getTableData() != null) {
                        log.info("Found data table in results resource");
                        TableData tableData = results.getTable().getTableData();
                        ResourceIterator<List<Object>> iterator = tableData.iterator();
                        int iteratorCount = 0;

                        while (iterator.hasNext()) {
                            List<Object> row = iterator.next();
                            StringBuilder sb = new StringBuilder();

                            for (Object data : row) {
                                if (data == null) {
                                    sb.append("null").append("\t");
                                } else if (data.getClass().isArray()) { // This might overwhelm the logs if there are large arrays. So comment if you experience it hard to load the standard output.
                                    int length = Array.getLength(data);
                                    sb.append("[");
                                    for (int i = 0; i < length; i++) {
                                        sb.append(Array.get(data, i));
                                        if (i < length - 1) {
                                            sb.append(", ");
                                        }
                                    }
                                    sb.append("]").append("\t");
                                } else {
                                    sb.append(data).append("\t");
                                }
                            }

                            log.info("row: " + sb);
                            iteratorCount++;
                        }
                        log.info("Total Rows count: " + iteratorCount);
                    }
                    for (VOTableField field : results.getTable().getFields()) {
                        log.info("field: " + field.getName() + " & type: " + field.getDatatype()
                            + "," + field.getArraysize() + "," + field.xtype);
                    }
                } else {
                    log.info("No results resource found");
                }

            } else {
                log.info("No table found in results resource");
            }
        }
    }

}
