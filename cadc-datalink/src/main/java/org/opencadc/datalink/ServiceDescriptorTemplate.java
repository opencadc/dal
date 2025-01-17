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
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

/**
 * Datalink Service Descriptor Template
 */
public class ServiceDescriptorTemplate {
    private static final Logger log = Logger.getLogger(ServiceDescriptorTemplate.class);

    // The descriptor name, must contain only letters, numbers, or a dash.
    private final String name;

    // VOTable describing the descriptor.
    private final String template;

    // List of ID attributes from the VOTABLE INFO elements in the template.
    private final List<String> identifiers;

    public ServiceDescriptorTemplate(final String name, final String template) {
        if (!StringUtil.hasLength(name)) {
            throw new IllegalArgumentException("name cannot be null or empty.");
        }
        if (!StringUtil.hasLength(template)) {
            throw new IllegalArgumentException("template cannot be null or empty.");
        }
        if (!isValidString(name)) {
            throw new IllegalArgumentException("Invalid descriptor name: " + name);
        }

        this.name = name;
        this.template = template;
        this.identifiers = parseIdentifiers(template);
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
     * Validate that a string is not be null or empty, and contains only letters, numbers, or a dash.
     *
     * @param input the string to validate.
     */
    private boolean isValidString(String input) {
        if (!StringUtil.hasLength(input)) {
            return false;
        }
        return input.matches("[a-zA-Z0-9-]+");
    }

    /**
     * Parse the template and extract the identifiers. A template must have at least one
     * INFO element with an ID attribute in the document root. The template must also have
     * a group element named 'inputParams' containing a PARAM element with a ref attribute
     * that matches the ID attribute of the INFO element.
     */
    private List<String> parseIdentifiers(String template) {
        VOTableReader reader = new VOTableReader();
        VOTableDocument votable;
        try {
            votable = reader.read(template);
        } catch (IOException e) {
            throw new IllegalArgumentException("Error reading VOTable template: " + e.getMessage());
        }

        // List of ID's from the INFO elements
        List<String> infoIDs = votable.getInfos().stream()
                .filter(info -> StringUtil.hasText(info.ID))
                .map(info -> info.ID)
                .collect(Collectors.toList());
        if (infoIDs.isEmpty()) {
            throw new IllegalArgumentException("template must contain one or more info elements " +
                    "with an ID attribute in the votable root.");
        }

        List<VOTableResource> resources = votable.getResources();
        if (resources.isEmpty()) {
            throw new IllegalArgumentException("template must contain at least one resource element.");
        }
        VOTableResource resource = resources.get(0);
        if (!"meta".equals(resource.getType())) {
            throw new IllegalArgumentException("a template resource element must have attribute type = 'meta'.");
        }

        // List of ref's from the inputParams group
        List<String> refs = resource.getGroups().stream()
                .filter(group -> "inputParams".equals(group.getName()))
                .flatMap(group -> group.getParams().stream())
                .filter(param -> StringUtil.hasText(param.ref))
                .map(param -> param.ref)
                .collect(Collectors.toList());
        if (refs.isEmpty()) {
            throw new IllegalArgumentException("group inputParams must contain one or more param elements with a ref attribute.");
        }

        // Validate that all ID's have a corresponding ref
        if (!new HashSet<>(refs).containsAll(infoIDs)) {
            throw new IllegalArgumentException("template must contain a ref for each ID.");
        }

        return infoIDs;
    }

}
