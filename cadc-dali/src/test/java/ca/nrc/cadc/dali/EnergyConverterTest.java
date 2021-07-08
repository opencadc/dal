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

package ca.nrc.cadc.dali;

import ca.nrc.cadc.util.Log4jInit;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class EnergyConverterTest {
    private static final Logger LOGGER = Logger.getLogger(EnergyConverterTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.fits.slice", Level.DEBUG);
    }

    @Test
    public void metresToHz() {
        final EnergyConverter testSubject = new EnergyConverter();

        final double hz = testSubject.fromMetres(0.1D, "Hz");
        Assert.assertEquals("Wrong Hz.", 2997924580.0D, hz, 0.0D);
    }

    @Test
    public void metresToMHz() {
        final EnergyConverter testSubject = new EnergyConverter();

        final double mhz = testSubject.fromMetres(2.2D, "MHz");
        Assert.assertEquals("Wrong MHz.", 136.2700D, mhz, 0.01D);
    }

    @Test
    public void metresToGHz() {
        final EnergyConverter testSubject = new EnergyConverter();

        final double ghz = testSubject.fromMetres(12.4D, "GHz");
        Assert.assertEquals("Wrong GHz.", 0.0241768111D, ghz, 0.0000001D);
    }

    @Test
    public void metresToEv() {
        final EnergyConverter testSubject = new EnergyConverter();

        final double eV = testSubject.fromMetres(0.68D, "eV");
        LOGGER.debug("Calculated eV " + eV);
        Assert.assertEquals("Wrong eV.", 2.9212439E-25D, eV, 0.00001E-22D);
    }

    @Test
    public void metresToKev() {
        final EnergyConverter testSubject = new EnergyConverter();

        final double keV = testSubject.fromMetres(4.6D, "keV");
        LOGGER.debug("Calculated keV " + keV);
        Assert.assertEquals("Wrong keV.", 4.31836E-29D, keV, 0.00001E-18D);
    }

    @Test
    public void metresToNm() {
        final EnergyConverter testSubject = new EnergyConverter();

        final double nm = testSubject.fromMetres(44.6D, "nm");
        LOGGER.debug("Calculated nm " + nm);
        Assert.assertEquals("Wrong nm.", 4.46E10D, nm, 0.0D);
    }

    @Test
    public void metresToA() {
        final EnergyConverter testSubject = new EnergyConverter();

        final double angstrom = testSubject.fromMetres(13.99D, "A");
        LOGGER.debug("Calculated A " + angstrom);
        Assert.assertEquals("Wrong A.", 1.399E11D, angstrom, 0.0D);
    }
}
