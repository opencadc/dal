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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author pdowler
 */
public class SiaValidator 
{
    private static final Logger log = Logger.getLogger(SiaValidator.class);
    
    private static final String POS = "POS";
    private static final String BAND = "BAND";
    private static final String TIME = "TIME";
    private static final String POL = "POL";
    private static final String FOV = "FOV";
    private static final String SPATRES = "SPATRES";
    private static final String EXPTIME = "EXPTIME";
    private static final String ID = "ID";
    private static final String COLLECTION = "COLLECTION";
    private static final String FACILITY = "FACILITY";
    private static final String INSTRUMENT = "INSTRUMENT";
    private static final String DPTYPE = "DPTYPE";
    private static final String CALIB = "CALIB";
    private static final String TARGET = "TARGET";
    private static final String TIMERES = "TIMERES";
    private static final String SPECRP = "SPECRP";
    private static final String FORMAT = "FORMAT";

    // used by the SiaRunner to pick out supported params only
    static final List<String> QUERY_PARAMS = Arrays.asList(POS, BAND, TIME, POL, FOV, SPATRES, EXPTIME,
                                                           ID, COLLECTION, FACILITY, INSTRUMENT, DPTYPE,
                                                           CALIB, TARGET, TIMERES, SPECRP, FORMAT);
    
    // pol_states values are always upper case so use List
    static final List<String> POL_STATES = Arrays.asList("I", "Q", "U", "V", "RR", "LL", "RL", "LR", "XX", "YY", "XY", "YX");

    // allowed data product types are image and cube
    static final List<String> ALLOWED_DPTYPES = Arrays.asList("cube", "image");

    private static final String CIRCLE = "CIRCLE";
    private static final String RANGE = "RANGE";
    private static final String POLYGON = "POLYGON";
    
    public SiaValidator() { }
    
    public List<Shape> validatePOS(Map<String,List<String>> params)
    {
        List<Shape> ret = new ArrayList<Shape>();
        if (params == null)
            return ret;
        List<String> values = params.get(POS);
        if (values == null)
            return ret;
        for (String v : values)
        {
            log.debug("validatePos: " + v);
            String[] tokens = v.split(" ");
            if ( CIRCLE.equalsIgnoreCase(tokens[0]) )
            {
                if (tokens.length != 4)
                    throw new IllegalArgumentException("POS invalid CIRCLE: " + v);
                try
                {
                    double ra = Double.parseDouble(tokens[1]);
                    double dec = Double.parseDouble(tokens[2]);
                    double rad = Double.parseDouble(tokens[3]);
                    ret.add(new CoordCircle(ra, dec, rad));
                }
                catch(NumberFormatException ex)
                {
                    throw new IllegalArgumentException("POS number in: " + v);
                }
            }
            else if (RANGE.equalsIgnoreCase(tokens[0]))
            {
                if (tokens.length != 5)
                    throw new IllegalArgumentException("POS invalid RANGE: " + v);
                try
                {
                    String[] rra = new String[] { tokens[1], tokens[2] };
                    String[] rde = new String[] { tokens[3], tokens[4] };
                    Range<String> s1 = parseStringRange(rra);
                    Range<String> s2 = parseStringRange(rde);
                    Range<Double> ra = parseDoubleRange("POS", s1);
                    Range<Double> dec = parseDoubleRange("POS", s2);
                    ret.add(new CoordRange(ra, dec));
                }
                catch(NumberFormatException ex)
                {
                    throw new IllegalArgumentException("POS number in: " + v);
                }
            }
            else if (POLYGON.equalsIgnoreCase(tokens[0]))
            {
                int len = tokens.length - 1;
                if (len < 6)
                    throw new IllegalArgumentException("POS invalid POLYGON (not enough coordinate values): " + v);
                if (len % 2 != 0)
                    throw new IllegalArgumentException("POS invalid POLYGON (odd number of coordinate values): " + v);
                CoordPolygon poly = new CoordPolygon();
                for (int i=1; i<=len; i+=2)
                {
                    try
                    {
                        Double d1 = new Double(tokens[i]);
                        Double d2 = new Double(tokens[i+1]);
                        poly.getVertices().add(new CoordPolygon.Vertex(d1, d2));
                    }
                    catch(NumberFormatException ex)
                    {
                        throw new IllegalArgumentException("POS invalid POLYGON ("+ex+"): " + v);
                    }
                }
                ret.add(poly);
            }
            else
                throw new IllegalArgumentException("POS invalid shape: " + v);
        }
        
        return ret;
    }
    
    public List<Range<Double>> validateBAND(Map<String,List<String>> params)
    {
        List<Range<Double>> ret = new ArrayList<Range<Double>>();
        if (params == null)
            return ret;
        List<String> values = params.get(BAND);
        if (values == null)
            return ret;
        for (String v : values)
        {
            log.debug("validateBAND: " + v);
            Range<String> sr = parseStringRange(v, true);
            ret.add( parseDoubleRange(BAND, sr) );
        }
        
        return ret;
    }
    
    public List<Range<Double>> validateTIME(Map<String,List<String>> params)
    {
        List<Range<Double>> ret = new ArrayList<Range<Double>>();
        if (params == null)
            return ret;
        List<String> values = params.get(TIME);
        if (values == null)
            return ret;
        for (String v : values)
        {
            log.debug("validateTIME: " + v);
            Range<String> sr = parseStringRange(v, true);
            ret.add( parseDoubleRange(TIME, sr) );
        }
        
        return ret;
    }
    
    public List<String> validatePOL(Map<String,List<String>> params)
    {
        return validateString(POL, params, POL_STATES);
    }
    
    public List<Range<Double>> validateFOV(Map<String,List<String>> params)
    {
        return validateNumeric(FOV, params);
    }
    public List<Range<Double>> validateSPATRES(Map<String,List<String>> params)
    {
        return validateNumeric(SPATRES, params);
    }
    public List<Range<Double>> validateEXPTIME(Map<String,List<String>> params)
    {
        return validateNumeric(EXPTIME, params);
    }

    public List<String> validateID(Map<String,List<String>> params)
    {
        return validateString(ID, params, null);
    }

    public List<String> validateCOLLECTION(Map<String, List<String>> params)
    {
        return validateString(COLLECTION, params, null);
    }

    public List<String> validateFACILITY(Map<String, List<String>> params)
    {
        return validateString(FACILITY, params, null);
    }

    public List<String> validateINSTRUMENT(Map<String, List<String>> params)
    {
        return validateString(INSTRUMENT, params, null);
    }

    public List<String> validateDPTYPE(Map<String, List<String>> params)
    {
        return validateString(DPTYPE, params, ALLOWED_DPTYPES);
    }

    public List<Integer> validateCALIB(Map<String, List<String>> params)
    {
        return validateInteger(CALIB, params);
    }

    public List<String> validateTARGET(Map<String, List<String>> params)
    {
        return validateString(TARGET, params, null);
    }

    public List<Range<Double>> validateTIMERES(Map<String, List<String>> params)
    {
        return validateNumeric(TIMERES, params);
    }

    public List<Range<Double>> validateSPECRP(Map<String, List<String>> params)
    {
        return validateNumeric(SPECRP, params);
    }

    public List<String> validateFORMAT(Map<String, List<String>> params)
    {
        return validateString(FORMAT, params, null);
    }

    public List<String> validateString(String paramName, Map<String,List<String>> params, Collection<String> allowedValues)
    {
        List<String> ret = new ArrayList<String>();
        if (params == null)
            return ret;
        List<String> values = params.get(paramName);
        if (values == null)
            return ret;
        for (String s : values)
        {
            log.debug("validateString " + paramName + ": " + s);
            if (allowedValues == null)
                ret.add(s);
            else if (allowedValues.contains(s))
                ret.add(s);
            else
                throw new IllegalArgumentException(paramName + " invalid value: " + s);
        }
        return ret;
    }

    List<Integer> validateInteger(String paramName, Map<String,List<String>> params)
    {
       List<Integer> ret = new ArrayList<Integer>();
        if (params == null)
            return ret;
        List<String> values = params.get(paramName);
        if (values == null)
            return ret;
        for (String v : values)
        {
            log.debug("validateNumeric " + paramName + ": "  + v);
            try
            {
                ret.add(new Integer(v));
            }
            catch(NumberFormatException ex)
            {
                throw new IllegalArgumentException(paramName + " invalid value: " + v);
            }
            finally { }
        }

        return ret;
    }
    
    List<Range<Double>> validateNumeric(String paramName, Map<String,List<String>> params)
    {
        List<Range<Double>> ret = new ArrayList<Range<Double>>();
        if (params == null)
            return ret;
        List<String> values = params.get(paramName);
        if (values == null)
            return ret;
        for (String v : values)
        {
            log.debug("validateNumeric " + paramName + ": "  + v);
            Range<String> sr = parseStringRange(v);
            ret.add( parseDoubleRange(paramName, sr) );
        }
        
        return ret;
    }

    static Range<Double> parseDoubleRange(String pname, Range<String> sr)
    {
        try
        {
            Double lb = null;
            Double ub = null;
            if (sr.getLower() != null)
                lb = new Double(sr.getLower());
            if (sr.getUpper() != null)
                ub = new Double(sr.getUpper());
            // subsequent code treats null bound as unspecified (aka -inf or inf)
            if (lb.isInfinite())
                lb = null;
            if (ub.isInfinite())
                ub = null;
            return new Range<Double>(lb, ub);
        }
        catch(NumberFormatException ex)
        {
            throw new IllegalArgumentException(pname + " cannot parse to double: " + sr);
        }
    }

    private Range<String> parseStringRange(String v) {
        return parseStringRange(v, false);
    }
    
    private Range<String> parseStringRange(String v, boolean allowScalar)
    {
        String[] vals = v.split(" ");
        if (allowScalar && vals.length == 1) {
            String vv = vals[0];
            vals = new String[] { vv, vv } ;
        }
        if (vals.length != 2) {
            throw new IllegalArgumentException("invalid range (must have 2 values): " + v);
        }
        return parseStringRange(vals);
    }
    
    private static Range<String> parseStringRange(String[] vals)
    {
        // make this directly parseable by java.lang.Double
        for (int i=0; i<2; i++)
        {
            if (vals[i].equalsIgnoreCase("-inf"))
                vals[i] = "-Infinity";
            else if (vals[i].equalsIgnoreCase("+inf"))
                vals[i] = "Infinity";
        }
        return new Range(vals[0], vals[1]);
    }
}
