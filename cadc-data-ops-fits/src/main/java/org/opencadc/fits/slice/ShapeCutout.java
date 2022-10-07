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

import ca.nrc.cadc.dali.Shape;

import java.io.Serializable;

import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCardException;
import nom.tam.fits.header.Compression;
import nom.tam.fits.header.Standard;
import org.apache.log4j.Logger;


/**
 * Base class for Shapes (Spatial cutouts).  It mainly contains a convenience method that can be shared.
 *
 * @param <T> Shape.
 */
public abstract class ShapeCutout<T extends Shape> extends FITSCutout<T> {
    private static final Logger LOGGER = Logger.getLogger(ShapeCutout.class);

    private static final int MAX_NAXIS_ALLOWED = 999;

    public ShapeCutout(final Header header) throws HeaderCardException {
        super(header);
    }

    public ShapeCutout(FITSHeaderWCSKeywords fitsHeaderWCSKeywords) {
        super(fitsHeaderWCSKeywords);
    }

    /**
     * Ensure the proper axis values are used in the case of a compressed HDU (ZNAXISn) or a regular Image HDU.
     *
     * @return the dimensions of the axis.  Returns an empty array for no NAXIS values.
     */
    int[] getDimensions() {
        final int nAxis = this.fitsHeaderWCSKeywords.getIntValue(Compression.ZNAXIS.key(),
                                                                 this.fitsHeaderWCSKeywords.getIntValue(
                                                                         Standard.NAXIS.key()));

        if (nAxis == 0) {
            return new int[0];
        }

        final int[] axes = new int[nAxis];
        for (int i = 1; i <= nAxis; i++) {
            axes[i - 1] = this.fitsHeaderWCSKeywords.getIntValue(Compression.ZNAXISn.n(i).key(),
                                                                 this.fitsHeaderWCSKeywords.getIntValue(
                                                                         Standard.NAXISn.n(i).key()));
        }

        return axes;
    }

    /**
     * Deduce the Coordinate System used.  This will scan the Header for CTYPEn values.
     *
     * @return CoordSys instance, or null if the Header has no CTYPEn value(s).
     */
    CoordSys inferCoordSys() {
        final CoordSys ret;
        final int spatialLongitudeAxis = this.fitsHeaderWCSKeywords.getSpatialLongitudeAxis();
        final int spatialLatitudeAxis = this.fitsHeaderWCSKeywords.getSpatialLatitudeAxis();

        if (spatialLongitudeAxis < 0 || spatialLatitudeAxis < 0) {
            ret = null;
        } else {
            final String longitudeCType =
                    fitsHeaderWCSKeywords.getStringValue(Standard.CTYPEn.n(spatialLongitudeAxis).key());

            LOGGER.debug("CTYPE" + spatialLongitudeAxis + " is " + longitudeCType);

            final String latitudeCType =
                    fitsHeaderWCSKeywords.getStringValue(Standard.CTYPEn.n(spatialLatitudeAxis).key());

            LOGGER.debug("CTYPE" + spatialLatitudeAxis + " is " + latitudeCType);

            final float equinox = fitsHeaderWCSKeywords.getFloatValue(Standard.EQUINOX.key());

            ret = new CoordSys();
            ret.longitudeAxis = spatialLongitudeAxis;
            ret.latitudeAxis = spatialLatitudeAxis;
            ret.name = fitsHeaderWCSKeywords.getStringValue(Standard.RADESYS.key());
            ret.supported = false;

            if (CoordSys.GAPPT.equals(ret.name)) {
                ret.timeDependent = Boolean.TRUE;
            } else if ((longitudeCType.startsWith("ELON") && latitudeCType.startsWith("ELAT"))
                       || (longitudeCType.startsWith("ELAT") && latitudeCType.startsWith("ELON"))) {
                // ecliptic
                ret.name = CoordSys.ECL;
                ret.timeDependent = Boolean.TRUE;
            } else if ((longitudeCType.startsWith("HLON") && latitudeCType.startsWith("HLAT"))
                       || (longitudeCType.startsWith("HLAT") && latitudeCType.startsWith("HLON"))) {
                // helio-ecliptic
                ret.name = CoordSys.HECL;
                ret.timeDependent = Boolean.TRUE;
            } else if ((longitudeCType.startsWith("GLON") && latitudeCType.startsWith("GLAT"))
                       || (longitudeCType.startsWith("GLAT") && latitudeCType.startsWith("GLON"))) {
                if (CoordSys.GAL.equals(ret.name)) {
                    LOGGER.debug("found coordsys=" + ret.name + " with GLON,GLAT - OK");
                } else if (ret.name != null) {
                    LOGGER.debug("found coordsys=" + ret.name + " with GLON,GLAT - ignoring and assuming GAL");
                    ret.name = null;
                }
                if (ret.name == null) {
                    ret.name = CoordSys.GAL;
                }
                if (longitudeCType.startsWith("GLAT")) {
                    ret.swappedAxes = true;
                }
                ret.supported = true;
            } else if ((longitudeCType.startsWith("RA") && latitudeCType.startsWith("DEC"))
                       || (longitudeCType.startsWith("DEC") && latitudeCType.startsWith("RA"))) {
                if (ret.name == null) {
                    if (equinox == 0.0F) {
                        ret.name = CoordSys.ICRS;
                    } else if (Math.abs(equinox - 1950.0) < 1.0) {
                        ret.name = CoordSys.FK4;
                    } else if (Math.abs(equinox - 2000.0) < 1.0) {
                        ret.name = CoordSys.FK5;
                    } else {
                        LOGGER.debug("cannot infer coordinate system from RA,DEC and equinox = " + equinox);
                    }
                }

                if (spatialLatitudeAxis < spatialLongitudeAxis) {
                    ret.swappedAxes = true;
                }
                if (ret.name != null) {
                    ret.supported = true;
                }
            }
        }

        return ret;
    }

    /**
     * Simple DTO class to bundle similar Shape data.
     */
    public static class CoordSys implements Serializable {
        private static final long serialVersionUID = 201207300900L;

        public static String ICRS = "ICRS";
        public static String GAL = "GAL";
        public static String FK4 = "FK4";
        public static String FK5 = "FK5";

        public static String ECL = "ECL";
        public static String HECL = "HELIOECLIPTIC";
        public static String GAPPT = "GAPPT";

        String name;
        Boolean timeDependent;
        boolean supported;
        boolean swappedAxes = false;
        int longitudeAxis;
        int latitudeAxis;

        public String getName() {
            return name;
        }

        public boolean isSwappedAxes() {
            return swappedAxes;
        }

        /**
         * Returns a string representation of the object. In general, the
         * {@code toString} method returns a string that
         * "textually represents" this object. The result should
         * be a concise but informative representation that is easy for a
         * person to read.
         * It is recommended that all subclasses override this method.
         *
         * <p>The {@code toString} method for class {@code Object}
         * returns a string consisting of the name of the class of which the
         * object is an instance, the at-sign character `{@code @}', and
         * the unsigned hexadecimal representation of the hash code of the
         * object. In other words, this method returns a string equal to the
         * value of:
         * <blockquote>
         * <pre>
         * getClass().getName() + '@' + Integer.toHexString(hashCode())
         * </pre></blockquote>
         *
         * @return a string representation of the object.
         */
        @Override
        public String toString() {
            return CoordSys.class.getName() + "["
                   + "Name: " + name + ", "
                   + "Time dependant: " + timeDependent + ", "
                   + "Supported Flag: " + supported + ", "
                   + "Swapped Axes flag: " + swappedAxes + ", "
                   + "Longitude axis: " + longitudeAxis + ", "
                   + "Latitude axis: " + latitudeAxis + "]";

        }
    }
}
