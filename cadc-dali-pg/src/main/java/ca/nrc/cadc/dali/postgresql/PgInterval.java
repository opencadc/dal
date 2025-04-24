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

import ca.nrc.cadc.dali.DoubleInterval;
import ca.nrc.cadc.dali.Interval;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.log4j.Logger;
import org.postgresql.geometric.PGpoint;
import org.postgresql.geometric.PGpolygon;

/**
 * This class supports storing a DALI Interval in a PostgreSQL 2D polygon.
 * The polygon form supports contains and intersects queries where target values
 * are 2D points or line segments respectively.
 *
 * @author pdowler
 */
public class PgInterval {

    private static final Logger log = Logger.getLogger(PgInterval.class);

    /**
     * Constant y-value for query shapes. A query should be implemented as a
     * point or line segment at this value for the Y-axis.
     */
    public static final double YVALUE = 0.0;

    private static final double y1 = -2.0;  // bottom of comb
    private static final double ym = -1.0;  // bottom of teeth
    private static final double y2 = 1.0;   // top of teeth

    private final boolean logScale;

    public PgInterval() {
        this.logScale = false;
    }

    public static boolean isCompatResultObject(Object o) {
        return PGpolygon.class.equals(o.getClass());
    }

    /**
     * Convert the interval bounds to log scale (log10) when generating the PGpolygon.
     *
     * @param logScale
     */
    public PgInterval(boolean logScale) {
        this.logScale = logScale;
    }

    private double rescale(double d) {
        if (logScale) {
            return Math.log10(d);
        }
        return  d;
    }
    
    private double unscale(double d) {
        if (logScale) {
            return Math.pow(10.0, d);
        }
        return d;
    }

    /**
     * Generate a PGpolygon that straddles the x-axis and represents the interval.
     *
     * @param val value to transform, may be null
     * @return simple 4-point polygon or null
     */
    public PGpolygon generatePolygon2D(Interval<Double> val) {
        if (val == null) {
            return null;
        }

        List<PGpoint> verts = new ArrayList<>();

        double lb = rescale(val.getLower());
        double ub = rescale(val.getUpper());
        
        // draw a 2D polygon that looks like a tooth-up-comb with each tooth having x-range that
        // corresponds to one (sub) interval... it is a simple box for an Interval with no sub-samples
        // full-span line at y1
        verts.add(new PGpoint(lb, y1));
        verts.add(new PGpoint(ub, y1));

        // just the basic bounds interval
        verts.add(new PGpoint(ub, y2));
        verts.add(new PGpoint(lb, y2));

        return new PGpolygon(verts.toArray(new PGpoint[verts.size()]));
    }

    /**
     * Generate a PGpolygon that straddles the x-axis and represents the array of
     * disjoint intervals.
     *
     * @param vals value to transform, may be null
     * @return a n-point polygon shaped like a comb with teeth crossing PgInterval.YVALUE or null
     */
    public PGpolygon generatePolygon2D(Interval<Double>[] vals) {
        if (vals == null || vals.length == 0) {
            return null;
        }

        List<PGpoint> verts = new ArrayList<>();

        // draw a 2D polygon that looks like a tooth-up-comb with each tooth having x-range that
        // corresponds to one (sub) interval... it is a simple box for an Interval with no sub-samples
        LinkedList<Interval<Double>> samples = new LinkedList<>();
        for (Interval<Double> ii : vals) {
            samples.add(ii); // TODO: sort and verify the intervals are disjoint
        }
        // full-span line at y1
        double lb = rescale(samples.getFirst().getLower());
        double ub = rescale(samples.getLast().getUpper());
        
        verts.add(new PGpoint(lb, y1));
        verts.add(new PGpoint(ub, y1));

        Iterator<Interval<Double>> iter = samples.descendingIterator();
        Interval<Double> prev = null;
        while (iter.hasNext()) {
            Interval<Double> si = iter.next();
            if (prev != null) {
                verts.add(new PGpoint(rescale(prev.getLower()), ym));
                verts.add(new PGpoint(rescale(si.getUpper()), ym));
            }
            verts.add(new PGpoint(rescale(si.getUpper()), y2));
            verts.add(new PGpoint(rescale(si.getLower()), y2));
            prev = si;
        }

        return new PGpolygon(verts.toArray(new PGpoint[verts.size()]));
    }

    /**
     * Convert the internal database object from ResultSet.getObject().
     * 
     * @param o value from ResultSet.getObject()
     * @return the interval or null
     */
    public Interval<Double> getInterval(Object o) {
        PGpolygon pgp = (PGpolygon) o;
        List<Double> vals = parsePolygon2D(pgp);
        if (vals.size() == 2) {
            return new Interval<>(unscale(vals.get(0)), unscale(vals.get(1)));
        }
        throw new RuntimeException("BUG: found " + vals.size() + " values for Interval<Double>");
    }
    
    /**
     * Parse the string representation of a polygon value (from ResultSet.getString(...)).
     *
     * @param s string returned from ResultSet.getString
     * @return the interval or null
     */
    @Deprecated
    public Interval<Double> getInterval(String s) {
        if (s == null) {
            return null;
        }

        List<Double> vals = parsePolygon2D(s);
        if (vals.size() == 2) {
            return new Interval<>(unscale(vals.get(0)), unscale(vals.get(1)));
        }
        throw new RuntimeException("BUG: found " + vals.size() + " values for Interval<Double>");
    }

    /**
     * Convert the internal database object from ResultSet.getObject().
     * 
     * @param o value from ResultSet.getObject()
     * @return the array of intervals or null
     */
    public Interval<Double>[] getIntervalArray(Object o) {
        PGpolygon pgp = (PGpolygon) o;
        List<Double> vals = parsePolygon2D(pgp);
        Interval<Double>[] ret = new Interval[vals.size() / 2];
        for (int i = 0; i < vals.size(); i += 2) {
            ret[i / 2] = new Interval<>(unscale(vals.get(i)), unscale(vals.get(i + 1)));
        }
        return ret;
    }

    /**
     * Parse the string representation of a polygon value (from ResultSet.getString(...)).
     *
     * @param s string returned from ResultSet.getString
     * @return the interval array or null
     */
    @Deprecated
    public Interval<Double>[] getIntervalArray(String s) {
        if (s == null) {
            return null;
        }

        List<Double> vals = parsePolygon2D(s);
        Interval<Double>[] ret = new Interval[vals.size() / 2];
        for (int i = 0; i < vals.size(); i += 2) {
            ret[i / 2] = new Interval<>(unscale(vals.get(i)), unscale(vals.get(i + 1)));
        }
        return ret;
    }

    private List<Double> parsePolygon2D(String s) {
        s = s.replaceAll("[()]", ""); // remove all ( )
        //log.debug("strip: '" + s + "'");
        String[] points = s.split(",");
        List<Double> vals = new ArrayList<>();
        for (int i = 0; i < points.length; i += 2) {
            String xs = points[i];
            String ys = points[i + 1];
            //log.debug("check: " + xs + "," + ys);
            double y = Double.parseDouble(ys);
            if (y > YVALUE) {
                vals.add(Double.parseDouble(xs));
            }
        }
        // sort so we don't care about winding direction of the polygon impl
        Collections.sort(vals);

        return vals;
    }
    
    private List<Double> parsePolygon2D(PGpolygon poly) {
        List<Double> vals = new ArrayList<>();
        for (int i = 0; i < poly.points.length; i++) {
            PGpoint p = poly.points[i];
            if (p.y > YVALUE) {
                vals.add(p.x);
            }
        }
        // sort so we don't care about winding direction of the polygon impl
        Collections.sort(vals);

        return vals;
    }
}
