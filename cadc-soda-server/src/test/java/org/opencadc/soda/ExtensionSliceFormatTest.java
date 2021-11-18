/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2021.                            (c) 2021.
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

package org.opencadc.soda;

import ca.nrc.cadc.util.Log4jInit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author pdowler
 */
public class ExtensionSliceFormatTest {
    private static final Logger log = Logger.getLogger(ExtensionSliceFormatTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.soda", Level.INFO);
    }
    
    private final ExtensionSliceFormat fmt = new ExtensionSliceFormat();
    
    public ExtensionSliceFormatTest() { 
    }
    
    @Test
    public void testNull() throws Exception {
        ExtensionSlice e = fmt.parse(null);
        Assert.assertNull(e);
        
        e = fmt.parse("");
        Assert.assertNull(e);
        
        String result = fmt.format(null);
        Assert.assertTrue(result.isEmpty());
    }
    
    @Test
    public void testExtensionIndex() {
        ExtensionSlice expected = new ExtensionSlice(2);
        String str = fmt.format(expected);
        log.info("formatted: " + str);
        
        ExtensionSlice actual = fmt.parse(str);
        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.extensionIndex);
        Assert.assertEquals(expected.extensionIndex, actual.extensionIndex);
        
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testExtensionName() {
        ExtensionSlice expected = new ExtensionSlice("foo");
        String str = fmt.format(expected);
        log.info("formatted: " + str);
        
        ExtensionSlice actual = fmt.parse(str);
        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.extensionName);
        Assert.assertEquals(expected.extensionName, actual.extensionName);
        
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testExtensionNameVersion() {
        ExtensionSlice expected = new ExtensionSlice("foo", 2);
        String str = fmt.format(expected);
        log.info("formatted: " + str);
        
        ExtensionSlice actual = fmt.parse(str);
        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.extensionName);
        Assert.assertNotNull(actual.extensionVersion);
        Assert.assertEquals(expected.extensionName, actual.extensionName);
        Assert.assertEquals(expected.extensionVersion, actual.extensionVersion);
        
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testNameVersionCut2D() {
        ExtensionSlice expected = new ExtensionSlice("foo", 2);
        expected.getPixelRanges().add(new PixelRange(0, 100));
        expected.getPixelRanges().add(new PixelRange(200, 300));
        String str = fmt.format(expected);
        log.info("formatted: " + str);
        
        ExtensionSlice actual = fmt.parse(str);
        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.extensionName);
        Assert.assertNotNull(actual.extensionVersion);
        Assert.assertEquals(expected.extensionName, actual.extensionName);
        Assert.assertEquals(expected.extensionVersion, actual.extensionVersion);
        
        Assert.assertEquals(expected.getPixelRanges(), actual.getPixelRanges());
    }
    
    @Test
    public void testNameVersionCutStep2D() {
        ExtensionSlice expected = new ExtensionSlice("foo", 2);
        expected.getPixelRanges().add(new PixelRange(0, 100, 2));
        expected.getPixelRanges().add(new PixelRange(200, 300, 4));
        String str = fmt.format(expected);
        log.info("formatted: " + str);
        
        ExtensionSlice actual = fmt.parse(str);
        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.extensionName);
        Assert.assertNotNull(actual.extensionVersion);
        Assert.assertEquals(expected.extensionName, actual.extensionName);
        Assert.assertEquals(expected.extensionVersion, actual.extensionVersion);
        
        Assert.assertEquals(expected.getPixelRanges(), actual.getPixelRanges());
    }
    
    @Test
    public void testNameVersionExplicitAll2D() {
        ExtensionSlice expected = new ExtensionSlice("foo", 2);
        expected.getPixelRanges().add(new PixelRange(0, Integer.MAX_VALUE));
        expected.getPixelRanges().add(new PixelRange(0, Integer.MAX_VALUE));
        String str = fmt.format(expected);
        log.info("formatted: " + str);
        
        ExtensionSlice actual = fmt.parse(str);
        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.extensionName);
        Assert.assertNotNull(actual.extensionVersion);
        Assert.assertEquals(expected.extensionName, actual.extensionName);
        Assert.assertEquals(expected.extensionVersion, actual.extensionVersion);
        
        Assert.assertEquals(expected.getPixelRanges(), actual.getPixelRanges());
    }
    
    @Test
    public void testNameVersionExplicitAllStep2D() {
        ExtensionSlice expected = new ExtensionSlice("foo", 2);
        expected.getPixelRanges().add(new PixelRange(0, Integer.MAX_VALUE, 2));
        expected.getPixelRanges().add(new PixelRange(0, Integer.MAX_VALUE, 3
        ));
        String str = fmt.format(expected);
        log.info("formatted: " + str);
        
        ExtensionSlice actual = fmt.parse(str);
        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.extensionName);
        Assert.assertNotNull(actual.extensionVersion);
        Assert.assertEquals(expected.extensionName, actual.extensionName);
        Assert.assertEquals(expected.extensionVersion, actual.extensionVersion);
        
        Assert.assertEquals(expected.getPixelRanges(), actual.getPixelRanges());
    }
    
    @Test
    public void testNameVersionFlip() {
        ExtensionSlice expected = new ExtensionSlice("foo", 2);
        expected.getPixelRanges().add(new PixelRange(100, 0));
        String str = fmt.format(expected);
        log.info("formatted: " + str);
        
        ExtensionSlice actual = fmt.parse(str);
        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.extensionName);
        Assert.assertNotNull(actual.extensionVersion);
        Assert.assertEquals(expected.extensionName, actual.extensionName);
        Assert.assertEquals(expected.extensionVersion, actual.extensionVersion);
        
        Assert.assertEquals(expected.getPixelRanges(), actual.getPixelRanges());
    }
    
    @Test
    public void testNameVersionAllFlip() {
        ExtensionSlice expected = new ExtensionSlice("foo", 2);
        expected.getPixelRanges().add(new PixelRange(Integer.MAX_VALUE, 0));
        String str = fmt.format(expected);
        log.info("formatted: " + str);
        
        ExtensionSlice actual = fmt.parse(str);
        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.extensionName);
        Assert.assertNotNull(actual.extensionVersion);
        Assert.assertEquals(expected.extensionName, actual.extensionName);
        Assert.assertEquals(expected.extensionVersion, actual.extensionVersion);
        
        Assert.assertEquals(expected.getPixelRanges(), actual.getPixelRanges());
    }
    
    @Test
    public void testNameVersionDefaultPrimary() {
        ExtensionSlice expected = new ExtensionSlice(0);
        expected.getPixelRanges().add(new PixelRange(0, 100));
        expected.getPixelRanges().add(new PixelRange(200, 300));
        String str = fmt.format(expected);
        log.info("formatted: " + str);
        
        ExtensionSlice actual = fmt.parse(str);
        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.extensionIndex);
        Assert.assertEquals(expected.extensionIndex, actual.extensionIndex);
        Assert.assertEquals(expected.getPixelRanges(), actual.getPixelRanges());
        
        // default: primary
        str = "[0:100, 200:300]";
        log.info("default primary: " + str);
        actual = fmt.parse(str);
        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.extensionIndex);
        Assert.assertEquals(expected.extensionIndex, actual.extensionIndex);
        Assert.assertEquals(expected.getPixelRanges(), actual.getPixelRanges());
    }
    
    @Test
    public void testNameVersionSinglePixShortcut() {
        ExtensionSlice expected = new ExtensionSlice("foo", 2);
        expected.getPixelRanges().add(new PixelRange(100, 100));
        String str = fmt.format(expected);
        log.info("formatted: " + str);
        
        ExtensionSlice actual = fmt.parse(str);
        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.extensionName);
        Assert.assertNotNull(actual.extensionVersion);
        Assert.assertEquals(expected.extensionName, actual.extensionName);
        Assert.assertEquals(expected.extensionVersion, actual.extensionVersion);
        
        Assert.assertEquals(expected.getPixelRanges(), actual.getPixelRanges());
        
        // now the shortcut
        str = "[foo,2][100]";
        log.info("shortcut: " + str);
        
        actual = fmt.parse(str);
        Assert.assertNotNull(actual);
        Assert.assertNotNull(actual.extensionName);
        Assert.assertNotNull(actual.extensionVersion);
        Assert.assertEquals(expected.extensionName, actual.extensionName);
        Assert.assertEquals(expected.extensionVersion, actual.extensionVersion);
        
        Assert.assertEquals(expected.getPixelRanges(), actual.getPixelRanges());
    }
}
