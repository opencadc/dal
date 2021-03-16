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

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.wcs.exceptions.NoSuchKeywordException;
import ca.nrc.cadc.wcs.exceptions.WCSLibRuntimeException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;


public class CircleCutout extends ShapeCutout<Circle> {

    public CircleCutout(final Header header) throws HeaderCardException {
        super(header);
    }


    /**
     * Obtain the bounds of the given cutout.
     *
     * @param cutoutBound The bounds (shape, interval etc.) of the cutout.
     * @return long[] array of overlapping bounds, or long[0] if all pixels are included.
     * @throws NoSuchKeywordException Unknown keyword found.
     * @throws WCSLibRuntimeException WCSLib (C) error.
     * @throws HeaderCardException    If a FITS Header card couldn't be read.
     */
    @Override
    public long[] getBounds(final Circle cutoutBound) throws NoSuchKeywordException, WCSLibRuntimeException,
                                                             HeaderCardException {
        return getPositionBounds(cutoutBound);
    }

    /**
     * Find the pixel bounds that enclose the specified circle.
     *
     * @param circle circle with center in ICRS coordinates
     * @return int[4] holding [x1, x2, y1, y2], int[0] if all pixels are included,
     * or null if the circle does not intersect the WCS
     * @throws NoSuchKeywordException Unknown keyword found.
     * @throws WCSLibRuntimeException WCSLib (C) error.
     */
    private long[] getPositionBounds(final Circle circle)
            throws NoSuchKeywordException, WCSLibRuntimeException, HeaderCardException {
        final double x = circle.getCenter().getLongitude();
        final double y = circle.getCenter().getLatitude();
        final double radius = circle.getRadius();
        final double dx = Math.abs(radius / Math.cos(Math.toRadians(y)));

        final Polygon boundingBox = new Polygon();
        boundingBox.getVertices().add(rangeReduce(x - dx, y - radius));
        boundingBox.getVertices().add(rangeReduce(x + dx, y - radius));
        boundingBox.getVertices().add(rangeReduce(x + dx, y + radius));
        boundingBox.getVertices().add(rangeReduce(x - dx, y + radius));

        final PolygonCutout polygonCutout = new PolygonCutout(this.fitsHeaderWCSKeywords.getHeader());
        return polygonCutout.getBounds(boundingBox);
    }

    /**
     * Modify argument vertex so that coordinates are in [0,360] and [-90,90].
     *
     * @param longitude The longitude to check
     * @param latitude  The latitude to check
     * @return the same vertex for convenience
     *
     */
    private Point rangeReduce(final double longitude, final double latitude) {
        double retLongitude = longitude;
        double retLatitude = latitude;

        if (retLatitude > 90.0) {
            retLongitude += 180.0;
            retLatitude = 180.0 - retLatitude;
        }

        if (retLatitude < -90.0) {
            retLongitude += 180.0;
            retLatitude = -180.0 - retLatitude;
        }

        if (retLongitude < 0) {
            retLongitude += 360.0;
        }

        if (retLongitude > 360.0) {
            retLongitude -= 360.0;
        }

        return new Point(retLongitude, retLatitude);
    }
}
