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
import ca.nrc.cadc.wcs.WCSKeywords;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.header.Standard;
import nom.tam.fits.header.extra.NOAOExt;
import nom.tam.util.Cursor;
import org.apache.log4j.Logger;
import org.opencadc.fits.CADCExt;


/**
 * Create WCS Keywords class from a FITS header, using the header as a source.  Note that the put() calls are all
 * unsupported in this implementation to avoid unintentional modification of the underlying Header and, possibly, the
 * underlying Fits object.
 */
public class FITSHeaderWCSKeywords implements WCSKeywords {
    private static final Logger LOGGER = Logger.getLogger(FITSHeaderWCSKeywords.class);

    // Source for values.
    private final Header header;


    /**
     * Empty constructor.
     * @throws HeaderCardException  If cloning the header fails.
     */
    public FITSHeaderWCSKeywords() throws HeaderCardException {
        this(new Header());
    }

    public FITSHeaderWCSKeywords(final WCSKeywords wcsKeywords) throws HeaderCardException {
        this.header = cloneHeader(wcsKeywords);
    }

    /**
     * Iterate the header cards and add each keyword with its associated value.
     *
     * @param header The Header object.
     *               Expect an IllegalArgumentException if this is null as it is believed that the expectation is
     *               to return a populated WCSKeywords.  If an empty WCSKeywords is expected, then use the empty
     *               constructor.
     * @throws HeaderCardException If the header cards cannot be read.
     */
    public FITSHeaderWCSKeywords(final Header header) throws HeaderCardException {
        if (header == null) {
            throw new IllegalArgumentException("Header is required.");
        }

        this.header = cloneHeader(header);
        LOGGER.trace("Constructor OK.");
    }


    /**
     * Returns true if the specified key exists in the keywords.
     *
     * @param key key whose presence in keywords is to be tested.
     * @return True if keywords contains key, False otherwise.
     */
    @Override
    public boolean containsKey(String key) {
        LOGGER.trace("containsKey(" + key + ")");
        return header.containsKey(key);
    }

    /**
     * Returns a double value for the specified key. It is expected that
     * a default value, typically 0.0, will be returned if the key
     * does not exist in the keywords.
     *
     * @param key key whose double value is to be returned.
     * @return double value corresponding to the specified key.
     */
    @Override
    public double getDoubleValue(String key) {
        LOGGER.trace("getDoubleValue(" + key + ")");
        return header.getDoubleValue(key);
    }

    /**
     * Returns a double value for the specified key. If the key does not
     * exist in the keywords, the default value is returned.
     *
     * @param key   key whose double value is to be returned.
     * @param value default value returned if the specified key does not exist in keywords.
     * @return double value corresponding to the specified key.
     */
    @Override
    public double getDoubleValue(String key, double value) {
        LOGGER.trace("getDoubleValue(" + key + "/" + value + ")");
        return header.getDoubleValue(key, value);
    }

    /**
     * Returns a float value for the specified key. It is expected that
     * a de7fault value, typically 0.0, will be returned if the key
     * does not exist in the keywords.
     *
     * @param key key whose float value is to be returned.
     * @return float value corresponding to the specified key.
     */
    @Override
    public float getFloatValue(String key) {
        LOGGER.trace("getFloatValue(" + key + ")");
        return header.getFloatValue(key);
    }

    /**
     * Returns a float value for the specified key. If the key does not
     * exist in the keywords, the default value is returned.
     *
     * @param key   key whose float value is to be returned.
     * @param value default value returned if the specified key does not exist in keywords.
     * @return float value corresponding to the specified key.
     */
    @Override
    public float getFloatValue(String key, float value) {
        LOGGER.trace("getFloatValue(" + key + "/" + value + ")");
        return header.getFloatValue(key, value);
    }

    /**
     * Returns an int value for the specified key. It is expected that
     * a default value, typically 0, will be returned if the key
     * does not exist in the keywords.
     *
     * @param key key whose int value is to be returned.
     * @return int value corresponding to the specified key.
     */
    @Override
    public int getIntValue(String key) {
        LOGGER.trace("getIntValue(" + key + ")");
        return header.getIntValue(key);
    }

    /**
     * Returns an int value for the specified key. If the key does not
     * exist in the keywords, the default value is returned.
     *
     * @param key   key whose int value is to be returned.
     * @param value default value returned if the specified key does not exist in keywords.
     * @return int value corresponding to the specified key.
     */
    @Override
    public int getIntValue(String key, int value) {
        LOGGER.trace("getIntValue(" + key + "/" + value + ")");
        return header.getIntValue(key, value);
    }

    /**
     * Returns a String value for the specified key. It is expected that
     * a default value, typically an empty String, will be returned if
     * the key does not exist in the keywords.
     *
     * @param key key whose String value is to be returned.
     * @return String value corresponding to the specified key.
     */
    @Override
    public String getStringValue(String key) {
        LOGGER.trace("getStringValue(" + key + ")");
        final HeaderCard card = header.findCard(key);
        return card == null ? null : card.getValue();
    }

    /**
     * Returns a String value for the specified key. If the key does not
     * exist in the keywords, the default value is returned.
     *
     * @param key   key whose String value is to be returned.
     * @param value default value returned if the specified key does not exist in keywords.
     * @return String value corresponding to the specified key.
     */
    @Override
    public String getStringValue(String key, String value) {
        LOGGER.trace("getStringValue(" + key + "/" + value + ")");
        final String currVal = getStringValue(key);
        return currVal == null ? value : currVal;
    }

    /**
     * Add the key and String value to the keywords.
     *
     * @param key   keywords name.
     * @param value keywords value.
     */
    @Override
    public void put(String key, String value) {
        throw new UnsupportedOperationException("Unsupported put(String, String)");
    }

    /**
     * Add the key and int value to the keywords.
     *
     * @param key   key keywords name.
     * @param value value keywords value.
     */
    @Override
    public void put(String key, int value) {
        throw new UnsupportedOperationException("Unsupported put(String, int)");
    }

    /**
     * Add the key and double value to the keywords.
     *
     * @param key   key keywords name.
     * @param value value keywords value.
     */
    @Override
    public void put(String key, double value) {
        throw new UnsupportedOperationException("Unsupported put(String, double)");
    }

    /**
     * Add the key and Integer value to the keywords.
     *
     * @param key   key keywords name.
     * @param value value keywords value.
     */
    @Override
    public void put(String key, Integer value) {
        throw new UnsupportedOperationException("Unsupported put(String, Integer)");
    }

    /**
     * Add the key and Double value to the keywords.
     *
     * @param key   key keywords name.
     * @param value value keywords value.
     */
    @Override
    public void put(String key, Double value) {
        throw new UnsupportedOperationException("Unsupported put(String, Double)");
    }

    /**
     * Iterate the cards of the Header and create Map Entries as needed.
     *
     * @return An Iterator instance.  Never null.
     */
    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return new Iterator<Map.Entry<String, Object>>() {
            final Cursor<String, HeaderCard> source = header.iterator();

            @Override
            public boolean hasNext() {
                return source.hasNext();
            }

            /**
             * Convert to a Map.Entry object to adhere to the contract of this interface.  Be aware that BLANKS and
             * some COMMENTs will create empty keys and possibly empty values.
             * @return Map.Entry object, never null.
             */
            @Override
            public Map.Entry<String, Object> next() {
                final Map<String, Object> convertor = new HashMap<>();
                final HeaderCard nextHeaderCard = source.next();
                final Class<?> valueType = nextHeaderCard.valueType();
                final String headerCardValue = nextHeaderCard.getValue();
                final Object value;

                if (valueType == null || valueType == String.class || valueType == Boolean.class) {
                    value = headerCardValue;
                } else if (valueType == Integer.class) {
                    value = Integer.parseInt(headerCardValue);
                } else if (valueType == Long.class) {
                    value = Long.parseLong(headerCardValue);
                } else if (valueType == Double.class || valueType == BigDecimal.class
                           || valueType == BigInteger.class) {
                    value = Double.parseDouble(headerCardValue);
                } else {
                    value = "";
                }

                convertor.put(nextHeaderCard.getKey(), value);
                return convertor.entrySet().iterator().next();
            }
        };
    }

    public final Header getHeader() {
        return this.header;
    }

    private Header cloneHeader(final WCSKeywords wcsKeywords) throws HeaderCardException {
        final Header destination = new Header();
        for (final Iterator<Map.Entry<String, Object>> entryIterator = wcsKeywords.iterator();
             entryIterator.hasNext(); ) {
            final Map.Entry<String, Object> entry = entryIterator.next();
            final String key = entry.getKey();
            final Object value = entry.getValue();
            final Class<?> valueType = value.getClass();

            cloneHeaderCard(destination, key, valueType, "", value.toString());
        }

        destination.setNaxes(wcsKeywords.getIntValue(Standard.NAXIS.key()));
        sanitizeHeader(destination);

        return destination;
    }

    /**
     * Make a copy of the header.  Adjusting the source Header directly with an underlying File can result in the source
     * file being modified, so we duplicate it here to remove references.
     *
     * @param source The source Header.
     * @return Header object with reproduced cards.  Never null.
     * @throws HeaderCardException Any I/O with Header Cards.
     */
    private Header cloneHeader(final Header source) throws HeaderCardException {
        final Header destination = new Header();

        // Use a for loop here rather than Java Collections stream to pass the exception up properly.
        for (final Iterator<HeaderCard> headerCardIterator = source.iterator(); headerCardIterator.hasNext(); ) {
            final HeaderCard headerCard = headerCardIterator.next();
            cloneHeaderCard(destination, headerCard.getKey(), headerCard.valueType(), headerCard.getComment(),
                            headerCard.getValue());
        }

        sanitizeHeader(destination);

        return destination;
    }

    private void cloneHeaderCard(final Header destination, final String headerCardKey, final Class<?> valueType,
                                 final String comment, final String value) throws HeaderCardException {
        // Check for blank lines or just plain comments that are not standard FITS comments.
        if (!StringUtil.hasText(headerCardKey)) {
            destination.addValue(headerCardKey, (String) null, comment);
        } else if (Standard.COMMENT.key().equals(headerCardKey)) {
            destination.insertComment(comment);
        } else if (Standard.HISTORY.key().equals(headerCardKey)) {
            destination.insertHistory(comment);
        } else if (headerCardKey.startsWith(CADCExt.CDELT.key())) {
            // CDELT values cannot be zero.
            final double cdeltValue = Double.parseDouble(value);
            destination.addValue(headerCardKey, cdeltValue == 0.0D ? 1.0D : cdeltValue,
                                 comment);
        } else {
            if (valueType == String.class || valueType == null) {
                destination.addValue(headerCardKey, value, comment);
            } else if (valueType == Boolean.class) {
                destination.addValue(headerCardKey, Boolean.parseBoolean(value) || value.equals("T"), comment);
            } else if (valueType == Integer.class) {
                destination.addValue(headerCardKey, Integer.parseInt(value),
                                     comment);
            } else if (valueType == BigInteger.class) {
                destination.addValue(headerCardKey, new BigInteger(value), comment);
            } else if (valueType == Long.class) {
                destination.addValue(headerCardKey, Long.parseLong(value),
                                     comment);
            } else if (valueType == Double.class) {
                destination.addValue(headerCardKey, Double.parseDouble(value),
                                     comment);
            } else if (valueType == BigDecimal.class) {
                destination.addValue(headerCardKey, new BigDecimal(value),
                                     comment);
            }
        }
    }

    private void sanitizeHeader(final Header destination) throws HeaderCardException {
        final int naxis = destination.getIntValue(Standard.NAXIS);
        final boolean expectCD = destination.containsKey(NOAOExt.CD1_1);
        final boolean expectPC = destination.containsKey(CADCExt.PC1_1);
        final boolean expectPV = destination.containsKey(CADCExt.RESTFRQ)
                                 || destination.containsKey(CADCExt.RESTFREQ);
        final int spectralAxis = getSpectralAxis(destination);

        for (int x = 1; x <= naxis; x++) {
            for (int y = 1; y <= naxis; y++) {
                final String cdMatrixKey = String.format("CD%d_%d", x, y);
                final String pcMatrixKey = String.format("PC%d_%d", x, y);
                final String pvMatrixKey = String.format("PV%d_%d", x, y);


                // The wcslib library wants the PC/CD matrix intact for spatial cutouts.
                if (expectCD && !destination.containsKey(cdMatrixKey)) {
                    destination.addValue(cdMatrixKey, (x == y) ? 1.0D : 0.0D, null);
                }

                if (expectPC && !destination.containsKey(pcMatrixKey)) {
                    destination.addValue(pcMatrixKey, (x == y) ? 1.0D : 0.0D, null);
                }

                // If the RESTFRQ header is present, the PV values seem to be necessary as well.  Spatial (2D) cutouts
                // will fail if they exist for the spatial axes however, so keep it to the spectral axis.
                if (expectPV && (x == spectralAxis) && !destination.containsKey(pvMatrixKey)) {
                    destination.addValue(pvMatrixKey, 0.0D, null);
                }
            }

            // Ensure the default units are added when missing.
            final String cType = destination.getStringValue(Standard.CTYPEn.n(x));
            if (cType != null) {
                final String cUnit = destination.getStringValue(CADCExt.CUNITn.n(x));
                if (cUnit == null) {
                    destination.addValue(CADCExt.CUNITn.n(x), CoordTypeCode.getDefaultUnit(cType));
                }
            }
        }
    }

    /**
     * Obtain the energy (1-based) axis from the current header.  Return -1 if none found that match the Spectral types.
     *
     * @return int axis, or -1 if no spectral axis present.
     */
    int getSpectralAxis() {
        return getSpectralAxis(this.header);
    }

    /**
     * Obtain the energy (1-based) axis from the given header.  Return -1 if none found that match the Spectral types.
     *
     * @return integer axis, or -1 if not found.
     */
    int getSpectralAxis(final Header h) {
        final int naxis = h.getIntValue(Standard.NAXIS);
        for (int i = 1; i <= naxis; i++) {
            final String ctypeValue = h.getStringValue(Standard.CTYPEn.n(i));
            if (ctypeValue != null && Arrays.stream(CoordTypeCode.values()).anyMatch(
                coOrdTypeCode -> ctypeValue.startsWith(coOrdTypeCode.name()) && coOrdTypeCode.isSpectral())) {
                return i;
            }
        }

        return -1;
    }

    int getPolarizationAxis() {
        return getPolarizationAxis(this.header);
    }

    int getPolarizationAxis(final Header h) {
        final int naxis = h.getIntValue(Standard.NAXIS);
        for (int i = 1; i <= naxis; i++) {
            final String ctypeValue = h.getStringValue(Standard.CTYPEn.n(i));
            if (ctypeValue != null && Arrays.stream(CoordTypeCode.values()).anyMatch(
                coOrdTypeCode -> ctypeValue.startsWith(coOrdTypeCode.name()) && coOrdTypeCode.isPolarization())) {
                return i;
            }
        }

        return -1;
    }
}
