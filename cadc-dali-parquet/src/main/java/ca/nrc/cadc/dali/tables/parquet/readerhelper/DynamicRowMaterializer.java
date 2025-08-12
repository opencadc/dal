package ca.nrc.cadc.dali.tables.parquet.readerhelper;

import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.RecordMaterializer;
import org.apache.parquet.schema.MessageType;

public class DynamicRowMaterializer extends RecordMaterializer<DynamicRow> {
    private final DynamicRowConverter root;

    public DynamicRowMaterializer(MessageType schema) {
        this.root = new DynamicRowConverter(schema);
    }

    @Override
    public DynamicRow getCurrentRecord() {
        return root.getCurrentRow();
    }

    @Override
    public GroupConverter getRootConverter() {
        return root;
    }
}
