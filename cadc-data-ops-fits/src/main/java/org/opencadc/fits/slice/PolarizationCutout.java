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

import ca.nrc.cadc.wcs.exceptions.WCSLibRuntimeException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.header.Standard;
import org.apache.log4j.Logger;


public class PolarizationCutout extends FITSCutout<String[]> {
    private static final Logger LOGGER = Logger.getLogger(PolarizationCutout.class);

    public PolarizationCutout(final Header header) throws HeaderCardException {
        super(header);
    }

    public PolarizationCutout(final FITSHeaderWCSKeywords fitsHeaderWCSKeywords) {
        super(fitsHeaderWCSKeywords);
    }

    /**
     * Obtain the bounds of the given cutout.
     *
     * @param states The bounds (Stokes states).
     * @return long[] array of overlapping bounds, or long[0] if all pixels are included.
     * @throws WCSLibRuntimeException WCSLib (C) error.
     */
    @Override
    public long[] getBounds(final String[] states) throws WCSLibRuntimeException {
        final int polarizationAxis = this.fitsHeaderWCSKeywords.getPolarizationAxis();
        final double crpix = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CRPIXn.n(polarizationAxis).key());
        final double crval = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CRVALn.n(polarizationAxis).key());
        final double cdelt = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CDELTn.n(polarizationAxis).key());

        double pix1 = Double.MAX_VALUE;
        double pix2 = (-1 * Double.MAX_VALUE) - 1.0D;
        for (final PolarizationState headerState : getHeaderStates(polarizationAxis)) {
            LOGGER.debug("Checking next header state " + headerState.name());
            for (final String cutoutState : states) {
                if (cutoutState.equals(headerState.name())) {
                    final int value = headerState.getValue();
                    final double pix = crpix + (value - crval) / cdelt;
                    pix1 = Math.min(pix1, pix);
                    pix2 = Math.max(pix2, pix);

                    LOGGER.debug("Values now (" + pix1 + ", " + pix2 + ")");
                }
            }
        }

        return clip(polarizationAxis, pix1, pix2);
    }

    public PolarizationState[] getHeaderStates(final int polarizationAxis) {
        final int naxis = this.fitsHeaderWCSKeywords.getIntValue(Standard.NAXIS.key());
        final double crpix = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CRPIXn.n(polarizationAxis).key());
        final double crval = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CRVALn.n(polarizationAxis).key());
        final double cdelt = this.fitsHeaderWCSKeywords.getDoubleValue(Standard.CDELTn.n(polarizationAxis).key());

        final List<PolarizationState> polarizationStates = new ArrayList<>();

        IntStream.range(1, naxis + 1)
                 .map(i -> (int) (crpix + (i - crval) / cdelt))
                 .filter(i -> PolarizationState.fromValue(i) != null)
                 .forEach(i -> polarizationStates.add(PolarizationState.fromValue(i)));

        LOGGER.debug("Found states " + polarizationStates);
        return polarizationStates.toArray(new PolarizationState[0]);
    }

    long[] clip(final int polarizationAxis, final double lower, final double upper) {
        // Round floats to individual pixels
        long p1 = (long) Math.floor(lower);
        long p2 = (long) Math.ceil(upper);

        // Bounds check
        if (p1 < 1) {
            p1 = 1L;
        }

        if (p2 > polarizationAxis) {
            p2 = polarizationAxis;
        }

        LOGGER.debug("Clipped to (" + p1 + ", " + p2 + ")");

        // Validity check, no pixels included
        if (p1 > polarizationAxis || p2 < 1) {
            return null;
        }

        // an actual cutout
        return new long[]{p1, p2};
    }
}
