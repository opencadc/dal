package ca.nrc.cadc.dali.tables.parquet.readerhelper;

import ca.nrc.cadc.dali.tables.votable.VOTableField;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.io.api.RecordMaterializer;

import org.apache.parquet.schema.LogicalTypeAnnotation;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;



public class ParquetRowIterator implements Iterator<List<Object>> {
    private final ParquetFileReader reader;
    private final MessageType schema;
    private final List<VOTableField> fields;
    private PageReadStore currentRowGroup;
    private RecordReader<DynamicRow> recordReader;
    private long rowsInGroup;
    private long rowIndex;

    public ParquetRowIterator(ParquetFileReader reader, MessageType schema, List<VOTableField> fields) {
        this.reader = reader;
        this.schema = schema;
        this.fields = fields;
        this.currentRowGroup = null;
        this.recordReader = null;
        this.rowsInGroup = 0;
        this.rowIndex = 0;
        advanceRowGroup();
    }

    private void advanceRowGroup() {
        try {
            currentRowGroup = reader.readNextRowGroup();
            if (currentRowGroup != null) {
                rowsInGroup = currentRowGroup.getRowCount();
                rowIndex = 0;
                recordReader = createRecordReader(currentRowGroup, schema, fields);
            } else {
                rowsInGroup = 0;
                recordReader = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read next row group", e);
        }
    }

    @Override
    public boolean hasNext() {
        if (recordReader == null) {
            return false;
        }
        if (rowIndex < rowsInGroup) {
            return true;
        }
        advanceRowGroup();
        return recordReader != null && rowIndex < rowsInGroup;
    }

    @Override
    public List<Object> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        rowIndex++;

        DynamicRow dynamicRow = recordReader.read();
        List<Object> row = new ArrayList<>();
        for (VOTableField field : fields) {
            String fieldName = field.getName();
            Type parquetField = schema.getType(fieldName);
            Object valueObj = dynamicRow.get(fieldName);
            String value;
            if (valueObj == null) {
                value = null;
            } else if (parquetField.isPrimitive()
                    && parquetField.asPrimitiveType().getLogicalTypeAnnotation() instanceof LogicalTypeAnnotation.TimestampLogicalTypeAnnotation) {
                if (valueObj instanceof Long) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                            .withZone(ZoneId.of("UTC"));
                    value = formatter.format(Instant.ofEpochMilli((long) valueObj));
                } else {
                    value = valueObj.toString();
                }

            } else {
                value = valueObj.toString().replaceAll("[\\[\\],]", "");
            }
            row.add(field.getFormat().parse(value));
        }

        return row;
    }

    private RecordReader<DynamicRow> createRecordReader(PageReadStore rowGroup, MessageType schema, List<VOTableField> fields) {
        MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);
        RecordMaterializer<DynamicRow> materializer = new DynamicRowMaterializer(schema, fields);
        return columnIO.getRecordReader(rowGroup, materializer);
    }
}
