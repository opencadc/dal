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

import ca.nrc.cadc.dali.util.UTCTimestampFormat;
import ca.nrc.cadc.date.DateUtil;
import ca.nrc.cadc.util.CaseInsensitiveStringComparator;
import ca.nrc.cadc.util.Log4jInit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
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
public class SiaValidatorTest 
{
    private static final Logger log = Logger.getLogger(SiaValidatorTest.class);
    
    static
    {
        Log4jInit.setLevel("ca.nrc.cadc.sia2", Level.INFO);
    }
    
    SiaValidator sia = new SiaValidator();
    
    
    @Test
    public void testValidatePOS()
    {
        String[] testParams = new String[] { "POS", "pos", "PoS", };
        String[] testCircles = new String[]
        {
            "CIRCLE 12.3 34.5 0.1",
            "Circle 1.23e1 3.45e1 1.0e-1",
            "circle 1.23E1 3.45E1 1.0E-1"
        };
        String[] testRanges = new String[]
        {
            "RANGE 10 11 20 21",
            "Range 10 11 20 21",
            "range 10 11 20 21"
        };
        String[] testPolygons = new String[]
        {
            "POLYGON 10 10 10 11 11 11 11 10",
            "Polygon 10 10 10 11 11 11 11 10",
            "polygon 10 10 10 11 11 11 11 10",
        };
        String[] invalidValues = new String[]
        {
            "Circle 12.3 34.5", // no radius
            "Circle x1.23e1 3.45e1 1.0e-1", // invalid number
            "Range 1 2",         // not enough axes
            "Range 1 2 3 4 5 6", // too many axes
            "Range x1 x2 3 4",   // invalid number
            "Polygon 10 10 11 11 12x 13", // one value invalid number
            "Polygon 10 10 11 11", // not enough vertices
            "Polygon 10 10 11 10 11 11 12", // odd number of coord values
            "Donut 1.23E1 3.45E1 1.0E-1" // invalid shape
        };
        try
        {
            List empty = sia.validatePOS(null); // null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());
            
            Map<String,List<String>> params = new TreeMap<String,List<String>>(new CaseInsensitiveStringComparator());
            
            for (String tp : testParams)
            {
                for (String tv : testCircles)
                {
                    List<String> vals = new ArrayList<String>();
                    vals.add(tv);
                    params.put(tp, vals);
                    List<Shape> pos = sia.validatePOS(params);
                    Assert.assertNotNull(pos);
                    Assert.assertEquals(1, pos.size());
                    Shape s = pos.get(0);
                    log.debug("shape: " + s);
                    Assert.assertEquals(CoordCircle.class, s.getClass());
                    CoordCircle c = (CoordCircle) s;
                    Assert.assertEquals(12.3, c.getLongitude(), 0.001);
                    Assert.assertEquals(34.5, c.getLatitude(), 0.001);
                    Assert.assertEquals(0.1, c.getRadius(), 0.001);
                }
                for (String tv : testRanges)
                {
                    List<String> vals = new ArrayList<String>();
                    vals.add(tv);
                    params.put(tp, vals);
                    List<Shape> pos = sia.validatePOS(params);
                    Assert.assertNotNull(pos);
                    Assert.assertEquals(1, pos.size());
                    Shape s = pos.get(0);
                    log.debug("shape: " + s);
                    Assert.assertEquals(CoordRange.class, s.getClass());
                    CoordRange c = (CoordRange) s;
                    Assert.assertEquals(10.0, c.getLongitudeRange().getLower(), 0.0001);
                    Assert.assertEquals(11.0, c.getLongitudeRange().getUpper(), 0.0001);
                    Assert.assertEquals(20.0, c.getLatitudeRange().getLower(), 0.0001);
                    Assert.assertEquals(21.0, c.getLatitudeRange().getUpper(), 0.0001);
                }
                for (String tv : testPolygons)
                {
                    List<String> vals = new ArrayList<String>();
                    vals.add(tv);
                    params.put(tp, vals);
                    List<Shape> pos = sia.validatePOS(params);
                    Assert.assertNotNull(pos);
                    Assert.assertEquals(1, pos.size());
                    Shape s = pos.get(0);
                    log.debug("shape: " + s);
                    Assert.assertEquals(CoordPolygon.class, s.getClass());
                    CoordPolygon c = (CoordPolygon) s;
                    Assert.assertEquals(4, c.getVertices().size());
                }
            }
            
            // test invalid
            for (String tp : testParams)
            {
                for (String tv : invalidValues)
                {
                    List<String> vals = new ArrayList<String>();
                    vals.add(tv);
                    params.put(tp, vals);
                    try
                    {
                        List<Shape> pos = sia.validatePOS(params);
                        Assert.fail("expected IllegalArgumentException, got: " + pos.size() + " Shape(s)");
                    }
                    catch(IllegalArgumentException expected)
                    {
                        log.debug("caught expected: " + expected);
                    }
                }
            }
            
            // test multiple values
            params.clear();
            List<String> vals = new ArrayList<String>();
            for (String tv : testCircles)
            {
                vals.add(tv);
            }
            params.put("POS", vals);
            List<Shape> pos = sia.validatePOS(params);
            Assert.assertNotNull(pos);
            Assert.assertEquals(testCircles.length, pos.size());
            for (Shape s : pos)
            {
                Assert.assertEquals(CoordCircle.class, s.getClass());
                CoordCircle c = (CoordCircle) s;
                Assert.assertEquals(12.3, c.getLongitude(), 0.001);
                Assert.assertEquals(34.5, c.getLatitude(), 0.001);
                Assert.assertEquals(0.1, c.getRadius(), 0.001);
            }
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testValidateBAND()
    {
        
        String[] testParams = new String[] { "BAND", "band", "BaNd" };
        
        try
        {
            sia.validateBAND(null); // compile and null arg check
            Method m = SiaValidator.class.getMethod("validateBAND", Map.class);
            doValidateNumeric(m, "BAND", testParams);
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testValidateTIME()
    {
        String LB = "54321.0 +Inf";
        String UB = "-Inf 55432.1";
        String OPEN = "-Inf +Inf";
        String[] testParams = new String[] { "TIME", "time", "TiMe" };
        String[] testValues = new String[]
        {
            "54321.0 55432.1",
            "5.43210e4 5.54321e4",
            "5.43210E4 5.54321E4",
            LB,
            UB,
            OPEN
        };
        String[] invalidValues = new String[]
        {
            "54321.0 55432.1 123", // too many slashes
            "54321.0x 55432.1", // number format
        };
        try
        {
            List empty = sia.validateTIME(null); // null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());
            
            Map<String,List<String>> params = new TreeMap<String,List<String>>(new CaseInsensitiveStringComparator());
            
            for (String tp : testParams)
            {
                for (String tv : testValues)
                {
                    List<String> vals = new ArrayList<String>();
                    vals.add(tv);
                    params.put(tp, vals);
                    List<Range<Double>> times = sia.validateTIME(params);
                    Assert.assertNotNull(times);
                    Assert.assertEquals(1, times.size());
                    Range<Double> r = times.get(0);
                    
                    if (tv == LB)
                    {
                        Assert.assertEquals(54321.0, r.getLower(), 0.001);
                        Assert.assertNull(r.getUpper());
                    }
                    else if (tv == UB)
                    {
                        Assert.assertNull(r.getLower());
                        Assert.assertEquals(55432.1, r.getUpper(), 0.001);
                    }
                    else if (tv == OPEN)
                    {
                        Assert.assertNull(r.getLower());
                        Assert.assertNull(r.getUpper());
                    }
                    else
                    {
                        Assert.assertEquals(54321.0, r.getLower(), 0.001);
                        Assert.assertEquals(55432.1, r.getUpper(), 0.001);
                    }
                }
            }
            
            // test invalid
            for (String tp : testParams)
            {
                for (String tv : invalidValues)
                {
                    List<String> vals = new ArrayList<String>();
                    vals.add(tv);
                    params.put(tp, vals);
                    try
                    {
                        List<Range<Double>> ranges = sia.validateTIME(params);
                        Assert.fail("expected IllegalArgumentException, got: " + ranges.size() + " Range(s)");
                    }
                    catch(IllegalArgumentException expected)
                    {
                        log.debug("caught expected: " + expected);
                    }
                }
            }
            
            // test multiple values
            params.clear();
            List<String> vals = new ArrayList<String>();
            for (int i=0; i<3; i++)
            {
                vals.add(testValues[i]);
            }
            params.put("TIME", vals);
            List<Range<Double>> times = sia.validateTIME(params);
            Assert.assertNotNull(times);
            Assert.assertEquals(3, times.size());
            for (Range<Double> r : times)
            {
                Assert.assertEquals(54321.0, r.getLower(), 0.001);
                Assert.assertEquals(55432.1, r.getUpper(), 0.001);
            } 
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testValidatePOL()
    {
        String[] testParams = new String[] { "POL", "pol", "PoL" };
        String[] testValues = new String[]
        {
            "I", "Q", "U", "V", "RR", "LL", "RL", "LR", "XX", "YY", "XY", "YX"
        };
        int[] len = new int[] { 1, 4, testValues.length };
        try
        {
            List empty = sia.validatePOL(null); // null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());
            
            Map<String,List<String>> params = new TreeMap<String,List<String>>(new CaseInsensitiveStringComparator());
            for (String tp : testParams)
            {
                for (int i=0; i<len.length; i++)
                {
                    List<String> vals = new ArrayList<String>();
                    for (int j=0; j<len[i]; j++)
                    {
                        vals.add(testValues[j]);
                    }
                    params.put(tp, vals);
                    List<String> pols = sia.validatePOL(params);
                    Assert.assertNotNull(pols);
                    Assert.assertEquals(len[i], pols.size());
                }
            }
            
            // test invalid value
            List<String> vals = new ArrayList<String>();
            vals.add("FOO");
            params.clear();
            params.put("POL", vals);
            try
            {
                List<String> pols = sia.validatePOL(params);
                Assert.fail("expected IllegalArgumentException,. got: " + pols.size() + " String(s)");
            }
            catch(IllegalArgumentException expected)
            {
                log.debug("caught expected: " + expected);
            }
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testValidateFOV()
    {
        
        String[] testParams = new String[] { "FOV", "fov", "FoV" };
        
        try
        {
            List empty = sia.validateFOV(null); // compile and null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());
            
            Method m = SiaValidator.class.getMethod("validateFOV", Map.class);
            doValidateNumeric(m, "FOV", testParams);
            
            // invalid: code is more or less tested already in testValidateBand
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testValidateSPATRES()
    {
        
        String[] testParams = new String[] { "SPATRES", "spatres", "SpAtReS" };
        
        try
        {
            List empty = sia.validateSPATRES(null); // compile and null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());
            
            Method m = SiaValidator.class.getMethod("validateSPATRES", Map.class);
            doValidateNumeric(m, "SPATRES", testParams);
            
            // invalid: code is more or less tested already in testValidateBand
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
    @Test
    public void testValidateEXPTIME()
    {
        
        String[] testParams = new String[] { "EXPTIME", "exptime", "ExPtImE" };
        
        try
        {
            List empty = sia.validateEXPTIME(null); // compile and null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());
            
            Method m = SiaValidator.class.getMethod("validateEXPTIME", Map.class);
            doValidateNumeric(m, "EXPTIME", testParams);
            
            // invalid: code is more or less tested already in testValidateBand
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateID()
    {
        try
        {
            String[] testParams = new String[] { "ID", "id", "Id" };

            // null arg check
            List<String> empty = sia.validateID(null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method method = SiaValidator.class.getMethod("validateID", Map.class);
            doValidateString(method, testParams, null);
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateCOLLECTION()
    {
        try
        {
            String[] testParams = new String[] { "COLLECTION", "collection", "CoLlEcTiOn" };

            // null arg check
            List<String> empty = sia.validateCOLLECTION(null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method method = SiaValidator.class.getMethod("validateCOLLECTION", Map.class);
            doValidateString(method, testParams, null);
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateFACILITY()
    {
        try
        {
            String[] testParams = new String[] { "FACILITY", "facility", "FaCiLiTy" };

            // null arg check
            List<String> empty = sia.validateFACILITY(null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method method = SiaValidator.class.getMethod("validateFACILITY", Map.class);
            doValidateString(method, testParams, null);
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateINSTRUMENT()
    {
        try
        {
            String[] testParams = new String[] { "INSTRUMENT", "instrument", "InStRuMeNt" };

            // null arg check
            List<String> empty = sia.validateINSTRUMENT(null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method method = SiaValidator.class.getMethod("validateINSTRUMENT", Map.class);
            doValidateString(method, testParams, null);
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateDPTYPE()
    {
        try
        {
            String[] testParams = new String[] { "DPTYPE", "dptype", "DpTyPe" };
            String[] testValues = new String[] { "cube", "image" };

            // null arg check
            List<String> empty = sia.validateDPTYPE(null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method method = SiaValidator.class.getMethod("validateDPTYPE", Map.class);
            doValidateString(method, testParams, testValues);

            // test invalid value
            Map<String, List<String>> params = new TreeMap<String, List<String>>(new CaseInsensitiveStringComparator());
            List<String> vals = new ArrayList<String>();
            vals.add("FOO");
            params.clear();
            params.put(testParams[0], vals);
            try
            {
                List<String> ret = sia.validateDPTYPE(params);
                Assert.fail("expected IllegalArgumentException,. got: " + ret.size() + " String(s)");
            }
            catch (IllegalArgumentException expected)
            {
                log.debug("caught expected: " + expected);
            }
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateCALIB()
    {
        try
        {
            String[] testParams = new String[] { "CALIB", "calib", "CaLiB" };

            List empty = sia.validateCALIB(null); // compile and null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = SiaValidator.class.getMethod("validateCALIB", Map.class);
            doValidateInteger(m, "CALIB", testParams);
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateTARGET()
    {
        try
        {
            String[] testParams = new String[] { "TARGET", "target", "TaRgEt" };

            // null arg check
            List<String> empty = sia.validateTARGET(null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method method = SiaValidator.class.getMethod("validateTARGET", Map.class);
            doValidateString(method, testParams, null);
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateTIMERES()
    {
        try
        {
            String[] testParams = new String[] { "TIMERES", "timeres", "TiMeReS" };

            List empty = sia.validateTIMERES(null); // compile and null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = SiaValidator.class.getMethod("validateTIMERES", Map.class);
            doValidateNumeric(m, "TIMERES", testParams);
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateSPECRP()
    {
        try
        {
            String[] testParams = new String[] { "SPECRP", "specrp", "SpEcRp" };

            List empty = sia.validateSPECRP(null); // compile and null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = SiaValidator.class.getMethod("validateSPECRP", Map.class);
            doValidateNumeric(m, "SPECRP", testParams);
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateFORMAT()
    {
        try
        {
            String[] testParams = new String[] { "FORMAT", "format", "FoRmAt" };

            // null arg check
            List<String> empty = sia.validateFORMAT(null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method method = SiaValidator.class.getMethod("validateFORMAT", Map.class);
            doValidateString(method, testParams, null);
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateCustomParam()
    {
        try
        {
            List empty = sia.validateString("FOO", null, null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());
            
            Map<String,List<String>> params = new TreeMap<String,List<String>>(new CaseInsensitiveStringComparator());
            List<String> vals = new ArrayList<String>();
            vals.add("abc");
            params.put("FOO", vals);
            List<String> strs = sia.validateString("FOO", params, null);
            Assert.assertNotNull(strs);
            Assert.assertEquals(1, strs.size());
            String s = strs.get(0);
            Assert.assertEquals("abc", s);
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    public void doValidateString(Method testMethod, String[] testParams, String[] testValues)
        throws Exception
    {
        if (testValues == null)
        {
            testValues = new String[]
            {
                "12345",
                "ABCDEF",
                "abcdef",
                "a1b2c3",
                "A1b2C3"
            };
        }

        int[] len = new int[] {1, testValues.length/2, testValues.length};
        Map<String, List<String>> params = new TreeMap<String, List<String>>(new CaseInsensitiveStringComparator());
        for (String tp : testParams)
        {
            for (int i = 0; i < len.length; i++)
            {
                List<String> vals = new ArrayList<String>();
                for (int j = 0; j < len[i]; j++)
                {
                    vals.add(testValues[j]);
                }
                params.put(tp, vals);
                List<String> pols = (List<String>) testMethod.invoke(sia, params);
                Assert.assertNotNull(pols);
                Assert.assertEquals(len[i], pols.size());
            }
        }
    }

    public void doValidateInteger(Method m, String paramName, String[] testParams)
    {
        String[] testValues = new String[]
        {
            "0",
            "1",
            "2",
            "666"
        };
        try
        {
            Map<String,List<String>> params = new TreeMap<String,List<String>>(new CaseInsensitiveStringComparator());

            for (String tp : testParams)
            {
                for (String tv : testValues)
                {
                    List<String> expected = new ArrayList<String>();
                    expected.add(tv);
                    params.put(tp, expected);

                    List<Integer> actual = (List<Integer>) m.invoke(sia, params);

                    Assert.assertNotNull(actual);
                    Assert.assertEquals(1, actual.size());
                    Integer i = actual.get(0);
                    Assert.assertEquals(tv, i.toString());
                }
            }

            // test multiple values
            params.clear();
            List<String> expected = new ArrayList<String>();
            for (String e : testValues)
            {
                expected.add(e);
            }
            params.put(paramName, expected);
            List<Integer> actual = (List<Integer>) m.invoke(sia, params);
            Assert.assertNotNull(actual);
            Assert.assertEquals(expected.size(), actual.size());
            for (String e : expected)
            {
                Integer ei = new Integer(e);
                Assert.assertTrue(actual.contains(ei));
            }
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    public void doValidateNumeric(Method m, String paramName, String[] testParams)
    {
        String LB = "12.3 +Inf";
        String UB = "-Inf 34.5";
        String OPEN = "-Inf +Inf";
        String SCALAR = "1.0 1.0";
        String[] testValues = new String[]
        {
            "12.3 34.5",
            "1.23e1 3.45e1",
            "1.23E1 3.45E1",
            SCALAR,
            LB,
            UB,
            OPEN
        };
        try
        {
            Map<String,List<String>> params = new TreeMap<String,List<String>>(new CaseInsensitiveStringComparator());
            
            for (String tp : testParams)
            {
                for (String tv : testValues)
                {
                    List<String> vals = new ArrayList<String>();
                    vals.add(tv);
                    params.put(tp, vals);
                    
                    List<Range<Double>> ranges = (List<Range<Double>>) m.invoke(sia, params);
                    
                    Assert.assertNotNull(ranges);
                    Assert.assertEquals(1, ranges.size());
                    Range<Double> r = ranges.get(0);
                    
                    if (tv == SCALAR)
                    {
                        Assert.assertEquals(1.0, r.getLower(), 0.001);
                        Assert.assertEquals(1.0, r.getUpper(), 0.001);
                    }
                    else if (tv == LB)
                    {
                        Assert.assertEquals(12.3, r.getLower(), 0.001);
                        Assert.assertNull(r.getUpper());
                    }
                    else if (tv == UB)
                    {
                        Assert.assertNull(r.getLower());
                        Assert.assertEquals(34.5, r.getUpper(), 0.001);
                    }
                    else if (tv == OPEN)
                    {
                        Assert.assertNull(r.getLower());
                        Assert.assertNull(r.getUpper());
                    }
                    else
                    {
                        Assert.assertEquals(12.3, r.getLower(), 0.001);
                        Assert.assertEquals(34.5, r.getUpper(), 0.001);
                    }
                }
            }
            
            // test multiple values
            params.clear();
            List<String> vals = new ArrayList<String>();
            for (int i=0; i<3; i++)
            {
                vals.add(testValues[i]);
            }
            params.put(paramName, vals);
            List<Range<Double>> ranges = (List<Range<Double>>) m.invoke(sia, params);
            Assert.assertNotNull(ranges);
            Assert.assertEquals(3, ranges.size());
            for (Range<Double> r : ranges)
            {
                Assert.assertEquals(12.3, r.getLower(), 0.001);
                Assert.assertEquals(34.5, r.getUpper(), 0.001);
            } 
        }
        catch (Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }
    
}
