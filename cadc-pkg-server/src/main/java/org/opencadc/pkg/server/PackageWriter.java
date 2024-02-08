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
*  $Revision: 5 $
*
************************************************************************
*/

package org.opencadc.pkg.server;

import ca.nrc.cadc.io.MultiBufferIO;
import ca.nrc.cadc.net.HttpGet;
import ca.nrc.cadc.net.ResourceAlreadyExistsException;
import ca.nrc.cadc.net.ResourceNotFoundException;
import ca.nrc.cadc.net.TransientException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.log4j.Logger;

public abstract class PackageWriter {
    private static final Logger log = Logger.getLogger(PackageWriter.class);

    ArchiveOutputStream archiveOutputStream;

    public PackageWriter(ArchiveOutputStream archiveOutputStream) {
        this.archiveOutputStream = archiveOutputStream;
    }

    /**
     * Implement this so the correct type of entry is created for writing.
     *
     * @param relativePath - relative path + filename (needed so directory structure is created)
     * @param size - entry size
     * @param lastModifiedDate - entry last modified date
     * @return ArchiveEntry
     */
    abstract ArchiveEntry createFileEntry(String relativePath, long size, Date lastModifiedDate);

    abstract ArchiveEntry createDirectoryEntry(String relativePath);

    abstract ArchiveEntry createSymbolicLinkEntry(String relativePath, String linkRelativePath);

    public void close() throws IOException {
        if (archiveOutputStream != null) {
            archiveOutputStream.finish();
            archiveOutputStream.close();
        }
    }

    /**
     *  Write the given packageItem to the ArchiveOutputStream local to this ArchiveWriter instance.
     *
     * @param packageItem - item to be written to archive
     * @throws IOException
     * @throws InterruptedException
     * @throws ResourceNotFoundException
     * @throws TransientException
     * @throws ResourceAlreadyExistsException
     */
    public void write(PackageItem packageItem) throws IOException, InterruptedException,
        ResourceNotFoundException, TransientException, ResourceAlreadyExistsException {

        // Implementations of PackageRunner that build the PackageItem list
        // should ensure that files exist before submitting as part of the
        // Iterator<PackageItem>
        if (packageItem.isDirectory()) {
            writeDirectory(packageItem);
        } else if (packageItem.isFile()) {
            if (packageItem.getContent().getProtocol().equals("file")) {
                writeFile(packageItem);
            } else {
                writeHTTPFile(packageItem);
            }
        } else if (packageItem.isSymbolicLink()) {
            writeSymbolicLink(packageItem);
        } else {
            throw new IllegalArgumentException("Unknown PackageItem type: " + packageItem);
        }
    }

    /**
     * Write a directory to the archive.
     */
    private void writeDirectory(PackageItem packageItem)
            throws IOException {
        log.debug("write directory: " + packageItem.getRelativePath());

        ArchiveEntry archiveEntry = createDirectoryEntry(packageItem.getRelativePath());
        archiveOutputStream.putArchiveEntry(archiveEntry);
        archiveOutputStream.closeArchiveEntry();
    }

    /**
     * Write a file to the archive with the file content is a local file.
     */
    private void writeFile(PackageItem packageItem)
            throws IOException, InterruptedException {
        log.debug(String.format("write file %s to %s", packageItem.getContent(), packageItem.getRelativePath()));

        URL fileURL = packageItem.getContent();
        Path filePath = FileSystems.getDefault().getPath(fileURL.getPath());
        long contentLength = Files.size(filePath);
        FileTime lastMod = Files.getLastModifiedTime(filePath);

        Date lastModified = new Date(lastMod.toMillis());
        String relativePath = packageItem.getRelativePath();
        ArchiveEntry archiveEntry = createFileEntry(relativePath, contentLength, lastModified);

        // put archive entry to stream
        archiveOutputStream.putArchiveEntry(archiveEntry);

        // copy the file to archive output stream
        InputStream stream = fileURL.openStream();
        MultiBufferIO multiBufferIO = new MultiBufferIO();
        multiBufferIO.copy(stream, archiveOutputStream);
        stream.close();
        archiveOutputStream.closeArchiveEntry();
    }

    /**
     * Write a file to the archive where the file content is an HTTP URL.
     */
    private void writeHTTPFile(PackageItem packageItem)
            throws IOException, InterruptedException, ResourceNotFoundException,
            TransientException, ResourceAlreadyExistsException {
        log.debug(String.format("write file %s to %s", packageItem.getContent(), packageItem.getRelativePath()));

        // HEAD to get entry metadata
        HttpGet get = new HttpGet(packageItem.getContent(), true);
        get.prepare();

        String relativePath = packageItem.getRelativePath();
        long contentLength = get.getContentLength();
        Date lastModified = get.getLastModified();
        ArchiveEntry archiveEntry = createFileEntry(relativePath, contentLength, lastModified);

        // put archive entry to stream
        archiveOutputStream.putArchiveEntry(archiveEntry);

        // copy the file to archive output stream
        // copy the get InputStream to the package OutputStream
        // this is 'writing' the content of the file into the
        // package file
        InputStream getIOStream = get.getInputStream();
        MultiBufferIO multiBufferIO = new MultiBufferIO();
        multiBufferIO.copy(getIOStream, archiveOutputStream);
        getIOStream.close();
        archiveOutputStream.closeArchiveEntry();
    }

    /**
     * Write a symbolic link to the archive.
     */
    private void writeSymbolicLink(PackageItem packageItem)
            throws IOException {
        log.debug(String.format("write link %s to %s", packageItem.getRelativePath(), packageItem.getLinkTarget()));

        String relativePath = packageItem.getRelativePath();
        String linkTarget = packageItem.getLinkTarget();
        ArchiveEntry archiveEntry = createSymbolicLinkEntry(relativePath, linkTarget);

        // put archive entry to stream
        archiveOutputStream.putArchiveEntry(archiveEntry);

        // entry content is the symbolic link target path
        archiveOutputStream.write(linkTarget.getBytes(StandardCharsets.UTF_8));
        archiveOutputStream.closeArchiveEntry();
    }

}
