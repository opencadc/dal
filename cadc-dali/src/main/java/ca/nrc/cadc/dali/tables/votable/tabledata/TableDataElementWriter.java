package ca.nrc.cadc.dali.tables.votable.tabledata;

import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.util.Format;
import ca.nrc.cadc.dali.util.FormatFactory;
import ca.nrc.cadc.xml.MaxIterations;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.jdom2.Element;

public class TableDataElementWriter {

    private final Iterator<List<Object>> rowIter;
    private final List<VOTableField> fields;
    private final FormatFactory formatFactory;
    private final MaxIterations maxIterations;
    private final Element trailer;

    public TableDataElementWriter(Iterator<List<Object>> rowIter, List<VOTableField> fields, MaxIterations maxIterations,
                                  Element trailer, FormatFactory formatFactory) {
        this.rowIter = rowIter;
        this.fields = fields;
        this.maxIterations = maxIterations;
        this.trailer = trailer;
        this.formatFactory = formatFactory;
    }

    public void write(Writer out) throws IOException {
        out.write("<TABLEDATA>");

        long rowCount = 1;

        while (rowIter.hasNext()) {
            List<Object> row = rowIter.next();
            writeRow(out, row);

            // check for max iterations
            if (maxIterations != null && rowCount == maxIterations.getMaxIterations()) {
                maxIterations.maxIterationsReached(rowIter.hasNext());
                break;
            }
            rowCount++;
        }
        out.write("</TABLEDATA>");
    }

    private void writeRow(Writer out, List<Object> row) throws IOException {
        out.write("<TR>");

        for (int i = 0; i < row.size(); i++) {
            Object value = row.get(i);
            Format fmt = formatFactory.getFormat(fields.get(i));

            if (value == null) {
                out.write("<TD/>");
            } else {
                out.write("<TD>");
                try {
                    out.write(escapeXml(fmt.format(value)));
                } catch (Exception ex) {
                    // DALI error
                    trailer.setAttribute("name", "QUERY_STATUS");
                    trailer.setAttribute("value", "ERROR");
                    trailer.setText(ex.toString());
                }
                out.write("</TD>");
            }
        }

        out.write("</TR>");
    }

    // Utility method to escape XML special characters
    private static String escapeXml(String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
