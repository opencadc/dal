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

import ca.nrc.cadc.util.StringUtil;
import java.net.URL;

/**
 * Base class that describes the path to a resource in a package.
 */
public class PackageItem {

    private final String relativePath;
    private final String linkTarget;
    private final URL content;

    /**
     * Creates a PackageItem for a directory. The relative path is the path to the directory within the package.
     *
     * @param relativePath path to directory in the package.
     */
    public PackageItem(String relativePath) {
        if (!StringUtil.hasText(relativePath)) {
            throw new IllegalArgumentException("relativePath is null or empty");
        }
        this.relativePath = relativePath;
        this.linkTarget = null;
        this.content = null;
    }

    /**
     * Creates a resource for a package. The relative path is used to create
     * the file structure inside a package.
     *
     * @param relativePath path to the resource in the package.
     * @param content URL to the file.
     */
    public PackageItem(String relativePath, URL content) {
        if (!StringUtil.hasText(relativePath)) {
            throw new IllegalArgumentException("relativePath is null or empty");
        }
        if (content == null) {
            throw new IllegalArgumentException("content is null");
        }
        this.relativePath = relativePath;
        this.content = content;
        this.linkTarget = null;
    }

    /**
     * Creates a resource for a package. The relative path is used to create
     * the file structure inside a package.
     *
     * @param relativePath path to the resource in the package.
     * @param linkTarget path to the resource in the package.
     */
    public PackageItem(String relativePath, String linkTarget) {
        if (!StringUtil.hasText(relativePath)) {
            throw new IllegalArgumentException("relativePath is null or empty");
        }
        if (!StringUtil.hasText(relativePath)) {
            throw new IllegalArgumentException("linkTarget is null or empty");
        }
        this.relativePath = relativePath;
        this.linkTarget = linkTarget;
        this.content = null;
    }

    /**
     * Get the relative path of the item (directory, file, or symbolic link) within the package archive.
     *
     * @return relative path to the item.
     */
    public String getRelativePath() {
        return relativePath;
    }

    /**
     * Get the relative path within the package archive for the target of a symbolic link item.
     *
     * @return relative path of the symbolic link target, null if the item is a file or directory.
     */
    public String getLinkTarget() {
        return linkTarget;
    }

    /**
     * Get the URL to the content of a file item.
     *
     * @return URL to the content for a file, null if the item is a directory or symbolic link.
     */
    public URL getContent() {
        return content;
    }


    /**
     * Check if the item is a directory.
     *
     * @return  true if the item is a directory, false otherwise.
     */
    public boolean isDirectory() {
        return linkTarget == null && content == null;
    }

    /**
     * Check if the item is a file.
     *
     * @return true if the item is a file, false otherwise.
     */
    public boolean isFile() {
        return content != null;
    }

    /**
     * Check if the item is a symbolic link.
     *
     * @return true if the item is a symbolic link, false otherwise.
     */
    public boolean isSymbolicLink() {
        return linkTarget != null;
    }

}
