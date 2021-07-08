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

import ca.nrc.cadc.dali.DaliUtil;
import ca.nrc.cadc.wcs.exceptions.NoSuchKeywordException;
import ca.nrc.cadc.wcs.exceptions.WCSLibRuntimeException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import org.apache.log4j.Logger;


/**
 * Abstract base class for Cutouts.
 * @param <T>   The type of input to the cutout.
 */
public abstract class FITSCutout<T> {
    private static final Logger LOGGER = Logger.getLogger(FITSCutout.class);

    protected final FITSHeaderWCSKeywords fitsHeaderWCSKeywords;

    public FITSCutout(final Header header) throws HeaderCardException {
        DaliUtil.assertNotNull("header", header);
        postProcess(header);
        this.fitsHeaderWCSKeywords = new FITSHeaderWCSKeywords(header);
    }

    protected FITSCutout(final FITSHeaderWCSKeywords fitsHeaderWCSKeywords) {
        DaliUtil.assertNotNull("fitsHeaderWCSKeywords", fitsHeaderWCSKeywords);
        this.fitsHeaderWCSKeywords = fitsHeaderWCSKeywords;
    }

    /**
     * Implementors can override this to further process the Header to accommodate different cutout types.  Leave empty
     * if no further processing needs to be done.
     * This method MUST be called before the fitsHeaderWCSKeywords is created as that object cannot be modified.
     * @param header    The Header to modify.
     * @throws HeaderCardException  if modification fails.
     */
    protected void postProcess(final Header header) throws HeaderCardException {

    }

    /**
     * Obtain the bounds of the given cutout.
     * @param cutoutBound   The bounds (shape, interval etc.) of the cutout.
     * @return  long[] array of overlapping bounds, long[0] if all pixels are included, or null if no overlap.
     *
     * @throws NoSuchKeywordException Unknown keyword found.
     * @throws WCSLibRuntimeException WCSLib (C) error.
     * @throws HeaderCardException  If a FITS Header card couldn't be read.
     */
    public abstract long[] getBounds(final T cutoutBound)
            throws NoSuchKeywordException, WCSLibRuntimeException, HeaderCardException;

    /**
     * Clip the given bounds for the bounding range of the given axis.
     * @param len   The max length to clip at.
     * @param lower The lower end to check.
     * @param upper The upper end to check.
     * @return  The array clipped, or empty array for entire data, or null if no overlap.
     */
    long[] clip(final long len, final double lower, final double upper) {

        long x1 = (long) Math.floor(lower);
        long x2 = (long) Math.ceil(upper);

        if (x1 < 1) {
            x1 = 1;
        }

        if (x2 > len) {
            x2 = len;
        }

        LOGGER.debug("clip: " + len + " (" + x1 + ":" + x2 + ")");

        // all pixels includes
        if (x1 == 1 && x2 == len) {
            LOGGER.warn("clip: all");
            return new long[0];
        }

        // no pixels included
        if (x1 > len || x2 < 1) {
            LOGGER.warn("clip: none");
            return null;
        }

        // an actual cutout
        return new long[]{x1, x2};
    }
}
