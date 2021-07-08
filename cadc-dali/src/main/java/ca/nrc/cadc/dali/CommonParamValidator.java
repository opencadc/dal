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

package ca.nrc.cadc.dali;

import ca.nrc.cadc.dali.util.CircleFormat;
import ca.nrc.cadc.dali.util.DoubleIntervalFormat;
import ca.nrc.cadc.dali.util.PolygonFormat;
import ca.nrc.cadc.dali.util.ShapeFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.log4j.Logger;

/**
 *
 * @author pdowler
 */
public class CommonParamValidator {

    private static final Logger log = Logger.getLogger(CommonParamValidator.class);

    // DALI params
    public static final String RUNID = "RUNID";
    public static final String RESPONSEFORMAT = "RESPONSEFORMAT";
    
    // common params
    public static final String POS = "POS";
    public static final String CIRCLE = "CIRCLE";
    public static final String POLYGON = "POLYGON";
    public static final String BAND = "BAND";
    public static final String TIME = "TIME";
    public static final String POL = "POL";
    public static final String ID = "ID";
    
    // pol_states values are always upper case so use List
    public static final List<String> POL_STATES = Arrays.asList(
        "I", "Q", "U", "V", 
        "RR", "LL", "RL", "LR", 
        "XX", "YY", "XY", "YX",
        "POLI", "POLA"
    );

    public CommonParamValidator() {
    }
    
    /**
     * @param params parameter map (e.g. from cadc-rest SyncInput or ParamExtractor)
     * @return single RUNID value or null
     */
    public String getRunID(Map<String, List<String>> params) {
        List<String> values = params.get(RUNID);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }
    
    /**
     * @param params parameter map (e.g. from cadc-rest SyncInput or ParamExtractor)
     * @return single RESPONSEFORMAT value or null
     */
    public String getResponseFormat(Map<String, List<String>> params) {
        List<String> values = params.get(RESPONSEFORMAT);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private String scalarToInterval(String s) {
        String[] ss = s.split(" ");
        if (ss.length == 1) {
            return s + " " + s;
        }
        return s;
    }
    
    public List<Shape> validatePOS(Map<String, List<String>> params) {
        List<Shape> ret = new ArrayList<>();
        if (params == null) {
            return ret;
        }
        List<String> values = params.get(POS);
        if (values == null) {
            return ret;
        }
        ShapeFormat fmt = new ShapeFormat(true);
        for (String v : values) {
            log.debug("validatePos: " + v);
            Shape shape = fmt.parse(v);
            ret.add(shape);
        }

        return ret;
    }
    
    public List<Circle> validateCircle(Map<String, List<String>> params) {
        List<Circle> ret = new ArrayList<>();
        List<String> values = params.get(CIRCLE);
        if (values == null) {
            return ret;
        }
        CircleFormat cf = new CircleFormat();
        for (String v : values) {
            Circle c = cf.parse(v);
            ret.add(c);
        }
        return ret;
    }
    
    public List<Polygon> validatePolygon(Map<String, List<String>> params) {
        List<Polygon> ret = new ArrayList<>();
        List<String> values = params.get(POLYGON);
        if (values == null) {
            return ret;
        }
        PolygonFormat pf = new PolygonFormat();
        for (String v : values) {
            Polygon p = pf.parse(v);
            ret.add(p);
        }
        return ret;
    }
    
    public List<Shape> validateAllShapes(Map<String, List<String>> params) {
        List<Shape> ret = validatePOS(params);

        List<String> values = params.get(CIRCLE);
        if (values == null) {
            return ret;
        }
        CircleFormat cf = new CircleFormat();
        for (String v : values) {
            Shape shape = cf.parse(v);
            ret.add(shape);
        }
        
        values = params.get(POLYGON);
        if (values == null) {
            return ret;
        }
        PolygonFormat pf = new PolygonFormat();
        for (String v : values) {
            Shape shape = pf.parse(v);
            ret.add(shape);
        }
        return ret;
    }

    public List<Interval> validateBAND(Map<String, List<String>> params) {
        List<Interval> ret = new ArrayList<>();
        if (params == null) {
            return ret;
        }
        List<String> values = params.get(BAND);
        if (values == null) {
            return ret;
        }
        DoubleIntervalFormat fmt = new DoubleIntervalFormat();
        for (String v : values) {
            String vv = scalarToInterval(v);
            log.debug("validateBAND: " + v + " aka " + vv);
            DoubleInterval di = fmt.parse(vv);
            ret.add(di);
        }

        return ret;
    }

    public List<Interval> validateTIME(Map<String, List<String>> params) {
        List<Interval> ret = new ArrayList<>();
        if (params == null) {
            return ret;
        }
        List<String> values = params.get(TIME);
        if (values == null) {
            return ret;
        }
        DoubleIntervalFormat fmt = new DoubleIntervalFormat();
        for (String v : values) {
            String vv = scalarToInterval(v);
            log.debug("validateBAND: " + v + " aka " + vv);
            DoubleInterval di = fmt.parse(vv);
            ret.add(di);
        }

        return ret;
    }

    /**
     * For any POL parameters with unexpected values, this will throw an IllegalArgumentException.
     * @param params    The Request parameters.
     * @return  List of PolarizationState instances.  Never null.
     * @throws IllegalArgumentException for a bad POL param.
     */
    public List<PolarizationState> validatePOL(Map<String, List<String>> params) {
        final List<PolarizationState> polStates = new ArrayList<>();

        if (params != null) {
            final List<String> polParameters = params.get(POL);
            if (polParameters != null) {
                for (final String polParam : polParameters) {
                    polStates.add(PolarizationState.valueOf(polParam.toUpperCase(Locale.ROOT)));
                }
            }
        }

        return polStates;
    }

    public List<String> validateID(Map<String, List<String>> params) {
        return validateString(ID, params, null);
    }

    public List<String> validateString(String paramName, Map<String, List<String>> params, Collection<String> allowedValues) {
        List<String> ret = new ArrayList<String>();
        if (params == null) {
            return ret;
        }
        List<String> values = params.get(paramName);
        if (values == null) {
            return ret;
        }
        for (String s : values) {
            log.debug("validateString " + paramName + ": " + s);
            if (allowedValues == null) {
                ret.add(s);
            } else if (allowedValues.contains(s)) {
                ret.add(s);
            } else {
                throw new IllegalArgumentException(paramName + " invalid value: " + s);
            }
        }
        return ret;
    }

    public List<Integer> validateInteger(String paramName, Map<String, List<String>> params) {
        List<Integer> ret = new ArrayList<Integer>();
        if (params == null) {
            return ret;
        }
        List<String> values = params.get(paramName);
        if (values == null) {
            return ret;
        }
        for (String v : values) {
            log.debug("validateNumeric " + paramName + ": " + v);
            try {
                ret.add(new Integer(v));
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(paramName + " invalid value: " + v);
            }
        }

        return ret;
    }

    public List<Interval> validateNumericInterval(String paramName, Map<String, List<String>> params) {
        List<Interval> ret = new ArrayList<>();
        if (params == null) {
            return ret;
        }
        List<String> values = params.get(paramName);
        if (values == null) {
            return ret;
        }
        DoubleIntervalFormat fmt = new DoubleIntervalFormat();
        for (String v : values) {
            String vv = v; //scalar2interval(v);
            log.debug("validateNumeric " + paramName + ": " + v + " aka " + vv);
            DoubleInterval di = fmt.parse(vv);
            ret.add(di);
        }

        return ret;
    }
}
