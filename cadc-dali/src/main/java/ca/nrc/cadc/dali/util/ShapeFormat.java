/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2019.                            (c) 2019.
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

package ca.nrc.cadc.dali.util;

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.dali.Range;
import ca.nrc.cadc.dali.Shape;
import org.apache.log4j.Logger;

/**
 * Formatter to handle shapes in polymorphic serialisation (DALI-1.2).
 *
 * @author pdowler
 */
public class ShapeFormat implements Format<Shape> {

    private static final Logger log = Logger.getLogger(ShapeFormat.class);

    private boolean sia2 = false;

    public ShapeFormat() {
    }

    public ShapeFormat(boolean supportSIA2) {
        this.sia2 = supportSIA2;
    }

    @Override
    public Shape parse(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s.isEmpty()) {
            return null;
        }
        String[] parts = separateKey(s);
        if (Point.class.getSimpleName().equalsIgnoreCase(parts[0])) {
            PointFormat fmt = new PointFormat();
            return fmt.parse(parts[1]);
        } else if (Circle.class.getSimpleName().equalsIgnoreCase(parts[0])) {
            CircleFormat fmt = new CircleFormat();
            return fmt.parse(parts[1]);
        } else if (Range.class.getSimpleName().equalsIgnoreCase(parts[0])) {
            RangeFormat fmt = new RangeFormat(sia2);
            return fmt.parse(parts[1]);
        } else if (Polygon.class.getSimpleName().equalsIgnoreCase(parts[0])) {
            PolygonFormat fmt = new PolygonFormat();
            return fmt.parse(parts[1]);
        }

        throw new IllegalArgumentException("unexpected shape: " + parts[0]);
    }

    @Override
    public String format(Shape t) {
        if (t == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(t.getClass().getSimpleName().toLowerCase()).append(" ");
        if (t instanceof Point) {
            PointFormat fmt = new PointFormat();
            sb.append(fmt.format((Point) t));
        } else if (t instanceof Circle) {
            CircleFormat fmt = new CircleFormat();
            sb.append(fmt.format((Circle) t));
        } else if (t instanceof Range) {
            RangeFormat fmt = new RangeFormat();
            sb.append(fmt.format((Range) t));
        } else if (t instanceof Polygon) {
            PolygonFormat fmt = new PolygonFormat();
            sb.append(fmt.format((Polygon) t));
        } else {
            throw new IllegalArgumentException("unsupported shape: " + t.getClass().getName());
        }
        return sb.toString();
    }

    /**
     * Separate the key (first word) from the value(remaining words).
     *
     * @param s
     * @return
     */
    public static String[] separateKey(String s) {
        String[] ret = new String[2];
        int i = s.indexOf(" ");
        if (i > 0) {

            ret[0] = s.substring(0, i);
            if (i + 1 < s.length() - 1) {
                ret[1] = s.substring(i + 1, s.length());
            }
            return ret;
        } else {
            ret[0] = s; // one word
        }
        return ret;
    }
}
