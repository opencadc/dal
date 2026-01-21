/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2009.                            (c) 2009.
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
*  $Revision: 4 $
*
************************************************************************
*/

package ca.nrc.cadc.dali.util;

import ca.nrc.cadc.date.DateUtil;
import ca.nrc.cadc.util.Log4jInit;

import java.text.DateFormat;
import java.util.Date;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jburke
 */
public class UTCTimestampFormatTest
{
    private static final Logger log = Logger.getLogger(UTCTimestampFormatTest.class);
    static
    {
        Log4jInit.setLevel("ca", Level.INFO);
    }

    public UTCTimestampFormatTest() { }

    @Test
    public void testValue() throws Exception
    {
        log.debug("testValue");
        try
        {
            UTCTimestampFormat format = new UTCTimestampFormat(null, null);
            Date expected = new Date();

            String result = format.format(expected);
            Assert.assertEquals("no extra whitespace", result.trim(), result);
            Date actual = format.parse(result);
            assertEquals(expected, actual);

            Date object = new java.sql.Date(expected.getTime());
            result = format.format(object);
            Assert.assertEquals("no extra whitespace", result.trim(), result);
            actual = format.parse(result);
            assertEquals(expected, actual);

            object = new java.sql.Timestamp(expected.getTime());
            result = format.format(object);
            Assert.assertEquals("no extra whitespace", result.trim(), result);
            actual = format.parse(result);
            assertEquals(expected, actual);

            log.info("testValue passed");
        }
        catch(Exception unexpected)
        {
            log.error("unexpected exception", unexpected);
            fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testNull() throws Exception
    {
        log.debug("testNull");

        UTCTimestampFormat format = new UTCTimestampFormat(null, null);
        
        String result = format.format(null);
        assertEquals("", result);

        Date object = format.parse(null);
        assertNull(object);

        log.info("testNull passed");
    }

    private static DateFormat dateFormat = DateUtil.getDateFormat(DateUtil.IVOA_DATE_FORMAT, DateUtil.UTC);
    @Test
    public void testDates() throws Exception {

        UTCTimestampFormat format23 = new UTCTimestampFormat(23, null);
        UTCTimestampFormat format19 = new UTCTimestampFormat(19, null);
        UTCTimestampFormat format10 = new UTCTimestampFormat(10, null);
        UTCTimestampFormat format11 = new UTCTimestampFormat(11, true);
        UTCTimestampFormat format21 = new UTCTimestampFormat(21, true);
        UTCTimestampFormat format = new UTCTimestampFormat(null, null);

        Date date = dateFormat.parse("2009-01-02T11:04:05.678");
        String formattedDate23 = format23.format(date);
        String formattedDate19 = format19.format(date);
        String formattedDate10 = format10.format(date);
        String formattedDate11 = format11.format(date);
        String formattedDate21 = format21.format(date);
        String formattedDate = format.format(date); // arraysize = *

        Assert.assertEquals("2009-01-02T11:04:05.678", formattedDate23);
        Assert.assertEquals("2009-01-02T11:04:05", formattedDate19);
        Assert.assertEquals("2009-01-02", formattedDate10);
        Assert.assertEquals("2009-01-02T11:04:05.678", formattedDate);
        Assert.assertEquals("2009-01-02", formattedDate11);
        Assert.assertEquals("2009-01-02T11:04:05", formattedDate21);

        Assert.assertThrows("Expected an exception. isValue has to be true for a non-standard arraysize", IllegalArgumentException.class, () -> {
            new UTCTimestampFormat(22, false);
        });
    }

}
