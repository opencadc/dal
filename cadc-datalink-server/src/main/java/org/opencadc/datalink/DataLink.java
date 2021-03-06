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

package org.opencadc.datalink;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single result output by the data link service. Exactly one of accessURL, serviceDef,
 * and errorMessage must be set prior to output.
 *
 * @author pdowler
 */
public class DataLink {

    /**
     * Terms from the http://www.ivoa.net/rdf/datalink/core vocabulary plus
     * CAOM extensions.
     */
    public enum Term { // TODO: re-use the VocabularyTerm code once extracted from caom2
        THIS("#this"),

        @Deprecated
        DATALINK("#datalink"), // recursive

        ALT("#alt"), // Alternative link
        
        PROGENITOR("#progenitor"),
        DERIVATION("#derivation"),
        
        AUXILIARY("#auxiliary"),
        WEIGHT("#weight"),
        ERROR("#error"),
        NOISE("#noise"),
        
        CALIBRATION("#calibration"),
        BIAS("#bias"),
        DARK("#dark"),
        FLAT("#flat"),
        
        PREVIEW("#preview"),
        PREVIEW_IMAGE("#preview-image"),
        PREVIEW_PLOT("#preview-plot"),
        THUMBNAIL("http://www.opencadc.org/caom2#thumbnail"),
        
        PROC("#proc"),
        CUTOUT("#cutout"),
        
        PKG("http://www.opencadc.org/caom2#pkg");

        private final String value;

        private Term(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // standard DataLink fields
    private final String id;
    private final List<Term> semantics = new ArrayList<Term>();

    /**
     * The access_url field for downloads. Exactly one of accessURL, serviceDef,
     * and errorMessage must be set prior to output.
     */
    public URL accessURL;

    /**
     * The service_def field with ID of a service descriptor. Exactly one of accessURL, serviceDef,
     * and errorMessage must be set prior to output.
     */
    public String serviceDef;

    /**
     * The error_message field if a link could not be created. Exactly one of accessURL, serviceDef,
     * and errorMessage must be set prior to output.
     */
    public String errorMessage;

    public String description;
    public String contentType;
    public Long contentLength;

    /**
     * If the serviceDef specifies a link-specific service descriptor, this is it.
     */
    public ServiceDescriptor descriptor;

    // custom CADC fields
    public Boolean readable; // predict that the current user is allowed to download

    /**
     * Constructor. There must be at least one semantics tag for each link.
     *
     * @param id Input ID value
     * @param semantics single semantics tag for this link
     */
    public DataLink(String id, Term semantics) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        if (semantics == null) {
            throw new IllegalArgumentException("semantics cannot be null");
        }
        this.id = id;
        this.semantics.add(semantics);
    }

    public void addSemantics(Term semantics) {
        if (semantics == null) {
            throw new IllegalArgumentException("semantics cannot be null");
        }
        this.semantics.add(semantics);
    }
    
    /**
     * @return the ID value
     */
    public String getID() {
        return id;
    }

    /** 
     * @return unmodifiable list of semantics tags
     */
    public List<Term> getSemantics() {
        return Collections.unmodifiableList(semantics);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DataLink[").append(id).append(",[");
        for (Term t : semantics) {
            sb.append(t.getValue()).append(",");
        }
        sb.setCharAt(sb.length() - 1, ']'); // replace last comma
        
        if (accessURL != null) {
            sb.append(",a=").append(accessURL.toExternalForm());
        } else if (serviceDef != null) {
            sb.append(",s=").append(serviceDef);
        } else if (errorMessage != null) {
            sb.append(",e=").append(errorMessage);
        }
        sb.append("]");
        return sb.toString();
    }
}
