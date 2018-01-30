/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2014.                            (c) 2014.
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

package ca.nrc.cadc.sia2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 * Generate TAP query of the ivoa.ObsCore table from the SIAv2 query parameters.
 * 
 * @author jburke
 */
public class AdqlQueryGenerator
{
    private static Logger log = Logger.getLogger(AdqlQueryGenerator.class);
    
    private Map<String,List<String>> queryParams;

    /**
     * The input SIA query parameters as structured by the ParamExtractor in cadcDALI.
     *
     * @param query query input parameters
     * @see ca.nrc.cadc.dali.ParamExtractor
     */
    public AdqlQueryGenerator(Map<String,List<String>> query)
    {
        this.queryParams = query;
    }

    /**
     * Map with the REQUEST, LANG, and QUERY parameters.
     * 
     * @return map of parameter names and values
     */
    public Map<String,Object> getParameterMap()
    {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("REQUEST", "doQuery");
        map.put("LANG", "ADQL");
        String adql = getQuery();
        log.debug("SIAv2 query:\n" + adql);
        map.put("QUERY", adql);
        return map;
    }

    protected String getQuery()
    {
        StringBuilder query = new StringBuilder();
        query.append("SELECT * FROM ivoa.ObsCore WHERE dataproduct_type IN ( 'image', 'cube' )");
        
        SiaValidator sia = new SiaValidator();
        List<Shape> pos = sia.validatePOS(queryParams);
        if ( !pos.isEmpty())
        {
            boolean needOr = false;
            if (pos.size() > 1)
                query.append(" AND (");
            else
                query.append(" AND ");
            for (Shape s : pos)
            {
                if (needOr)
                    query.append(" OR ");
                query.append("(");
                
                query.append("INTERSECTS(");
                if (s instanceof CoordCircle)
                {
                    CoordCircle c = (CoordCircle) s;
                    query.append("CIRCLE('ICRS',");
                    query.append(c.getLongitude());
                    query.append(",");
                    query.append(c.getLatitude());
                    query.append(",");
                    query.append(c.getRadius());
                    query.append(")");
                }
                else if (s instanceof CoordRange)
                {
                    CoordRange r = (CoordRange) s;
                    query.append("RANGE_S2D(");
                    double ralb = 0.0;
                    double raub = 360.0;
                    double declb = -90.0;
                    double decub = 90.0;
                    if (r.getLongitudeRange().getLower() != null)
                        ralb = r.getLongitudeRange().getLower();
                    if (r.getLongitudeRange().getUpper() != null)
                        raub = r.getLongitudeRange().getUpper();
                    if (r.getLatitudeRange().getLower() != null)
                        declb = r.getLatitudeRange().getLower();
                    if (r.getLatitudeRange().getUpper() != null)
                        decub = r.getLatitudeRange().getUpper();
                    query.append(ralb);
                    query.append(",");
                    query.append(raub);
                    query.append(",");
                    query.append(declb);
                    query.append(",");
                    query.append(decub);
                    query.append(")");
                }
                else if (s instanceof CoordPolygon)
                {
                    CoordPolygon p = (CoordPolygon) s;
                    query.append("POLYGON('ICRS',");
                    boolean needComma = false;
                    for (CoordPolygon.Vertex v : p.getVertices())
                    {
                        if (needComma)
                            query.append(",");
                        query.append(v.getLongitude()).append(",").append(v.getLatitude());
                        needComma = true;
                    }
                    query.append(")");
                }
                query.append(", s_region) = 1");
                query.append(")");
                needOr = true;
            }
            if (pos.size() > 1)
                query.append(")");
        }
        
        List<Range<Double>> bands = sia.validateBAND(queryParams);
        addNumericRangeConstraint(query, "em_min", "em_max", bands);
        
        List<Range<Double>> times = sia.validateTIME(queryParams);
        addNumericRangeConstraint(query, "t_min", "t_max", times);
        
        List<String> pols = sia.validatePOL(queryParams);
        if (!pols.isEmpty())
        {
            // for a single pattern-matching LIKE statement, we need to sort the POL values in canoncial order
            // and stick in wildcard % whenever there is a gap
            // use caom2 PolarizationState for now, possibly copy/move that to an OpenCADC module
            //SortedSet<PolarizationState> polStates = new TreeSet<PolarizationState>(new PolarizationState.PolStateComparator());
            //for (String p : pols)
            //{
            //    polStates.add( PolarizationState.valueOf(p));
            //}
            
            if (pols.size() > 1)
                query.append(" AND (");
            else
                query.append(" AND ");
            boolean needOr = false;
            for (String p : pols)
            {
                if (needOr)
                    query.append(" OR ");
                query.append("(");
                query.append("pol_states LIKE '%").append(p).append("%'");
                query.append(")");
                needOr = true;
            }
            if (pols.size() > 1)
                query.append(")");
        }
        
        List<Range<Double>> fovs = sia.validateFOV(queryParams);
        addNumericRangeConstraint(query, "s_fov", "s_fov", fovs);
        
        List<Range<Double>> ress = sia.validateSPATRES(queryParams);
        addNumericRangeConstraint(query, "s_resolution", "s_resolution", ress);
        
        List<Range<Double>> exptimes = sia.validateEXPTIME(queryParams);
        addNumericRangeConstraint(query, "t_exptime", "t_exptime", exptimes);

        List<String> ids = sia.validateID(queryParams);
        addStringListConstraint(query, "obs_publisher_did", ids);

        List<String> collections = sia.validateCOLLECTION(queryParams);
        addStringListConstraint(query, "obs_collection", collections);

        List<String> facilities = sia.validateFACILITY(queryParams);
        addStringListConstraint(query, "facility_name", facilities);

        List<String> instruments = sia.validateINSTRUMENT(queryParams);
        addStringListConstraint(query, "instrument_name", instruments);

        List<String> dptypes = sia.validateDPTYPE(queryParams);
        addStringListConstraint(query, "dataproduct_type", dptypes);

        List<Integer> calibs = sia.validateCALIB(queryParams);
        addIntegerListConstraint(query, "calib_level", calibs);

        List<String> targets = sia.validateTARGET(queryParams);
        addStringListConstraint(query, "target_name", targets);

        List<Range<Double>> timeress = sia.validateTIMERES(queryParams);
        addNumericRangeConstraint(query, "t_resolution", "t_resolution", timeress);

        List<Range<Double>> specrps = sia.validateSPECRP(queryParams);
        addNumericRangeConstraint(query, "em_res_power", "em_res_power", specrps);

        List<String> formats = sia.validateFORMAT(queryParams);
        addStringListConstraint(query, "access_format", formats);

        return query.toString();
    }

    private void addIntegerRangeConstraint(StringBuilder query, String lbCol, String ubCol, List<Range<Integer>> ranges)
    {
        if (!ranges.isEmpty())
        {
            if (ranges.size() > 1)
                query.append(" AND (");
            else
                query.append(" AND ");
            boolean needOr = false;
            for (Range<Integer> r : ranges)
            {
                if (needOr)
                    query.append(" OR ");
                query.append("(");
                if (lbCol.equals(ubCol) && r.getLower() != null && r.getUpper() != null) // nicer syntax, maybe better optimised in DB
                    query.append(lbCol).append(" BETWEEN ").append(r.getLower()).append(" AND ").append(r.getUpper());
                else
                {
                    if (r.getUpper() != null)
                        query.append(lbCol).append(" <= ").append(r.getUpper());
                    if (r.getLower() != null && r.getUpper() != null)
                        query.append(" AND ");
                    if (r.getLower() != null)
                        query.append(r.getLower()).append(" <= ").append(ubCol);
                }
                query.append(")");
                needOr = true;
            }
            if (ranges.size() > 1)
                query.append(")");
        }
    }

    private void addNumericRangeConstraint(StringBuilder query, String lbCol, String ubCol, List<Range<Double>> ranges)
    {
        if (!ranges.isEmpty())
        {
            if (ranges.size() > 1)
                query.append(" AND (");
            else
                query.append(" AND ");
            boolean needOr = false;
            for (Range<Double> r : ranges)
            {
                if (needOr)
                    query.append(" OR ");
                query.append("(");
                if (lbCol.equals(ubCol) && r.getLower() != null && r.getUpper() != null) // nicer syntax, maybe better optimised in DB
                    query.append(lbCol).append(" BETWEEN ").append(r.getLower()).append(" AND ").append(r.getUpper());
                else
                {
                    if (r.getUpper() != null)
                        query.append(lbCol).append(" <= ").append(r.getUpper());
                    if (r.getLower() != null && r.getUpper() != null)
                        query.append(" AND ");
                    if (r.getLower() != null)
                        query.append(r.getLower()).append(" <= ").append(ubCol);
                }
                query.append(")");
                needOr = true;
            }
            if (ranges.size() > 1)
                query.append(")");
        }
    }

    private void addFooRangeConstraint(StringBuilder query, String lbCol, String ubCol, List<Range<Double>> ranges)
    {
        if (!ranges.isEmpty())
        {
            if (ranges.size() > 1)
                query.append(" AND (");
            else
                query.append(" AND ");
            boolean needOr = false;
            for (Range<Double> r : ranges)
            {
                if (needOr)
                    query.append(" OR ");
                query.append("(");
                if (lbCol.equals(ubCol) && r.getLower() != null && r.getUpper() != null) // nicer syntax, maybe better optimised in DB
                    query.append(lbCol).append(" BETWEEN ").append(r.getLower()).append(" AND ").append(r.getUpper());
                else
                {
                    if (r.getUpper() != null)
                        query.append(lbCol).append(" <= ").append(r.getUpper());
                    if (r.getLower() != null && r.getUpper() != null)
                        query.append(" AND ");
                    if (r.getLower() != null)
                        query.append(r.getLower()).append(" <= ").append(ubCol);
                }
                query.append(")");
                needOr = true;
            }
            if (ranges.size() > 1)
                query.append(")");
        }
    }

    private void addIntegerListConstraint(StringBuilder query, String column, List<Integer> values)
    {
        if (!values.isEmpty())
        {
            query.append(" AND ").append(column);
            if (values.size() == 1)
            {
                query.append(" = ").append(values.get(0));
            }
            else
            {
                query.append(" IN ( ");
                boolean first = true;
                for (Integer value : values)
                {
                    if (first)
                    {
                        first = false;
                    }
                    else
                    {
                        query.append(",");
                    }
                    query.append(value);
                }
                query.append(" )");
            }
        }
    }
    
    private void addStringListConstraint(StringBuilder query, String column, List<String> values)
    {
        if (!values.isEmpty())
        {
            query.append(" AND ").append(column);
            if (values.size() == 1)
            {
                query.append(" = '").append(values.get(0)).append("'");
            }
            else
            {
                query.append(" IN ( ");
                boolean first = true;
                for (String value : values)
                {
                    if (first)
                    {
                        first = false;
                    }
                    else
                    {
                        query.append(",");
                    }
                    query.append("'").append(value).append("'");
                }
                query.append(" )");
            }
        }
    }

}
