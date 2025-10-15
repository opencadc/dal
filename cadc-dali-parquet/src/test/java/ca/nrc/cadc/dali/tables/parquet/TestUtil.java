package ca.nrc.cadc.dali.tables.parquet;

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.dali.tables.TableData;
import ca.nrc.cadc.dali.tables.votable.*;
import ca.nrc.cadc.date.DateUtil;
import ca.nrc.cadc.io.ResourceIterator;
import ca.nrc.cadc.util.Log4jInit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;

import java.io.IOException;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class TestUtil {

    private static final Logger log = Logger.getLogger(TestUtil.class);

    private static final String DATE_TIME = "2009-01-02T11:04:05.678";
    private static final DateFormat dateFormat;
    private static final UUID uuid = UUID.randomUUID();

    static {
        Log4jInit.setLevel("ca.nrc.cadc.dali.tables", Level.INFO);
        dateFormat = DateUtil.getDateFormat(DateUtil.IVOA_DATE_FORMAT, DateUtil.UTC);
    }

    public VOTableDocument prepareVOTable() throws Exception {
        VOTableDocument voTableDocument = new VOTableDocument();
        try {
            String resourceName = "VOTable resource name";

            // Add INFO's to the VOTableDocument.
            voTableDocument.getInfos().addAll(getTestInfos("a"));

            VOTableResource vr = new VOTableResource("meta");
            vr.description = "what is a meta?";
            voTableDocument.getResources().add(vr);

            vr.getParams().addAll(getMetaParams());
            vr.getGroups().add(getMetaGroup());

            // Add INFO's to meta VOTableResource.
            vr.getInfos().addAll(getTestInfos("b"));

            vr = new VOTableResource("results");
            voTableDocument.getResources().add(vr);
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
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
        return voTableDocument;
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

        VOTableField uuidColumn = new VOTableField("uuid column", "char", "36");
        uuidColumn.id = "uuidColumn.ID";
        uuidColumn.ucd = "uuidColumn.ucd";
        uuidColumn.unit = "uuidColumn.unit";
        uuidColumn.utype = "uuidColumn.utype";
        uuidColumn.xtype = "uuid";
        uuidColumn.description = "UUID Column";
        fields.add(uuidColumn);

        VOTableField uriColumn = new VOTableField("uri column", "char", "*");
        uriColumn.id = "uriColumn.ID";
        uriColumn.ucd = "uriColumn.ucd";
        uriColumn.unit = "uriColumn.unit";
        uriColumn.utype = "uriColumn.utype";
        uriColumn.xtype = "uri";
        uriColumn.description = "URI Column";
        fields.add(uriColumn);

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
            row1.add(Byte.valueOf("1"));
            row1.add(new double[]{3.3, 4.4});
            row1.add(5.5D);
            row1.add(new float[]{6.6f, 7.7f});
            row1.add(8.8F);
            row1.add(new int[]{9, 10});
            row1.add(11);
            row1.add(new long[]{12L, 13L});
            row1.add(14L);
            row1.add(new short[]{15, 16});
            row1.add(Short.valueOf("17"));
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

            row1.add("foo:bar/baz");
            row1.add(uuid);
            row1.add(new URI("http://www.example.com/"));
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
                }
            };
        }

        @Override
        public void close() throws IOException {
            // nothing to close
        }
    }

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
        while (expectedIter.hasNext()) {
            iteratorCount++;
            log.debug("iteratorCount: " + (iteratorCount));

            List<Object> expectedList = expectedIter.next();
            log.debug("expected row: " + expectedList);

            List<Object> actualList = actualIter.next();
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
                } else if (expectedObject instanceof short[] && actualObject instanceof int[]) {
                    Assert.assertArrayEquals(convertShortArrayToIntArray((short[]) expectedObject), (int[]) actualObject);
                } else if (expectedObject instanceof Short && actualObject instanceof Integer) {
                    Assert.assertEquals(((Number) expectedObject).intValue(), actualObject);
                } else {
                    Assert.assertEquals("Incorrect value at " + i, expectedObject, actualObject);
                }
            }
        }

        if (actualMax != null) {
            Assert.assertEquals("wrong number of iterations", actualMax.intValue(), iteratorCount);
        }
    }

    private int[] convertShortArrayToIntArray(short[] arr) {
        int[] result = new int[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[i];
        }
        return result;
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
            Assert.assertEquals(expectedParam.getDatatype(), actualParam.getDatatype());
            Assert.assertEquals(expectedParam.getValue(), actualParam.getValue());
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
            System.out.println("expectedField: " + expectedField);
            Assert.assertEquals(expectedField.getName(), actualField.getName());
            if (expectedField.getDatatype().equalsIgnoreCase("short")) {
                Assert.assertEquals("int", actualField.getDatatype());
            } else {
                Assert.assertEquals(expectedField.getDatatype(), actualField.getDatatype());
            }
            Assert.assertEquals(expectedField.id, actualField.id);
            Assert.assertEquals(expectedField.ucd, actualField.ucd);
            Assert.assertEquals(expectedField.unit, actualField.unit);
            Assert.assertEquals(expectedField.utype, actualField.utype);
            if (expectedField.xtype != null && !expectedField.xtype.equalsIgnoreCase("timestamp")) {
                Assert.assertEquals(expectedField.xtype, actualField.xtype);
                Assert.assertEquals(expectedField.getArraysize(), actualField.getArraysize());
            }
            Assert.assertEquals(expectedField.description, actualField.description);
        }
    }
}
