package ca.nrc.cadc.dali.tables.parquet;

import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ParquetReaderWriterTest extends TestUtil {

    private static final Logger log = Logger.getLogger(ParquetReaderWriterTest.class);

    @Test
    public void testReadWriteParquet() throws Exception {
        log.debug("testReadWriteParquet");
        VOTableDocument originalVOTableDoc = prepareVOTable();

        ParquetWriter writer = new ParquetWriter();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writer.write(prepareVOTable(), out);

        ParquetReader reader = new ParquetReader();
        InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
        VOTableDocument actualVOTableDoc = reader.read(inputStream);

        compareVOTable(originalVOTableDoc, actualVOTableDoc, null);
    }

}
