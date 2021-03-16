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
 * A Position (spatial) cutout.
 */
public class PolygonCutout extends ShapeCutout<Polygon> {
    private static final Logger LOGGER = Logger.getLogger(PolygonCutout.class);

    public PolygonCutout(final Header header) throws HeaderCardException {
        super(header);
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
        return getPositionBounds(cutoutBound);
    }

    /**
     * Compute the range of pixel indices that correspond to the supplied
     * polygon. This method computes the cutout ranges within the image. The
     * returned value is null if there is no intersection, an int[4] for a
     * cutout, and an int[0] when the image is wholly contained within the
     * polygon (and no cutout is necessary).
     *
     * @param polygon The shape to cutout
     * @return int[4] holding [x1, x2, y1, y2], int[0] if all pixels are included,
     *      or null if the circle does not intersect the WCS
     */
    private long[] getPositionBounds(final Polygon polygon) throws NoSuchKeywordException {
        final CoordSys coOrdSys = inferCoordSys();

        // detect necessary conversion of target coOrds to native WCS coOrdSys
        final boolean gal = CoordSys.GAL.equals(coOrdSys.getName());
        final boolean fk4 = CoordSys.FK4.equals(coOrdSys.getName());
        final int naxis = this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXIS.key());
        final long naxis1 = this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXIS1.key());
        final long naxis2 = this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXIS2.key());

        if (!CoordSys.ICRS.equals(coOrdSys.getName())
            && !CoordSys.FK5.equals(coOrdSys.getName())
            && !gal
            && !fk4) {
            throw new UnsupportedOperationException("unexpected coordsys: " + coOrdSys.getName());
        }

        final Transform transform = new Transform(this.fitsHeaderWCSKeywords);
        LOGGER.debug("Transform is\n" + transform);
        final Polygon polygonToCut = new Polygon();

        // npoly is in ICRS
        if (gal || fk4) {
            // convert npoly to native coordsys, in place since we created a new npoly above
            LOGGER.debug("converting coordinate system to " + coOrdSys);
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
        double y1 = x1;
        double y2 = -1.0D * y1;
        LOGGER.debug("Bounding box is " + polygonToCut);
        for (final Point point : polygonToCut.getVertices()) {
            final double[] spatialCoords = new double[2];

            if (coOrdSys.isSwappedAxes()) {
                LOGGER.debug("Coordinates are swapped.");
                spatialCoords[0] = point.getLatitude();
                spatialCoords[1] = point.getLongitude();
            } else {
                spatialCoords[0] = point.getLongitude();
                spatialCoords[1] = point.getLatitude();
            }

            // Pad the axes that go beyond what we're interested in (NAXISn where n > 2).
            final double[] worldCoords = Arrays.copyOf(spatialCoords, naxis);

            for (int i = spatialCoords.length; i < worldCoords.length; i++) {
                worldCoords[i] = this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXISn.n(i + 1).key());
            }

            LOGGER.debug("Coordinates to transform are " + Arrays.toString(worldCoords));
            final Transform.Result tr = transform.sky2pix(worldCoords);

            // if tr is null, it was a long way away from the WCS and does not
            // impose a limit/cutout - so we can safely skip it
            if (tr != null) {
                LOGGER.debug("Transformed coordinates: " + Arrays.toString(tr.coordinates));

                x1 = Math.min(x1, tr.coordinates[0]);
                x2 = Math.max(x2, tr.coordinates[0]);
                y1 = Math.min(y1, tr.coordinates[1]);
                y2 = Math.max(y2, tr.coordinates[1]);
            }
        }

        final double buffer = 0.5D;
        final long ix1 = (long) Math.floor(x1 + buffer);
        final long ix2 = (long) Math.ceil(x2 - buffer);
        final long iy1 = (long) Math.floor(y1 + buffer);
        final long iy2 = (long) Math.ceil(y2 - buffer);

        LOGGER.debug("Rounded from [" + Arrays.toString(new double[]{x1, x2, y1, y2}) + "]");

        // clipping
        LOGGER.debug("Clipping box " + Arrays.toString(new long[]{ix1, ix2, iy1, iy2}) + " into "
                     + Arrays.toString(new long[]{naxis1, naxis2}));

        final long[] clippedBox = clip(naxis1, naxis2, ix1, ix2, iy1, iy2);

        LOGGER.debug("Clipping OK: " + Arrays.toString(clippedBox));

        return clippedBox;
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
