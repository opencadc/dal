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
 ************************************************************************
 */

package org.opencadc.pkg.server;

import ca.nrc.cadc.util.Log4jInit;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

public class TarWriterTest {
    private static final Logger log = Logger.getLogger(TarWriterTest.class);
    
    static {
        Log4jInit.setLevel("org.opencadc.pkg.server", Level.INFO);
    }

    @Test
    public void testCreateTar() {
        try {
            // Create PackageItems for testing
            // Files are in test/resources
            String dir1Path = "some/path/";
            PackageItem dir1 = new PackageItem(dir1Path);
            log.debug(dir1);

            String dir2Path = "some/empty/path/";
            PackageItem dir2 = new PackageItem(dir2Path);
            log.debug(dir2);

            String file1Path = "some/path/GovCanada.gif";
            URL file1URL = getClass().getClassLoader().getResource("GovCanada.gif");
            PackageItem file1 = new PackageItem(file1Path, file1URL);
            log.debug(file1);

            String file2Path = "another/path/SymbolCanada.gif";
            URL file2URL = getClass().getClassLoader().getResource("SymbolCanada.gif");
            PackageItem file2 = new PackageItem(file2Path, file2URL);
            log.debug(file2);

            String link1Path = "some/path/link2SymbolCanada.gif";
            PackageItem link1 = new PackageItem(link1Path, file2Path);
            log.debug(link1);

            List<PackageItem> packageContents = new ArrayList<>();
            packageContents.add(dir1);
            packageContents.add(dir2);
            packageContents.add(file1);
            packageContents.add(file2);
            packageContents.add(link1);

            File tmp = File.createTempFile("tartest", ".tar");
            log.debug("tmp file: " + tmp.getAbsolutePath());
            FileOutputStream fos =  new FileOutputStream(tmp);
            TarWriter fw = new TarWriter(fos);
            for (PackageItem pi : packageContents) {
                fw.write(pi);
            }
            fw.close();
            
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            TarWriter bw = new TarWriter(bos);
            for (PackageItem pi : packageContents) {
                bw.write(pi);
            }
            bw.close();

            ByteArrayInputStream in = new ByteArrayInputStream(bos.toByteArray());
            TarArchiveInputStream tar = new TarArchiveInputStream(in);

            // some/path/
            Entry entry = getEntry(tar);
            Assert.assertEquals("name", dir1Path, entry.name);

            // some/empty/path
            entry = getEntry(tar);
            Assert.assertEquals("name", dir2Path, entry.name);

            // some/path/GovCanada.gif
            entry = getEntry(tar);
            Assert.assertEquals("name", file1Path, entry.name);
            Path url1Path = Paths.get(file1URL.getPath());
            log.debug("url1Path: " + url1Path.toString());
            Assert.assertArrayEquals(entry.content, Files.readAllBytes(url1Path));

            // another/path/SymbolCanada.gif
            entry = getEntry(tar);
            Assert.assertEquals("name", file2Path, entry.name);
            Path url2Path = Paths.get(file2URL.getPath());
            log.debug("url2Path: " + url2Path.toString());
            Assert.assertArrayEquals(entry.content, Files.readAllBytes(url2Path));

            // some/path/link2SymbolCanada.gif
            entry = getEntry(tar);
            Assert.assertEquals("name", link1Path, entry.name);

            ArchiveEntry archiveEntry = tar.getNextEntry();
            Assert.assertNull(archiveEntry);

        } catch (Exception unexpected) {
            log.error("unexpected exception", unexpected);
            Assert.fail("Unexpected exception: " + unexpected);
        }
    }

    static class Entry {
        String name;
        byte[] content;
    }

    private Entry getEntry(TarArchiveInputStream tar) throws IOException {
        Entry ret = new Entry();
        
        TarArchiveEntry entry = tar.getNextEntry();
        ret.name = entry.getName();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] bytes = new byte[(int)entry.getSize()];
        int read = 0;
        while ((read = tar.read(bytes)) > 0) {
            out.write(bytes, 0, read);
        }
        ret.content = out.toByteArray();

        return ret;
    }

}
