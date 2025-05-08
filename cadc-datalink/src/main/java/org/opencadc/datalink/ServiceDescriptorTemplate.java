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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.security.auth.Subject;

/**
 * Datalink Service Descriptor Template
 */
public class ServiceDescriptorTemplate {

    private final String name;
    private final String template;

    // The VOTableResource from the template.
    private transient VOTableResource resource;

    // The list of REF attributes in the template.
    private final List<String> identifiers = new ArrayList<>();

    /**
     * Server-side support for tracking the owner of a descriptor. This reference is
     * included to support server-side permission checking and is generally
     * reconstructed from the persisted ownerID.
     */
    public transient Subject owner;
    public Object ownerID;

    /**
     * Constructor for ServiceDescriptorTemplate.
     *
     * @param name The descriptor name, must contain only letters, numbers, or a dash.
     * @param template The VOTable describing the descriptor.
     */
    public ServiceDescriptorTemplate(final String name, final String template) {
        if (!StringUtil.hasLength(name)) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        if (!isValidString(name)) {
            throw new IllegalArgumentException("Invalid descriptor name: " + name);
        }
        if (!StringUtil.hasLength(template)) {
            throw new IllegalArgumentException("template cannot be null or empty");
        }

        this.name = name;
        this.template = template;
        parseTemplate();
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
     * Get the list of REF attributes from the template.
     *
     * @return the list of identifiers.
     */
    public List<String> getIdentifiers() {
        return this.identifiers;
    }

    /**
     * Get the VOTableResource from the template.
     *
     * @return a VOTableResource..
     */
    public VOTableResource getResource() {
        return this.resource;
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
     * Parses the template and extract VOTableResource and the identifiers
     * from the REF attribute. A template must have a single RESOURCE element
     * with type = 'meta'. The meta resource must have a GROUP element 'inputParams'
     * that has a PARAM elements with a REF attributes. The template must have
     * one or more INFO elements in the document root with an ID attribute.
     * The REF attributes must match the ID attributes.
     */
    private void parseTemplate() {
        VOTableDocument votable;
        VOTableReader reader = new VOTableReader();
        try {
            votable = reader.read(template);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading VOTable template: " + e.getMessage());
        }

        List<VOTableResource> resources = votable.getResources();
        if (resources.size() != 1) {
            throw new IllegalArgumentException("invalid template: expected a single RESOURCE element");
        }

        if (!"meta".equals(resources.get(0).getType())) {
            throw new IllegalArgumentException("invalid template: expected RESOURCE element with attribute type = 'meta'");
        }
        resource = resources.get(0);

        getIdentifiers().addAll(resource.getGroups().stream()
                .filter(group -> "inputParams".equals(group.getName()))
                .flatMap(group -> group.getParams().stream())
                .filter(param -> "ID".equals(param.getName()) && StringUtil.hasText(param.ref))
                .map(param -> param.ref)
                .collect(Collectors.toList()));
    }

}
