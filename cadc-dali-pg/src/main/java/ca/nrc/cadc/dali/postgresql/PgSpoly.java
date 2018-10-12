/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2017.                            (c) 2017.
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
************************************************************************
*/

package ca.nrc.cadc.dali.postgresql;


import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.Polygon;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.postgresql.util.PGobject;

/**
 *
 * @author pdowler
 */
public class PgSpoly 
{
    private static final Logger log = Logger.getLogger(PgSpoly.class);

    public PgSpoly() { }
    
    /**
     * Generate a PGobject suitable for use in a PreparedStatement (insert or update
     * of an spoly column).
     * 
     * @param poly value to transform, may be null
     * @return PGobject or null
     * @throws SQLException if PGobject creation fails
     */
    public PGobject generatePolygon(Polygon poly)
    {
        if (poly == null)
            return null;
        
        StringBuilder sval = new StringBuilder();
        sval.append("{");
        for (Point p : poly.getVertices())
        {
            sval.append("(");
            sval.append(Math.toRadians(p.getLongitude()));
            sval.append(",");
            sval.append(Math.toRadians(p.getLatitude()));
            sval.append(")");
            sval.append(",");
        }
        sval.setCharAt(sval.length()-1, '}'); // replace last comma with closing }
        String spoly = sval.toString();

        try {
            PGobject pgo = new PGobject();
            pgo.setType("spoly");
            pgo.setValue(spoly);
            return pgo;
        } catch (SQLException ex) {
            throw new RuntimeException("BUG: failed to convert polygon to PGobject", ex);
        }
    }
    
    /**
     * Parse the string representation of an spoly value (from ResultSet.getString(...)).
     * A round-trip to the database spoly column does not preserve starting vertex 
     * or numeric values exactly. TODO: verify that round-trip preserves winding
     * direction.
     * 
     * @param s value to transform, may be null
     * @return Polygon or null
     */
    public Polygon getPolygon(String s)
    {
        // spoly string format: {(a,b),(c,d),(e,f) ... }
        if (s == null)
            return null;

        // Get the string inside the enclosing brackets.
        int open = s.indexOf("{");
        int close = s.indexOf("}");
        if (open == -1 || close == -1)
            throw new IllegalArgumentException("Missing opening or closing { } " + s);

        // Get the string inside the enclosing parentheses.
        s = s.substring(open + 1, close);
        open = s.indexOf("(");
        close = s.lastIndexOf(")");
        if (open == -1 || close == -1)
            throw new IllegalArgumentException("Missing opening or closing ( ) " + s);

        // Each set of vertices is '),(' separated.
        s = s.substring(open + 1, close);
        String[] vertices = s.split("\\){1}?\\s*,\\s*{1}\\({1}?");

        // Check minimum vertices to make a polygon.
        if (vertices.length < 3)
            throw new IllegalArgumentException("Minimum 3 vertices required to form a Polygon " + s);

        Polygon ret = new Polygon();

        // Loop through each set of vertices.
        for (int i = 0; i < vertices.length; i++)
        {
            // Each vertex is 2 values separated by a comma.
            String vertex = vertices[i];
            String[] values = vertex.split(",");
            if (values.length != 2)
                throw new IllegalArgumentException("Each set of vertices must have only 2 values " + vertex);

            double x = Double.parseDouble(values[0]);
            double y = Double.parseDouble(values[1]);

            x = Math.toDegrees(x);
            y = Math.toDegrees(y);
            ret.getVertices().add(new Point(x, y));
        }
        return ret;
    }
}
