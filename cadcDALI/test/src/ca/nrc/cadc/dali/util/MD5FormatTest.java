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

package ca.nrc.cadc.dali.util;


import ca.nrc.cadc.util.HexUtil;
import ca.nrc.cadc.util.Log4jInit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author pdowler
 */
public class MD5FormatTest 
{
    private static final Logger log = Logger.getLogger(MD5FormatTest.class);
    static
    {
        Log4jInit.setLevel("ca", Level.INFO);
    }
    
    public MD5FormatTest() { }
    
    /**
     * Test of format and parse method, of class ByteArrayFormat.
     */
    @Test
    public void testValue()
    {
        log.debug("testValue");
        try
        {
            MD5Format format = new MD5Format();
            byte[] expected = new byte[] { 1,2,3,4,5,6,7,8,8,7,6,5,4,3,2,1 };
            assertEquals("setup", 16, expected.length);

            String result = format.format(expected);
            byte[] actual = format.parse(result);

            assertEquals(expected.length, actual.length);
            for (int i = 0; i < expected.length; i++)
            {
                assertEquals(expected[i], actual[i]);
            }

            log.info("testValue passed");
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testInvalidByteArray() throws Exception
    {
        log.debug("testInvalidByteArray");

        MD5Format format = new MD5Format();
        
        byte[] tooShort = new byte[] { 1,2,3,4,5,6,7,8,8,7,6,5,4,3,2 };
        byte[] tooLong = new byte[] { 1,2,3,4,5,6,7,8,8,7,6,5,4,3,2,1,0 };

        try { format.format(tooShort); }
        catch(IllegalArgumentException expected) { }
        
        try { format.format(tooLong); }
        catch(IllegalArgumentException expected) { }
    }
    
    @Test
    public void testInvalidHex() throws Exception
    {
        log.debug("testInvalidHex");

        MD5Format format = new MD5Format();
        byte[] tooShort = new byte[] { 1,2,3,4,5,6,7,8,8,7,6,5,4,3,2 };
        byte[] tooLong = new byte[] { 1,2,3,4,5,6,7,8,8,7,6,5,4,3,2,1,0 };
        
        String tooShortMD5 = HexUtil.toHex(tooShort);
        assertEquals("setup", 30, tooShortMD5.length());
        
        String tooLongMD5 = HexUtil.toHex(tooLong);
        assertEquals("setup", 34, tooLongMD5.length());
        
        try { format.parse(tooShortMD5); }
        catch(IllegalArgumentException expected) { }
        
        try { format.parse(tooShortMD5); }
        catch(IllegalArgumentException expected) { }
    }
    
    @Test
    public void testNull() throws Exception
    {
        log.debug("testNull");

        MD5Format format = new MD5Format();

        String s = format.format(null);
        assertEquals("", s);

        byte[] object = format.parse(null);
        assertNull(object);

        log.info("testNull passed");
    }
}
