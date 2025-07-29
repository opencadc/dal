package ca.nrc.cadc.dali.tables.parquet.writerhelper;

import ca.nrc.cadc.dali.tables.votable.VOTableField;

import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.OutputFile;
import org.apache.parquet.schema.MessageType;

public class DynamicParquetWriterBuilder extends ParquetWriter.Builder<List<Object>, DynamicParquetWriterBuilder> {

    List<VOTableField> voTableFields;
    private final MessageType schema;
    private final Map<String, String> extraMetaData;

    public DynamicParquetWriterBuilder(OutputFile outputFile, MessageType schema, List<VOTableField> fields, Map<String, String> extraMetaData) {
        super(outputFile);
        this.schema = schema;
        this.voTableFields = fields;
        this.extraMetaData = extraMetaData;
    }

    @Override
    protected DynamicParquetWriterBuilder self() {
        return this;
    }

    @Override
    protected WriteSupport<List<Object>> getWriteSupport(Configuration conf) {
        return new CustomWriteSupport(schema, voTableFields, extraMetaData);
    }
}
