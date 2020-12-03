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
import ca.nrc.cadc.util.StringUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import nom.tam.fits.BasicHDU;
import nom.tam.fits.Data;
import nom.tam.fits.Fits;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.ImageData;
import nom.tam.fits.ImageHDU;
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
     * @throws FitsException    Any FITS related errors from the NOM TAM Fits library.
     * @throws IOException      Reading Writing errors.
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
     * @param randomAccessDataObject         The RandomAccess object to read bytes from.  This method will not close
     *                                       this file.
     * @param slices       The string value of the pixels to extract.
     * @param outputStream Where to write bytes to.  This method will not close this stream.
     * @throws FitsException    Any FITS related errors from the NOM TAM Fits library.
     * @throws IOException      Reading Writing errors.
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

        final Fits fits = new Fits(randomAccessDataObject);

        // Reduce the requested extensions to only those that overlap.
        final Slices.ExtensionSliceValue[] extensionSliceValues = getOverlap(fits, requestedExtensionSliceValues);

        if (ArrayUtil.isEmpty(extensionSliceValues)) {
            throw new FitsException("No overlap found.");
        } else {
            LOGGER.debug("Found " + extensionSliceValues.length + " overlapping slices.");
        }

        // This count is available because the getOverlap call above will read through and cache the HDUs.
        final int hduCount = fits.getNumberOfHDUs();

        // Read the primary header first.
        final BasicHDU<?> firstHDU = fits.getHDU(0);
        final Header primaryHeader = firstHDU.getHeader();
        final boolean mefOutput = extensionSliceValues.length > 1;
        final boolean mefInput = hduCount > 1;

        LOGGER.debug("\nMEF Output: " + mefOutput + "\nMEF Input: " + mefInput);

        // The caller is expecting more than one extension.
        boolean firstHDUAlreadyWritten = false;
        if (mefInput && mefOutput) {
            final Header headerCopy = ImageHDU.manufactureHeader(firstHDU.getData());
            final boolean primaryHDUHasData = hasData(firstHDU);
            copyHeader(primaryHeader, headerCopy);

            // If the primary HDU contains data, then the MEF output will contain just XTENSION HDUs each with
            // data.
            if (primaryHDUHasData) {
                headerCopy.deleteKey(Standard.SIMPLE);

                final HeaderCard xtensionCard = headerCopy.findCard(Standard.XTENSION);

                if (xtensionCard == null) {
                    headerCopy.addValue(Standard.XTENSION, Standard.XTENSION_IMAGE);
                }
            } else {
                // We're cutting from a Simple FITS file, or an MEF with no data in the primary HDU.  Either way,
                // the header gets modified.

                final HeaderCard nextEndCard = headerCopy.findCard(DataDescription.NEXTEND);

                // Adjust the NEXTEND appropriately.
                if (nextEndCard == null) {
                    headerCopy.addValue(DataDescription.NEXTEND, Integer.toString(extensionSliceValues.length));
                } else {
                    nextEndCard.setValue(Integer.toString(extensionSliceValues.length));
                }
            }

            headerCopy.write(output);
            output.flush();

            firstHDUAlreadyWritten = true;
        }
        // Output a simple FITS file otherwise.

        // Iterate the requested cutouts.  Lookup the HDU for each one and use the NOM TAM Tiler to pull out an
        // array of primitives.
        for (Slices.ExtensionSliceValue extensionSliceValue : extensionSliceValues) {
            LOGGER.debug("Next extension slice value " + extensionSliceValue);
            try {
                final BasicHDU<?> hdu = getHDU(fits, extensionSliceValue);

                // This is possible if the HDU cannot be found.
                if (hdu != null) {
                    if (hdu instanceof ImageHDU) {
                        final ImageHDU imageHDU = (ImageHDU) hdu;
                        final Header header = imageHDU.getHeader();
                        final int[] dimensions = imageHDU.getAxes();

                        if (dimensions == null) {
                            throw new FitsException("Sub-image not within image");
                        }

                        final int dimensionLength = dimensions.length;
                        final int[] corners = new int[dimensionLength];
                        final int[] lengths = new int[dimensionLength];
                        final int[] steps = new int[dimensionLength];

                        fillCornersAndLengths(dimensionLength, dimensions, header, extensionSliceValue, corners,
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
                            final Header headerCopy = new Header();

                            copyHeader(header, headerCopy);

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

                            final Data imageData = new ImageData(imageTiler.getTile(corners, lengths));

                            headerCopy.write(output);
                            imageData.write(output);

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
                    } else {
                        // Try to dump the header out to show that it matched
                        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                             final PrintStream printStream = new PrintStream(byteArrayOutputStream)) {
                            hdu.getHeader().dumpHeader(printStream);
                            LOGGER.warn("Skipping matching extension \n\n " + byteArrayOutputStream.toString()
                                        + "\n\n as it's NOT an Image HDU.");
                        } catch (Exception e) {
                            LOGGER.warn(e.getMessage(), e);
                        }
                    }
                } else {
                    LOGGER.warn("No such extension for requested cutout " + extensionSliceValue);
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
    }

    /**
     * Populate the corners and lengths of the tile to pull.  This method will fill the <code>corners</code>,
     * <code>lengths</code>, and <code>steps</code> arrays to be used by the FITS Tiler.
     *
     * @param dimensionLength       The full size of the dimension.  Used to fill in values that are not specified.
     * @param dimensions            The dimension values to pad with.
     * @param header                The Header to set NAXIS values for as they are calculated.
     * @param extensionSliceValue   The requested cutout.
     * @param corners               The corners array to indicate starting pixel points.
     * @param lengths               The lengths of each dimension to cutout.
     * @param steps                 For striding, these values will be something other than 1.
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
     * @param source        The source Header.
     * @param destination   The Header to write to.
     * @throws HeaderCardException  Any I/O with Header Cards.
     */
    private void copyHeader(final Header source, final Header destination) throws HeaderCardException {
        for (final Iterator<HeaderCard> headerCardIterator = source.iterator();
             headerCardIterator.hasNext();) {
            final HeaderCard headerCard = headerCardIterator.next();
            final String headerCardKey = headerCard.getKey();
            LOGGER.debug("Checking next card " + headerCardKey + "(" + headerCard.getComment() + ")");
            final HeaderCard copyHeaderCard = destination.findCard(headerCardKey);
            final Class<?> valueType = headerCard.valueType();

            if (copyHeaderCard == null) {
                LOGGER.debug("No " + headerCardKey + " found.  Adding it with value "
                             + headerCard.getValue());
                if (Standard.COMMENT.key().equals(headerCardKey)) {
                    destination.insertComment(headerCard.getComment());
                } else if (Standard.HISTORY.key().equals(headerCardKey)) {
                    destination.insertHistory(headerCard.getComment());
                } else {
                    if (valueType == String.class) {
                        destination.addValue(headerCard.getKey(), headerCard.getValue(),
                                            headerCard.getComment());
                    } else if (valueType == Boolean.class) {
                        destination.addValue(headerCard.getKey(), Boolean.parseBoolean(headerCard.getValue()),
                                            headerCard.getComment());
                    } else if (valueType == Integer.class) {
                        destination.addValue(headerCard.getKey(), Integer.parseInt(headerCard.getValue()),
                                            headerCard.getComment());
                    } else if (valueType == Long.class) {
                        destination.addValue(headerCard.getKey(), Long.parseLong(headerCard.getValue()),
                                            headerCard.getComment());
                    } else if (valueType == Double.class) {
                        destination.addValue(headerCard.getKey(), Double.parseDouble(headerCard.getValue()),
                                            headerCard.getComment());
                    } else if (valueType == BigDecimal.class || valueType == BigInteger.class) {
                        destination.addValue(headerCard.getKey(), new BigDecimal(headerCard.getValue()),
                                            headerCard.getComment());
                    }
                }
            } else {
                LOGGER.debug("Updating " + headerCardKey + " with value " + headerCard.getValue()
                             + " (" + valueType + ")");

                if (valueType == String.class) {
                    copyHeaderCard.setValue(headerCard.getValue());
                } else if (valueType == Boolean.class) {
                    copyHeaderCard.setValue(Boolean.parseBoolean(headerCard.getValue()));
                } else if (valueType == Integer.class) {
                    copyHeaderCard.setValue(Integer.parseInt(headerCard.getValue()));
                } else if (valueType == Long.class) {
                    copyHeaderCard.setValue(Long.parseLong(headerCard.getValue()));
                } else if (valueType == BigDecimal.class || valueType == BigInteger.class) {
                    copyHeaderCard.setValue(new BigDecimal(headerCard.getValue()));
                } else if (valueType == Double.class) {
                    copyHeaderCard.setValue(Double.parseDouble(headerCard.getValue()));
                }

            }

            // A new HeaderCard must be created here to remove the reference from the previous one.
            destination.updateLine(headerCardKey, new HeaderCard(headerCardKey, headerCard.getValue(),
                                                                 headerCard.getComment()));
        }
    }

    private boolean hasData(final BasicHDU<?> hdu) {
        final Data data = hdu.getData();
        return (data != null) && (data.getSize() > 0L);
    }

    private BasicHDU<?> getHDU(final Fits fits, final Slices.ExtensionSliceValue extensionSliceValue)
            throws FitsException, IOException {
        final String sliceValueExtensionName = extensionSliceValue.getExtensionName();

        if (StringUtil.hasText(sliceValueExtensionName)) {
            return getHDU(fits, sliceValueExtensionName, extensionSliceValue.getExtensionVersion());
        } else if (extensionSliceValue.getExtensionIndex() != null) {
            return fits.getHDU(extensionSliceValue.getExtensionIndex());
        } else {
            return null;
        }
    }

    /**
     * Obtain the HDU whose Extension name (<code>EXTNAME</code>) and (optionally) Extension version
     * (<code>EXTVER</code>) values match.
     *
     * <p>If no <code>extensionVersion</code> is provided, then this will return the first HDU whose name
     * (<code>EXTNAME</code>) matches the given <code>extensionName</code>.
     *
     * <p>This traverses one way through the file until the sought-after HDU is found.  This could leave this Fits in an
     * inconsistent state as it will not start at the top.  The caller would need to create a new Fits every time they
     * want to find an HDU this way.
     *
     * @param extensionName
     *            The name (<code>EXTNAME</code>) value of the HDU header to be read.  Required.
     * @param extensionVersion
     *            The version (<code>EXTVER</code>) value of the HDU header to match (if present).  Optional.
     * @return The HDU matching the arguments, or null if it could not be found.
     * @throws FitsException
     *             if the header could not be read
     * @throws IOException
     *             if the underlying buffer threw an error
     */
    public BasicHDU<?> getHDU(Fits fits, String extensionName, Integer extensionVersion)
            throws FitsException, IOException {
        // Check the cache first.
        int size = fits.getNumberOfHDUs();
        for (int i = 0; i <= size; i++) {
            final BasicHDU<?> nextHDU = fits.getHDU(i);
            if (matchHDU(nextHDU, extensionName, extensionVersion)) {
                return nextHDU;
            }
        }

        // Read the rest of the HDUs next.
        BasicHDU<?> hdu;
        while ((hdu = fits.readHDU()) != null) {
            if (matchHDU(hdu, extensionName, extensionVersion)) {
                return hdu;
            }
        }

        return null;
    }

    private boolean matchHDU(final BasicHDU<?> hdu, String extensionName, Integer extensionVersion) {
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



    private Slices.ExtensionSliceValue[] getOverlap(final Fits fits,
                                                    final Slices.ExtensionSliceValue[] extensionSliceValues)
            throws FitsException, IOException {

        // A Set is used to eliminate duplicates from the inner loop below.
        final Set<Slices.ExtensionSliceValue> extensionSliceValueList = new LinkedHashSet<>();

        for (final Slices.ExtensionSliceValue extensionSliceValue : extensionSliceValues) {
            final BasicHDU<?> hdu = getHDU(fits, extensionSliceValue);
            if (hdu != null) {
                final int[] dimensions = hdu.getAxes();

                if (dimensions != null) {
                    for (final int maxUpperBound : dimensions) {
                        for (final PixelRange pixelRange : extensionSliceValue.getRanges(maxUpperBound)) {
                            if (pixelRange.getLowerBound() < maxUpperBound) {
                                extensionSliceValueList.add(extensionSliceValue);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return extensionSliceValueList.toArray(new Slices.ExtensionSliceValue[0]);
    }
}
