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

import ca.nrc.cadc.util.ArrayUtil;

import org.apache.log4j.Logger;


/**
 * Conversion functions for energy units.  This converter is concerned with conversions from metres as that is the
 * defined input unit for DAL SODA.  This could just as easily be called EnergyMetreConverter.
 */
public class EnergyConverter {
    private static final Logger LOGGER = Logger.getLogger(EnergyConverter.class);

    private static final String[] FREQ_UNITS = new String[] {"Hz", "kHz", "MHz", "GHz" };
    private static final double[] FREQ_MULT = new double[] {1.0, 1.0e3, 1.0e6, 1.0e9 };

    private static final String[] EN_UNITS = new String[] {"eV", "keV", "MeV", "GeV" };
    private static final double[] EN_MULT = new double[] {1.0, 1.0e3, 1.0e6, 1.0e9 };

    private static final String[] WAVE_UNITS = new String[] {"m", "cm", "mm",
                                                             "um", "µm", "nm",
                                                             "Angstrom", "A" }; // A  deprecated
    private static final double[] WAVE_MULT = new double[] {1.0, 1.0e-2, 1.0e-3,
                                                            1.0e-6, 1.0e-6, 1.0e-9,
                                                            1.0e-10, 1.0e-10,};

    protected static final double c = 299792458.0D; // m/sec - Speed of light in a vacuum
    protected static final double h = 6.62607015e-34; // J/sec - Planck constant

    /**
     * Convert from known metres (user input) to the given unit.
     * @param metres    Value to convert in metres (wavelength).
     * @param cunit     The unit to convert to.
     * @return          Converted value.
     *
     * @throws IllegalArgumentException For unknown or unusable unit value.
     */
    public double fromMetres(final double metres, final String cunit) {
        final boolean inverse;
        final String unit;

        LOGGER.debug("Converting from metres (" + metres + ") to " + cunit);

        // 1 / the provided unit.
        if (cunit.startsWith("/")) {
            inverse = true;
            unit = cunit.substring(1);
        } else if (!cunit.contains(" ") && cunit.endsWith("-1")) {
            inverse = true;
            unit = cunit.substring(0, cunit.indexOf("-1"));
        } else {
            inverse = false;
            unit = cunit;
        }

        int i = ArrayUtil.matches("^" + unit + "$", FREQ_UNITS, true);
        if (i != -1) {
            final double result = metresToFreq(metres, i);
            return inverse ? 1.0D / result : result;
        }

        i = ArrayUtil.matches("^" + unit + "$", EN_UNITS, true);
        if (i != -1) {
            final double result = metresToEnergy(metres, i);
            return inverse ? 1.0D / result : result;
        }

        i = ArrayUtil.matches("^" + unit + "$", WAVE_UNITS, true);
        if (i != -1) {
            final double result = metresToWavelength(metres, i);
            LOGGER.debug("Wavelength " + result);
            final double conversionResult = inverse ? 1.0D / result : result;
            LOGGER.debug("Wavelength conversion " + conversionResult);
            return conversionResult;
        }

        throw new IllegalArgumentException("Unknown units: " + unit);
    }

    private double metresToFreq(final double metres, final int factorIndex) {
        return (c / metres) / FREQ_MULT[factorIndex];
    }

    private double metresToEnergy(final double metres, final int factorIndex) {
        return ((c * h) / metres) / EN_MULT[factorIndex];
    }

    private double metresToWavelength(double metres, int factorIndex) {
        return metres / WAVE_MULT[factorIndex];
    }
}
