/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2017.                            (c) 2017.
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

package ca.nrc.cadc.dali.util;

import ca.nrc.cadc.dali.DoubleInterval;
import ca.nrc.cadc.dali.Interval;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author pdowler
 */
public class DoubleIntervalArrayFormatTest {

    private static final Logger log = Logger.getLogger(DoubleIntervalArrayFormatTest.class);

    public DoubleIntervalArrayFormatTest() {
    }

    @Test
    public void testValue() {
        log.debug("testValue");
        DoubleIntervalArrayFormat format = new DoubleIntervalArrayFormat();

        try {
            Interval<Double>[] expected = new Interval[]{
                new DoubleInterval(1.0, 2.0),
                new DoubleInterval(3.0, 4.0),
                new DoubleInterval(5.0, 6.0)
            };

            String result = format.format(expected);
            Assert.assertEquals("no extra whitespace", result.trim(), result);
            Interval<Double>[] actual = format.parse(result);

            Assert.assertEquals(expected.length, actual.length);
            for (int i = 0; i < expected.length; i++) {
                Assert.assertEquals(expected[i], actual[i]);
            }
        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testInvalidStringRep() throws Exception {
        log.debug("testInvalidStringRep");

        DoubleIntervalArrayFormat format = new DoubleIntervalArrayFormat();

        String tooShort = "1.0";
        String oddLength = "1.0 2.0 3.0 4.0 5.0";

        try {
            format.parse(tooShort);
        } catch (IllegalArgumentException expected) {
        }

        try {
            format.parse(oddLength);
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testNull() throws Exception {
        log.debug("testNull");

        DoubleIntervalArrayFormat format = new DoubleIntervalArrayFormat();

        String s = format.format(null);
        Assert.assertEquals("", s);

        Interval<Double>[] object = format.parse(null);
        Assert.assertNull(object);
    }
}
