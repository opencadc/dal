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

import java.util.Arrays;
import java.util.Locale;


public enum CoordTypeCode {
    // Spatial type codes.
    RA("RA", "deg", CoordType.SPATIAL),
    DEC("DEC", "deg", CoordType.SPATIAL),
    GLON("GLON", "deg", CoordType.SPATIAL),
    GLAT("GLAT", "deg", CoordType.SPATIAL),
    ELON("ELON", "deg", CoordType.SPATIAL),
    ELAT("ELAT", "deg", CoordType.SPATIAL),

    // Spectral type codes.
    FREQ("FREQ", "Hz", CoordType.SPECTRAL),
    ENER("ENER", "J", CoordType.SPECTRAL),
    WAVN("WAVN", "/m", CoordType.SPECTRAL),
    VRAD("VRAD", "m s-1", CoordType.SPECTRAL),
    WAVE("WAVE", "m", CoordType.SPECTRAL),
    VOPT("VOPT", "m s-1", CoordType.SPECTRAL),
    ZOPT("ZOPT", "", CoordType.SPECTRAL),
    AWAV("AWAV", "m", CoordType.SPECTRAL),
    VELO("VELO", "m s-1", CoordType.SPECTRAL),
    BETA("BETA", "", CoordType.SPECTRAL);

    private final String typeCodeString;
    private final String defaultUnit;
    private final CoordType coordType;


    CoordTypeCode(final String typeCodeString, final String defaultUnit, final CoordType coordType) {
        this.typeCodeString = typeCodeString;
        this.defaultUnit = defaultUnit;
        this.coordType = coordType;
    }

    public boolean isSpectral() {
        return coordType == CoordType.SPECTRAL;
    }

    public boolean isVelocity() {
        return this == VOPT || this == VELO || this == VRAD;
    }

    public String getDefaultUnit() {
        return defaultUnit;
    }

    public static String getDefaultUnit(final String ctype) {
        if (ctype == null) {
            return null;
        }

        final CoordTypeCode matchedCoordTypeCode =
                Arrays.stream(values()).filter(coordTypeCode -> ctype.toUpperCase(Locale.ROOT).startsWith(
                        coordTypeCode.typeCodeString)).findFirst().orElse(null);
        return (matchedCoordTypeCode == null) ? "" : matchedCoordTypeCode.getDefaultUnit();
    }

    public static CoordTypeCode fromCType(final String ctype) {
        final int hyphenIndex = ctype.indexOf("-");
        return CoordTypeCode.valueOf(hyphenIndex > 0 ? ctype.substring(0, hyphenIndex) : ctype);
    }
}
