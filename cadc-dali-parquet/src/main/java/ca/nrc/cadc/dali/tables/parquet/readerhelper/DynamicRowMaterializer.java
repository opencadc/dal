package ca.nrc.cadc.dali.tables.parquet.readerhelper;

import ca.nrc.cadc.dali.tables.votable.VOTableField;

import java.util.List;

import org.apache.parquet.io.api.GroupConverter;
import org.apache.parquet.io.api.RecordMaterializer;
import org.apache.parquet.schema.MessageType;

public class DynamicRowMaterializer extends RecordMaterializer<DynamicRow> {
    private final DynamicRowConverter root;

    public DynamicRowMaterializer(MessageType schema, List<VOTableField> fields) {
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
