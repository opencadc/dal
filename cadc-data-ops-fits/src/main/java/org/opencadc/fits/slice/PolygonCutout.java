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

import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.wcs.Transform;
import ca.nrc.cadc.wcs.exceptions.NoSuchKeywordException;
import ca.nrc.cadc.wcs.exceptions.WCSLibRuntimeException;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jsky.coords.wcscon;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.header.Standard;
import org.apache.log4j.Logger;


/**
 * A Spatial Polygon cutout.  This class is executed after the inputs are parsed into an appropriate Polygon shape.
 */
public class PolygonCutout extends ShapeCutout<Polygon> {
    private static final Logger LOGGER = Logger.getLogger(PolygonCutout.class);

    public PolygonCutout(final Header header) throws HeaderCardException {
        super(header);
    }

    public PolygonCutout(FITSHeaderWCSKeywords fitsHeaderWCSKeywords) {
        super(fitsHeaderWCSKeywords);
    }

    /**
     * Obtain the bounds of the given cutout.
     *
     * @param cutoutBound The bounds (shape, interval etc.) of the cutout.
     * @return long[] array of overlapping bounds, or long[0] if all pixels are included.
     * @throws NoSuchKeywordException Unknown keyword found.
     * @throws WCSLibRuntimeException WCSLib (C) error.
     */
    @Override
    public long[] getBounds(final Polygon cutoutBound) throws NoSuchKeywordException, WCSLibRuntimeException {
        try {
            return getPositionBounds(cutoutBound);
        } catch (WCSLibRuntimeException wcsLibRuntimeException) {
            if (wcsLibRuntimeException.getMessage().equals(FITSCutout.INPUT_TOO_DISTANT_ERROR_MESSAGE)) {
                // No overlap
                return null;
            } else {
                throw wcsLibRuntimeException;
            }
        }
    }

    /**
     * Compute the range of pixel indices that correspond to the supplied
     * polygon. This method computes the cutout ranges within the image. The
     * returned value is null if there is no intersection, an long[NAXIS] for a
     * cutout, and an int[0] when the image is wholly contained within the
     * polygon (and no cutout is necessary).
     *
     * @param polygon The shape to cutout
     * @return long[NAXIS], or long[0] if all pixels are included,
     *      or null if the circle does not intersect the WCS
     */
    private long[] getPositionBounds(final Polygon polygon) throws NoSuchKeywordException {
        final int naxis = this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXIS.key());
        final CoordSys coordSys = inferCoordSys();

        // No coordsys could be inferred, or there is no data array, so no cutout available.
        if (coordSys == null || naxis == 0 || coordSys.longitudeAxis < 0 || coordSys.latitudeAxis < 0) {
            return null;
        }

        LOGGER.debug("CoordSys found: " + coordSys);

        // detect necessary conversion of target coords to native WCS coordSys
        final boolean gal = CoordSys.GAL.equals(coordSys.getName());
        final boolean fk4 = CoordSys.FK4.equals(coordSys.getName());
        final long naxisLongitude =
                this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXISn.n(coordSys.longitudeAxis).key());
        final long naxisLatitude =
                this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXISn.n(coordSys.latitudeAxis).key());

        if (!CoordSys.ICRS.equals(coordSys.getName())
            && !CoordSys.FK5.equals(coordSys.getName())
            && !gal
            && !fk4) {
            throw new UnsupportedOperationException("unexpected coordsys: " + coordSys.getName());
        }

        final Transform transform = new Transform(this.fitsHeaderWCSKeywords);
        LOGGER.debug("Transform is\n" + transform);
        final Polygon polygonToCut = new Polygon();

        // npoly is in ICRS
        if (gal || fk4) {
            // convert npoly to native coordsys, in place since we created a new npoly above
            LOGGER.debug("converting coordinate system to " + coordSys);
            final java.util.List<Point> vertices = polygon.getVertices();
            final List<Point> convertedVertices = new ArrayList<>(vertices.size());
            for (final Point point : vertices) {
                final Point2D.Double pp = new Point2D.Double(point.getLongitude(), point.getLatitude());
                final Point2D.Double convertedPP;

                // convert poly coords to native WCS coordsys
                if (gal) {
                    convertedPP = wcscon.fk52gal(pp);
                } else {
                    convertedPP = wcscon.fk524(pp);
                }

                convertedVertices.add(new Point(convertedPP.x, convertedPP.y));
            }

            polygonToCut.getVertices().addAll(convertedVertices);
        } else {
            polygonToCut.getVertices().addAll(polygon.getVertices());
        }

        // convert polygonToCut to pixel coordinates and find min/max
        double x1 = Double.MAX_VALUE;
        double x2 = -1.0D * x1;
        double y1 = Double.MAX_VALUE;
        double y2 = -1.0D * y1;
        final int spatialLongitudeAxisIndex = coordSys.longitudeAxis - 1;
        final int spatialLatitudeAxisIndex = coordSys.latitudeAxis - 1;

        LOGGER.debug("Bounding box is " + polygonToCut);
        for (final Point point : polygonToCut.getVertices()) {
            final double[] worldCoords = new double[naxis];
            // Set the spatial values where they should be.
            worldCoords[spatialLongitudeAxisIndex] = point.getLongitude();
            worldCoords[spatialLatitudeAxisIndex] = point.getLatitude();

            // Fill in the rest of the world coordinates.
            for (int i = 0; i < worldCoords.length; i++) {
                if (i != spatialLongitudeAxisIndex && i != spatialLatitudeAxisIndex) {
                    worldCoords[i] = this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXISn.n(i + 1).key());
                }
            }

            LOGGER.debug("Coordinates to transform are " + Arrays.toString(worldCoords));
            final Transform.Result tr = transform.sky2pix(worldCoords);

            // if tr is null, it was a long way away from the WCS and does not
            // impose a limit/cutout - so we can safely skip it
            if (tr != null) {
                LOGGER.debug("Transformed coordinates: " + Arrays.toString(tr.coordinates));

                x1 = Math.min(x1, tr.coordinates[spatialLongitudeAxisIndex]);
                x2 = Math.max(x2, tr.coordinates[spatialLongitudeAxisIndex]);
                y1 = Math.min(y1, tr.coordinates[spatialLatitudeAxisIndex]);
                y2 = Math.max(y2, tr.coordinates[spatialLatitudeAxisIndex]);

                LOGGER.debug("Current (x, y) values are [" + x1 + ", " + x2 + ", " + y1 + ", " + y2 + "]");
            }
        }

        final double buffer = 0.5D;
        final long ix1 = (long) Math.floor(x1 + buffer);
        final long ix2 = (long) Math.ceil(x2 - buffer);
        final long iy1 = (long) Math.floor(y1 + buffer);
        final long iy2 = (long) Math.ceil(y2 - buffer);

        // clipping
        LOGGER.debug("Clipping box " + Arrays.toString(new long[]{ix1, ix2, iy1, iy2}) + " into "
                     + Arrays.toString(new long[]{naxisLongitude, naxisLatitude}));

        final long[] clippedBox = clip(naxisLongitude, naxisLatitude, ix1, ix2, iy1, iy2);
        final long[] entireBounds = clippedBox == null ? null : Arrays.copyOf(clippedBox, naxis * 2);

        // Pad the entire range to include each axis bounds.
        if (entireBounds != null) {
            for (int i = clippedBox.length; i < entireBounds.length; i += 2) {
                final int axis = (i + 2) / 2;
                entireBounds[i] = 1L;
                entireBounds[i + 1] = (long) this.fitsHeaderWCSKeywords.getDoubleValue(Standard.NAXISn.n(axis).key());
            }
        }

        LOGGER.debug("Clipping OK: " + Arrays.toString(clippedBox));

        return entireBounds;
    }

    private long[] clip(long w, long h, long x1, long x2, long y1, long y2) {
        if (x1 < 1) {
            x1 = 1;
        }
        if (x2 > w) {
            x2 = w;
        }
        if (y1 < 1) {
            y1 = 1;
        }
        if (y2 > h) {
            y2 = h;
        }

        // validity check
        // no pixels included
        if (x1 >= w || x2 <= 1 || y1 >= h || y2 <= 1) {
            return null;
        }
        // all pixels includes
        if (x1 == 1 && y1 == 1 && x2 == w && y2 == h) {
            return new long[0];
        }
        return new long[]{x1, x2, y1, y2}; // an actual cutout
    }
}
