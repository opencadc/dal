/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2023.                            (c) 2023.
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
 *  : 5 $
 *
 ************************************************************************
 */

package org.opencadc.pkg.server;

import ca.nrc.cadc.util.Log4jInit;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class PackageWriterTest {
    private static final Logger log = Logger.getLogger(PackageWriterTest.class);

    static {
        Log4jInit.setLevel("org.opencadc.pkg.server", Level.INFO);
    }

    @Test
    public void testCreateTarFile() {
        try {
            List<PackageItem> testPackageItems = getTestPackageItems();

            File tarFile = File.createTempFile("tartest", ".tar");
            log.info("tar archive: " + tarFile.getAbsolutePath());
            FileOutputStream fos =  new FileOutputStream(tarFile);
            TarWriter fw = new TarWriter(fos);
            for (PackageItem pi : testPackageItems) {
                fw.write(pi);
                log.debug("wrote item: " + pi.getRelativePath());
            }
            fw.close();

            Assert.assertTrue(tarFile.canRead());
            Assert.assertTrue(tarFile.length() > 0);

        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("Unexpected exception: " + unexpected);
        }
    }

    @Test
    public void testCreateZipFile() {
        try {
            //
            List<PackageItem> testPackageItems = getTestPackageItems();

            File zipFile = File.createTempFile("ziptest", ".zip");
            log.info("zip archive: " + zipFile.getAbsolutePath());
            FileOutputStream fos =  new FileOutputStream(zipFile);
            PackageWriter fw = new ZipWriter(fos);
            for (PackageItem pi : testPackageItems) {
                fw.write(pi);
                log.debug("wrote item: " + pi.getRelativePath());
            }
            fw.close();

            Assert.assertTrue(zipFile.canRead());
            Assert.assertTrue(zipFile.length() > 0);

            // Need to use ZipArchiveInputStream to read the zip file, because UNIX permissions are not
            // retrieved using the ZipArchiveInputStream.
            final ZipFile zipFileObj = ZipFile.builder().setFile(zipFile).get();
            for (final Enumeration<ZipArchiveEntry> entries = zipFileObj.getEntries(); entries.hasMoreElements();) {
                final ZipArchiveEntry zipArchiveEntry = entries.nextElement();
                if (zipArchiveEntry.isDirectory()) {
                    Assert.assertEquals("Incorrect dir perms (" + zipArchiveEntry.getName() + ")",
                                        UnixStat.DIR_FLAG + UnixStat.DEFAULT_DIR_PERM,
                                        zipArchiveEntry.getUnixMode());
                } else if (zipArchiveEntry.isUnixSymlink()) {
                    Assert.assertEquals("Incorrect link perms (" + zipArchiveEntry.getName() + ")",
                                        UnixStat.LINK_FLAG + UnixStat.DEFAULT_LINK_PERM,
                                        zipArchiveEntry.getUnixMode());
                } else {
                    Assert.assertEquals("Incorrect file perms (" + zipArchiveEntry.getName() + ")",
                                        UnixStat.FILE_FLAG + UnixStat.DEFAULT_FILE_PERM,
                                        zipArchiveEntry.getUnixMode());
                }
            }

        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("Unexpected exception: " + unexpected);
        }
    }


    protected List<PackageItem> getTestPackageItems() {
        // Create PackageItems for testing
        // Files are in test/resources
        String some = "some/";
        PackageItem someDir = new PackageItem(some);
        log.debug(someDir);

        String somePath = "some/path/";
        PackageItem somePathDir = new PackageItem(somePath);
        log.debug(somePathDir);

        String someEmpty = "some/empty/";
        PackageItem someEmptyDir = new PackageItem(someEmpty);
        log.debug(someEmptyDir);

        String someEmptyPath = "some/empty/path/";
        PackageItem someEmptyPathDir = new PackageItem(someEmptyPath);
        log.debug(someEmptyPathDir);

        String govCanadaGif = "some/path/GovCanada.gif";
        URL govCanadaURL = getClass().getClassLoader().getResource("GovCanada.gif");
        PackageItem govCanadaFile = new PackageItem(govCanadaGif, govCanadaURL);
        log.debug(govCanadaFile);

        String another = "another/";
        PackageItem anotherDir = new PackageItem(another);
        log.debug(anotherDir);

        String anotherPath = "another/path/";
        PackageItem anotherPathDir = new PackageItem(anotherPath);
        log.debug(anotherPathDir);

        String symbolCanadaGif = "another/path/SymbolCanada.gif";
        URL symbolCanadaURL = getClass().getClassLoader().getResource("SymbolCanada.gif");
        PackageItem symbolCanadaFile = new PackageItem(symbolCanadaGif, symbolCanadaURL);
        log.debug(symbolCanadaFile);

        String linkPath = "some/path/link2SymbolCanada.gif";
        PackageItem link = new PackageItem(linkPath, "../../another/path/SymbolCanada.gif");
        log.debug(link);

        List<PackageItem> packageItems = new ArrayList<>();
        packageItems.add(someDir);
        packageItems.add(somePathDir);
        packageItems.add(someEmptyPathDir);
        packageItems.add(govCanadaFile);
        packageItems.add(anotherDir);
        packageItems.add(anotherPathDir);
        packageItems.add(symbolCanadaFile);
        packageItems.add(link);

        String fooPath = "foo";
        PackageItem foo = new PackageItem(fooPath);
        packageItems.add(foo);

        return packageItems;
    }

}
