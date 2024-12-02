package ca.nrc.cadc.dali.tables.parquet;

import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;

import java.io.File;

//TODO: Temporary implementation
public class ParquetReader {

    public String read(String filePath) {
        Configuration configuration = new Configuration();

        try (org.apache.parquet.hadoop.ParquetReader<GenericRecord> reader = AvroParquetReader.<GenericRecord>builder(
                HadoopInputFile.fromPath(new org.apache.hadoop.fs.Path(new File(filePath).getAbsolutePath()), configuration)
        ).build()) {

            GenericRecord record;
            while ((record = reader.read()) != null) {
                System.out.println(record);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Worked";
    }

}
