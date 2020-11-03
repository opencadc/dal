/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2020.                            (c) 2020.
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


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Class to represent a full slice request.
 */
public final class Slices {
    private static final Logger LOGGER = LogManager.getLogger(Slices.class);

    static {
        Configurator.setLevel(Slices.class.getCanonicalName(), Level.DEBUG);
    }

    private static final String PIXEL_RANGE_PATTERN_STRING = "\\[([\\d*]+:?\\d*,?\\s*)*\\]";
    private static final String ENTIRE_RANGE_PATTERN_STRING =
            "(\\s*\\[[\\w]*,?\\s*\\d*\\])?" + PIXEL_RANGE_PATTERN_STRING;

    private final ExtensionSliceValue[] extensionSliceValues;

    public static Slices fromString(final String value) {
        System.out.println("Matching on " + value);
        final Pattern entireRangePattern = Pattern.compile(ENTIRE_RANGE_PATTERN_STRING);
        final List<ExtensionSliceValue> extensionSliceValues = matchAndCollect(entireRangePattern, value);

        if (extensionSliceValues.isEmpty()) {
            throw new IllegalArgumentException("No parsable values in " + value);
        } else {
            return new Slices(extensionSliceValues.toArray(new ExtensionSliceValue[0]));
        }
    }

    private static List<ExtensionSliceValue> matchAndCollect(final Pattern pattern, final String value) {
        final Matcher matcher = pattern.matcher(value);
        final List<ExtensionSliceValue> extensionSliceValues = new ArrayList<>();

        while (matcher.find()) {
            final String nextMatch = matcher.group();
            System.out.println("Next match is " + nextMatch);
            extensionSliceValues.add(ExtensionSliceValue.fromString(nextMatch));
        }

        return extensionSliceValues;
    }

    Slices(final ExtensionSliceValue[] extensionSliceValues) {
        this.extensionSliceValues = extensionSliceValues;
    }

    public boolean hasRanges() {
        return this.extensionSliceValues != null && this.extensionSliceValues.length > 0;
    }

    public ExtensionSliceValue[] getExtensionSliceValues() {
        return this.extensionSliceValues;
    }


    /**
     * Represents a single extension and set of pixel ranges.
     */
    public static final class ExtensionSliceValue {

        private final static String ALL_DATA = "*";
        private final static String SEPARATOR = ",";
        private final static String PIXEL_VALUE_DELIMITER = ":";
        private static final Pattern VALID_SINGLE_RANGE = Pattern.compile("[\\d*\\*]+:?\\d*");

        private final String value;
        private final String extensionName;
        private final Integer extensionVersion;
        private final Integer extensionIndex;


        public static ExtensionSliceValue fromString(final String value) {
            final List<String> stringValues = Arrays.stream(value.split("]"))
                                                    .map(s -> s.split("\\[")[1])
                                                    .collect(Collectors.toUnmodifiableList());

            Integer index;
            String extensionName;
            Integer version;
            final String values;

            LOGGER.debug("Found string values " + stringValues);

            // Parse out the requested Extension values (either name and version, just name, or just an index).
            if (stringValues.size() == 2) {
                final String extension = stringValues.get(0).trim();
                final String[] extensionParts = extension.split(SEPARATOR);

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

                values = stringValues.get(1);
            } else if (stringValues.size() == 1) {
                final String stringInputValue = stringValues.get(0).trim();
                if (stringInputValue.contains(PIXEL_VALUE_DELIMITER)) {
                    values = stringInputValue;
                    index = 0;
                    extensionName = null;
                    version = null;
                } else {
                    try {
                        index = Integer.parseInt(stringInputValue);
                        extensionName = null;
                        version = null;
                    } catch (NumberFormatException numberFormatException) {
                        index = 1;
                        extensionName = stringInputValue;
                        version = null;
                    }
                    values = ALL_DATA;
                }
            } else {
                throw new IllegalArgumentException(String.format("No usable values from (%s).", value));
            }

            return new ExtensionSliceValue(values, extensionName, version, index);
        }

        ExtensionSliceValue(final String value, final String extensionName, final Integer extensionVersion,
                            final Integer extensionIndex) {
            this.value = value;
            this.extensionName = extensionName;
            this.extensionVersion = extensionVersion;
            this.extensionIndex = extensionIndex;
        }

        public String getExtensionName() {
            return extensionName;
        }

        public Integer getExtensionVersion() { return extensionVersion; }

        public Integer getExtensionIndex() {
            return extensionIndex;
        }

        @Override
        public String toString() {
            return "ExtensionSliceValue{" +
                   "value='" + value + '\'' +
                   ", extensionName='" + extensionName + '\'' +
                   ", extensionVersion=" + extensionVersion +
                   ", extensionIndex=" + extensionIndex +
                   '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final ExtensionSliceValue that = (ExtensionSliceValue) o;
            return value.equals(that.value) &&
                   Objects.equals(extensionName, that.extensionName) &&
                   Objects.equals(extensionVersion, that.extensionVersion) &&
                   Objects.equals(extensionIndex, that.extensionIndex);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value, extensionName, extensionVersion, extensionIndex);
        }

        /**
         * Turns the pixel ranges into Range tuples.
         *
         * <p>Given [9][200:600, 300:1000, 1100:1200:3]
         * this method will return a list of iterables over integers:
         * [Range{200,600,1}, Range{300,1000,1}, Range{1100,1200,3}]
         *
         * @param maxSize The max boundary.
         * @return A list of Range objects.
         */
        public List<Range> getRanges(final int maxSize) {
            return Arrays.stream(value.split(SEPARATOR))
                         .filter(s -> VALID_SINGLE_RANGE.matcher(s.trim()).matches() || s.trim().contains(ALL_DATA))
                         .map(s -> s.split(PIXEL_VALUE_DELIMITER))
                         .map(tuple -> {
                             LOGGER.debug("Next tuple parsed: " + Arrays.toString(tuple));
                             final Range range;
                             if (tuple.length == 3) {
                                 final Integer[] intTuples = Arrays.stream(tuple).map(Integer::parseInt).collect(
                                         Collectors.toUnmodifiableList()).toArray(new Integer[0]);
                                 range = new Range(intTuples[0], intTuples[1], intTuples[2]);
                             } else if (tuple.length == 2) {
                                 if (tuple[0].equals(ALL_DATA)) {
                                     range = new Range(0, maxSize, Integer.parseInt(tuple[1]));
                                 } else {
                                     final Integer[] intTuples = Arrays.stream(tuple).map(Integer::parseInt).collect(
                                             Collectors.toUnmodifiableList()).toArray(new Integer[0]);
                                     range = new Range(intTuples[0], intTuples[1]);
                                 }
                                 return range;
                             } else if (tuple.length == 1) {
                                 final String tupleValue = tuple[0];
                                 range = tupleValue.equals(ALL_DATA) ? new Range(maxSize)
                                                                     : new Range(Integer.parseInt(tupleValue));
                             } else {
                                 throw new IllegalArgumentException("No usable values found from '%s'.");
                             }

                             return range;
                         })
                         .collect(Collectors.toUnmodifiableList());
        }
    }
}
