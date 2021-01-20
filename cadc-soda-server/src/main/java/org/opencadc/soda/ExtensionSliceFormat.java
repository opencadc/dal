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
************************************************************************
 */

package org.opencadc.soda;

import ca.nrc.cadc.dali.util.Format;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;

/**
 * Formatter to parse and format ExtensionSlice values.
 * 
 * @author pdowler
 */
public class ExtensionSliceFormat implements Format<ExtensionSlice> {

    private static final Logger log = Logger.getLogger(ExtensionSliceFormat.class);

    public static final String ALL_DATA = "*";
    public static final String ALL_DATA_FLIP = "-*";
    
    private static final String EXTENSION_NAME_VERSION_SEPARATOR = ",";
    private static final String PIXEL_AXIS_DELIMITER = ",";
    private static final String PIXEL_VALUE_DELIMITER = ":";
    
    private final Pattern singleRangePattern = Pattern.compile("\\*?(\\d+)?(:\\d+)?(:\\d*)?");

    public ExtensionSliceFormat() {
    }

    private ExtensionSlice parseSlice(final String value) {
        final List<String> stringValues = Arrays.stream(value.split("]"))
                .map(s -> s.split("\\[")[1])
                .collect(Collectors.toList());

        Integer index;
        String extensionName;
        Integer version;
        String pixelSpec = null;

        log.debug("Found string values " + stringValues);

        // Parse out the requested Extension values (either name and version, just name, or just an index).
        if (stringValues.size() == 2) {
            final String extension = stringValues.get(0).trim();
            final String[] extensionParts = extension.split(EXTENSION_NAME_VERSION_SEPARATOR);

            if (extensionParts.length == 2) {
                extensionName = extensionParts[0];
                version = Integer.parseInt(extensionParts[1]);
                index = null;

                // EXTNAME and EXTVER are 1-based.
                if (version == 0) {
                    version = 1;
                }
            } else if (extensionParts.length == 1) {
                final String firstExtensionPart = extensionParts[0];
                try {
                    index = Integer.parseInt(firstExtensionPart);
                    extensionName = null;
                    version = null;
                } catch (NumberFormatException nfe) {
                    // Then it's just an extension name.
                    index = null;
                    extensionName = firstExtensionPart;
                    version = null;
                }
            } else {
                throw new IllegalArgumentException("Specifying XTENSION return type is not supported.");
            }

            pixelSpec = stringValues.get(1);
        } else if (stringValues.size() == 1) {
            final String stringInputValue = stringValues.get(0).trim();
            if (stringInputValue.contains(PIXEL_VALUE_DELIMITER)) {
                pixelSpec = stringInputValue;
                index = 0; // default: primary
                extensionName = null;
                version = null;
            } else if (stringInputValue.contains(EXTENSION_NAME_VERSION_SEPARATOR)) {
                final String[] extensionParts = stringInputValue.split(EXTENSION_NAME_VERSION_SEPARATOR);
                extensionName = extensionParts[0].trim();
                version = Integer.parseInt(extensionParts[1].trim());
                index = null;

                // EXTNAME and EXTVER are 1-based.
                if (version == 0) {
                    version = 1;
                }
            } else {
                // try to parse extension index, fallback to name
                try {
                    index = Integer.parseInt(stringInputValue);
                    extensionName = null;
                    version = null;
                } catch (NumberFormatException numberFormatException) {
                    index = null;
                    extensionName = stringInputValue;
                    version = null;
                }
            }
        } else {
            throw new IllegalArgumentException(String.format("No usable values from (%s).", value));
        }

        ExtensionSlice ret;
        if (extensionName != null && version != null) {
            ret = new ExtensionSlice(extensionName, version);
        } else if (extensionName != null) {
            ret = new ExtensionSlice(extensionName);
        } else {
            ret = new ExtensionSlice(index);
        }
        
        if (pixelSpec != null) {
            // null -> empty means "all data" in the extension
            ret.getPixelRanges().addAll(parseRanges(pixelSpec));
        }

        return ret;
    }

    private List<PixelRange> parseRanges(String value) {
        log.warn("parseRanges: " + value);
        value = value.replaceAll(" ", "");
        return Arrays.stream(value.split(PIXEL_AXIS_DELIMITER))
                .filter(s -> singleRangePattern.matcher(s).matches() || s.trim().contains(ALL_DATA))
                .map(s -> s.split(PIXEL_VALUE_DELIMITER))
                .map(tuple -> {
                    log.warn("parseRanges: Next tuple parsed: " + Arrays.toString(tuple));
                    final PixelRange pixelRange;
                    if (tuple.length == 3) {
                        // lb:ub:step
                        final Integer[] intTuples = Arrays.stream(tuple).map(Integer::parseInt)
                                .toArray(Integer[]::new);
                        pixelRange = new PixelRange(intTuples[0], intTuples[1], intTuples[2]);
                    } else if (tuple.length == 2) {
                        if (tuple[0].equals(ALL_DATA)) {
                            // *:step
                            pixelRange = new PixelRange(0, Integer.MAX_VALUE, Integer.parseInt(tuple[1]));
                        } else if (tuple[0].equals(ALL_DATA_FLIP)) {
                            // -*:step
                            pixelRange = new PixelRange(Integer.MAX_VALUE, 0, Integer.parseInt(tuple[1]));
                        } else {
                            // lb:ub
                            final Integer[] intTuples = Arrays.stream(tuple).map(Integer::parseInt)
                                    .toArray(Integer[]::new);
                            pixelRange = new PixelRange(intTuples[0], intTuples[1]);
                        }
                    } else if (tuple.length == 1) {
                        if (tuple[0].equals(ALL_DATA)) {
                            // *
                            pixelRange = new PixelRange(0, Integer.MAX_VALUE);
                        } else if (tuple[0].equals(ALL_DATA_FLIP)) {
                            // -*
                            pixelRange = new PixelRange(Integer.MAX_VALUE, 0);
                        } else {
                            // TODO: this interpretation and testNameVersionSinglePixShortcut
                            // are debatable - "default: primary" and "single pixel shortcut"
                            // eg [2] are ambiguous: extension 2 or pixel 2 from primary
                            int pix = Integer.parseInt(tuple[0]);
                            pixelRange = new PixelRange(pix, pix);
                        }
                    } else {
                        throw new IllegalArgumentException("No usable values found from '%s'.");
                    }
                    return pixelRange;
                })
                .collect(Collectors.toList());
    }

    @Override
    public ExtensionSlice parse(String s) {
        if (s == null) {
            return null;
        }
        s = s.trim();
        if (s.isEmpty()) {
            return null;
        }
        return parseSlice(s);
    }

    @Override
    public String format(ExtensionSlice es) {
        if (es == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        if (es.extensionIndex != null) {
            sb.append(es.extensionIndex);
        } else {
            sb.append(es.extensionName);
            if (es.extensionVersion != null) {
                sb.append(EXTENSION_NAME_VERSION_SEPARATOR).append(es.extensionVersion);
            }
        }
        sb.append("]");

        if (!es.getPixelRanges().isEmpty()) {
            sb.append("[");
            Iterator<PixelRange> i = es.getPixelRanges().iterator();
            while (i.hasNext()) {
                PixelRange pr = i.next();
                sb.append(format(pr));
                if (i.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("]");
        }
        return sb.toString();
    }

    private String format(PixelRange pr) {
        StringBuilder sb = new StringBuilder();
        if (pr.lowerBound == 0 && pr.upperBound == Integer.MAX_VALUE) {
            sb.append(ALL_DATA);
        } else if (pr.lowerBound == Integer.MAX_VALUE && pr.upperBound == 0) {
            sb.append(ALL_DATA_FLIP);
        } else {
            sb.append(pr.lowerBound).append(":").append(pr.upperBound);
        }
        if (pr.step > 1) {
            sb.append(":").append(pr.step);
        }
        return sb.toString();
    }
}
