package ca.nrc.cadc.dali.tables.parquet;

import org.apache.avro.Schema;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DynamicSchemaGenerator {

    public static Schema generateSchema(ResultSet rs) throws Exception {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();

        // List to hold Avro fields
        List<Schema.Field> fields = new ArrayList<>();

        for (int i = 1; i <= columnCount; i++) {
            String columnName = metaData.getColumnName(i);
            Schema.Field field = new Schema.Field(columnName, getAvroFieldType(metaData.getColumnType(i)), null, null);
            fields.add(field);
        }

        // Define the Avro record schema with the fields
        Schema schema = Schema.createRecord("DynamicRecord", null, null, false);
        schema.setFields(fields);

        return schema;
    }

    private static Schema getAvroFieldType(int sqlType) {
        // Map SQL data types to Avro data types
        Schema fieldType;
        switch (sqlType) {
            case Types.INTEGER:
                fieldType = Schema.create(Schema.Type.INT);
                break;
            case Types.BIGINT:
                fieldType = Schema.create(Schema.Type.LONG);
                break;
            case Types.FLOAT:
                fieldType = Schema.create(Schema.Type.FLOAT);
                break;
            case Types.DOUBLE:
                fieldType = Schema.create(Schema.Type.DOUBLE);
                break;
            case Types.VARCHAR:
            case Types.CHAR:
                fieldType = Schema.create(Schema.Type.STRING);
                break;
            case Types.BOOLEAN:
                fieldType = Schema.create(Schema.Type.BOOLEAN);
                break;
            case Types.DATE:
            case Types.TIMESTAMP:
                fieldType = Schema.create(Schema.Type.STRING); // Or Schema.Type.LONG if you want timestamps in millis
                break;
            default:
                fieldType = Schema.create(Schema.Type.STRING); // Default to STRING for unknown types
        }
        return Schema.createUnion(Arrays.asList(Schema.create(Schema.Type.NULL), fieldType));

    }
}

