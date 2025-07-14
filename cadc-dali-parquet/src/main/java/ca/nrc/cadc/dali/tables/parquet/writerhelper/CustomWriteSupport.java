package ca.nrc.cadc.dali.tables.parquet.writerhelper;

import ca.nrc.cadc.dali.tables.votable.VOTableField;

import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;

public class CustomWriteSupport extends WriteSupport<List<Object>> {

    private static final Logger log = Logger.getLogger(CustomWriteSupport.class);

    private final List<VOTableField> voTableFields;
    private RecordConsumer recordConsumer;
    private final MessageType schema;
    private final Map<String, String> extraMetaData;

    CustomWriteSupport(MessageType schema, List<VOTableField> voTableFields, Map<String, String> extraMetaData) {
        super();
        this.voTableFields = voTableFields;
        this.schema = schema;
        this.extraMetaData = extraMetaData;
    }

    @Override
    public WriteContext init(Configuration configuration) {
        return new WriteContext(schema, extraMetaData);
    }

    @Override
    public void prepareForWrite(RecordConsumer recordConsumer) {
        this.recordConsumer = recordConsumer;
    }

    @Override
    public void write(List<Object> dataList) {
        recordConsumer.startMessage();

        VOTableField voTableField;
        for (int i = 0; i < dataList.size(); i++) {
            voTableField = voTableFields.get(i);
            Object data = dataList.get(i);
            recordConsumer.startField(voTableField.getName(), i);
            if (data != null) {
                if (data.getClass().isArray()) {
                    fillUpArrayData(recordConsumer, data);
                } else {
                    fillUpPrimitiveData(recordConsumer, data);
                }
            }
            recordConsumer.endField(voTableField.getName(), i);
        }
        recordConsumer.endMessage();
    }

    private void fillUpPrimitiveData(RecordConsumer recordConsumer, Object data) {
        if (data instanceof String) {
            recordConsumer.addBinary(Binary.fromString((String) data));
        } else if (data instanceof Integer) {
            recordConsumer.addInteger((Integer) data);
        } else if (data instanceof Long) {
            recordConsumer.addLong((Long) data);
        } else if (data instanceof Double) {
            recordConsumer.addDouble((Double) data);
        } else if (data instanceof Float) {
            recordConsumer.addFloat((Float) data);
        } else if (data instanceof Boolean) {
            recordConsumer.addBoolean((Boolean) data);
        } else {
            log.debug("Unsupported data type: " + data.getClass().getName());
            throw new UnsupportedOperationException("Unsupported data type: " + data.getClass().getName());
        }
    }

    private void fillUpArrayData(RecordConsumer recordConsumer, Object data) {
        recordConsumer.startGroup();
        recordConsumer.startField("element", 0);
        if (data instanceof int[]) {
            int[] array = (int[]) data;
            for (int value : array) {
                recordConsumer.addInteger(value);
            }
        } else if (data instanceof long[]) {
            long[] array = (long[]) data;
            for (long value : array) {
                recordConsumer.addLong(value);
            }
        } else if (data instanceof double[]) {
            double[] array = (double[]) data;
            for (double value : array) {
                recordConsumer.addDouble(value);
            }
        } else if (data instanceof float[]) {
            float[] array = (float[]) data;
            for (float value : array) {
                recordConsumer.addFloat(value);
            }
        } else if (data instanceof Long[]) {
            Long[] array = (Long[]) data;
            for (Long value : array) {
                recordConsumer.addLong(value);
            }
        } else if (data instanceof Double[]) {
            Double[] array = (Double[]) data;
            for (Double value : array) {
                recordConsumer.addDouble(value);
            }
        } else if (data instanceof short[]) {
            short[] array = (short[]) data;
            for (short value : array) {
                recordConsumer.addInteger(value); // Parquet does not have a short type, so we use int
            }
        } else {
            log.debug("Unsupported array data type: " + data.getClass().getName());
            throw new UnsupportedOperationException("Unsupported array data type: " + data.getClass().getName());
        }

        recordConsumer.endField("element", 0);
        recordConsumer.endGroup();
    }
}
