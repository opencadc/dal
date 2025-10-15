/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2009.                            (c) 2009.
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

package ca.nrc.cadc.dali.tables.votable;

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.dali.tables.ListTableData;
import ca.nrc.cadc.dali.tables.TableData;
import ca.nrc.cadc.dali.util.Format;
import ca.nrc.cadc.dali.util.FormatFactory;
import ca.nrc.cadc.dali.util.PointFormat;
import ca.nrc.cadc.dali.util.ShortFormat;
import ca.nrc.cadc.date.DateUtil;
import ca.nrc.cadc.io.ResourceIterator;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.Log4jInit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import ca.nrc.cadc.util.StringUtil;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author jburke
 */
public class VOTableReaderWriterTest {

    private static final Logger log = Logger.getLogger(VOTableReaderWriterTest.class);

    private static final String DATE_TIME = "2009-01-02T11:04:05.678";
    private static DateFormat dateFormat;

    static {
        Log4jInit.setLevel("ca.nrc.cadc.dali.tables", Level.INFO);
        dateFormat = DateUtil.getDateFormat(DateUtil.IVOA_DATE_FORMAT, DateUtil.UTC);
    }

    public VOTableReaderWriterTest() {
    }

    @Test
    public void testReadWriteVOTable() throws Exception {
        log.debug("testReadWriteVOTable");
        try {
            String resourceName = "VOTable resource name";

            // Build a VOTable.
            VOTableDocument expected = new VOTableDocument();

            // Add INFO's to the VOTableDocument.
            expected.getInfos().addAll(getTestInfos("a"));

            VOTableResource vr = new VOTableResource("meta");
            vr.description = "what is a meta?";
            expected.getResources().add(vr);

            vr.getParams().addAll(getMetaParams());
            vr.getGroups().add(getMetaGroup());

            // Add INFO's to meta VOTableResource.
            vr.getInfos().addAll(getTestInfos("b"));

            vr = new VOTableResource("results");
            expected.getResources().add(vr);
            vr.setName(resourceName);

            // Add INFO's to results VOTableResource.
            vr.getInfos().addAll(getTestInfos("c"));

            VOTableTable vot = new VOTableTable();
            vr.setTable(vot);

            // Add INFO's to VOTableTable.
            vot.getInfos().addAll(getTestInfos("d"));

            // Add VOTableFields.
            vot.getParams().addAll(getTestParams());
            vot.getFields().addAll(getTestFields());

            // Add TableData.
            vot.setTableData(new TestTableData());

            // Write VOTable to xml.
            StringWriter sw = new StringWriter();
            VOTableWriter writer = new VOTableWriter();
            writer.write(expected, sw);
            String xml = sw.toString();
            log.debug("XML: \n\n" + xml);

            // Read in xml to VOTable with schema validation.
            VOTableReader reader = new VOTableReader();
            VOTableDocument actual = reader.read(xml);

            log.debug("Expected:\n\n" + expected);
            log.debug("Actual:\n\n" + actual);

            // writer always places this placeholder after a table
            vr.getInfos().add(new VOTableInfo("placeholder", "ignore"));
            compareVOTable(expected, actual, null);

        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testReadWriteVOTableWithMax() throws Exception {
        log.debug("testReadWriteVOTable");
        long maxrec = 3L;
        try {
            String resourceName = "VOTable resource name";

            // Build a VOTable.
            VOTableDocument expected = new VOTableDocument();

            // Add INFO's to document.
            expected.getInfos().addAll(getTestInfos("a"));

            VOTableResource vr = new VOTableResource("results");
            vr.getInfos().add(new VOTableInfo("QUERY_STATUS", "OK"));
            expected.getResources().add(vr);
            vr.setName(resourceName);

            // Add INFO's to resource.
            vr.getInfos().addAll(getTestInfos("b"));

            VOTableTable vot = new VOTableTable();
            vr.setTable(vot);

            // Add INFO's to table.
            vot.getInfos().addAll(getTestInfos("c"));

            // Add VOTableFields.
            vot.getParams().addAll(getTestParams());
            vot.getFields().addAll(getTestFields());

            // Add TableData.
            vot.setTableData(new TestTableData(maxrec + 1L));

            // Write VOTable to xml.
            StringWriter sw = new StringWriter();
            VOTableWriter writer = new VOTableWriter();
            writer.write(expected, sw, maxrec);
            String xml = sw.toString();
            log.info("XML: \n\n" + xml);

            // Read in xml to VOTable with schema validation.
            VOTableReader reader = new VOTableReader();
            VOTableDocument actual = reader.read(xml);

            // the write should stick in this extra INFO element, so add to expected
            VOTableInfo vi = new VOTableInfo("QUERY_STATUS", "OVERFLOW");
            vr.getInfos().add(vi);
            compareVOTable(expected, actual, 3L);

        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testReadWriteVOTableWithIterationFail() throws Exception {
        log.debug("testReadWriteVOTable");
        try {
            String resourceName = "VOTable resource name";

            // Build a VOTable.
            VOTableDocument expected = new VOTableDocument();

            // Add INFO's to document.
            expected.getInfos().addAll(getTestInfos("a"));

            VOTableResource vr = new VOTableResource("results");
            vr.getInfos().add(new VOTableInfo("QUERY_STATUS", "OK"));
            expected.getResources().add(vr);
            vr.setName(resourceName);

            // Add INFO's to resource.
            vr.getInfos().addAll(getTestInfos("b"));

            VOTableTable vot = new VOTableTable();
            vr.setTable(vot);

            // Add INFO's to table.
            vot.getInfos().addAll(getTestInfos("c"));

            // Add VOTableFields.
            vot.getParams().addAll(getTestParams());
            vot.getFields().addAll(getTestFields());

            // Add TableData.
            vot.setTableData(new TestTableData());

            // Write VOTable to xml.
            StringWriter sw = new StringWriter();
            VOTableWriter writer = new VOTableWriter();
            writer.setFormatFactory(new BrokenFormatFactory());
            writer.write(expected, sw);
            String xml = sw.toString();
            log.debug("XML: \n\n" + xml);

            // Read in xml to VOTable with schema validation.
            VOTableReader reader = new VOTableReader();
            VOTableDocument actual = reader.read(xml);

            VOTableResource ar = actual.getResourceByType("results");
            
            Assert.assertFalse(ar.getInfos().isEmpty());
            VOTableInfo trailer = ar.getInfos().get(ar.getInfos().size() - 1);
            Assert.assertEquals("QUERY_STATUS", trailer.getName());
            Assert.assertEquals("ERROR", trailer.getValue());

        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    private class BrokenFormatFactory extends FormatFactory {

        @Override
        public Format getFormat(VOTableField field) {
            Format ret = super.getFormat(field);
            if (ret instanceof PointFormat) {
                return new ShortFormat();
            }
            return ret;
        }
        
    }

    @Test
    public void testReadWriteVOTableArraysize() throws Exception {
        log.debug("testReadWriteVOTableArraysize");
        try {
            String resourceName = "VOTable resource name";

            // Build a VOTable.
            VOTableDocument expected = new VOTableDocument();

            // Add INFO's to document.
            expected.getInfos().addAll(getTestInfos("a"));

            VOTableResource vr = new VOTableResource("meta");
            expected.getResources().add(vr);
            vr = new VOTableResource("results");
            VOTableInfo vi = new VOTableInfo("FOO", "bar");
            vi.content = "useful message";
            vr.getInfos().add(vi);
            expected.getResources().add(vr);
            vr.setName(resourceName);

            VOTableTable vot = new VOTableTable();
            vr.setTable(vot);

            VOTableField f;

            f = new VOTableField("scalar", "double", null);
            vot.getFields().add(f);
            f = new VOTableField("array-1", "double", "1");
            vot.getFields().add(f);
            f = new VOTableField("array-4", "double", "4");
            vot.getFields().add(f);
            f = new VOTableField("array-4*", "double", "4*");
            vot.getFields().add(f);
            f = new VOTableField("array-2x3", "double", "4x3");
            vot.getFields().add(f);
            f = new VOTableField("array-4x*", "double", "4x*");
            vot.getFields().add(f);
            f = new VOTableField("array-*", "double", "*");
            vot.getFields().add(f);

            ListTableData tData = new ListTableData();
            List<Object> nullData = new ArrayList<>();
            nullData.add(null);
            nullData.add(null);
            nullData.add(null);
            nullData.add(null);
            nullData.add(null);
            nullData.add(null);
            nullData.add(null);
            tData.getArrayList().add(nullData);

            List<Object> valData = new ArrayList<>();
            valData.add(1.0);
            double[] a1 = new double[1];
            valData.add(a1);
            double[] fa = new double[4];
            valData.add(fa);
            double[] va = new double[4];
            valData.add(va);
            double[][] fa2 = new double[4][3];
            valData.add(fa2);
            double[][] va2 = new double[4][5];
            valData.add(va2);
            for (int i = 0; i < 4; i++) {
                fa[i] = i;
                if (i < 5) {
                    va[i] = i;
                }
                for (int j = 0; j < 3; j++) {
                    fa2[i][j] = i + j;
                }
                for (int j = 0; j < 5; j++) {
                    va2[i][j] = i + j;
                }
            }
            double[] va3 = new double[7];
            valData.add(va3);
            for (int i = 0; i < 7; i++) {
                va3[i] = i;
            }
            tData.getArrayList().add(valData);

            // Add TableData.
            vot.setTableData(tData);

            // Write VOTable to xml.
            StringWriter sw = new StringWriter();
            VOTableWriter writer = new VOTableWriter();
            writer.write(expected, sw);
            String xml = sw.toString();
            log.debug("XML: \n\n" + xml);

            // Read in xml to VOTable with schema validation.
            VOTableReader reader = new VOTableReader();
            VOTableDocument actual = reader.read(xml);

            log.debug("Expected:\n\n" + expected);
            log.debug("Actual:\n\n" + actual);

            // writer always places this placeholder after a table
            vr.getInfos().add(new VOTableInfo("placeholder", "ignore"));
            compareVOTable(expected, actual, null);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    /**
     * Returns a test table that matches the output BINARY file's table.
     *
     * The table returned here MUST match the file represented in
     * src/test/resources/VOTableReaderWriterTest_testReadVOTableBinaryData.xml.
     *
     * Ideally this test would create a table and write out the BINARY STREAM data, but no such logic exists yet
     * (outside of the Starjava library), so this test will rely on an existing table with a known structure.
     *
     * @return A VOTableDocument representing the test file.
     */
    VOTableDocument createExpectedUploadTable() {
        final int nrow = 23;
        /* Set up data arrays per column. */
        short[] shortData = new short[nrow];
        int[] intData = new int[nrow];
        long[] longData = new long[nrow];
        float[] floatData = new float[nrow];
        double[] doubleData = new double[nrow];
        char[] charData = new char[nrow];
        String[] stringData = new String[nrow];
        String[] timeData = new String[nrow];
        final DateFormat dateFormat = DateUtil.getDateFormat(DateUtil.ISO8601_DATE_FORMAT_LOCAL, DateUtil.UTC);

        /* Initialise values for each element of each data array. */
        /* I've avoided byte types here because STIL is not comfortable
         * with them; it tends to convert them to shorts when writing
         * VOTables.  That is probably a deficiency in STIL. */
        for (int ir = 0; ir < nrow; ir++) {
            shortData[ir] = (short) ir;
            intData[ir] = ir;
            longData[ir] = ir;
            floatData[ir] = 1.125F * ir;
            doubleData[ir] = 1.125D * ir;
            charData[ir] = (char) ('A' + ir);
            stringData[ir] = Integer.toString(ir);
            timeData[ir] = dateFormat.format(DateUtil.fromModifiedJulianDate(51544.0D + 1.01D * ir));
        }

        /* Construct a table based on the data arrays. */
        final VOTableDocument voTableDocument = new VOTableDocument();
        final VOTableResource voTableResource = new VOTableResource("meta");
        final VOTableTable voTableTable = new VOTableTable();

        final List<VOTableField> fields = new ArrayList<>();
        fields.add(makeField("d_short", shortData, null));
        fields.add(makeField("d_int", intData, null));
        fields.add(makeField("d_long", longData, null));
        fields.add(makeField("d_float", floatData, null));
        fields.add(makeField("d_double", doubleData, null));

        // Having arraysize="1" is still allowed but deprecated with warnings in VOTable 1.3.
        final VOTableField dCharField = makeField("d_char", charData, null);
        dCharField.arraysize = "1";
        dCharField.arrayShape = VOTableUtil.getArrayShape(dCharField.getArraysize());
        fields.add(dCharField);

        fields.add(makeField("d_string", stringData, null));
        fields.add(makeField("d_time", timeData, "adql:TIMESTAMP"));
        voTableTable.getFields().addAll(fields);

        final ListTableData listTableData = new ListTableData();

        final List<List<Object>> arrayList = listTableData.getArrayList();

        for (int r = 0; r < nrow; r++) {
            final List<Object> row = new ArrayList<>();
            row.add(shortData[r]);
            row.add(intData[r]);
            row.add(longData[r]);
            row.add(floatData[r]);
            row.add(doubleData[r]);
            row.add(charData[r]);
            row.add(stringData[r]);
            row.add(timeData[r]);
            arrayList.add(row);
        }

        // Populate the file row with blank values, where appropriate.
        final List<Object> emptyRow = new ArrayList<>();
        final List<Object> lastRow = arrayList.get(nrow - 1);

        for (int i = 0; i < fields.size(); i++) {
            final VOTableField field = fields.get(i);

            // char fields are not considered nullable in the data.
            if (!"char".equals(field.getDatatype())) {
                emptyRow.add(null);
            } else {
                emptyRow.add(lastRow.get(i));
            }
        }

        arrayList.set(nrow - 1, emptyRow);

        voTableTable.setTableData(listTableData);
        voTableResource.setTable(voTableTable);
        voTableDocument.getResources().add(voTableResource);

        return voTableDocument;
    }

    /**
     * Returns a new VOTableField for the given data.
     *
     * @param name      Field name.
     * @param data      The data to be expected in this field.
     * @param xtype     The optional xtype.
     * @return  VOTableField instance.  Never null.
     */
    VOTableField makeField(final String name, final Object data, final String xtype) {
        final String datatype;
        final Class<?> contentClass = data.getClass().getComponentType();

        if (contentClass == byte.class) {
            datatype = "unsignedByte";
        } else if (contentClass == short.class) {
            datatype = "short";
        } else if (contentClass == int.class) {
            datatype = "int";
        } else if (contentClass == long.class) {
            datatype = "long";
        } else if (contentClass == float.class) {
            datatype = "float";
        } else if (contentClass == double.class) {
            datatype = "double";
        } else if (contentClass == char.class || contentClass.equals(String.class)) {
            datatype = "char";
        } else if (contentClass == boolean.class) {
            datatype = "boolean";
        } else {
            throw new AssertionError("Unsupported type " + contentClass);
        }

        final VOTableField voTableField;

        if (contentClass == String.class) {
            String[] sdata = (String[]) data;
            int size = 0;
            for (String sdatum : sdata) {
                size = Math.max(size, sdatum.length());
            }
            voTableField = new VOTableField(name, datatype, Integer.toString(size));
        } else if (contentClass.isArray()) {
            int size = 0;
            for (int i = 0; i < Array.getLength(data); i++) {
                Object item = Array.get(data, i);
                size = Math.max(size, Array.getLength(item));
            }
            voTableField = new VOTableField(name, datatype, Integer.toString(size));
        } else {
            voTableField = new VOTableField(name, datatype);
        }
        voTableField.xtype = xtype;
        voTableField.nullValue = null;
        return voTableField;
    }

    /**
     * Uses a BINARY element with a base64 encoded STREAM to emulate the taplint test.
     *
     * @throws Exception For any issues to report.
     */
    @Test
    public void testReadVOTableBinaryData() throws Exception {
        final File tempFile = FileUtil.getFileFromResource(getClass().getSimpleName()
                                                           + "_testReadVOTableBinaryData.xml", getClass());
        final VOTableDocument resultTable;
        try (final FileInputStream fileInputStream = new FileInputStream(tempFile)) {
            final VOTableReader voTableReader = new VOTableReader();
            resultTable = voTableReader.read(fileInputStream);
        }

        final VOTableDocument expectedVOTableDocument = createExpectedUploadTable();
        final VOTableResource expectedVOTableResource = expectedVOTableDocument.getResources().get(0);
        final VOTableTable expectedVOTableTable = expectedVOTableResource.getTable();
        final List<VOTableField> expectedFields = expectedVOTableTable.getFields();
        final List<List<Object>> expectedRows = new ArrayList<>();
        final FormatFactory formatFactory = new FormatFactory();
        expectedVOTableTable.getTableData().iterator().forEachRemaining(expectedRows::add);

        // Match the formatting.
        for (final List<Object> row : expectedRows) {
            for (int i = 0; i < expectedFields.size(); i++) {
                final VOTableField field = expectedFields.get(i);
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
        }

        final VOTableResource resultResource = resultTable.getResources().get(0);
        final List<List<Object>> resultRows = new ArrayList<>();
        resultResource.getTable().getTableData().iterator().forEachRemaining(resultRows::add);

        compareFields(expectedFields, resultResource.getTable().getFields());

        for (int i = 0; i < expectedRows.size(); i++) {
            final List<Object> expectedRow = expectedRows.get(i);
            final List<Object> resultRow = resultRows.get(i);

            for (int f = 0; f < expectedFields.size(); f++) {
                final VOTableField field = expectedFields.get(f);
                final String xtype = field.xtype;
                if ("adql:TIMESTAMP".equals(xtype) || "timestamp".equals(xtype)) {
                    final Date expectedDate = (Date) expectedRow.get(f);
                    final Date resultDate = (Date) resultRow.get(f);
                    Assert.assertEquals("Row date value (" + i + ") and field (" + f + ") don't match.",
                                        expectedDate.getTime(), resultDate.getTime(), 1000.0D);
                } else {
                    Assert.assertEquals("Row value (" + i + ") and field (" + f + ") don't match.",
                                        expectedRow.get(f), resultRow.get(f));
                }
            }
        }
    }

    /**
     * Test might be a bit dodgy since it's assuming the VOTable
     * elements will be written and read in the same order.
     */
    public void compareVOTable(VOTableDocument expected, VOTableDocument actual, Long actualMax) throws IOException {
        Assert.assertNotNull(expected);
        Assert.assertNotNull(actual);

        Assert.assertEquals(expected.getResources().size(), actual.getResources().size());

        for (int i = 0; i < expected.getResources().size(); i++) {
            compareVOTableResource(expected.getResources().get(i), actual.getResources().get(i), actualMax);
        }
    }

    public void compareVOTableResource(VOTableResource expected, VOTableResource actual, Long actualMax) throws IOException {
        Assert.assertEquals(expected.getName(), actual.getName());
        
        Assert.assertEquals(expected.description, actual.description);

        compareInfos(expected.getInfos(), actual.getInfos());

        compareParams(expected.getParams(), actual.getParams());

        compareGroups(expected.getGroups(), actual.getGroups());

        compareTables(expected.getTable(), actual.getTable(), actualMax);
    }

    public void compareTables(VOTableTable expected, VOTableTable actual, Long actualMax) throws IOException {
        if (expected != null) {
            Assert.assertNotNull(actual);
        } else {
            Assert.assertNull(actual);
            return;
        }

        compareInfos(expected.getInfos(), actual.getInfos());
        compareParams(expected.getParams(), actual.getParams());
        compareFields(expected.getFields(), actual.getFields());

        // TABLEDATA
        Assert.assertNotNull(expected.getTableData());
        Assert.assertNotNull(actual.getTableData());
        TableData expectedTableData = expected.getTableData();
        TableData actualTableData = actual.getTableData();
        Iterator<List<Object>> expectedIter = expectedTableData.iterator();
        Iterator<List<Object>> actualIter = actualTableData.iterator();
        Assert.assertNotNull(expectedIter);
        Assert.assertNotNull(actualIter);
        int iteratorCount = 0;
        while (actualIter.hasNext()) // this one is the smaller list
        {
            iteratorCount++;
            log.debug("iteratorCount: " + (iteratorCount));

            List<Object> actualList = actualIter.next();
            List<Object> expectedList = expectedIter.next();
            log.debug("expected row: " + expectedList);
            log.debug("  actual row: " + actualList);

            Assert.assertEquals(expectedList.size(), actualList.size());
            for (int i = 0; i < expectedList.size(); i++) {
                Object expectedObject = expectedList.get(i);
                Object actualObject = actualList.get(i);

                if (expectedObject instanceof byte[] && actualObject instanceof byte[]) {
                    Assert.assertArrayEquals((byte[]) expectedObject, (byte[]) actualObject);
                } else if (expectedObject instanceof double[] && actualObject instanceof double[]) {
                    Assert.assertArrayEquals((double[]) expectedObject, (double[]) actualObject, 0.0);
                } else if (expectedObject instanceof double[][] && actualObject instanceof double[][]) {
                    double[][] exp = (double[][]) expectedObject;
                    double[][] act = (double[][]) actualObject;
                    Assert.assertEquals(exp.length, act.length);
                    for (int i1 = 0; i1 < exp.length; i1++) {
                        Assert.assertEquals(exp[i1].length, act[i1].length);
                        for (int i2 = 0; i2 < exp[i1].length; i2++) {
                            Assert.assertEquals("array[" + i1 + "," + i2 + "]", exp[i1][i2], act[i1][i2], 0.0);
                        }
                    }
                } else if (expectedObject instanceof float[] && actualObject instanceof float[]) {
                    Assert.assertArrayEquals((float[]) expectedObject, (float[]) actualObject, 0.0f);
                } else if (expectedObject instanceof int[] && actualObject instanceof int[]) {
                    Assert.assertArrayEquals((int[]) expectedObject, (int[]) actualObject);
                } else if (expectedObject instanceof long[] && actualObject instanceof long[]) {
                    Assert.assertArrayEquals((long[]) expectedObject, (long[]) actualObject);
                } else if (expectedObject instanceof short[] && actualObject instanceof short[]) {
                    Assert.assertArrayEquals((short[]) expectedObject, (short[]) actualObject);
                } /*
                else if (expectedObject instanceof Position && actualObject instanceof Position)
                {
                    Position expectedPosition = (Position) expectedObject;
                    Position actaulPosition = (Position) actualObject;
                    Assert.assertEquals(STC.format(expectedPosition), STC.format(actaulPosition));
                }
                else if (expectedObject instanceof Region && actualObject instanceof Region)
                {
                    Region expectedRegion = (Region) expectedObject;
                    Region actualRegion = (Region) actualObject;
                    Assert.assertEquals(STC.format(expectedRegion), STC.format(actualRegion));
                }
                 */ else {
                    Assert.assertEquals("Incorrect value at " + i, expectedObject, actualObject);
                }
            }
        }

        if (actualMax != null) {
            Assert.assertEquals("wrong number of iterations", actualMax.intValue(), iteratorCount);
        }
    }

    public void compareInfos(List<VOTableInfo> expected, List<VOTableInfo> actual) {
        // INFO
        Assert.assertNotNull(expected);
        Assert.assertNotNull(actual);
        for (VOTableInfo e : expected) {
            log.warn("expected: " + e);
        }
        for (VOTableInfo a : actual) {
            log.warn("actual: " + a);
        }
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            VOTableInfo expectedInfo = expected.get(i);
            VOTableInfo actualInfo = actual.get(i);
            Assert.assertNotNull(expectedInfo);
            Assert.assertNotNull(actualInfo);
            Assert.assertEquals(expectedInfo.getName(), actualInfo.getName());
            Assert.assertEquals(expectedInfo.getValue(), actualInfo.getValue());
            Assert.assertEquals(expectedInfo.id, actualInfo.id);
            Assert.assertEquals(expectedInfo.content, actualInfo.content);
        }
    }

    public void compareGroups(List<VOTableGroup> expected, List<VOTableGroup> actual) {
        Assert.assertEquals("number of groups", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            VOTableGroup eg = expected.get(i);
            VOTableGroup ag = actual.get(i);
            Assert.assertEquals(eg.getName(), ag.getName());
            compareParams(eg.getParams(), ag.getParams());
            compareGroups(eg.getGroups(), ag.getGroups());
        }
    }

    public void compareParams(List<VOTableParam> expected, List<VOTableParam> actual) {
        // PARAM
        Assert.assertNotNull(expected);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            VOTableParam expectedParam = expected.get(i);
            VOTableParam actualParam = actual.get(i);
            Assert.assertNotNull(expectedParam);
            Assert.assertNotNull(actualParam);
            Assert.assertEquals(expectedParam.getName(), actualParam.getName());
            Assert.assertEquals(expectedParam.getDatatype(), expectedParam.getDatatype());
            Assert.assertEquals(expectedParam.getValue(), expectedParam.getValue());
            Assert.assertEquals(expectedParam.id, actualParam.id);
            Assert.assertEquals(expectedParam.ucd, actualParam.ucd);
            Assert.assertEquals(expectedParam.unit, actualParam.unit);
            Assert.assertEquals(expectedParam.utype, actualParam.utype);
            Assert.assertEquals(expectedParam.xtype, actualParam.xtype);
            Assert.assertEquals(expectedParam.getArraysize(), actualParam.getArraysize());
            Assert.assertEquals(expectedParam.description, actualParam.description);
            List<String> expectedValues = expectedParam.getValues();
            List<String> actualValues = actualParam.getValues();
            if (expectedValues == null) {
                Assert.assertNull(actualValues);
                continue;
            }
            Assert.assertEquals(expectedValues.size(), actualValues.size());
            for (int j = 0; j < expectedValues.size(); j++) {
                Assert.assertEquals(expectedValues.get(j), actualValues.get(j));
            }
        }
    }

    public void compareFields(List<VOTableField> expected, List<VOTableField> actual) {
        // FIELD
        Assert.assertNotNull(expected);
        Assert.assertNotNull(actual);
        Assert.assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            VOTableField expectedField = expected.get(i);
            VOTableField actualField = actual.get(i);
            Assert.assertNotNull(expectedField);
            Assert.assertNotNull(actualField);
            Assert.assertEquals(expectedField.getName(), actualField.getName());
            Assert.assertEquals(expectedField.getDatatype(), expectedField.getDatatype());
            Assert.assertEquals(expectedField.id, actualField.id);
            Assert.assertEquals(expectedField.ucd, actualField.ucd);
            Assert.assertEquals(expectedField.unit, actualField.unit);
            Assert.assertEquals(expectedField.utype, actualField.utype);
            Assert.assertEquals(expectedField.xtype, actualField.xtype);
            Assert.assertEquals(expectedField.getArraysize(), actualField.getArraysize());
            Assert.assertEquals(expectedField.description, actualField.description);
        }
    }

    public static List<VOTableInfo> getTestInfos(String idPrefix) {
        List<VOTableInfo> infos = new ArrayList<VOTableInfo>();

        VOTableInfo info1 = new VOTableInfo("QUERY1", "select foo from ivoa.ObsCore");
        info1.id = idPrefix + "-id1";
        info1.content = "content 1";
        infos.add(info1);

        VOTableInfo info2 = new VOTableInfo("QUERY2", "select bar from ivoa.ObsCore");
        info2.content = "content 2";
        infos.add(info2);

        return infos;
    }

    public static List<VOTableParam> getMetaParams() {
        List<VOTableParam> params = new ArrayList<VOTableParam>();
        params.add(new VOTableParam("standardID", "char", "ivo://ivoa.net/std/DataLink#links-1.0"));
        params.add(new VOTableParam("resourceIdentifier", "char", "ivo://cadc.nrc.ca/caom2ops"));
        params.add(new VOTableParam("accessURL", "char", "http://www.cadc.hia.nrc.gc.ca/caom2ops/datalink"));
        return params;
    }

    public static VOTableGroup getMetaGroup() {
        VOTableGroup ret = new VOTableGroup("input");

        VOTableParam servParam = new VOTableParam("ID", "char", "");
        servParam.ref = "someID";
        ret.getParams().add(servParam);
        ret.getParams().add(new VOTableParam("MAXREC", "int", "666"));

        return ret;
    }

    public static List<VOTableParam> getTestParams() {
        List<VOTableParam> params = new ArrayList<VOTableParam>();

        VOTableParam intersects = new VOTableParam("INPUT:INTERSECTS", "char", "*", "OVERLAPS");
        intersects.id = null;
        intersects.ucd = null;
        intersects.unit = null;
        intersects.utype = null;
        intersects.xtype = null;
        intersects.description = null;
        intersects.getValues().add("ALL");
        intersects.getValues().add("BLAST");
        intersects.getValues().add("CFHT");
        intersects.getValues().add("HST");
        intersects.getValues().add("JCMT");
        params.add(intersects);

        VOTableParam collection = new VOTableParam("INPUT:COLLECTION", "char", "*", "ALL");
        collection.id = null;
        collection.ucd = null;
        collection.unit = null;
        collection.utype = null;
        collection.xtype = null;
        collection.description = null;
        params.add(collection);

        return params;
    }

    public static List<VOTableField> getTestFields() {
        List<VOTableField> fields = new ArrayList<VOTableField>();

        // Add VOTableFields.
        VOTableField booleanColumn = new VOTableField("boolean column", "boolean");
        booleanColumn.id = "booleanColumn.id";
        booleanColumn.ucd = "booleanColumn.ucd";
        booleanColumn.unit = "booleanColumn.unit";
        booleanColumn.utype = "booleanColumn.utype";
        booleanColumn.xtype = null;
        booleanColumn.description = "boolean column";
        fields.add(booleanColumn);

        VOTableField byteArrayColumn = new VOTableField("byte[] column", "unsignedByte", "*");
        byteArrayColumn.id = "byteArrayColumn.id";
        byteArrayColumn.ucd = "byteArrayColumn.ucd";
        byteArrayColumn.unit = "byteArrayColumn.unit";
        byteArrayColumn.utype = "byteArrayColumn.utype";
        byteArrayColumn.xtype = null;
        byteArrayColumn.description = "byte[] column";
        fields.add(byteArrayColumn);

        VOTableField byteColumn = new VOTableField("byte column", "unsignedByte");
        byteColumn.id = "byteColumn.id";
        byteColumn.ucd = "byteColumn.ucd";
        byteColumn.unit = "byteColumn.unit";
        byteColumn.utype = "byteColumn.utype";
        byteColumn.xtype = null;
        byteColumn.description = "byte column";
        fields.add(byteColumn);

        VOTableField doubleArrayColumn = new VOTableField("double[] column", "double", "*");
        doubleArrayColumn.id = "doubleArrayColumn.id";
        doubleArrayColumn.ucd = "doubleArrayColumn.ucd";
        doubleArrayColumn.unit = "doubleArrayColumn.unit";
        doubleArrayColumn.utype = "doubleArrayColumn.utype";
        doubleArrayColumn.xtype = null;
        doubleArrayColumn.description = "double[] column";
        fields.add(doubleArrayColumn);

        VOTableField doubleColumn = new VOTableField("double column", "double");
        doubleColumn.id = "doubleColumn.id";
        doubleColumn.ucd = "doubleColumn.ucd";
        doubleColumn.unit = "doubleColumn.unit";
        doubleColumn.utype = "doubleColumn.utype";
        doubleColumn.xtype = null;
        doubleColumn.description = "double column";
        fields.add(doubleColumn);

        VOTableField floatArrayColumn = new VOTableField("float[] column", "float", "*");
        floatArrayColumn.id = "floatArrayColumn.id";
        floatArrayColumn.ucd = "floatArrayColumn.ucd";
        floatArrayColumn.unit = "floatArrayColumn.unit";
        floatArrayColumn.utype = "floatArrayColumn.utype";
        floatArrayColumn.xtype = null;
        floatArrayColumn.description = "float[] column";
        fields.add(floatArrayColumn);

        VOTableField floatColumn = new VOTableField("float column", "float");
        floatColumn.id = "floatColumn.id";
        floatColumn.ucd = "floatColumn.ucd";
        floatColumn.unit = "floatColumn.unit";
        floatColumn.utype = "floatColumn.utype";
        floatColumn.xtype = null;
        floatColumn.description = "float column";
        fields.add(floatColumn);

        VOTableField intArrayColumn = new VOTableField("int[] column", "int", "*");
        intArrayColumn.id = "intArrayColumn.id";
        intArrayColumn.ucd = "intArrayColumn.ucd";
        intArrayColumn.unit = "intArrayColumn.unit";
        intArrayColumn.utype = "intArrayColumn.utype";
        intArrayColumn.xtype = null;
        intArrayColumn.description = "int[] column";
        fields.add(intArrayColumn);

        VOTableField integerColumn = new VOTableField("int column", "int");
        integerColumn.id = "integerColumn.id";
        integerColumn.ucd = "integerColumn.ucd";
        integerColumn.unit = "integerColumn.unit";
        integerColumn.utype = "integerColumn.utype";
        integerColumn.xtype = null;
        integerColumn.description = "float column";
        fields.add(integerColumn);

        VOTableField longArrayColumn = new VOTableField("long[] column", "long", "*");
        longArrayColumn.id = "longArrayColumn.id";
        longArrayColumn.ucd = "longArrayColumn.ucd";
        longArrayColumn.unit = "longArrayColumn.unit";
        longArrayColumn.utype = "longArrayColumn.utype";
        longArrayColumn.xtype = null;
        longArrayColumn.description = "long[] column";
        fields.add(longArrayColumn);

        VOTableField longColumn = new VOTableField("long column", "long");
        longColumn.id = "longColumn.id";
        longColumn.ucd = "longColumn.ucd";
        longColumn.unit = "longColumn.unit";
        longColumn.utype = "longColumn.utype";
        longColumn.xtype = null;
        longColumn.description = "long column";
        fields.add(longColumn);

        VOTableField shortArrayColumn = new VOTableField("short[] column", "short", "*");
        shortArrayColumn.id = "shortArrayColumn.id";
        shortArrayColumn.ucd = "shortArrayColumn.ucd";
        shortArrayColumn.unit = "shortArrayColumn.unit";
        shortArrayColumn.utype = "shortArrayColumn.utype";
        shortArrayColumn.xtype = null;
        shortArrayColumn.description = "short[] column";
        fields.add(shortArrayColumn);

        VOTableField shortColumn = new VOTableField("short column", "short");
        shortColumn.id = "shortColumn.id";
        shortColumn.ucd = "shortColumn.ucd";
        shortColumn.unit = "shortColumn.unit";
        shortColumn.utype = "shortColumn.utype";
        shortColumn.xtype = null;
        shortColumn.description = "short column";
        fields.add(shortColumn);

        VOTableField charColumn = new VOTableField("char column", "char", "*");
        charColumn.id = "charColumn.id";
        charColumn.ucd = "charColumn.ucd";
        charColumn.unit = "charColumn.unit";
        charColumn.utype = "charColumn.utype";
        charColumn.xtype = null;
        charColumn.description = "char column";
        fields.add(charColumn);

        VOTableField dateColumn = new VOTableField("date column", "char", "*");
        dateColumn.id = "dateColumn.id";
        dateColumn.ucd = "dateColumn.ucd";
        dateColumn.unit = "dateColumn.unit";
        dateColumn.utype = "dateColumn.utype";
        dateColumn.xtype = "timestamp";
        dateColumn.description = "date column";
        fields.add(dateColumn);

        VOTableField pointColumn = new VOTableField("point column", "double", "2");
        pointColumn.id = "pointColumn.id";
        pointColumn.ucd = "pointColumn.ucd";
        pointColumn.unit = "pointColumn.unit";
        pointColumn.utype = "pointColumn.utype";
        pointColumn.xtype = "point";
        pointColumn.description = "point column";
        fields.add(pointColumn);

        VOTableField circleColumn = new VOTableField("circle column", "double", "3");
        circleColumn.id = "circleColumn.id";
        circleColumn.ucd = "circleColumn.ucd";
        circleColumn.unit = "circleColumn.unit";
        circleColumn.utype = "circleColumn.utype";
        circleColumn.xtype = "circle";
        circleColumn.description = "circle column";
        fields.add(circleColumn);

        VOTableField polyColumn = new VOTableField("polygon column", "double", "*");
        polyColumn.id = "polyColumn.id";
        polyColumn.ucd = "polyColumn.ucd";
        polyColumn.unit = "polyColumn.unit";
        polyColumn.utype = "polyColumn.utype";
        polyColumn.xtype = "polygon";
        polyColumn.description = "poly column";
        fields.add(polyColumn);

        //VOTableField regionColumn = new VOTableField("region column", "char", "*");
        //regionColumn.id = "regionColumn.id";
        //regionColumn.ucd = "regionColumn.ucd";
        //regionColumn.unit = "regionColumn.unit";
        //regionColumn.utype = "regionColumn.utype";
        //regionColumn.xtype = "adql:REGION";
        //regionColumn.description = "region column";
        //fields.add(regionColumn);
        VOTableField idColumn = new VOTableField("id column", "char", "*");
        idColumn.id = "someID";
        idColumn.ucd = "idColumn.ucd";
        idColumn.unit = "idColumn.unit";
        idColumn.utype = "idColumn.utype";
        idColumn.description = "id column";
        fields.add(idColumn);

        return fields;
    }

    public static class TestTableData implements TableData {

        List<List<Object>> rowData;

        public TestTableData() throws Exception {
            this(2);
        }

        public TestTableData(long numrows) throws Exception {
            rowData = new ArrayList<List<Object>>();

            List<Object> row1 = new ArrayList<Object>();
            row1.add(Boolean.TRUE);
            row1.add(new byte[]{1, 2});
            row1.add(new Byte("1"));
            row1.add(new double[]{3.3, 4.4});
            row1.add(new Double("5.5"));
            row1.add(new float[]{6.6f, 7.7f});
            row1.add(new Float("8.8"));
            row1.add(new int[]{9, 10});
            row1.add(new Integer("11"));
            row1.add(new long[]{12l, 13l});
            row1.add(new Long("14"));
            row1.add(new short[]{15, 16});
            row1.add(new Short("17"));
            row1.add("string value");
            row1.add(dateFormat.parse(DATE_TIME));

            // DALI geom
            row1.add(new Point(1.0, 2.0));
            row1.add(new Circle(new Point(1.0, 2.0), 0.2));
            Polygon poly = new Polygon();
            poly.getVertices().add(new Point(2.0, 2.0));
            poly.getVertices().add(new Point(1.0, 4.0));
            poly.getVertices().add(new Point(3.0, 5.0));
            poly.getVertices().add(new Point(4.0, 3.0));
            row1.add(poly);

            // STC 
            //row1.add(new Position(Frame.ICRS, ReferencePosition.BARYCENTER, Flavor.SPHERICAL2, 1.0, 2.0));
            //row1.add(new Circle(Frame.ICRS, ReferencePosition.GEOCENTER, Flavor.SPHERICAL2, 1.0, 2.0, 3.0));
            row1.add("foo:bar/baz");
            rowData.add(row1);

            List<Object> row2 = new ArrayList<Object>();
            row2.add(Boolean.FALSE);
            for (int i = 1; i < row1.size(); i++) {
                row2.add(null);
            }
            rowData.add(row2);

            for (int i = 2; i < numrows; i++) {
                rowData.add(row1);
            }
            log.info("TestData: " + rowData.size());
        }

        public ResourceIterator<List<Object>> iterator() {
            return new ResourceIterator<>() {
                private final Iterator<List<Object>> it = rowData.iterator();

                public boolean hasNext() {
                    return it.hasNext();
                }

                public List<Object> next() {
                    return it.next();
                }

                public void remove() {
                    it.remove();
                }

                public void close() throws IOException {
                // Nothing to close.
                }
            };
        }

        @Override
        public void close() throws IOException {
            // nothing to close
        }
    }

}
