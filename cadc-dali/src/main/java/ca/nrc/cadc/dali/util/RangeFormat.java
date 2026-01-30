/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2019.                            (c) 2019.
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

package ca.nrc.cadc.dali.util;

import ca.nrc.cadc.dali.DoubleInterval;
import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.dali.Range;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

/**
 *
 * @author pdowler
 */
public class RangeFormat implements Format<Range> {

    private static final Logger log = Logger.getLogger(RangeFormat.class);

    private final DoubleIntervalArrayFormat diaf = new DoubleIntervalArrayFormat();
    private final DoubleArrayFormat fmt = new DoubleArrayFormat();

    private final boolean sia2;

    public RangeFormat() {
        this.sia2 = false;
    }

    /**
     * @param supportSIA2 if true, parse function allows ranges to use -Inf and +inf
     */
    public RangeFormat(boolean supportSIA2) {
        this.sia2 = supportSIA2;
    }

    @Override
    public Range parse(String s) {
        Interval<Double>[] dis = diaf.parse(s);
        if (dis.length != 2) {
            throw new IllegalArgumentException("invalid range: found " + dis.length + " intervals");
        }
        if (sia2) {
            // clip infinite to coordinate limits
            double long1 = dis[0].getLower();
            if (dis[0].getLower().isInfinite()) {
                long1 = 0.0;
            }
            double long2 = dis[0].getUpper();
            if (dis[0].getUpper().isInfinite()) {
                long2 = 360.0;
            }
            double lat1 = dis[1].getLower();
            if (dis[1].getLower().isInfinite()) {
                lat1 = -90.0;
            }
            double lat2 = dis[1].getUpper();
            if (dis[1].getUpper().isInfinite()) {
                lat2 = 90.0;
            }
            return new Range(new DoubleInterval(long1, long2), new DoubleInterval(lat1, lat2));
        }
        return new Range(dis[0], dis[1]);
    }

    @Override
    public String format(final Range t) {
        if (t == null) {
            return "";
        }
        return fmt.format(new Iterator<Double>() {
            private int num = 0;

            @Override
            public boolean hasNext() {
                return (num < 4);
            }

            @Override
            public Double next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                double d;
                num++;
                switch (num) {
                    case 1:
                        d = t.getLongitude().getLower();
                        return d;
                    case 2:
                        d = t.getLongitude().getUpper();
                        return d;
                    case 3:
                        d = t.getLatitude().getLower();
                        return d;
                    case 4:
                        d = t.getLatitude().getUpper();
                        return d;
                    default:
                        throw new RuntimeException("BUG: range coordinate iteration reached " + num);
                }
            }

            // java7 support
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        });
    }
}
