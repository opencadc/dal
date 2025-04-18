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

package ca.nrc.cadc.dali;

/**
 *
 * @author pdowler
 * @param <T>
 */
public class Interval<T extends Number> {

    private T lower;
    private T upper;

    public Interval(T lower, T upper) {
        DaliUtil.assertNotNull("lower", lower);
        DaliUtil.assertNotNull("upper", upper);
        validateBounds(lower, upper);
        this.lower = lower;
        this.upper = upper;
    }

    private void validateBounds(T lower, T upper) {
        if (lower instanceof Double) {
            if (upper.doubleValue() < lower.doubleValue()) {
                throw new IllegalArgumentException("invalid interval: " + upper + " < " + lower);
            }
        } else if (lower instanceof Long) {
            if (upper.longValue() < lower.longValue()) {
                throw new IllegalArgumentException("invalid interval: " + upper + " < " + lower);
            }
        } else {
            throw new UnsupportedOperationException("validateBounds numeric type not implemented: "
                    + lower.getClass().getName());
        }
    }

    public T getLower() {
        return lower;
    }

    public T getUpper() {
        return upper;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        Interval rhs = (Interval) obj;
        return lower.equals(rhs.lower) && upper.equals(rhs.upper);
    }

    public Object[] toArray() {
        if (lower instanceof Double && upper instanceof Double) {
            return new Double[]{(Double) lower, (Double) upper};
        } else if (lower instanceof Long && upper instanceof Long) {
            return new Long[]{(Long) lower, (Long) upper};
        }
        throw new UnsupportedOperationException("unsupported interval type: " + lower.getClass().getName());
    }
    
    public static Object[] toArray(Interval[] arr) {
        Object[] ret;
        if (arr[0].getLower() instanceof Double) {
            ret = new Double[2 * arr.length];
        } else if (arr[0].getLower() instanceof Long) {
            ret = new Long[2 * arr.length];
        } else {
            throw new UnsupportedOperationException("unsupported interval array type: " + arr[0].getLower().getClass().getName());
        }
        int j = 0;
        for (int i = 0; i < arr.length; i++) {
            ret[j] = arr[i].getLower();
            ret[j + 1] = arr[i].getUpper();
            j += 2;
        }
        return ret;
    }
    
    public static Interval<Double> intersection(Interval<Double> i1, Interval<Double> i2) {
        if (i1.getLower() > i2.getUpper() || i1.getUpper() < i2.getLower()) {
            return null; // no overlap
        }

        double lb = Math.max(i1.getLower(), i2.getLower());
        double ub = Math.min(i1.getUpper(), i2.getUpper());
        return new Interval<>(lb, ub);
    }
}
