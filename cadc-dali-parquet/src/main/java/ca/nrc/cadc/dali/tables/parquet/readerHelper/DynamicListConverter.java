package ca.nrc.cadc.dali.tables.parquet.readerHelper;

import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.Converter;
import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.PrimitiveConverter;
import org.apache.parquet.schema.GroupType;
import org.apache.parquet.schema.PrimitiveType;
import org.apache.parquet.schema.Type;

import java.util.ArrayList;
import java.util.List;

public class DynamicListConverter extends GroupConverter {
    private final List<Object> list = new ArrayList<>();
    private final DynamicRow row;
    private final String field;
    private final Converter elementConverter;

    public DynamicListConverter(DynamicRow row, String field, GroupType listGroup) {
        this.row = row;
        this.field = field;

        Type repeatedField = listGroup.getType(0); // can be group or primitive

        if (repeatedField.isRepetition(Type.Repetition.REPEATED)) {
            if (repeatedField.isPrimitive()) {
                // repeated primitive (double, int, etc.)
                PrimitiveType.PrimitiveTypeName type = repeatedField.asPrimitiveType().getPrimitiveTypeName();
                this.elementConverter = new ListElementPrimitiveConverter(list, type);
            } else {
                GroupType repeatedGroup = repeatedField.asGroupType();
                if (repeatedGroup.getFieldCount() == 1 && repeatedGroup.getType(0).isPrimitive()) {
                    // repeated group with primitive element (standard 3-level LIST)
                    PrimitiveType.PrimitiveTypeName type = repeatedGroup.getType(0).asPrimitiveType().getPrimitiveTypeName();
                    this.elementConverter = new ListElementGroupConverter(list, type);
                } else {
                    throw new UnsupportedOperationException("Unsupported group structure in array: " + repeatedGroup);
                }
            }
        } else {
            throw new UnsupportedOperationException("First field in LIST group is not repeated: " + repeatedField);
        }
    }

    @Override
    public Converter getConverter(int fieldIndex) {
        return elementConverter;
    }

    @Override
    public void start() {
        list.clear();
    }

    @Override
    public void end() {
        row.put(field, new ArrayList<>(list));
    }

    // For 3-level
    static class ListElementGroupConverter extends GroupConverter {
        private final PrimitiveConverter elementConverter;

        public ListElementGroupConverter(List<Object> list, PrimitiveType.PrimitiveTypeName type) {
            this.elementConverter = new ListElementPrimitiveConverter(list, type);
        }

        @Override
        public Converter getConverter(int i) {
            return elementConverter;
        }

        @Override
        public void start() {
        }

        @Override
        public void end() {
        }
    }

    // For 2-level or 3-level element primitive
    static class ListElementPrimitiveConverter extends PrimitiveConverter {
        private final List<Object> list;
        private final PrimitiveType.PrimitiveTypeName type;

        public ListElementPrimitiveConverter(List<Object> list, PrimitiveType.PrimitiveTypeName type) {
            this.list = list;
            this.type = type;
        }

        @Override
        public void addInt(int v) {
            list.add(v);
        }

        @Override
        public void addLong(long v) {
            list.add(v);
        }

        @Override
        public void addFloat(float v) {
            list.add(v);
        }

        @Override
        public void addDouble(double v) {
            list.add(v);
        }

        @Override
        public void addBinary(Binary v) {
            list.add(v.toStringUsingUTF8());
        }
    }
}
