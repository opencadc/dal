/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2021.                            (c) 2021.
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

package org.opencadc.fits.slice;

import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.wcs.WCSKeywordsImpl;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.header.Standard;
import org.apache.log4j.Logger;


/**
 * Create WCS Keywords class from a FITS header.  Conversions are limited to the underlying data types.
 */
public class FITSHeaderWCSKeywords extends WCSKeywordsImpl {
    private static final Logger LOGGER = Logger.getLogger(FITSHeaderWCSKeywords.class);
    private static final List<String> IGNORED_KEYWORDS =
            Arrays.asList(Standard.COMMENT.key(), Standard.HISTORY.key(), Standard.BLANKS.key());


    /**
     * Empty constructor.
     */
    public FITSHeaderWCSKeywords() {
        super();
    }

    /**
     * Iterate the header cards and add each keyword with its associated value.
     *
     * @param header The Header object.
     *               Expect an IllegalArgumentException if this is null as it is believed that the expectation is
     *               to return a populated WCSKeywords.  If an empty WCSKeywords is expected, then use the empty
     *               constructor.
     */
    public FITSHeaderWCSKeywords(final Header header) {
        LOGGER.debug("FITSHeaderWCSKeywords constructor");

        if (header == null) {
            LOGGER.error("FITSHeaderWCSKeywords error");
            throw new IllegalArgumentException("Header is required.");
        }

        header.iterator().forEachRemaining(headerCard -> {
            // Check for blank lines or just plain comments that are not relevant to WCS and ignore them.
            if (isRelevant(headerCard)) {
                final String headerCardKey = headerCard.getKey();
                final String headerCardValue = headerCard.getValue();

                final Class<?> valueType = headerCard.valueType();
                if (valueType == Integer.class) {
                    put(headerCardKey, Integer.parseInt(headerCardValue));
                } else if (valueType == Double.class) {
                    put(headerCardKey, Double.parseDouble(headerCardValue));
                } else if (valueType == BigDecimal.class) {
                    put(headerCardKey, new BigDecimal(headerCardValue).doubleValue());
                } else {
                    // Assume anything else is of a String type.
                    put(headerCardKey, headerCardValue);
                }
            } else {
                LOGGER.debug("Skipping header card '" + headerCard.getKey() + "'.");
            }
        });
        LOGGER.debug("FITSHeaderWCSKeywords constructor with " + getNumberOfKeywords() + " keywords : OK");
    }

    /**
     * Omit unnecessary header cards such as COMMENT or HISTORY.
     * @param headerCard    The card to check.
     * @return  True if not irrelevant, False otherwise.
     */
    boolean isRelevant(final HeaderCard headerCard) {
        final String key = headerCard.getKey();
        return StringUtil.hasText(key) && StringUtil.hasText(headerCard.getValue()) && !IGNORED_KEYWORDS.contains(key);
    }

    public int getNumberOfKeywords() {
        int count = 0;
        for (final Iterator<Map.Entry<String, Object>> entryIterator = iterator(); entryIterator.hasNext();) {
            entryIterator.next();
            count++;
        }

        return count;
    }
}
