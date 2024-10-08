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
*  $Revision: 5 $
*
************************************************************************
 */

package org.opencadc.dap;

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.PolarizationState;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.dali.Range;
import ca.nrc.cadc.dali.Shape;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * Generate TAP query of the ivoa.ObsCore table from the SIAv2 query parameters.
 *
 * @author jburke
 */
public class AdqlQueryGenerator {

    private static Logger log = Logger.getLogger(AdqlQueryGenerator.class);

    private final boolean sia2mode;
    private final String tableName;
    private final Map<String, List<String>> queryParams;

    /**
     * The input query parameters as structured by the ParamExtractor in cadc-dali.
     *
     * @param query query input parameters
     * @param tableName ivoa.ObsCore table name
     * @see ca.nrc.cadc.dali.ParamExtractor
     */
    public AdqlQueryGenerator(Map<String, List<String>> query, String tableName, boolean sia2mode) {
        this.tableName = tableName;
        this.queryParams = query;
        this.sia2mode = sia2mode;
    }

    /**
     * Map with the REQUEST, LANG, and QUERY parameters.
     *
     * @return map of parameter names and values
     */
    public Map<String, Object> getParameterMap() {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("LANG", "ADQL");
        String adql = getQuery();
        log.debug("SIAv2 query:\n" + adql);
        map.put("QUERY", adql);
        return map;
    }

    protected String getQuery() {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ");
        query.append(tableName);

        DapParamValidator dapParamValidator = new DapParamValidator(sia2mode);
        final List<Shape> pos = dapParamValidator.validatePOS(queryParams);
        final List<Interval> bands = dapParamValidator.validateBAND(queryParams);
        final List<Interval> times = dapParamValidator.validateTIME(queryParams);
        final List<PolarizationState> pols = dapParamValidator.validatePOL(queryParams);
        
        if (sia2mode) {
            query.append(" WHERE dataproduct_type IN ( 'image', 'cube' )");
        } else {
            // subsequent append to query is much simpler if there is already a
            // where clause and everything else is AND ...
            query.append(" WHERE dataproduct_type IS NOT NULL");
        }
        
        if (!pos.isEmpty()) {
            boolean needOr = false;
            query.append(" AND ");
            if (pos.size() > 1) {
                query.append("(");
            }
            for (Shape s : pos) {
                if (needOr) {
                    query.append(" OR ");
                }
                query.append("(");

                query.append("INTERSECTS(");
                if (s instanceof Circle) {
                    Circle c = (Circle) s;
                    query.append("CIRCLE('ICRS',");
                    query.append(c.getCenter().getLongitude());
                    query.append(",");
                    query.append(c.getCenter().getLatitude());
                    query.append(",");
                    query.append(c.getRadius());
                    query.append(")");
                } else if (s instanceof Range) {
                    Range r = (Range) s;
                    query.append("RANGE_S2D(");
                    double ralb = 0.0;
                    double raub = 360.0;
                    double declb = -90.0;
                    double decub = 90.0;
                    if (r.getLongitude().getLower() != null) {
                        ralb = r.getLongitude().getLower();
                    }
                    if (r.getLongitude().getUpper() != null) {
                        raub = r.getLongitude().getUpper();
                    }
                    if (r.getLatitude().getLower() != null) {
                        declb = r.getLatitude().getLower();
                    }
                    if (r.getLatitude().getUpper() != null) {
                        decub = r.getLatitude().getUpper();
                    }
                    query.append(ralb);
                    query.append(",");
                    query.append(raub);
                    query.append(",");
                    query.append(declb);
                    query.append(",");
                    query.append(decub);
                    query.append(")");
                } else if (s instanceof Polygon) {
                    Polygon p = (Polygon) s;
                    query.append("POLYGON('ICRS',");
                    boolean needComma = false;
                    for (Point v : p.getVertices()) {
                        if (needComma) {
                            query.append(",");
                        }
                        query.append(v.getLongitude()).append(",").append(v.getLatitude());
                        needComma = true;
                    }
                    query.append(")");
                }
                query.append(", s_region) = 1");
                query.append(")");
                needOr = true;
            }
            if (pos.size() > 1) {
                query.append(")");
            }
        }

        addNumericRangeConstraint(query, "em_min", "em_max", bands);
        
        addNumericRangeConstraint(query, "t_min", "t_max", times);
        
        if (!pols.isEmpty()) {
            // for a single pattern-matching LIKE statement, we need to sort the POL values in canoncial order
            // and stick in wildcard % whenever there is a gap
            // use caom2 PolarizationState for now, possibly copy/move that to an OpenCADC module
            //SortedSet<PolarizationState> polStates = new TreeSet<PolarizationState>(new PolarizationState.PolStateComparator());
            //for (String p : pols)
            //{
            //    polStates.add( PolarizationState.valueOf(p));
            //}

            query.append(" AND ");
            if (pols.size() > 1) {
                query.append(" (");
            }
            boolean needOr = false;
            for (PolarizationState p : pols) {
                if (needOr) {
                    query.append(" OR ");
                }
                query.append("(");
                query.append("pol_states LIKE '%").append(p.name()).append("%'");
                query.append(")");
                needOr = true;
            }
            if (pols.size() > 1) {
                query.append(")");
            }
        }

        List<Interval> fovs = dapParamValidator.validateFOV(queryParams);
        addNumericRangeConstraint(query, "s_fov", "s_fov", fovs);

        List<Interval> ress = dapParamValidator.validateSPATRES(queryParams);
        addNumericRangeConstraint(query, "s_resolution", "s_resolution", ress);

        List<Interval> exptimes = dapParamValidator.validateEXPTIME(queryParams);
        addNumericRangeConstraint(query, "t_exptime", "t_exptime", exptimes);

        List<String> ids = dapParamValidator.validateID(queryParams);
        addStringListConstraint(query, "obs_publisher_did", ids);

        List<String> collections = dapParamValidator.validateCOLLECTION(queryParams);
        addStringListConstraint(query, "obs_collection", collections);

        List<String> facilities = dapParamValidator.validateFACILITY(queryParams);
        addStringListConstraint(query, "facility_name", facilities);

        List<String> instruments = dapParamValidator.validateINSTRUMENT(queryParams);
        addStringListConstraint(query, "instrument_name", instruments);

        List<Integer> calibs = dapParamValidator.validateCALIB(queryParams);
        addIntegerListConstraint(query, "calib_level", calibs);

        List<String> targets = dapParamValidator.validateTARGET(queryParams);
        addStringListConstraint(query, "target_name", targets);

        List<Interval> timeress = dapParamValidator.validateTIMERES(queryParams);
        addNumericRangeConstraint(query, "t_resolution", "t_resolution", timeress);

        List<Interval> specrps = dapParamValidator.validateSPECRP(queryParams);
        addNumericRangeConstraint(query, "em_res_power", "em_res_power", specrps);

        List<String> formats = dapParamValidator.validateFORMAT(queryParams);
        addStringListConstraint(query, "access_format", formats);

        List<String> dptypes = dapParamValidator.validateDPTYPE(queryParams);
        addStringListConstraint(query, "dataproduct_type", dptypes);

        return query.toString();
    }

    private void addNumericRangeConstraint(StringBuilder query, String lbCol, String ubCol, List<Interval> ranges) {
        if (!ranges.isEmpty()) {
            if (ranges.size() > 1) {
                query.append(" AND (");
            } else {
                query.append(" AND ");
            }
            boolean needOr = false;
            for (Interval r : ranges) {
                if (needOr) {
                    query.append(" OR ");
                }
                query.append("(");
                if (lbCol.equals(ubCol) && !Double.isInfinite(r.getLower().doubleValue()) && !Double.isInfinite(r.getUpper().doubleValue())) {
                    // nicer syntax, better optimised in DB?
                    query.append(lbCol).append(" BETWEEN ").append(r.getLower()).append(" AND ").append(r.getUpper());
                } else {
                    if (!Double.isInfinite(r.getUpper().doubleValue())) {
                        query.append(lbCol).append(" <= ").append(r.getUpper());
                    }
                    if (!Double.isInfinite(r.getLower().doubleValue()) && !Double.isInfinite(r.getUpper().doubleValue())) {
                        query.append(" AND ");
                    }
                    if (!Double.isInfinite(r.getLower().doubleValue())) {
                        query.append(r.getLower()).append(" <= ").append(ubCol);
                    }
                }
                query.append(")");
                needOr = true;
            }
            if (ranges.size() > 1) {
                query.append(")");
            }
        }
    }

    private void addIntegerListConstraint(StringBuilder query, String column, List<Integer> values) {
        if (!values.isEmpty()) {
            query.append(" AND ").append(column);
            if (values.size() == 1) {
                query.append(" = ").append(values.get(0));
            } else {
                query.append(" IN ( ");
                boolean first = true;
                for (Integer value : values) {
                    if (first) {
                        first = false;
                    } else {
                        query.append(",");
                    }
                    query.append(value);
                }
                query.append(" )");
            }
        }
    }

    private void addStringListConstraint(StringBuilder query, String column, List<String> values) {
        if (!values.isEmpty()) {
            query.append(" AND ").append(column);
            if (values.size() == 1) {
                query.append(" = '").append(values.get(0)).append("'");
            } else {
                query.append(" IN ( ");
                boolean first = true;
                for (String value : values) {
                    if (first) {
                        first = false;
                    } else {
                        query.append(",");
                    }
                    query.append("'").append(value).append("'");
                }
                query.append(" )");
            }
        }
    }

}
