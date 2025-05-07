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

import ca.nrc.cadc.dali.tables.votable.VOTableDocument;
import ca.nrc.cadc.dali.tables.votable.VOTableReader;
import ca.nrc.cadc.dali.tables.votable.VOTableResource;
import ca.nrc.cadc.util.StringUtil;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import javax.security.auth.Subject;

/**
 * Datalink Service Descriptor Template
 */
public class ServiceDescriptorTemplate {

    // The descriptor name, must contain only letters, numbers, or a dash.
    private final String name;

    // VOTable describing the descriptor.
    private final String template;

    // List of ID attributes from the VOTABLE INFO elements in the template.
    private final List<String> identifiers;

    // The Subject for the user who created the descriptor.
    public transient Subject owner;

    // The ID of the owner, which is a String representation of the Subject.
    public Object ownerID;

    public ServiceDescriptorTemplate(final String name, final String template, final List<String> identifiers) {
        if (!StringUtil.hasLength(name)) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        if (!isValidString(name)) {
            throw new IllegalArgumentException("Invalid descriptor name: " + name);
        }
        if (!StringUtil.hasLength(template)) {
            throw new IllegalArgumentException("template cannot be null or empty");
        }
        if (identifiers == null || identifiers.isEmpty()) {
            throw new IllegalArgumentException("identifiers cannot be null or empty");
        }
        getVOTableDocument(template);

        this.name = name;
        this.template = template;
        this.identifiers = identifiers;
    }

    /**
     * Get the descriptor name.
     * @return the descriptor name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Get the descriptor template.
     * @return the descriptor template.
     */
    public String getTemplate() {
        return this.template;
    }

    /**
     * Get the list of identifiers from the template.
     * The identifiers are the ID attributes from the VOTABLE INFO elements.
     *
     * @return the list of identifiers.
     */
    public List<String> getIdentifiers() {
        return this.identifiers;
    }

    /**
     * Parse the template and extract the identifiers from the REF attribute. A template
     * must have at least one INFO element in the document root with an ID attribute.
     * The template must also have a GROUP element named 'inputParams' containing
     * a PARAM element with a REF attribute that matches the ID attribute of the INFO element.
     */
    public static List<String> parseIdentifiers(String template) {
        VOTableDocument votable = getVOTableDocument(template);
        List<VOTableResource> resources = votable.getResources();
        if (resources.size() != 1) {
            throw new IllegalArgumentException("invalid template: expected a single RESOURCE element");
        }
        VOTableResource resource = resources.get(0);
        if (!"meta".equals(resource.getType())) {
            throw new IllegalArgumentException("invalid template: expected RESOURCE element with attribute type = 'meta'");
        }

        return resource.getGroups().stream()
                .filter(group -> "inputParams".equals(group.getName()))
                .flatMap(group -> group.getParams().stream())
                .filter(param -> "ID".equals(param.getName()) && StringUtil.hasText(param.ref))
                .map(param -> param.ref)
                .collect(Collectors.toList());
    }

    /**
     * Validate that a string is not be null or empty, and contains only letters, numbers, or a dash.
     *
     * @param input the string to validate.
     */
    protected boolean isValidString(String input) {
        if (!StringUtil.hasLength(input)) {
            return false;
        }
        return input.matches("[a-zA-Z0-9-]+");
    }

    /**
     * Get the VOTableDocument from the template.
     *
     * @param template the VOTable template.
     * @return the VOTableDocument.
     */
    protected static VOTableDocument getVOTableDocument(String template) {
        VOTableReader reader = new VOTableReader();
        try {
            return reader.read(template);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading VOTable template: " + e.getMessage());
        }
    }

}
