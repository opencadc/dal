package ca.nrc.cadc.dali.tables.parquet.readerhelper;

import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;

public class DynamicRowConverter extends GroupConverter {

    private final Converter[] converters;
    private final DynamicRow currentRow = new DynamicRow();

    public DynamicRowConverter(MessageType schema) {
        int count = schema.getFieldCount();
        this.converters = new Converter[count];

        for (int i = 0; i < count; i++) {
            Type field = schema.getType(i);
            String name = field.getName();

            if (field.isPrimitive()) {
                converters[i] = new DynamicPrimitiveConverter(currentRow, name, field.asPrimitiveType().getPrimitiveTypeName());
            } else if (field.getLogicalTypeAnnotation() != null
                    && field.getLogicalTypeAnnotation() instanceof LogicalTypeAnnotation.ListLogicalTypeAnnotation) {
                converters[i] = new DynamicListConverter(currentRow, name, field.asGroupType());
            } else {
                throw new UnsupportedOperationException("Unsupported type: " + field);
            }
        }
    }

    @Override
    public Converter getConverter(int i) {
        return converters[i];
    }

    @Override
    public void start() {
        currentRow.getFieldNames().forEach(f -> currentRow.put(f, null)); // reset fields
    }

    @Override
    public void end() {
    }

    public DynamicRow getCurrentRow() {
        return currentRow;
    }
}
