package ca.nrc.cadc.dali.tables.parquet;

import ca.nrc.cadc.dali.tables.votable.VOTableField;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.avro.LogicalTypes;
import org.apache.avro.Schema;
import org.apache.log4j.Logger;

public class DynamicSchemaGenerator {

    private static final Logger log = Logger.getLogger(DynamicSchemaGenerator.class);

    public static Schema generateSchema(List<VOTableField> voFields) {
        // List to hold Avro fields
        List<Schema.Field> fields = new ArrayList<>();
        try {
            int columnCount = voFields.size();
            log.debug("VOTable Column count = " + columnCount);
            for (VOTableField voField : voFields) {
                String columnName = voField.getName();
                Schema.Field field = new Schema.Field(columnName.replaceAll("\"", "_"), getAvroFieldType(voField), null, null);
                fields.add(field);
            }
            log.debug("Avro Schema.Field count = " + fields.size());
        } catch (Exception e) {
            log.debug("Failure while creating Avro Schema from VOTable", e);
            throw new RuntimeException("Failure while creating Avro Schema from VOTable : " + e.getMessage(), e);
        }

        // Define the Avro record schema with the fields
        Schema schema = Schema.createRecord("Record", null, null, Boolean.FALSE);
        schema.setFields(fields);
        log.debug("Schema Generated Successfully : " + schema);
        return schema;
    }

    private static Schema getAvroFieldType(VOTableField voTableField) {
        String datatype = voTableField.getDatatype();
        String arraysize = voTableField.getArraysize();
        String xtype = voTableField.xtype;

        Schema fieldType;
        switch (datatype) {
            case "short":
            case "int":
                fieldType = createSchema(Schema.Type.INT, arraysize);
                break;
            case "long":
                fieldType = createSchema(Schema.Type.LONG, arraysize);
                break;
            case "float":
                fieldType = createSchema(Schema.Type.FLOAT, arraysize);
                break;
            case "double":
                fieldType = createSchemaWithXType(Schema.Type.DOUBLE, arraysize, xtype);
                break;
            case "char":
                fieldType = "timestamp".equals(xtype)
                        ? LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG)) :
                        Schema.create(Schema.Type.STRING);
                break;
            case "boolean":
                fieldType = Schema.create(Schema.Type.BOOLEAN);
                break;
            case "date":
            case "timestamp":
                fieldType = LogicalTypes.timestampMillis().addToSchema(Schema.create(Schema.Type.LONG));
                break;
            case "byte":
                fieldType = createSchema(Schema.Type.BYTES, arraysize);
                break;
            default:
                fieldType = Schema.create(Schema.Type.STRING);
        }

        return Schema.createUnion(Arrays.asList(Schema.create(Schema.Type.NULL), fieldType));
    }

    private static Schema createSchema(Schema.Type type, String arraysize) {
        return arraysize == null ? Schema.create(type) : Schema.createArray(Schema.create(type));
    }

    private static Schema createSchemaWithXType(Schema.Type type, String arraysize, String xtype) {
        Schema schema = createSchema(type, arraysize);
        if (xtype != null) {
            schema.addProp("xtype", xtype);
        }
        return schema;
    }
}

