/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2020.                            (c) 2020.
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

import ca.nrc.cadc.dali.MultiPolygon;
import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.Polygon;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 *
 * @author pdowler
 */
public class MultiPolygonFormat implements Format<MultiPolygon> {
    private static final Logger log = Logger.getLogger(MultiPolygonFormat.class);

    private final PolygonFormat pf = new PolygonFormat();
    private final DoubleArrayFormat fmt = new DoubleArrayFormat();
    
    private static final String MP_SEPARATOR = " NaN NaN ";

    public MultiPolygonFormat() { 
    }

    @Override
    public MultiPolygon parse(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s.isEmpty()) {
            return null;
        }
        
        double[] dd = fmt.parse(s);
        return parseDoubleNaN(dd, s);
    }
    
    public MultiPolygon build(double[] dd) {
        return parseDoubleNaN(dd, null);
    }
    
    MultiPolygon parseSingleNaN(String s) {
        String[] comps = s.toLowerCase().split("nan");
        log.warn("MultiPolygonFormat.parse: " + comps.length);
        MultiPolygon ret = new MultiPolygon();
        for (String c : comps) {
            c = c.trim();
            if (c.isEmpty()) {
                throw new IllegalArgumentException("invalid polygon (interpretting 'NaN NaN' as a NaN coordinate value): " + s);
            }
            log.warn("MultiPolygonFormat.parse: '" + c + "'");
            Polygon p = pf.parse(c);
            ret.getPolygons().add(p);
        }
        
        return ret;
    }

    // string rep is for error messages
    MultiPolygon parseDoubleNaN(double[] dd, String s) {
        MultiPolygon ret = new MultiPolygon();
        Polygon poly = new Polygon();
        try {
            for (int i = 0; i < dd.length; i += 2) {
                if (Double.isNaN(dd[i])) {
                    if (Double.isNaN(dd[i + 1])) {
                        if (poly.getVertices().size() < 3) {
                            throw new IllegalArgumentException("invalid multipolygon (not enough points before NaN NaN separator): " + s);
                        }
                        ret.getPolygons().add(poly);
                        poly = new Polygon();
                    } else {
                        throw new IllegalArgumentException("invalid polygon (NaN coordinate value): " + s);
                    }
                } else {
                    if (Double.isNaN(dd[i + 1])) {
                        throw new IllegalArgumentException("invalid polygon (NaN coordinate value): " + s);
                    }
                    Point v = new Point(dd[i], dd[i + 1]);
                    poly.getVertices().add(v);
                }
            }
            if (poly.getVertices().size() < 3) {
                throw new IllegalArgumentException("invalid multipolygon (not enough points in last polygon): " + s);
            }
            ret.getPolygons().add(poly);
        } catch (IndexOutOfBoundsException ex) {
            throw new IllegalArgumentException("invalid polygon (odd number of coordinate values): " + s);
        }
        
        return ret;
    }

    @Override
    public String format(MultiPolygon mp) {
        if (mp == null || mp.getPolygons().isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        Iterator<Polygon> i = mp.getPolygons().iterator();
        while (i.hasNext()) {
            sb.append(pf.format(i.next()));
            if (i.hasNext()) {
                sb.append(MP_SEPARATOR);
            }
        }
        return sb.toString();
    }
}
