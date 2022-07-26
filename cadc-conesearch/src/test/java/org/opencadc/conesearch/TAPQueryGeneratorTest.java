/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2022.                            (c) 2022.
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
 *
 ************************************************************************
 */

package org.opencadc.conesearch;

import ca.nrc.cadc.dali.tables.votable.VOTableWriter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TAPQueryGeneratorTest {
    @Test
    public void testInvalidJobParameters() {
        final TAPQueryGenerator testSubject = new TAPQueryGenerator("badcat", "ra, dec",
                                                                    "obs_id, release_date, ra, dec", "*");
        final Map<String, List<String>> parameters = new HashMap<>();

        try {
            testSubject.getParameterMap("my_point", parameters);
            Assert.fail("Should throw IllegalArgumentException here.");
        } catch (IllegalArgumentException illegalArgumentException) {
            // Good
        }

        parameters.put("RA", Collections.singletonList("bogus"));
        parameters.put("DEC", Collections.singletonList("bogus"));
        parameters.put("SR", Collections.singletonList("bogus"));

        try {
            testSubject.getParameterMap("my_point_2", parameters);
            Assert.fail("Should throw IllegalArgumentException here.");
        } catch (IllegalArgumentException illegalArgumentException) {
            // Good
        }

        parameters.clear();
        parameters.put("RA", Collections.singletonList("12.3"));
        parameters.put("DEC", Collections.singletonList("45.6"));
        parameters.put("SR", Collections.singletonList("0.7"));
        parameters.put("VERB", Collections.singletonList("9"));

        try {
            testSubject.getParameterMap("my_point_3", parameters);
            Assert.fail("Should throw IllegalArgumentException here for VERB.");
        } catch (IllegalArgumentException illegalArgumentException) {
            // Good
        }

        parameters.clear();
        parameters.put("RA", Collections.singletonList("12.3"));
        parameters.put("DEC", Collections.singletonList("45.6"));
        parameters.put("SR", Collections.singletonList("0.7"));
        parameters.put("VERB", Collections.singletonList("bogus"));

        try {
            testSubject.getParameterMap("my_point_4", parameters);
            Assert.fail("Should throw IllegalArgumentException here for VERB.");
        } catch (IllegalArgumentException illegalArgumentException) {
            // Good
        }

        parameters.clear();
        parameters.put("RA", Collections.singletonList("12.3"));
        parameters.put("DEC", Collections.singletonList("108.6"));
        parameters.put("SR", Collections.singletonList("0.7"));
        parameters.put("VERB", Collections.singletonList("1"));

        try {
            testSubject.getParameterMap("my_point_5", parameters);
            Assert.fail("Should throw IllegalArgumentException here for invalid circle.");
        } catch (IllegalArgumentException illegalArgumentException) {
            // Good (invalid circle)
        }
    }

    @Test
    public void testValidJobParameters() {
        final TAPQueryGenerator testSubject = new TAPQueryGenerator("goodcat", "ra, dec",
                                                                    "obs_id, release_date, ra, dec", "*");
        final Map<String, List<String>> parameters = new HashMap<>();
        parameters.put("RA", Collections.singletonList("12.3"));
        parameters.put("DEC", Collections.singletonList("45.6"));
        parameters.put("SR", Collections.singletonList("0.7"));
        

        final Map<String, Object> tapQueryParameters1 = testSubject.getParameterMap("point_col",
                                                                                    parameters);
        Assert.assertEquals("Wrong response format.", VOTableWriter.CONTENT_TYPE,
                            tapQueryParameters1.get("RESPONSEFORMAT"));
        Assert.assertEquals("Wrong langauge", "ADQL", tapQueryParameters1.get("LANG"));
        Assert.assertEquals("Wrong query.",
                            "SELECT obs_id, release_date, ra, dec "
                            + "FROM goodcat WHERE 1 = CONTAINS(point_col, CIRCLE('ICRS', 12.3, 45.6, 0.7))",
                            tapQueryParameters1.get("QUERY"));
        Assert.assertEquals("Wrong max records", 1000, tapQueryParameters1.get("MAXREC"));

        // **** Next ****

        parameters.clear();
        parameters.put("RA", Collections.singletonList("12.3"));
        parameters.put("DEC", Collections.singletonList("45.6"));
        parameters.put("SR", Collections.singletonList("0.7"));
        parameters.put("RESPONSEFORMAT", Collections.singletonList("tsv"));
        parameters.put("MAXREC", Collections.singletonList("8000"));
        parameters.put("VERB", Collections.singletonList("3"));
        

        final Map<String, Object> tapQueryParameters2 = testSubject.getParameterMap("point_col_2",
                                                                                    parameters);
        Assert.assertEquals("Wrong response format.", "tsv", tapQueryParameters2.get("RESPONSEFORMAT"));
        Assert.assertEquals("Wrong langauge", "ADQL", tapQueryParameters2.get("LANG"));
        Assert.assertEquals("Wrong query.",
                            "SELECT * FROM goodcat WHERE 1 = CONTAINS(point_col_2, CIRCLE('ICRS', 12.3, 45.6, 0.7))",
                            tapQueryParameters2.get("QUERY"));
        Assert.assertEquals("Wrong max records", 8000, tapQueryParameters2.get("MAXREC"));
    }
}
