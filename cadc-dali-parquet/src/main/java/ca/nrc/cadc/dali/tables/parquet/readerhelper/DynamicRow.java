package ca.nrc.cadc.dali.tables.parquet.readerhelper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DynamicRow {
    private final Map<String, Object> fields = new LinkedHashMap<>();

    public void put(String field, Object value) {
        fields.put(field, value);
    }

    public Object get(String field) {
        return fields.get(field);
    }

    public Set<String> getFieldNames() {
        return fields.keySet();
    }

    @Override
    public String toString() {
        return fields.toString();
    }
}
