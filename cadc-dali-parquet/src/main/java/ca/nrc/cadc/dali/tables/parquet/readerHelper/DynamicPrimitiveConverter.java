package ca.nrc.cadc.dali.tables.parquet.readerHelper;

import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.PrimitiveConverter;
import org.apache.parquet.schema.PrimitiveType;

public class DynamicPrimitiveConverter extends PrimitiveConverter {
    private final DynamicRow row;
    private final String field;

    public DynamicPrimitiveConverter(DynamicRow row, String field, PrimitiveType.PrimitiveTypeName type) {
        this.row = row;
        this.field = field;
    }

    @Override
    public void addInt(int v) {
        row.put(field, v);
    }

    @Override
    public void addLong(long v) {
        row.put(field, v);
    }

    @Override
    public void addFloat(float v) {
        row.put(field, v);
    }

    @Override
    public void addDouble(double v) {
        row.put(field, v);
    }

    @Override
    public void addBinary(Binary v) {
        row.put(field, v.toStringUsingUTF8());
    }
}
