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

import org.opencadc.dap.DapParamValidator;
import ca.nrc.cadc.dali.DoubleInterval;
import ca.nrc.cadc.util.CaseInsensitiveStringComparator;
import ca.nrc.cadc.util.Log4jInit;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
public class DapParamValidatorTest {

    private static final Logger log = Logger.getLogger(DapParamValidatorTest.class);

    static {
        Log4jInit.setLevel("ca.nrc.cadc.sia2", Level.INFO);
    }

    DapParamValidator dapParamValidator = new DapParamValidator(false); // default: DAP mode

    @Test
    public void testValidateFOV() {

        String[] testParams = new String[]{"FOV", "fov", "FoV"};

        try {
            List empty = dapParamValidator.validateFOV(null); // compile and null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = DapParamValidator.class.getMethod("validateFOV", Map.class);
            doValidateNumeric(dapParamValidator, m, "FOV", testParams);

            // invalid: code is more or less tested already in testValidateBand
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateSPATRES() {

        String[] testParams = new String[]{"SPATRES", "spatres", "SpAtReS"};

        try {
            List empty = dapParamValidator.validateSPATRES(null); // compile and null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = DapParamValidator.class.getMethod("validateSPATRES", Map.class);
            doValidateNumeric(dapParamValidator, m, "SPATRES", testParams);

            // invalid: code is more or less tested already in testValidateBand
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateEXPTIME() {

        String[] testParams = new String[]{"EXPTIME", "exptime", "ExPtImE"};

        try {
            List empty = dapParamValidator.validateEXPTIME(null); // compile and null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = DapParamValidator.class.getMethod("validateEXPTIME", Map.class);
            doValidateNumeric(dapParamValidator, m, "EXPTIME", testParams);

            // invalid: code is more or less tested already in testValidateBand
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateCOLLECTION() {
        try {
            String[] testParams = new String[]{"COLLECTION", "collection", "CoLlEcTiOn"};

            // null arg check
            List<String> empty = dapParamValidator.validateCOLLECTION(null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = DapParamValidator.class.getMethod("validateCOLLECTION", Map.class);
            doValidateString(dapParamValidator, m, testParams, null);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateFACILITY() {
        try {
            String[] testParams = new String[]{"FACILITY", "facility", "FaCiLiTy"};

            // null arg check
            List<String> empty = dapParamValidator.validateFACILITY(null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = DapParamValidator.class.getMethod("validateFACILITY", Map.class);
            doValidateString(dapParamValidator, m, testParams, null);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateINSTRUMENT() {
        try {
            String[] testParams = new String[]{"INSTRUMENT", "instrument", "InStRuMeNt"};

            // null arg check
            List<String> empty = dapParamValidator.validateINSTRUMENT(null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = DapParamValidator.class.getMethod("validateINSTRUMENT", Map.class);
            doValidateString(dapParamValidator, m, testParams, null);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateDPTYPE() {
        // test invalid value: SIAv2 mode
        DapParamValidator sia = new DapParamValidator(true);
        
        try {
            String[] testParams = new String[]{"DPTYPE", "dptype", "DpTyPe"};
            String[] testValues = new String[]{"cube", "image"};

            // null arg check
            List<String> empty = dapParamValidator.validateDPTYPE(null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = DapParamValidator.class.getMethod("validateDPTYPE", Map.class);
            doValidateString(sia, m, testParams, testValues);
            
            Map<String, List<String>> params = new TreeMap<String, List<String>>(new CaseInsensitiveStringComparator());
            List<String> vals = new ArrayList<String>();
            vals.add("FOO");
            params.clear();
            params.put(testParams[0], vals);
            try {
                List<String> ret = sia.validateDPTYPE(params);
                Assert.fail("expected IllegalArgumentException,. got: " + ret.size() + " String(s)");
            } catch (IllegalArgumentException expected) {
                log.debug("caught expected: " + expected);
            }
            
            // DAP validator: no invalid values
            testValues = new String[] {"cube", "image", "spectrum", "timeseries", "something-else" };
            doValidateString(dapParamValidator, m, testParams, testValues);
            
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateCALIB() {
        try {
            String[] testParams = new String[]{"CALIB", "calib", "CaLiB"};

            List empty = dapParamValidator.validateCALIB(null); // compile and null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = DapParamValidator.class.getMethod("validateCALIB", Map.class);
            doValidateInteger(dapParamValidator, m, "CALIB", testParams);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateTARGET() {
        try {
            String[] testParams = new String[]{"TARGET", "target", "TaRgEt"};

            // null arg check
            List<String> empty = dapParamValidator.validateTARGET(null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = DapParamValidator.class.getMethod("validateTARGET", Map.class);
            doValidateString(dapParamValidator, m, testParams, null);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateTIMERES() {
        try {
            String[] testParams = new String[]{"TIMERES", "timeres", "TiMeReS"};

            List empty = dapParamValidator.validateTIMERES(null); // compile and null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = DapParamValidator.class.getMethod("validateTIMERES", Map.class);
            doValidateNumeric(dapParamValidator, m, "TIMERES", testParams);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateSPECRP() {
        try {
            String[] testParams = new String[]{"SPECRP", "specrp", "SpEcRp"};

            List empty = dapParamValidator.validateSPECRP(null); // compile and null arg check
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = DapParamValidator.class.getMethod("validateSPECRP", Map.class);
            doValidateNumeric(dapParamValidator, m, "SPECRP", testParams);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateFORMAT() {
        try {
            String[] testParams = new String[]{"FORMAT", "format", "FoRmAt"};

            // null arg check
            List<String> empty = dapParamValidator.validateFORMAT(null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Method m = DapParamValidator.class.getMethod("validateFORMAT", Map.class);
            doValidateString(dapParamValidator, m, testParams, null);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testValidateCustomParam() {
        try {
            List empty = dapParamValidator.validateString("FOO", null, null);
            Assert.assertNotNull(empty);
            Assert.assertTrue(empty.isEmpty());

            Map<String, List<String>> params = new TreeMap<String, List<String>>(new CaseInsensitiveStringComparator());
            List<String> vals = new ArrayList<String>();
            vals.add("abc");
            params.put("FOO", vals);
            List<String> strs = dapParamValidator.validateString("FOO", params, null);
            Assert.assertNotNull(strs);
            Assert.assertEquals(1, strs.size());
            String s = strs.get(0);
            Assert.assertEquals("abc", s);
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    public void doValidateString(DapParamValidator instance, Method testMethod, String[] testParams, String[] testValues)
            throws Exception {
        if (testValues == null) {
            testValues = new String[]{
                "12345",
                "ABCDEF",
                "abcdef",
                "a1b2c3",
                "A1b2C3"
            };
        }

        int[] len = new int[]{1, testValues.length / 2, testValues.length};
        Map<String, List<String>> params = new TreeMap<String, List<String>>(new CaseInsensitiveStringComparator());
        for (String tp : testParams) {
            for (int i = 0; i < len.length; i++) {
                List<String> vals = new ArrayList<String>();
                for (int j = 0; j < len[i]; j++) {
                    vals.add(testValues[j]);
                }
                params.put(tp, vals);
                List<String> pols = (List<String>) testMethod.invoke(instance, params);
                Assert.assertNotNull(pols);
                Assert.assertEquals(len[i], pols.size());
            }
        }
    }

    public void doValidateInteger(DapParamValidator instance, Method m, String paramName, String[] testParams) {
        String[] testValues = new String[]{
            "0",
            "1",
            "2",
            "666"
        };
        try {
            Map<String, List<String>> params = new TreeMap<String, List<String>>(new CaseInsensitiveStringComparator());

            for (String tp : testParams) {
                for (String tv : testValues) {
                    List<String> expected = new ArrayList<String>();
                    expected.add(tv);
                    params.put(tp, expected);

                    List<Integer> actual = (List<Integer>) m.invoke(instance, params);

                    Assert.assertNotNull(actual);
                    Assert.assertEquals(1, actual.size());
                    Integer i = actual.get(0);
                    Assert.assertEquals(tv, i.toString());
                }
            }

            // test multiple values
            params.clear();
            List<String> expected = new ArrayList<String>();
            for (String e : testValues) {
                expected.add(e);
            }
            params.put(paramName, expected);
            List<Integer> actual = (List<Integer>) m.invoke(instance, params);
            Assert.assertNotNull(actual);
            Assert.assertEquals(expected.size(), actual.size());
            for (String e : expected) {
                Integer ei = new Integer(e);
                Assert.assertTrue(actual.contains(ei));
            }
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    public void doValidateNumeric(DapParamValidator instance, Method m, String paramName, String[] testParams) {
        String LB = "12.3 +Inf";
        String UB = "-Inf 34.5";
        String OPEN = "-Inf +Inf";
        String SCALAR = "1.0 1.0";
        String[] testValues = new String[]{
            "12.3 34.5",
            "1.23e1 3.45e1",
            "1.23E1 3.45E1",
            SCALAR,
            LB,
            UB,
            OPEN
        };
        try {
            Map<String, List<String>> params = new TreeMap<String, List<String>>(new CaseInsensitiveStringComparator());

            for (String tp : testParams) {
                for (String tv : testValues) {
                    List<String> vals = new ArrayList<String>();
                    vals.add(tv);
                    params.put(tp, vals);

                    List<DoubleInterval> ranges = (List<DoubleInterval>) m.invoke(instance, params);

                    Assert.assertNotNull(ranges);
                    Assert.assertEquals(1, ranges.size());
                    DoubleInterval r = ranges.get(0);

                    if (tv == SCALAR) {
                        Assert.assertEquals(1.0, r.getLower(), 0.001);
                        Assert.assertEquals(1.0, r.getUpper(), 0.001);
                    } else if (tv == LB) {
                        Assert.assertEquals(12.3, r.getLower(), 0.001);
                        Assert.assertTrue(r.getUpper().isInfinite());
                    } else if (tv == UB) {
                        Assert.assertTrue(r.getLower().isInfinite());
                        Assert.assertEquals(34.5, r.getUpper(), 0.001);
                    } else if (tv == OPEN) {
                        Assert.assertTrue(r.getLower().isInfinite());
                        Assert.assertTrue(r.getUpper().isInfinite());
                    } else {
                        Assert.assertEquals(12.3, r.getLower(), 0.001);
                        Assert.assertEquals(34.5, r.getUpper(), 0.001);
                    }
                }
            }

            // test multiple values
            params.clear();
            List<String> vals = new ArrayList<String>();
            for (int i = 0; i < 3; i++) {
                vals.add(testValues[i]);
            }
            params.put(paramName, vals);
            List<DoubleInterval> ranges = (List<DoubleInterval>) m.invoke(instance, params);
            Assert.assertNotNull(ranges);
            Assert.assertEquals(3, ranges.size());
            for (DoubleInterval r : ranges) {
                Assert.assertEquals(12.3, r.getLower(), 0.001);
                Assert.assertEquals(34.5, r.getUpper(), 0.001);
            }
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

}
