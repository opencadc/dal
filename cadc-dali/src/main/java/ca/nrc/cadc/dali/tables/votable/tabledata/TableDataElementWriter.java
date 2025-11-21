/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2025.                            (c) 2025.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                       <http://www.gnu.org/licenses/>.
 *
 *  $Revision: 5 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.dali.tables.votable.tabledata;

import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.util.Format;
import ca.nrc.cadc.dali.util.FormatFactory;
import ca.nrc.cadc.xml.MaxIterations;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jdom2.Element;

public class TableDataElementWriter {

    private static final Logger log = Logger.getLogger(TableDataElementWriter.class);

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
        log.debug("Writing TABLEDATA element - starting");
        out.write("<TABLEDATA>");

        long rowCount = 0;
        boolean success;

        while (rowIter.hasNext()) {
            rowCount ++;
            List<Object> row = rowIter.next();
            success = writeRow(out, row);

            if (!success) {
                break; // Stop processing on failure
            }

            // check for max iterations
            if (maxIterations != null && rowCount == maxIterations.getMaxIterations()) {
                maxIterations.maxIterationsReached(rowIter.hasNext());
                break;
            }
        }
        out.write("</TABLEDATA>");
        log.debug("Finished writing TABLEDATA element. Wrote " + rowCount + " rows");
    }

    private boolean writeRow(Writer out, List<Object> row) throws IOException {
        out.write("\n<TR>");

        for (int i = 0; i < row.size(); i++) {
            Object value = row.get(i);
            VOTableField fd = fields.get(i);

            Format fmt;
            try {
                fmt = formatFactory.getFormat(fd);
            } catch (Exception e) {
                // DALI error
                log.warn("ERROR getting formatter for field: " + fd, e);
                trailer.setAttribute("name", "QUERY_STATUS");
                trailer.setAttribute("value", "ERROR");
                trailer.setText(e.toString());
                out.write("<TR/>");
                return false;
            }

            if (value == null) {
                out.write("<TD/>");
            } else {
                out.write("<TD>");
                try {
                    out.write(escapeXml(fmt.format(value)));
                } catch (Exception ex) {
                    // DALI error
                    log.warn("ERROR serializing row data: " + fd, ex);
                    trailer.setAttribute("name", "QUERY_STATUS");
                    trailer.setAttribute("value", "ERROR");
                    trailer.setText(ex.toString());
                    out.write("</TD></TR>");
                    return false;
                }
                out.write("</TD>");
            }
        }

        out.write("</TR>");
        return true;
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
