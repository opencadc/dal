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

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.CommonParamValidator;
import ca.nrc.cadc.dali.tables.votable.VOTableWriter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Class to produce an ADQL query from a set of Parameters
 */
public class TAPQueryGenerator extends CommonParamValidator {
    private static final int DEF_MAXREC = 1000;
    private static final int MAX_MAXREC = Integer.MAX_VALUE;
    private static final int MIN_VERB_VALUE = 1;
    private static final int MID_VERB_VALUE = 2;
    private static final int MAX_VERB_VALUE = 3;
    private static final Logger LOGGER = LogManager.getLogger(TAPQueryGenerator.class);

    protected final String catalog;
    protected final String lowVerbositySelectList;
    protected final String midVerbositySelectList;
    protected final String highVerbositySelectList;

    public TAPQueryGenerator(final String catalog, final String lowVerbositySelectList,
                             final String midVerbositySelectList, final String highVerbositySelectList) {
        this.catalog = catalog;
        this.lowVerbositySelectList = lowVerbositySelectList;
        this.midVerbositySelectList = midVerbositySelectList;
        this.highVerbositySelectList = highVerbositySelectList;
    }

    /**
     * Map with the REQUEST, LANG, and QUERY parameters.
     *
     * @param positionColumnName The name of the positional column to compare against the cone's position,
     *                           preferably spatially indexed.
     * @return map of parameter names and values
     */
    public Map<String, Object> getParameterMap(final String positionColumnName,
                                               final Map<String, List<String>> parameters) {
        final Map<String, Object> queryParameterMap = new HashMap<>();

        // Obtain and, if necessary, provide a default RESPONSEFORMAT.
        final String requestedResponseFormat = getFirstParameter(ParameterNames.RESPONSEFORMAT.name(), parameters,
                                                                 VOTableWriter.CONTENT_TYPE);
        queryParameterMap.put(ParameterNames.RESPONSEFORMAT.name(), requestedResponseFormat);

        // Obtain and validate the VERB (verbosity) output.
        final String requestedOutputVerbosity = getFirstParameter(ParameterNames.VERB.name(), parameters,
                                                                  Integer.toString(MID_VERB_VALUE));
        final NumberParameterValidator outputVerbosityValidator =
                new NumberParameterValidator(false, TAPQueryGenerator.MIN_VERB_VALUE,
                                             TAPQueryGenerator.MAX_VERB_VALUE, TAPQueryGenerator.MID_VERB_VALUE);
        final int outputVerbosity = outputVerbosityValidator.validate(requestedOutputVerbosity);

        // Obtain and validate the MAXREC value with a default if necessary.
        final String requestedMaxRecords = getFirstParameter(ParameterNames.MAXREC.name(), parameters,
                                                             Integer.toString(TAPQueryGenerator.DEF_MAXREC));
        final NumberParameterValidator maxRecordsValidator =
                new NumberParameterValidator(true, 0, TAPQueryGenerator.MAX_MAXREC,
                                             TAPQueryGenerator.DEF_MAXREC);
        final int maxRecordCount = maxRecordsValidator.validate(requestedMaxRecords);
        queryParameterMap.put(ParameterNames.MAXREC.name(), maxRecordCount);

        queryParameterMap.put("LANG", "ADQL");
        final String query = getQuery(positionColumnName, outputVerbosity, validateConePositionCenter(parameters));
        LOGGER.debug("Cone Search TAP query:\n" + query);
        queryParameterMap.put("QUERY", query);
        return queryParameterMap;
    }

    private String getQuery(final String positionColumnName, final int outputVerbosity, final Circle circle) {
        return "SELECT "
               + getSelectList(outputVerbosity)
               + " FROM "
               + this.catalog
               + " WHERE 1 = CONTAINS("
               + positionColumnName
               + ", "
               + "CIRCLE('ICRS', "
               + circle.getCenter().getLongitude()
               + ", "
               + circle.getCenter().getLatitude()
               + ", "
               + circle.getRadius()
               + "))";
    }

    private String getFirstParameter(final String key, final Map<String, List<String>> requestParameters,
                                     final String defaultValue) {
        final List<String> values = requestParameters.get(key);
        return (values == null || values.isEmpty()) ? defaultValue : values.get(0);
    }

    private Circle validateConePositionCenter(final Map<String, List<String>> requestParameters) {
        final String raValue = getFirstParameter(ParameterNames.RA.name(), requestParameters, null);
        final String decValue = getFirstParameter(ParameterNames.DEC.name(), requestParameters, null);
        final String searchRadiusValue = getFirstParameter(ParameterNames.SR.name(), requestParameters, null);
        final Map<String, List<String>> circleValidateParams = new HashMap<>();
        circleValidateParams.put(CommonParamValidator.CIRCLE, Collections.singletonList(
                String.format("%s %s %s", raValue, decValue, searchRadiusValue)));

        try {
            final List<Circle> validCircles = super.validateCircle(circleValidateParams);

            if (validCircles.isEmpty()) {
                throw new IllegalArgumentException("No valid input cone position center provided.");
            } else {
                return validCircles.get(0);
            }
        } catch (NumberFormatException numberFormatException) {
            throw new IllegalArgumentException("Cannot create cone position center from non-numeric input.");
        }
    }


    private String getSelectList(final int outputVerbosity) {
        switch (outputVerbosity) {
            case TAPQueryGenerator.MIN_VERB_VALUE: {
                return getLowVerbositySelectList();
            }

            case TAPQueryGenerator.MAX_VERB_VALUE: {
                return getHighVerbositySelectList();
            }

            // Default output is MID (2)
            default: {
                return getMidVerbositySelectList();
            }
        }
    }

    /**
     * Obtain a select list for the Low Verbosity (1).
     * @return  String select columns, or "*", never null.
     *          Example: "ra, dec, footprint"
     */
    public String getLowVerbositySelectList() {
        return this.lowVerbositySelectList;
    }

    /**
     * Obtain a select list for the Medium Verbosity (2), which is the default.
     * @return  String select columns, or "*", never null.
     *          Example: "obs_id, release_date, ra, dec, footprint"
     */
    public String getMidVerbositySelectList() {
        return this.midVerbositySelectList;
    }

    /**
     * Obtain a select list for the High Verbosity (3).
     * @return  String select columns, or "*", never null.
     *          Example: "*"
     */
    public String getHighVerbositySelectList() {
        return this.highVerbositySelectList;
    }
}
