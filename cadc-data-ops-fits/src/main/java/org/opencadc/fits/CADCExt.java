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

package org.opencadc.fits;

import nom.tam.fits.header.FitsHeaderImpl;
import nom.tam.fits.header.IFitsHeader;

/**
 * Extension to the standard set of FITS headers.
 */
public enum CADCExt implements IFitsHeader {

    CDELT(HDU.IMAGE, VALUE.REAL, "Coord value at incr deg/pixel origin on line axis"),
    CDELTn(HDU.IMAGE, VALUE.REAL, "Coord value at incr deg/pixel origin on line axis"),
    CUNITn(HDU.IMAGE, VALUE.STRING, "Units for axis"),
    LBOUNDn(HDU.IMAGE, VALUE.INTEGER, "Pixel origin along axis"),
    PC1_1(HDU.IMAGE, VALUE.REAL, ""),
    PC1_2(HDU.IMAGE, VALUE.REAL, ""),
    PC2_1(HDU.IMAGE, VALUE.REAL, ""),
    PC2_2(HDU.IMAGE, VALUE.REAL, ""),
    RESTFRQ(HDU.IMAGE, VALUE.REAL, ""),

    /**
     * Use RESTFRQ
     */
    @Deprecated
    RESTFREQ(HDU.IMAGE, VALUE.REAL, ""),

    RESTWAV(HDU.IMAGE, VALUE.REAL, ""),
    SPECSYS(HDU.IMAGE, VALUE.STRING, ""),

    // Temporal values.
    MJDREF(HDU.IMAGE, VALUE.REAL, "Reference time in MJD"),
    MJDREFI(HDU.IMAGE, VALUE.REAL, "Integer part of reference time in MJD"),
    MJDREFF(HDU.IMAGE, VALUE.REAL, "Fractional part of reference time in MJD"),
    JDREF(HDU.IMAGE, VALUE.REAL, "Reference time in JD"),
    JDREFI(HDU.IMAGE, VALUE.REAL, "Integer part of reference time in JD"),
    JDREFF(HDU.IMAGE, VALUE.REAL, "Fractional part of reference time in JD"),
    DATEBEG("DATE-BEG", HDU.PRIMARY_EXTENSION, VALUE.REAL, "Start time of data in iso-8601"),
    DATEOBS("DATE-OBS", HDU.PRIMARY_EXTENSION, VALUE.REAL, "Time (or start time) of data in iso-8601"),
    DATEEND("DATE-END", HDU.PRIMARY_EXTENSION, VALUE.REAL, "Stop time of data in iso-8601"),

    JDBEG("JD-BEG", HDU.PRIMARY_EXTENSION, VALUE.REAL, "Start time of data in JD"),
    JDOBS("JD-OBS", HDU.PRIMARY_EXTENSION, VALUE.REAL, "Time (or start time) of data in JD"),
    JDEND("JD-END", HDU.PRIMARY_EXTENSION, VALUE.REAL, "Stop time of data in JD"),

    MJDBEG("MJD-BEG", HDU.PRIMARY_EXTENSION, VALUE.REAL, "Start time of data in MJD"),
    MJDOBS("MJD-OBS", HDU.PRIMARY_EXTENSION, VALUE.REAL, "Time (or start time) of data in MJD"),
    MJDEND("MJD-END", HDU.PRIMARY_EXTENSION, VALUE.REAL, "Stop time of data in MJD"),

    TSTART(HDU.PRIMARY_EXTENSION, VALUE.REAL,
           "Start time of data in units of TIMEUNIT relative to MJDREF, JDREF or DATEREF according to TIMESYS."),
    TSTOP(HDU.PRIMARY_EXTENSION, VALUE.REAL,
          "Stop time of data in units of TIMEUNIT relative to MJDREF, JDREF or DATEREF according to TIMESYS."),

    DATEREF(HDU.IMAGE, VALUE.REAL, "Reference time in ISO-8601"),

    TIMESYS(HDU.PRIMARY_EXTENSION, VALUE.STRING, "Time scale.  Defaults to UTC."),
    TIMEUNIT(HDU.ANY, VALUE.STRING, "Unit of elapsed time.");



    private final IFitsHeader key;

    CADCExt(IFitsHeader.HDU hdu, IFitsHeader.VALUE valueType, String comment) {
        this.key = new FitsHeaderImpl(name(), IFitsHeader.SOURCE.NOAO, hdu, valueType, comment);
    }

    CADCExt(String key, IFitsHeader.HDU hdu, IFitsHeader.VALUE valueType, String comment) {
        this.key = new FitsHeaderImpl(name(), IFitsHeader.SOURCE.NOAO, hdu, valueType, comment);
    }

    @Override
    public String comment() {
        return this.key.comment();
    }

    @Override
    public IFitsHeader.HDU hdu() {
        return this.key.hdu();
    }

    @Override
    public String key() {
        return this.key.key();
    }

    @Override
    public IFitsHeader n(int... number) {
        return this.key.n(number);
    }

    @Override
    public IFitsHeader.SOURCE status() {
        return this.key.status();
    }

    @Override
    @SuppressWarnings("CPD-END")
    public IFitsHeader.VALUE valueType() {
        return this.key.valueType();
    }
}
