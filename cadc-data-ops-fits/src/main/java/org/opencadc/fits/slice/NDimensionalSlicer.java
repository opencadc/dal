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

import ca.nrc.cadc.util.ArrayUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ca.nrc.cadc.util.StringUtil;
import nom.tam.fits.*;
import nom.tam.fits.header.DataDescription;
import nom.tam.fits.header.Standard;
import nom.tam.image.ImageTiler;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.RandomAccessDataObject;
import nom.tam.util.RandomAccessFileExt;
import org.apache.log4j.Logger;

/**
 * Slice out a portion of an image.
 */
public class NDimensionalSlicer {
    private static final Logger LOGGER = Logger.getLogger(NDimensionalSlicer.class);


    public NDimensionalSlicer() {
    }

    /**
     * Perform a slice operation from the input File.  Implementors will walk through the File, skipping unwanted bytes.
     *
     * <p>This implementation relies on the NOM TAM FITS API to be able to look up an HDU by its Extension Name
     * (<code>EXTNAME</code>) and, optionally, it's Extension Version (<code>EXTVER</code>).  This may affect an
     * underlying <code>Fits(InputStream)</code> unless the <code>InputStream</code> can handle resetting and marking.
     *
     * <p>It is NOT the responsibility of this method to manage the given <code>OutputStream</code>.  The caller will
     * need to close it and ensure it's open outside the bounds of this method.
     *
     * @param fitsFile     The File to read bytes from.  This method will not close this file.
     * @param slices       The string value of the pixels to extract.
     * @param outputStream Where to write bytes to.  This method will not close this stream.
     * @throws FitsException Any FITS related errors from the NOM TAM Fits library.
     * @throws IOException   Reading Writing errors.
     */
    public void slice(final File fitsFile, final Slices slices, final OutputStream outputStream)
            throws FitsException, IOException {
        slice(new RandomAccessFileExt(fitsFile, "r"), slices, outputStream);
    }

    /**
     * Perform a slice operation from the input RandomAccess.  Implementors will walk through the RandomAccess, skipping
     * unwanted bytes.
     *
     * <p>This implementation relies on the NOM TAM FITS API to be able to look up an HDU by its Extension Name
     * (<code>EXTNAME</code>) and, optionally, it's Extension Version (<code>EXTVER</code>).  This may affect an
     * underlying <code>Fits(InputStream)</code> unless the <code>InputStream</code> can handle resetting and marking.
     *
     * <p>It is NOT the responsibility of this method to manage the given <code>OutputStream</code>.  The caller will
     * need to close it and ensure it's open outside the bounds of this method.
     *
     * @param randomAccessDataObject The RandomAccess object to read bytes from.  This method will not close
     *                               this file.
     * @param slices                 The string value of the pixels to extract.
     * @param outputStream           Where to write bytes to.  This method will not close this stream.
     * @throws FitsException Any FITS related errors from the NOM TAM Fits library.
     * @throws IOException   Reading Writing errors.
     */
    public void slice(final RandomAccessDataObject randomAccessDataObject, final Slices slices,
                      final OutputStream outputStream)
            throws FitsException, IOException {
        final ArrayDataOutput output = new BufferedDataOutputStream(outputStream);
        final Slices.ExtensionSliceValue[] extensionSliceValues = slices.getExtensionSliceValues();

        LOGGER.debug("Parsed extension slice values: " + Arrays.toString(extensionSliceValues));

        slice(randomAccessDataObject, extensionSliceValues, output);
    }

    private void slice(final RandomAccessDataObject randomAccessDataObject,
                       final Slices.ExtensionSliceValue[] requestedExtensionSliceValues, final ArrayDataOutput output)
            throws FitsException, IOException {
        if (ArrayUtil.isEmpty(requestedExtensionSliceValues)) {
            throw new IllegalStateException("No cutout specified.");
        }

        // Single Fits object for the slice.  It maintains state about itself such as the current read offset.
        final Fits fitsInput = new Fits(randomAccessDataObject);

        // Reduce the requested extensions to only those that overlap.
        final Map<Integer, Slices.ExtensionSliceValue[]> overlapHDUs = getOverlap(fitsInput,
                                                                                  requestedExtensionSliceValues);

        if (overlapHDUs.isEmpty()) {
            throw new FitsException("No overlap found.");
        } else {
            LOGGER.debug("Found " + overlapHDUs.size() + " overlapping slices.");
        }

        // This count is available because the getOverlap call above will read through and cache the HDUs.
        final int hduCount = fitsInput.getNumberOfHDUs();

        // The count will indicate the number of reads into the file that were needed to get to the farthest requested
        // HDU.  This DEBUG is here to help optimize the I/O.  All other getHDU() calls will use the cached list of HDUs
        // in the Fits object and will not need a read.
        LOGGER.debug("Number of reads: " + hduCount);

        // Read the primary header first.
        final BasicHDU<?> firstHDU = fitsInput.getHDU( 0);

        if (firstHDU == null) {
            throw new FitsException("Invalid FITS file (No primary HDU).");
        }

        // MEF output is expected if the number of requested HDUs, or the number of requested values is greater than
        // one.
        final boolean mefOutput = overlapHDUs.size() > 1
                                  || overlapHDUs.values().stream().mapToInt(e -> e.length).sum() > 1;

        final boolean mefInput = hduCount > 1;

        // Will write out to an ArrayDataOutput stream at the end.
        final Fits fitsOutput = new Fits();

        LOGGER.debug("\nMEF Output: " + mefOutput + "\nMEF Input: " + mefInput);

        // The caller is expecting more than one extension.
        boolean firstHDUAlreadyWritten = false;
        if (mefInput && mefOutput) {
            final Header primaryHeader = firstHDU.getHeader();
            final boolean primaryHDUHasData = hasData(firstHDU);
            final Header headerCopy = copyHeader(primaryHeader);

            // If the primary HDU contains data, then the MEF output will contain just XTENSION HDUs each with
            // data.
            if (primaryHDUHasData) {
                headerCopy.deleteKey(Standard.SIMPLE);

                final HeaderCard xtensionCard = headerCopy.findCard(Standard.XTENSION);

                if (xtensionCard == null) {
                    headerCopy.addValue(Standard.XTENSION, Standard.XTENSION_IMAGE);
                }

                headerCopy.deleteKey(Standard.EXTEND);
            } else {
                // We're cutting from a Simple FITS file, or an MEF with no data in the primary HDU.  Either way,
                // the header gets modified.
                headerCopy.setSimple(true);

                final HeaderCard nextEndCard = headerCopy.findCard(DataDescription.NEXTEND);

                // Adjust the NEXTEND appropriately.
                if (nextEndCard == null) {
                    headerCopy.addValue(DataDescription.NEXTEND, overlapHDUs.keySet().size());
                } else {
                    nextEndCard.setValue(overlapHDUs.keySet().size());
                }

                final HeaderCard extendFlagCard = headerCopy.findCard(Standard.EXTEND);

                // Adjust the EXTEND appropriately.
                if (extendFlagCard == null) {
                    headerCopy.addValue(Standard.EXTEND, true);
                } else {
                    extendFlagCard.setValue(true);
                }
            }

            fitsOutput.addHDU(FitsFactory.hduFactory(headerCopy, new ImageData()));

            firstHDUAlreadyWritten = true;
        }
        // Output a simple FITS file otherwise.

        // Iterate the requested cutouts.  Lookup the HDU for each one and use the NOM TAM Tiler to pull out an
        // array of primitives.
        for (final Map.Entry<Integer, Slices.ExtensionSliceValue[]> overlap : overlapHDUs.entrySet()) {
            final Integer nextHDUIndex = overlap.getKey();

            LOGGER.debug("Next extension slice value at extension " + nextHDUIndex);
            try {
                final BasicHDU<?> hdu = fitsInput.getHDU(nextHDUIndex);
                final ImageHDU imageHDU = (ImageHDU) hdu;
                final Header header = imageHDU.getHeader();
                final int[] dimensions = imageHDU.getAxes();

                if (dimensions == null) {
                    throw new FitsException("Sub-image not within image");
                }

                for (final Slices.ExtensionSliceValue extensionSliceValue : overlap.getValue()) {

                    final int dimensionLength = dimensions.length;
                    final int[] corners = new int[dimensionLength];
                    final int[] lengths = new int[dimensionLength];
                    final int[] steps = new int[dimensionLength];

                    final Header headerCopy = copyHeader(header);

                    fillCornersAndLengths(dimensionLength, dimensions, headerCopy, extensionSliceValue, corners,
                                          lengths, steps);

                    try {
                        // The data contained in this HDU cannot be used to slice from.
                        if (corners.length == 0) {
                            throw new FitsException("Sub-image not within image");
                        }

                        LOGGER.debug("Tiling out " + Arrays.toString(lengths) + " at corner "
                                     + Arrays.toString(corners) + " from extension "
                                     + hdu.getTrimmedString(Standard.EXTNAME) + ","
                                     + header.getIntValue(Standard.EXTVER, 1));
                        final ImageTiler imageTiler = imageHDU.getTiler();

                        // CRPIX values are not set automatically.  Adjust them here.
                        for (int i = 0; i < dimensionLength; i++) {
                            final HeaderCard crPixCard = headerCopy.findCard(Standard.CRPIXn.n(i + 1));
                            // Need to run backwards (reverse order) to match the dimensions.
                            final double nextValue = corners[corners.length - i - 1];
                            if (crPixCard != null) {
                                crPixCard.setValue(Double.parseDouble(crPixCard.getValue()) - nextValue);
                            } else {
                                headerCopy.addValue(Standard.CRPIXn.n(i + 1), nextValue);
                            }
                        }

                        if (mefOutput && firstHDUAlreadyWritten) {
                            headerCopy.setXtension(Standard.XTENSION_IMAGE);
                            final HeaderCard pcountHeaderCard = headerCopy.findCard(Standard.PCOUNT);
                            final HeaderCard gcountHeaderCard = headerCopy.findCard(Standard.GCOUNT);

                            if (pcountHeaderCard == null) {
                                headerCopy.addValue(Standard.PCOUNT, 0);
                            }

                            if (gcountHeaderCard == null) {
                                headerCopy.addValue(Standard.GCOUNT, 0);
                            }
                        } else {
                            // MEF input to simple output.
                            headerCopy.setSimple(true);
                        }

                        fitsOutput.addHDU(FitsFactory.hduFactory(headerCopy,
                                                                 new ImageData(imageTiler.getTile(corners, lengths))));

                        firstHDUAlreadyWritten = true;
                    } catch (IOException ioException) {
                        // No overlap means it gets skipped.
                        if (!ioException.getMessage().equals("Sub-image not within image")) {
                            throw new IllegalStateException(ioException.getMessage(), ioException);
                        } else {
                            LOGGER.warn("Skipping extension " + extensionSliceValue.toString() + " due to "
                                        + ioException.getMessage() + ".");
                        }
                    }
                }
            } finally {
                try {
                    // Flush out any buffers.
                    output.flush();
                } catch (IOException e) {
                    LOGGER.warn("Tried to flush output.", e);
                }
            }
        }

        fitsOutput.write(output);
    }

    /**
     * Populate the corners and lengths of the tile to pull.  This method will fill the <code>corners</code>,
     * <code>lengths</code>, and <code>steps</code> arrays to be used by the FITS Tiler.
     *
     * @param dimensionLength     The full size of the dimension.  Used to fill in values that are not specified.
     * @param dimensions          The dimension values to pad with.
     * @param header              The Header to set NAXIS values for as they are calculated.
     * @param extensionSliceValue The requested cutout.
     * @param corners             The corners array to indicate starting pixel points.
     * @param lengths             The lengths of each dimension to cutout.
     * @param steps               For striding, these values will be something other than 1.
     */
    private void fillCornersAndLengths(final int dimensionLength, final int[] dimensions, final Header header,
                                       final Slices.ExtensionSliceValue extensionSliceValue, final int[] corners,
                                       final int[] lengths, final int[] steps) {

        LOGGER.debug("Full dimensions are " + Arrays.toString(dimensions));

        // Pad the bounds with the full dimensions as necessary.
        for (int i = 0; i < dimensionLength; i++) {
            // Need to pull values in reverse order as the dimensions (axes) are delivered in reverse order.
            final int maxRegionSize = dimensions[dimensionLength - i - 1];
            final List<PixelRange> pixelRanges = extensionSliceValue.getRanges(maxRegionSize);
            final int rangeSize = pixelRanges.size();
            final PixelRange pixelRange;
            if (rangeSize > i) {
                pixelRange = pixelRanges.get(i);
            } else {
                pixelRange = new PixelRange(0, maxRegionSize);
            }

            final int rangeLowBound = pixelRange.getLowerBound();
            final int rangeUpBound = pixelRange.getUpperBound();
            final int rangeStep = pixelRange.getStep();

            final int lowerBound = rangeLowBound > 0 ? rangeLowBound - 1 : rangeLowBound;
            LOGGER.debug("Set lowerBound to " + lowerBound + " from rangeLowBound " + rangeLowBound);
            final int upperBound;
            final int step;

            if (lowerBound > rangeUpBound) {
                upperBound = rangeUpBound - 2;
                step = rangeStep * -1;
            } else {
                upperBound = rangeUpBound;
                step = rangeStep;
            }

            final int nextLength = Math.min((upperBound - lowerBound), maxRegionSize);
            LOGGER.debug("Length is " + nextLength + " (" + upperBound + " - " + lowerBound + ")");

            // Adjust the NAXISn header appropriately.
            header.setNaxis(i + 1, nextLength);

            // Need to set the values backwards (reverse order) to match the dimensions.
            corners[corners.length - i - 1] = lowerBound;

            // Need to set the values backwards (reverse order) to match the dimensions.
            lengths[lengths.length - i - 1] = nextLength;

            // Need to set the values backwards (reverse order) to match the dimensions.
            steps[steps.length - i - 1] = step;
        }
    }

    /**
     * Make a copy of the header.  Adjusting the source one directly with an underlying File will result in the source
     * file being modified.
     *
     * @param source      The source Header.
     * @return Header object with reproduced cards.  Never null.
     * @throws HeaderCardException Any I/O with Header Cards.
     */
    private Header copyHeader(final Header source) throws HeaderCardException {
        final Header destination = new Header();
        for (final Iterator<HeaderCard> headerCardIterator = source.iterator(); headerCardIterator.hasNext(); ) {
            final HeaderCard headerCard = headerCardIterator.next();
            final String headerCardKey = headerCard.getKey();
            LOGGER.debug("Checking next card " + headerCardKey + "(" + headerCard.getComment() + ")");
            final Class<?> valueType = headerCard.valueType();

            // Check for blank lines or just plain comments that are not standard FITS comments.
            if (!StringUtil.hasText(headerCardKey)) {
                destination.addValue(headerCardKey, (String) null, headerCard.getComment());
            } else if (Standard.COMMENT.key().equals(headerCardKey)) {
                destination.insertComment(headerCard.getComment());
            } else if (Standard.HISTORY.key().equals(headerCardKey)) {
                destination.insertHistory(headerCard.getComment());
            } else {
                if (valueType == String.class || valueType == null) {
                    destination.addValue(headerCard.getKey(), headerCard.getValue(), headerCard.getComment());
                } else if (valueType == Boolean.class) {
                    destination.addValue(headerCard.getKey(), Boolean.parseBoolean(headerCard.getValue()),
                                         headerCard.getComment());
                } else if (valueType == Integer.class || valueType == BigInteger.class) {
                    destination.addValue(headerCard.getKey(), new BigInteger(headerCard.getValue()),
                                         headerCard.getComment());
                } else if (valueType == Long.class) {
                    destination.addValue(headerCard.getKey(), Long.parseLong(headerCard.getValue()),
                                         headerCard.getComment());
                } else if (valueType == Double.class) {
                    destination.addValue(headerCard.getKey(), Double.parseDouble(headerCard.getValue()),
                                         headerCard.getComment());
                } else if (valueType == BigDecimal.class) {
                    destination.addValue(headerCard.getKey(), new BigDecimal(headerCard.getValue()),
                                         headerCard.getComment());
                }
            }
        }

        return destination;
    }

    private boolean hasData(final BasicHDU<?> hdu) {
        final Data data = hdu.getData();
        return (data != null) && (data.getSize() > 0L);
    }

    private boolean matchHDU(final BasicHDU<?> hdu, final String extensionName, final Integer extensionVersion) {
        final String extName = hdu.getTrimmedString(Standard.EXTNAME);

        // Only carry on if this HDU has an EXTNAME value.
        if (extName != null) {
            // FITS dictates the default EXTVER is 1 if not present.
            // https://heasarc.gsfc.nasa.gov/docs/fcg/standard_dict.html
            //
            final int extVer = hdu.getHeader().getIntValue(Standard.EXTVER, 1);

            // Ensure the extension name matches as that's a requirement.  By default the extVer value will be 1,
            // which will match if no extensionVersion was requested.  Otherwise, ensure the extVer matches the
            // requested value.  Boxing extVer into a new Integer() alleviates a NullPointerException from possibly
            // occurring.
            return extName.equalsIgnoreCase(extensionName)
                   && (((extensionVersion == null) && (extVer == 1))
                       || (Integer.valueOf(extVer).equals(extensionVersion)));
        }

        return false;
    }

    /**
     * Obtain the overlapping indexes of matching HDUs.  This will return a unique list of Integers in file order,
     * rather than the order that they were requested.  Since
     *
     * @param fits                  The Fits object to scan.
     * @param extensionSliceValues  The requested values.
     * @return  An Map of overlapping hduIndex->slice[], or empty Map.  Never null.
     * @throws FitsException if the header could not be read
     * @throws IOException   if the underlying buffer threw an error
     */
    private Map<Integer, Slices.ExtensionSliceValue[]> getOverlap(final Fits fits,
                                                                  final Slices.ExtensionSliceValue[] extensionSliceValues)
            throws FitsException, IOException {

        // A Set is used to eliminate duplicates from the inner loop below.
        final Map<Integer, Slices.ExtensionSliceValue[]> overlapHDUIndexesSlices = new LinkedHashMap<>();

        // Walk through the cache first.
        final int hduCount = fits.getNumberOfHDUs();
        int matchCount = 0;
        int hduIndex;
        for (hduIndex = 0; hduIndex < hduCount; hduIndex++) {
            final BasicHDU<?> hdu = fits.getHDU(hduIndex);
            final Slices.ExtensionSliceValue[] overlapSlices = getOverlap(hdu, hduIndex, extensionSliceValues);
            if (overlapSlices.length > 0) {
                overlapHDUIndexesSlices.put(hduIndex, overlapSlices);
                matchCount += overlapSlices.length;
            }
        }

        // Read in each HDU that is not in cache.  This will only read until it doesn't need to anymore.
        BasicHDU<?> hdu;
        while ((matchCount < extensionSliceValues.length) && ((hdu = fits.readHDU()) != null)) {
            final Slices.ExtensionSliceValue[] overlapSlices = getOverlap(hdu, hduIndex, extensionSliceValues);
            if (overlapSlices.length > 0) {
                overlapHDUIndexesSlices.put(hduIndex, overlapSlices);
                matchCount += overlapSlices.length;
            }
            hduIndex++;
        }

        return overlapHDUIndexesSlices;
    }

    private Slices.ExtensionSliceValue[] getOverlap(final BasicHDU<?> hdu, final int hduIndex,
                                                    final Slices.ExtensionSliceValue[] extensionSliceValues)
            throws FitsException {
        final List<Slices.ExtensionSliceValue> overlappingSlices = new ArrayList<>();

        if (hdu != null) {
            if (!(hdu instanceof ImageHDU)) {
                // Try to dump the header out to show that it matched
                try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                     final PrintStream printStream = new PrintStream(byteArrayOutputStream)) {
                    hdu.getHeader().dumpHeader(printStream);
                    LOGGER.warn("Skipping matching extension \n\n " + byteArrayOutputStream.toString()
                                + "\n\n as it's NOT an Image HDU.");
                } catch (Exception e) {
                    LOGGER.warn(e.getMessage(), e);
                }
            } else {
                for (final Slices.ExtensionSliceValue extensionSliceValue : extensionSliceValues) {
                    if (matchHDU(hdu, extensionSliceValue.getExtensionName(),
                                 extensionSliceValue.getExtensionVersion())
                        || ((extensionSliceValue.getExtensionIndex() != null)
                            && (hduIndex == extensionSliceValue.getExtensionIndex()))) {

                        // The HDU matches a requested one, now check if it overlaps at all.
                        final int[] dimensions = hdu.getAxes();

                        if (dimensions != null) {
                            for (int i = 0; i < dimensions.length; i++) {
                                final int maxUpperBound = dimensions[dimensions.length - i - 1];
                                final List<PixelRange> pixelRanges = extensionSliceValue.getRanges(maxUpperBound);
                                if ((pixelRanges.size() >= i)
                                    && (pixelRanges.get(i).getLowerBound() < maxUpperBound)) {
                                    overlappingSlices.add(extensionSliceValue);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }

        return overlappingSlices.toArray(new Slices.ExtensionSliceValue[0]);
    }
}
