/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2011.                            (c) 2011.
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

import ca.nrc.cadc.util.CaseInsensitiveStringComparator;
import ca.nrc.cadc.util.Log4jInit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author pdowler
 */
public class AdqlQueryGeneratorTest 
{
    private static final Logger log = Logger.getLogger(AdqlQueryGeneratorTest.class);
    
    static
    {
        Log4jInit.setLevel("ca.nrc.cadc.sia2", Level.INFO);
    }
    
    
    //@Test
    public void testTemplate()
    {
        
        try
        {
            
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testNoParams()
    {
        
        try
        {
            Map<String,List<String>> params = new TreeMap<String,List<String>>(new CaseInsensitiveStringComparator());
            AdqlQueryGenerator gen = new AdqlQueryGenerator(params);
            Map<String,Object> tapParams = gen.getParameterMap();
            
            String lang = (String) tapParams.get("LANG");
            String adql = (String) tapParams.get("QUERY");
            
            Assert.assertEquals("ADQL", lang);
            
            log.info("testNoParams ADQL:\n" + adql);
            Assert.assertTrue("dataproduct_type", adql.contains("dataproduct_type"));
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testScalarInterval()
    {
        
        try
        {
            Map<String,List<String>> params = new TreeMap<String,List<String>>(new CaseInsensitiveStringComparator());
            params.put("BAND", Arrays.asList("550e-9"));
            params.put("TIME", Arrays.asList("54321.0"));
            
            AdqlQueryGenerator gen = new AdqlQueryGenerator(params);
            Map<String,Object> tapParams = gen.getParameterMap();
            
            String lang = (String) tapParams.get("LANG");
            String adql = (String) tapParams.get("QUERY");
            
            Assert.assertEquals("ADQL", lang);
            
            log.info("testScalarInterval ADQL:\n" + adql);
            Assert.assertTrue("dataproduct_type", adql.contains("dataproduct_type"));
            Assert.assertTrue("em_min", adql.contains("em_min <="));
            Assert.assertTrue("em_max", adql.contains("<= em_max"));
            Assert.assertTrue("t_min", adql.contains("t_min <="));
            Assert.assertTrue("t_max", adql.contains("<= t_max"));
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testSingleParams()
    {
        
        try
        {
            Map<String,List<String>> params = new TreeMap<String,List<String>>(new CaseInsensitiveStringComparator());
            params.put("POS", Arrays.asList("CIRCLE 12.3 45.6 0.2"));
            params.put("BAND", Arrays.asList("500e-9 700e-9"));
            params.put("TIME", Arrays.asList("54321.0 55432.1"));
            params.put("POL", Arrays.asList("I"));
            params.put("FOV", Arrays.asList("0.5 +inf")); // > 0.5 deg
            params.put("SPATRES", Arrays.asList("-inf 0.2")); // < 0.2 arcsec
            params.put("EXPTIME", Arrays.asList("600.0 3600.0")); // 10-60 minutes

            params.put("ID", Arrays.asList("A12345"));
            params.put("COLLECTION", Arrays.asList("CFHT"));
            params.put("FACILITY", Arrays.asList("JCMT"));
            params.put("INSTRUMENT", Arrays.asList("WIRCam"));
            params.put("DPTYPE", Arrays.asList("cube"));
            params.put("CALIB", Arrays.asList("2"));
            params.put("TARGET", Arrays.asList("M33"));
            params.put("TIMERES", Arrays.asList("1.0 2.0"));
            params.put("SPECRP", Arrays.asList("-inf 500"));
            params.put("FORMAT", Arrays.asList("application/fits"));

            AdqlQueryGenerator gen = new AdqlQueryGenerator(params);
            String adql = gen.getQuery();
            log.info("testSingleParams ADQL:\n" + adql);
            
            Assert.assertTrue("dataproduct_type", adql.contains("dataproduct_type"));
            
            Assert.assertTrue("s_region", adql.contains("s_region"));
            Assert.assertTrue("em_min", adql.contains("em_min"));
            Assert.assertTrue("em_max", adql.contains("em_max"));
            Assert.assertTrue("t_min", adql.contains("t_min"));
            Assert.assertTrue("t_max", adql.contains("t_max"));
            Assert.assertTrue("pol_states", adql.contains("pol_states"));
            
            Assert.assertTrue("s_fov", adql.contains("s_fov"));
            Assert.assertTrue("s_resolution", adql.contains("s_resolution"));
            Assert.assertTrue("t_exptime", adql.contains("t_exptime"));

            Assert.assertTrue("obs_publisher_did", adql.contains("obs_publisher_did"));
            Assert.assertTrue("obs_collection", adql.contains("obs_collection"));
            Assert.assertTrue("facility_name", adql.contains("facility_name"));
            Assert.assertTrue("instrument_name", adql.contains("instrument_name"));
            Assert.assertTrue("dataproduct_type", adql.contains("dataproduct_type"));
            Assert.assertTrue("calib_level", adql.contains("calib_level"));
            Assert.assertTrue("target_name", adql.contains("target_name"));
            Assert.assertTrue("t_resolution", adql.contains("t_resolution"));
            Assert.assertTrue("em_res_power", adql.contains("em_res_power"));
            Assert.assertTrue("access_format", adql.contains("access_format"));
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testMultipleParams()
    {
        
        try
        {
            Map<String,List<String>> params = new TreeMap<String,List<String>>(new CaseInsensitiveStringComparator());
            params.put("POS", Arrays.asList("CIRCLE 12.3 45.6 0.2", "RANGE -10 -8 +20 +22", "POLYGON 10 10 12 10 11 11"));
            params.put("BAND", Arrays.asList("500e-9 700e-9", "200e-9 400e-9"));
            params.put("TIME", Arrays.asList("54321.0 55432.1", "56789.0 +Inf"));
            params.put("POL", Arrays.asList("I", "Q", "U"));
            params.put("FOV", Arrays.asList("0.5 +Inf", "-Inf 2.0"));
            params.put("SPATRES", Arrays.asList("-Inf 0.2", "0.02 +Inf"));
            params.put("EXPTIME", Arrays.asList("10 20", "600.0 3600.0"));

            params.put("ID", Arrays.asList("A12345","12345B"));
            params.put("COLLECTION", Arrays.asList("CFHT","JCMT"));
            params.put("FACILITY", Arrays.asList("JCMT","BLAST"));
            params.put("INSTRUMENT", Arrays.asList("WIRCam","MEGAPipe"));
            params.put("DPTYPE", Arrays.asList("cube","image"));
            params.put("CALIB", Arrays.asList("2","4"));
            params.put("TARGET", Arrays.asList("M33","LMC"));
            params.put("TIMERES", Arrays.asList("1.0 2.0","-Inf 3.0"));
            params.put("SPECRP", Arrays.asList("-Inf 500","200 300"));
            params.put("FORMAT", Arrays.asList("application/fits","text/xml"));
            
            AdqlQueryGenerator gen = new AdqlQueryGenerator(params);
            String adql = gen.getQuery();
            log.info("testMultipleParams ADQL:\n" + adql);
            
            Assert.assertTrue("dataproduct_type", adql.contains("dataproduct_type"));
            
            Assert.assertTrue("s_region", adql.contains("s_region"));
            Assert.assertTrue("em_min", adql.contains("em_min"));
            Assert.assertTrue("em_max", adql.contains("em_max"));
            Assert.assertTrue("t_min", adql.contains("t_min"));
            Assert.assertTrue("t_max", adql.contains("t_max"));
            Assert.assertTrue("pol_states", adql.contains("pol_states"));
            
            Assert.assertTrue("s_fov", adql.contains("s_fov"));
            Assert.assertTrue("s_resolution", adql.contains("s_resolution"));
            Assert.assertTrue("t_exptime", adql.contains("t_exptime"));

            Assert.assertTrue("obs_publisher_did", adql.contains("obs_publisher_did"));
            Assert.assertTrue("obs_collection", adql.contains("obs_collection"));
            Assert.assertTrue("facility_name", adql.contains("facility_name"));
            Assert.assertTrue("instrument_name", adql.contains("instrument_name"));
            Assert.assertTrue("dataproduct_type", adql.contains("dataproduct_type"));
            Assert.assertTrue("calib_level", adql.contains("calib_level"));
            Assert.assertTrue("target_name", adql.contains("target_name"));
            Assert.assertTrue("t_resolution", adql.contains("t_resolution"));
            Assert.assertTrue("em_res_power", adql.contains("em_res_power"));
            Assert.assertTrue("access_format", adql.contains("access_format"));
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testCoordRanges()
    {
        
        try
        {
            Map<String,List<String>> params = new TreeMap<String,List<String>>(new CaseInsensitiveStringComparator());
            params.put("POS", Arrays.asList("RANGE -Inf +Inf -2 2", "RANGE 10 20 -Inf +Inf", "RANGE 1 2 3 4"));
           
            AdqlQueryGenerator gen = new AdqlQueryGenerator(params);
            String adql = gen.getQuery();
            log.info("testCoordRanges ADQL:\n" + adql);
            
            Assert.assertTrue("dataproduct_type", adql.contains("dataproduct_type"));
            Assert.assertTrue("s_region", adql.contains("s_region"));
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
}
