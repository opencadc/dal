/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2017.                            (c) 2017.
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

package ca.nrc.cadc.dali.tables.votable;

import org.apache.log4j.Logger;

/**
 *
 * @author pdowler
 */
public abstract class VOTableUtil {

    private static final Logger log = Logger.getLogger(VOTableUtil.class);

    private VOTableUtil() {
    }

    public static int[] getArrayShape(String arraysize) {
        if (arraysize == null) {
            return null;
        }

        String[] sa = arraysize.split("x");
        int[] arrayShape = new int[sa.length];
        for (int i = 0; i < sa.length; i++) {
            String s = sa[i];
            int starIndex = s.indexOf('*');
            boolean variableSize = (starIndex >= 0);
            if (variableSize && i != sa.length - 1) {
                throw new IllegalArgumentException("invalid arraysize: " + arraysize + " found * in position " + i);
            }
            String dim = s;
            if (starIndex == 0) {
                dim = "";
            } else if (variableSize) {
                dim = s.substring(0, starIndex);
            }
            try {
                if (dim.length() > 0) {
                    arrayShape[i] = Integer.parseInt(dim);
                } else {
                    arrayShape[i] = -1; // better than default 0?
                }
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("invalid arraysize: " + arraysize + " found: " + dim + " expected: integer");
            }
        }
        return arrayShape;
    }

    // same as getArrayShape but does not preserve max length for variable dim like 100*. i.e.: sets -1 for 100* like dimensions.
    public static int[] parseArraySize(String arraysize) {
        if (arraysize == null) {
            return null;
        }

        String[] parts = arraysize.split("x");
        int[] arrayShape = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();

            // if any part ends with "*" or is "*" (like "10*", "*")
            if (part.endsWith("*")) {
                if (i != parts.length - 1) {
                    throw new IllegalArgumentException("invalid arraysize: " + arraysize + " . expected * only in last dimension");
                }
                arrayShape[i] = -1;
            } else {
                try {
                    arrayShape[i] = Integer.parseInt(part);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("invalid arraysize: " + arraysize + " found: " + part + " expected: integer");
                }
            }
        }
        return arrayShape;
    }
}
