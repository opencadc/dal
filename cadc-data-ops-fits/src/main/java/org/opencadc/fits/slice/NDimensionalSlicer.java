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

import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.wcs.exceptions.NoSuchKeywordException;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.FitsFactory;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
import nom.tam.fits.StreamingImageData;
import nom.tam.fits.header.DataDescription;
import nom.tam.fits.header.Standard;
import nom.tam.image.compression.hdu.CompressedImageHDU;
import nom.tam.util.ArrayDataOutput;
import nom.tam.util.BufferedDataOutputStream;
import nom.tam.util.RandomAccessDataObject;
import nom.tam.util.RandomAccessFileExt;
import org.apache.log4j.Logger;
import org.opencadc.fits.HDUIterator;
import org.opencadc.soda.ExtensionSlice;
import org.opencadc.soda.PixelRange;
import org.opencadc.soda.server.Cutout;

/**
 * Slice out a portion of an image.  This class will support the SODA Shapes and will delegate each cutout to its
 * appropriate handler (e.g. CircleCutout, EnergyCutout, etc.).
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
     * @param cutout       The cutout specification.
     * @param outputStream Where to write bytes to.  This method will not close this stream.
     * @throws FitsException Any FITS related errors from the NOM TAM Fits library.
     * @throws IOException   Reading Writing errors.
     * @throws NoSuchKeywordException   Reading the FITS file failed.
     */
    public void slice(final File fitsFile, final Cutout cutout, final OutputStream outputStream)
            throws FitsException, IOException, NoSuchKeywordException {
        slice(new RandomAccessFileExt(fitsFile, "r"), cutout, outputStream);
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
     * @param cutout                 The cutout specification.
     * @param outputStream           Where to write bytes to.  This method will not close this stream.
     * @throws FitsException Any FITS related errors from the NOM TAM Fits library.
     * @throws IOException   Reading Writing errors.
     * @throws NoSuchKeywordException   Reading the FITS file failed.
     */
    public void slice(final RandomAccessDataObject randomAccessDataObject, final Cutout cutout,
                      final OutputStream outputStream)
            throws FitsException, IOException, NoSuchKeywordException {
        final ArrayDataOutput output = new BufferedDataOutputStream(outputStream);
        slice(randomAccessDataObject, cutout, output);
    }

    private void slice(final RandomAccessDataObject randomAccessDataObject, final Cutout cutout,
                       final ArrayDataOutput output)
            throws FitsException, IOException, NoSuchKeywordException {
        if (isEmpty(cutout)) {
            throw new IllegalStateException("No cutout specified.");
        }

        // Single Fits object for the slice.  It maintains state about itself such as the current read offset.
        final Fits fitsInput = new Fits(randomAccessDataObject);
        fitsInput.setStreamWrite(true);

        // Reduce the requested extensions to only those that overlap.
        final Map<Integer, List<ExtensionSlice>> overlapHDUs = getOverlap(fitsInput, cutout);

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
        final BasicHDU<?> firstHDU = fitsInput.getHDU(0);

        if (firstHDU == null) {
            throw new FitsException("Invalid FITS file (No primary HDU).");
        }

        // MEF output is expected if the number of requested HDUs, or the number of requested values is greater than
        // one.
        final boolean mefOutput = overlapHDUs.size() > 1
                                  || overlapHDUs.values().stream().mapToInt(List::size).sum() > 1;

        final boolean mefInput = hduCount > 1;

        // Will write out to an ArrayDataOutput stream at the end.
        final Fits fitsOutput = new Fits();
        fitsOutput.setStreamWrite(true);

        LOGGER.debug("\nMEF Output: " + mefOutput + "\nMEF Input: " + mefInput);

        // The caller is expecting more than one extension.
        boolean firstHDUAlreadyWritten = false;
        if (mefInput && mefOutput) {
            final boolean hasData = (firstHDU.getData() != null && firstHDU.getData().getSize() > 0);

            // If this primary HDU has no data, then simply add it with some minor verification.
            if (!hasData) {
                final Header primaryHeader = firstHDU.getHeader();
                final Header headerCopy = copyHeader(primaryHeader);

                setupPrimaryHeader(headerCopy, overlapHDUs.size());
                fitsOutput.addHDU(FitsFactory.hduFactory(headerCopy, new ImageData()));

                firstHDUAlreadyWritten = true;
            }
            // Otherwise, the primary HDU has data so treat it like any other HDU in the loop below.
        }
        // Output a simple FITS file otherwise.

        // Iterate the requested cutouts.  Lookup the HDU for each one and use the NOM TAM Tiler to pull out an
        // array of primitives.
        for (final Map.Entry<Integer, List<ExtensionSlice>> overlap : overlapHDUs.entrySet()) {
            final Integer nextHDUIndex = overlap.getKey();

            LOGGER.debug("Next extension slice value at extension " + nextHDUIndex);
            try {
                final BasicHDU<?> hdu = fitsInput.getHDU(nextHDUIndex);
                writeSlices(hdu, overlap.getValue(), fitsOutput, mefOutput, firstHDUAlreadyWritten,
                            overlapHDUs.size() - 1);

                // If it wasn't true before, it is now.
                firstHDUAlreadyWritten = true;
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

    private void setupPrimaryHeader(final Header headerCopy, final int nextEndSize) throws HeaderCardException {
        final HeaderCard nextEndCard = headerCopy.findCard(DataDescription.NEXTEND);

        // Adjust the NEXTEND appropriately.
        if (nextEndCard == null) {
            headerCopy.addValue(DataDescription.NEXTEND, nextEndSize);
        } else {
            nextEndCard.setValue(nextEndSize);
        }

        final HeaderCard extendFlagCard = headerCopy.findCard(Standard.EXTEND);

        // Adjust the EXTEND appropriately.
        if (extendFlagCard == null) {
            headerCopy.addValue(Standard.EXTEND, true);
        } else {
            extendFlagCard.setValue(true);
        }
    }

    private void writeSlices(final BasicHDU<?> hdu, List<ExtensionSlice> extensionSliceList, final Fits fitsOutput,
                             final boolean mefOutput, final boolean firstHDUAlreadyWritten, final int nextEndSize)
            throws FitsException {
        final ImageHDU imageHDU = (hdu instanceof CompressedImageHDU)
                                  ? ((CompressedImageHDU) hdu).asImageHDU() : (ImageHDU) hdu;
        final Header header = imageHDU.getHeader();
        final int[] dimensions = imageHDU.getAxes();

        for (final ExtensionSlice extensionSliceValue : extensionSliceList) {
            if (extensionSliceValue.getPixelRanges().isEmpty()) {
                fitsOutput.addHDU(hdu);
            } else if (dimensions == null) {
                throw new FitsException("Sub-image not within image");
            } else {
                final int dimensionLength = dimensions.length;
                final int[] corners = new int[dimensionLength];
                final int[] lengths = new int[dimensionLength];
                final int[] steps = new int[dimensionLength];

                final Header headerCopy = copyHeader(header);

                fillCornersAndLengths(dimensions, headerCopy, extensionSliceValue, corners, lengths, steps);

                // The data contained in this HDU cannot be used to slice from.
                if (corners.length == 0) {
                    throw new FitsException("Sub-image not within image");
                }

                LOGGER.debug("Tiling out " + Arrays.toString(lengths) + " at corner "
                             + Arrays.toString(corners) + " from extension "
                             + hdu.getTrimmedString(Standard.EXTNAME) + ","
                             + header.getIntValue(Standard.EXTVER, 1));

                // CRPIX values are not set automatically.  Adjust them here, if present.
                for (int i = 0; i < dimensionLength; i++) {
                    final HeaderCard crPixCard = headerCopy.findCard(Standard.CRPIXn.n(i + 1));
                    if (crPixCard != null) {
                        // Need to run backwards (reverse order) to match the dimensions.
                        final double nextValue = corners[corners.length - i - 1];

                        crPixCard.setValue(Double.parseDouble(crPixCard.getValue()) - nextValue);
                    }
                }

                if (mefOutput) {
                    if (firstHDUAlreadyWritten) {
                        headerCopy.setXtension(Standard.XTENSION_IMAGE);
                        final HeaderCard pcountHeaderCard = headerCopy.findCard(Standard.PCOUNT);
                        final HeaderCard gcountHeaderCard = headerCopy.findCard(Standard.GCOUNT);

                        if (pcountHeaderCard == null) {
                            headerCopy.addValue(Standard.PCOUNT, 0);
                        }

                        if (gcountHeaderCard == null) {
                            headerCopy.addValue(Standard.GCOUNT, 1);
                        }
                    } else {
                        headerCopy.setSimple(true);
                        setupPrimaryHeader(headerCopy, nextEndSize);
                    }
                } else {
                    // MEF input to simple output.
                    headerCopy.setSimple(true);
                }

                final StreamingImageData newImageData =
                        (StreamingImageData) FitsFactory.dataFactory(headerCopy, true);
                newImageData.setTile(corners, lengths);

                fitsOutput.addHDU(FitsFactory.hduFactory(headerCopy, newImageData));
            }
        }
    }

    private boolean isEmpty(final Cutout cutout) {
        return cutout.pos == null && cutout.band == null && cutout.time == null
               && (cutout.pol == null || cutout.pol.isEmpty()) && cutout.custom == null
               && cutout.customAxis == null && (cutout.pixelCutouts == null || cutout.pixelCutouts.isEmpty());
    }

    /**
     * Populate the corners and lengths of the tile to pull.  This method will fill the <code>corners</code>,
     * <code>lengths</code>, and <code>steps</code> arrays to be used by the FITS Tiler.
     *
     * @param dimensions          The dimension values to pad with.
     * @param header              The Header to set NAXIS values for as they are calculated.
     * @param extensionSliceValue The requested cutout.
     * @param corners             The corners array to indicate starting pixel points.
     * @param lengths             The lengths of each dimension to cutout.
     * @param steps               For striding, these values will be something other than 1.
     */
    private void fillCornersAndLengths(final int[] dimensions, final Header header,
                                       final ExtensionSlice extensionSliceValue, final int[] corners,
                                       final int[] lengths, final int[] steps) {

        LOGGER.debug("Full dimensions are " + Arrays.toString(dimensions));
        final int dimensionLength = dimensions.length;

        // Pad the bounds with the full dimensions as necessary.
        for (int i = 0; i < dimensionLength; i++) {
            // Need to pull values in reverse order as the dimensions (axes) are delivered in reverse order.
            final int maxRegionSize = dimensions[dimensionLength - i - 1];
            final List<PixelRange> pixelRanges = extensionSliceValue.getPixelRanges();
            final int rangeSize = pixelRanges.size();
            final PixelRange pixelRange;
            if (rangeSize > i) {
                pixelRange = pixelRanges.get(i);
            } else {
                pixelRange = new PixelRange(0, maxRegionSize);
            }

            final int rangeLowBound = pixelRange.lowerBound;
            final int rangeUpBound = pixelRange.upperBound;
            final int rangeStep = pixelRange.step;

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

    private void mapOverlap(final Header header, final Cutout cutout, final int hduIndex,
                            final Map<Integer, List<ExtensionSlice>> overlapHDUIndexesSlices)
            throws HeaderCardException, NoSuchKeywordException {
        final PixelRange[] pixelCutoutBounds = WCSCutoutUtil.getBounds(header, cutout);
        if (pixelCutoutBounds.length > 0) {
            final ExtensionSlice overlapSlice = new ExtensionSlice(hduIndex);
            overlapSlice.getPixelRanges().addAll(Arrays.asList(pixelCutoutBounds));

            final List<ExtensionSlice> overlapSlices = overlapHDUIndexesSlices.containsKey(hduIndex)
                                                       ? overlapHDUIndexesSlices.get(hduIndex)
                                                       : new ArrayList<>();
            overlapSlices.add(overlapSlice);
            overlapHDUIndexesSlices.put(hduIndex, overlapSlices);
        }
    }

    // find the subset of slices that overlap the specified HDU; there can be multiple
    // because the pixel cutout API allows for multiple cutouts (pixel ranges) to be extracted
    // from a single extension
    // TODO: if the requested slices contain a mix of extensionName- and extensionIndex-specified
    // slices, then two of the slices could specify/generate the exact same (duplicate) output (if the
    // pixelrange(s) are the same or absent
    private int mapOverlap(final BasicHDU<?> hdu, final int hduIndex, final List<ExtensionSlice> extensionSlices,
                           final Map<Integer, List<ExtensionSlice>> overlapHDUIndexesSlices)
            throws FitsException {
        final List<ExtensionSlice> overlappingSlices = new ArrayList<>();

        if (hdu != null) {
            for (final ExtensionSlice slice : extensionSlices) {
                if (matchHDU(hdu, slice.extensionName, slice.extensionVersion)
                    || ((slice.extensionIndex != null) && (hduIndex == slice.extensionIndex))) {

                    // Entire extension requested, so it matters not that it may not be an Image HDU.
                    if (slice.getPixelRanges().isEmpty()) {
                        overlappingSlices.add(slice);
                    } else if (!(hdu instanceof ImageHDU) && !(hdu instanceof CompressedImageHDU)) {
                        throw new UnsupportedOperationException(
                                "Unable to slice from HDU of type: " + hdu.getClass().getSimpleName());
                    } else {
                        final PixelCutout pixelCutout = new PixelCutout(hdu.getHeader());
                        final ExtensionSlice overlapSlice = getOverlap(slice, pixelCutout);
                        if (overlapSlice != null) {
                            overlappingSlices.add(overlapSlice);
                        }
                    }
                }
            }
        }

        if (overlappingSlices.size() > 0) {
            overlapHDUIndexesSlices.put(hduIndex, overlappingSlices);
        }

        return overlappingSlices.size();
    }

    /**
     * Obtain the overlapping indexes of matching HDUs.  This will return a unique list of Integers in file order,
     * rather than the order that they were requested.
     *
     * @param fits    The Fits object to scan.
     * @param cutout  The requested cutout.
     * @return  An Map of overlapping hduIndex->slice[], or empty Map.  Never null.
     * @throws FitsException if the header could not be read
     */
    private Map<Integer, List<ExtensionSlice>> getOverlap(final Fits fits, final Cutout cutout)
            throws FitsException, NoSuchKeywordException {
        if ((cutout.pixelCutouts != null) && !cutout.pixelCutouts.isEmpty()) {
            return getOverlap(fits, cutout.pixelCutouts);
        } else {
            // A Set is used to eliminate duplicates from the inner loop below.
            final Map<Integer, List<ExtensionSlice>> overlapHDUIndexesSlices = new LinkedHashMap<>();

            // Walk through the cache first.
            int hduIndex = 0;

            for (final HDUIterator hduIterator = new HDUIterator(fits); hduIterator.hasNext();) {
                final BasicHDU<?> hdu = hduIterator.next();
                final Header header = hdu.getHeader();
                mapOverlap(header, cutout, hduIndex, overlapHDUIndexesSlices);
                hduIndex++;
            }

            return overlapHDUIndexesSlices;
        }
    }

    private Map<Integer, List<ExtensionSlice>> getOverlap(final Fits fits, final List<ExtensionSlice> extensionSlices)
            throws FitsException {
        // A Set is used to eliminate duplicates from the inner loop below.
        final Map<Integer, List<ExtensionSlice>> overlapHDUIndexesSlices = new LinkedHashMap<>();

        int matchCount = 0;
        int hduIndex = 0;
        for (final HDUIterator hduIterator = new HDUIterator(fits);
             hduIterator.hasNext() && matchCount < extensionSlices.size();) {
            final BasicHDU<?> hdu = hduIterator.next();
            matchCount += mapOverlap(hdu, hduIndex, extensionSlices, overlapHDUIndexesSlices);
            hduIndex++;
        }

        // Check for missing matches.
        final List<ExtensionSlice> matchedValues =
                overlapHDUIndexesSlices.values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        final List<ExtensionSlice> containsAll =
                extensionSlices.stream().filter(e -> {
                    boolean contained = false;
                    for (final ExtensionSlice extensionSlice : matchedValues) {
                        final List<PixelRange> matchedPixelRange = extensionSlice.getPixelRanges();
                        final List<PixelRange> requestedPixelRange = e.getPixelRanges();
                        if ((matchedPixelRange.isEmpty() && requestedPixelRange.isEmpty())
                            || requestedPixelRange.containsAll(matchedPixelRange)) {
                            contained = true;
                            break;
                        }
                    }
                    return !contained;
                }).collect(Collectors.toList());

        if (!containsAll.isEmpty()) {
            throw new IllegalArgumentException("One or more requested slices could not be found:\n" + containsAll);
        }

        return overlapHDUIndexesSlices;
    }

    private ExtensionSlice getOverlap(final ExtensionSlice extensionSlice, final PixelCutout pixelCutout) {
        final long[] pixelCutoutBounds = pixelCutout.getBounds(extensionSlice);
        if (pixelCutoutBounds == null) {
            return null;
        } else {
            final ExtensionSlice overlapSlice = extensionSlice.extensionIndex == null
                                                ? new ExtensionSlice(extensionSlice.extensionName,
                                                                     extensionSlice.extensionVersion)
                                                : new ExtensionSlice(extensionSlice.extensionIndex);

            for (int i = 0; i < pixelCutoutBounds.length; i += 2) {
                overlapSlice.getPixelRanges().add(
                        new PixelRange((int) pixelCutoutBounds[i], (int) pixelCutoutBounds[i + 1]));
            }

            return overlapSlice;
        }
    }
}
