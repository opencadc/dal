package ca.nrc.cadc.dali.tables.parquet.readerhelper;

import ca.nrc.cadc.dali.tables.TableData;
import ca.nrc.cadc.dali.tables.votable.VOTableField;

import java.util.Iterator;
import java.util.List;

import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.schema.MessageType;

public class ParquetTableData implements TableData {

    private final ParquetFileReader reader;
    private final MessageType schema;
    private final List<VOTableField> fields;

    public ParquetTableData(ParquetFileReader reader, MessageType schema, List<VOTableField> fields) {
        this.reader = reader;
        this.schema = schema;
        this.fields = fields;
    }

    @Override
    public Iterator<List<Object>> iterator() {
        return new ParquetRowIterator(reader, schema, fields);
    }
}
