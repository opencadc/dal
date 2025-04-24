/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2025.                            (c) 2025.
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
 *  : 5 $
 *
 ************************************************************************
 */

package org.opencadc.datalink;

import ca.nrc.cadc.util.FileUtil;
import java.io.File;
import java.nio.file.Files;
import org.junit.Assert;
import org.junit.Test;

public class ServiceDescriptorTemplateTest {

    @Test
    public void testInvalidNameArgument() throws Exception {

        File testFile = FileUtil.getFileFromResource("valid-template.xml", ServiceDescriptorTemplateTest.class);
        String template = Files.readString(testFile.toPath());

        try {
            new ServiceDescriptorTemplate(null, "owner", template);
             Assert.fail("Expected IllegalArgumentException for null name");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("name cannot be null"));
        }

        try {
            new ServiceDescriptorTemplate("", "owner", template);
            Assert.fail("Expected IllegalArgumentException for empty name");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("name cannot be null"));
        }

        try {
            new ServiceDescriptorTemplate("a-1?", "owner", template);
            Assert.fail("Expected IllegalArgumentException for name with invalid character");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid descriptor name"));
        }
    }

    @Test
    public void testInvalidOwnerArgument() throws Exception {

        File testFile = FileUtil.getFileFromResource("valid-template.xml", ServiceDescriptorTemplateTest.class);
        String template = Files.readString(testFile.toPath());

        try {
            new ServiceDescriptorTemplate("name", null, template);
            Assert.fail("Expected IllegalArgumentException for null owner");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("owner cannot be null"));
        }

        try {
            new ServiceDescriptorTemplate("name", "", template);
            Assert.fail("Expected IllegalArgumentException for empty owner");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("owner cannot be null"));
        }

        try {
            new ServiceDescriptorTemplate("name", "a-1?", template);
            Assert.fail("Expected IllegalArgumentException for owner with invalid character");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Invalid descriptor owner"));
        }
    }

    @Test
    public void testInvalidTemplateArgument() {

        try {
            ServiceDescriptorTemplate template = new ServiceDescriptorTemplate("name", "owner", null);
            Assert.fail("Expected IllegalArgumentException for null template");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("template cannot be null"));
        }

        try {
            ServiceDescriptorTemplate template = new ServiceDescriptorTemplate("name", "owner", "");
            Assert.fail("Expected IllegalArgumentException for empty template");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("template cannot be null"));
        }

        try {
            ServiceDescriptorTemplate template = new ServiceDescriptorTemplate("name", "owner", "template");
            Assert.fail("Expected IllegalArgumentException for template not a votable");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Error reading VOTable"));
        }

        try {
            ServiceDescriptorTemplate template = new ServiceDescriptorTemplate("name", "owner", "<foo></foo>");
            Assert.fail("Expected IllegalArgumentException for template an empty votable");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("Error reading VOTable"));
        }

    }

    @Test
    public void testInvalidIdRefTemplate() throws Exception {

        File testFile = FileUtil.getFileFromResource("mismatched-id-ref-template.xml", ServiceDescriptorTemplateTest.class);
        String template = Files.readString(testFile.toPath());

        try {
            new ServiceDescriptorTemplate("name", "owner", template);
            Assert.fail("Expected IllegalArgumentException for template with mismatched ID and ref attributes");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("no ID/IDREF binding"));
        }
    }

    @Test
    public void testInvalidTemplate() throws Exception {

        File testFile = FileUtil.getFileFromResource("missing-meta-resource-template.xml", ServiceDescriptorTemplateTest.class);
        String template = Files.readString(testFile.toPath());

        try {
            new ServiceDescriptorTemplate("name", "owner", template);
            Assert.fail("Expected IllegalArgumentException for template with a RESOURCE missing type='meta'");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("attribute type = 'meta'"));
        }

        testFile = FileUtil.getFileFromResource("multiple-resource-template.xml", ServiceDescriptorTemplateTest.class);
        template = Files.readString(testFile.toPath());

        try {
            new ServiceDescriptorTemplate("name", "owner", template);
            Assert.fail("Expected IllegalArgumentException for template with multiple RESOURCE elements");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("expected a single RESOURCE element"));
        }
    }

}
