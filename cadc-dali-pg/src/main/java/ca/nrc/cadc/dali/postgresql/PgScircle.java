/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2018.                            (c) 2018.
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

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Point;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.postgresql.util.PGobject;

/**
 *
 * @author pdowler
 */
public class PgScircle {

    private static final Logger log = Logger.getLogger(PgScircle.class);

    public PgScircle() {
    }

    /**
     * Generate a PGobject suitable for use in a PreparedStatement (insert or update
     * of an scircle column).
     *
     * @param c value to transform, may be null
     * @return PGobject or null
     */
    public PGobject generateCircle(Circle c) {
        if (c == null) {
            return null;
        }

        StringBuilder sval = new StringBuilder();
        sval.append("<");
        sval.append("(");
        sval.append(Math.toRadians(c.getCenter().getLongitude()));
        sval.append(",");
        sval.append(Math.toRadians(c.getCenter().getLatitude()));
        sval.append(")");
        sval.append(",");
        sval.append(Math.toRadians(c.getRadius()));
        sval.append(">");
        String spt = sval.toString();

        try {
            PGobject pgo = new PGobject();
            pgo.setType("scircle");
            pgo.setValue(spt);
            return pgo;
        } catch (SQLException ex) {
            throw new RuntimeException("BUG: failed to convert circle to PGobject", ex);
        }
    }

    /**
     * Parse the string representation of an scircle value (from ResultSet.getString(...)).
     *
     * @param s value to transform, may be null
     * @return Circle or null
     */
    public Circle getCircle(String s) {
        if (s == null) {
            return null;
        }

        int open = s.indexOf("<");
        int close = s.lastIndexOf(">");
        if (open == -1 || close == -1) {
            throw new IllegalArgumentException("Missing opening or closing < > " + s);
        }
        s = s.substring(open + 1, close);
        String[] values = s.split(",");

        double r = Double.parseDouble(values[2]);

        open = s.indexOf("(");
        close = s.lastIndexOf(")");
        int comma = s.lastIndexOf(',');
        if (open == -1 || close == -1) {
            throw new IllegalArgumentException("Missing opening or closing ( ) " + s);
        }

        s = s.substring(open + 1, close);
        values = s.split(",");
        if (values.length != 2) {
            throw new IllegalArgumentException("point must have only 2 values " + s);
        }

        double x = Double.parseDouble(values[0]);
        double y = Double.parseDouble(values[1]);

        x = Math.toDegrees(x);
        y = Math.toDegrees(y);
        r = Math.toDegrees(r);

        return new Circle(new Point(x, y), r);
    }
}
