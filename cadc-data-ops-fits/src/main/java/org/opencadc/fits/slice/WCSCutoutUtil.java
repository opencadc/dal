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
import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.dali.Range;
import ca.nrc.cadc.dali.Shape;
import ca.nrc.cadc.wcs.exceptions.NoSuchKeywordException;

import java.util.List;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import org.opencadc.soda.server.Cutout;


/**
 * Utility class to provide bounds for the given WCS Cutout.  In the unlikely event that multiple cutouts are specified,
 * only one will be honoured, with priority given to POSITION, then BAND, then TIME, and finally POLARIZATION.
 * Pixel cutouts are handled outside of this class.
 */
public class WCSCutoutUtil {
    public static long[] getBounds(final Header header, final Cutout cutout)
            throws HeaderCardException, NoSuchKeywordException {
        if (cutout.pos != null) {
            return WCSCutoutUtil.getSpatialBounds(header, cutout.pos);
        } else if (cutout.band != null) {
            return WCSCutoutUtil.getSpectralBounds(header, cutout.band);
        } else if (cutout.time != null) {
            return WCSCutoutUtil.getTemporalBounds(header, cutout.time);
        } else if ((cutout.pol != null) && !cutout.pol.isEmpty()) {
            return WCSCutoutUtil.getPolarizationBounds(header, cutout.pol);
        } else {
            return null;
        }
    }

    static long[] getSpatialBounds(final Header header, final Shape shape)
            throws HeaderCardException, NoSuchKeywordException {
        if (shape instanceof Circle) {
            return new CircleCutout(header).getBounds((Circle) shape);
        } else if (shape instanceof Polygon) {
            return new PolygonCutout(header).getBounds((Polygon) shape);
        } else if (shape instanceof Range) {
            return new RangeCutout(header).getBounds((Range) shape);
        } else {
            return null;
        }
    }

    static long[] getSpectralBounds(final Header header, final Interval<Number> spectralInterval)
            throws HeaderCardException, NoSuchKeywordException {
        return new EnergyCutout(header).getBounds(spectralInterval);
    }

    static long[] getTemporalBounds(final Header header, final Interval<Number> temporalInterval)
            throws HeaderCardException {
        throw new UnsupportedOperationException("Temporal not yet implemented.");
    }

    static long[] getPolarizationBounds(final Header header, final List<String> polarizationStates)
            throws HeaderCardException {
        throw new UnsupportedOperationException("Polarization not yet implemented.");
    }
}
