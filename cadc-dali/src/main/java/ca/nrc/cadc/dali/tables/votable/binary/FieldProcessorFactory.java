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
 *  $Revision: 5 $
 *
 ************************************************************************
 */

package ca.nrc.cadc.dali.tables.votable.binary;

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.dali.Point;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.dali.tables.votable.VOTableField;
import ca.nrc.cadc.dali.util.UTCTimestampFormat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class FieldProcessorFactory {

    private final Map<String, FieldProcessor> decoders = new HashMap<>();

    public FieldProcessorFactory() {
        decoders.put("int", new IntFieldProcessor());
        decoders.put("long", new LongFieldProcessor());
        decoders.put("short", new ShortFieldProcessor());
        decoders.put("float", new FloatFieldProcessor());
        decoders.put("double", new DoubleFieldProcessor());
        decoders.put("char", new StringFieldProcessor());
        decoders.put("boolean", new BooleanFieldProcessor());
        decoders.put("unsignedbyte", new ByteFieldProcessor());
    }

    public FieldProcessor getFieldProcessor(String datatype) {
        FieldProcessor decoder = decoders.get(datatype);
        if (decoder == null) {
            throw new IllegalArgumentException("Unsupported datatype: " + datatype);
        }
        return decoder;
    }

    public static class StringFieldProcessor implements FieldProcessor {
        @Override
        public Object deSerialize(DataInputStream in, VOTableField field, int length) throws IOException {
            byte[] bytes = new byte[length];
            in.readFully(bytes);
            return new String(bytes, StandardCharsets.UTF_8).trim();
        }

        @Override
        public void serialize(DataOutputStream out, VOTableField field, Object value) throws IOException {
            if (value instanceof Date) {
                UTCTimestampFormat timestampFormat = new UTCTimestampFormat();
                value = timestampFormat.format((Date) value);
            } else if (value instanceof Instant) {
                UTCTimestampFormat timestampFormat = new UTCTimestampFormat();
                value = timestampFormat.format(Date.from((Instant) value));
            }

            byte[] bytes = value.toString().getBytes(StandardCharsets.UTF_8);
            out.writeInt(bytes.length);  // variable-length prefix
            out.write(bytes);
        }

        @Override
        public String getStringFormat(int len, Object data) {
            return data.toString();
        }
    }

    public static class IntFieldProcessor implements FieldProcessor {
        @Override
        public Object deSerialize(DataInputStream in, VOTableField field, int length) throws IOException {
            if (length == 1) {
                return in.readInt();
            }
            int[] arr = new int[length];
            for (int i = 0; i < length; i++) {
                arr[i] = in.readInt();
            }

            return arr;
        }

        @Override
        public void serialize(DataOutputStream out, VOTableField field, Object value) throws IOException {
            if (field.getArraysize() == null) {
                out.writeInt(((Number) value).intValue());
                return;
            }

            boolean isVariable = field.getArraysize().contains("*");
            int[] array = (int[]) value;

            if (isVariable) {
                out.writeInt(array.length); // variable-length prefix
            }
            for (int data : array) {
                out.writeInt(data);
            }
        }

        @Override
        public String getStringFormat(int len, Object data) {
            if (len == 1) {
                return data.toString();
            } else {
                int[] arr = (int[]) data;
                return Arrays.stream(arr).mapToObj(Integer::toString).collect(Collectors.joining(" "));
            }
        }
    }

    public static class ShortFieldProcessor implements FieldProcessor {
        @Override
        public Object deSerialize(DataInputStream in, VOTableField field, int length) throws IOException {
            if (length == 1) {
                return in.readShort();
            }

            short[] arr = new short[length];
            for (int i = 0; i < length; i++) {
                arr[i] = in.readShort();
            }

            return arr;
        }

        @Override
        public void serialize(DataOutputStream out, VOTableField field, Object value) throws IOException {
            if (field.getArraysize() == null) {
                out.writeShort(((Number) value).shortValue());
                return;
            }

            boolean isVariable = field.getArraysize().contains("*");
            short[] array = (short[]) value;

            if (isVariable) {
                out.writeInt(array.length); // variable-length prefix
            }
            for (short data : array) {
                out.writeShort(data);
            }
        }

        @Override
        public String getStringFormat(int len, Object data) {
            if (len == 1) {
                return data.toString();
            }
            short[] arr = (short[]) data;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(arr[i]);
            }
            return sb.toString().trim();
        }
    }

    public static class FloatFieldProcessor implements FieldProcessor {
        @Override
        public Object deSerialize(DataInputStream in, VOTableField field, int length) throws IOException {
            if (length == 1) {
                return in.readFloat();
            }

            float[] arr = new float[length];
            for (int i = 0; i < length; i++) {
                arr[i] = in.readFloat();
            }

            return arr;
        }

        @Override
        public void serialize(DataOutputStream out, VOTableField field, Object value) throws IOException {
            if (field.getArraysize() == null) {
                out.writeFloat(((Number) value).floatValue());
                return;
            }

            boolean isVariable = field.getArraysize().contains("*");
            float[] array = (float[]) value;

            if (isVariable) {
                out.writeInt(array.length); // variable-length prefix
            }
            for (float data : array) {
                out.writeFloat(data);
            }
        }

        @Override
        public String getStringFormat(int len, Object data) {
            if (len == 1) {
                return data.toString();
            }
            float[] arr = (float[]) data;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if (i > 0) {
                    sb.append(' ');
                }
                sb.append(arr[i]);
            }
            return sb.toString().trim();
        }
    }

    public static class DoubleFieldProcessor implements FieldProcessor {
        @Override
        public Object deSerialize(DataInputStream in, VOTableField field, int length) throws IOException {
            if (length == 1) {
                return in.readDouble();
            }

            double[] arr = new double[length];
            for (int i = 0; i < length; i++) {
                arr[i] = in.readDouble();
            }

            return arr;
        }

        @Override
        public void serialize(DataOutputStream out, VOTableField field, Object value) throws IOException {
            if (field.getArraysize() == null) {
                out.writeDouble(((Number) value).doubleValue());
                return;
            }

            boolean isVariable = field.getArraysize().contains("*");
            double[] array;

            if (value instanceof double[]) {
                array = (double[]) value;
            } else if (value instanceof Point) {
                Point p = (Point) value;
                array = p.toArray();
            } else if (value instanceof Circle) {
                Circle c = (Circle) value;
                array = c.toArray();
            } else if (value instanceof Polygon) {
                Polygon p = (Polygon) value;
                array = p.toArray();
            } else if (value instanceof Interval) {
                Interval interval = (Interval) value;
                Object[] intervalArray = interval.toArray();
                array = new double[intervalArray.length];
                for (int i = 0; i < intervalArray.length; i++) {
                    array[i] = ((Number) intervalArray[i]).doubleValue();
                }
            } else if (value instanceof Interval[]) {
                Interval[] intervals = (Interval[]) value;
                Object[] intervalsArray = Interval.toArray(intervals);
                array = new double[intervalsArray.length];
                for (int i = 0; i < intervalsArray.length; i++) {
                    array[i] = ((Number) intervalsArray[i]).doubleValue();
                }
            } else {
                throw new UnsupportedOperationException("Unsupported array data type: " + value.getClass().getName());
            }

            if (isVariable) {
                out.writeInt(array.length); // variable-length prefix
            }
            for (double data : array) {
                out.writeDouble(data);
            }
        }

        @Override
        public String getStringFormat(int len, Object data) {
            if (len == 1) {
                return data.toString();
            } else {
                double[] arr = (double[]) data;
                return Arrays.stream(arr).mapToObj(Double::toString).collect(Collectors.joining(" "));
            }
        }
    }

    public static class LongFieldProcessor implements FieldProcessor {
        @Override
        public Object deSerialize(DataInputStream in, VOTableField field, int length) throws IOException {
            if (length == 1) {
                return in.readLong();
            }

            long[] arr = new long[length];
            for (int i = 0; i < length; i++) {
                arr[i] = in.readLong();
            }

            return arr;
        }

        @Override
        public void serialize(DataOutputStream out, VOTableField field, Object value) throws IOException {
            if (field.getArraysize() == null) {
                out.writeLong(((Number) value).longValue());
                return;
            }

            boolean isVariable = field.getArraysize().contains("*");
            long[] array;

            if (value instanceof long[]) {
                array = (long[]) value;
            } else if (value instanceof Interval) {
                Interval interval = (Interval) value;
                Object[] array1 = interval.toArray();
                array = new long[array1.length];
                for (int i = 0; i < array1.length; i++) {
                    array[i] = ((Number) array1[i]).longValue();
                }
            } else if (value instanceof Interval[]) {
                Interval[] intervals = (Interval[]) value;
                Object[] intervalsArray = Interval.toArray(intervals);
                array = new long[intervalsArray.length];
                for (int i = 0; i < intervalsArray.length; i++) {
                    array[i] = ((Number) intervalsArray[i]).longValue();
                }
            } else {
                throw new UnsupportedOperationException("Unsupported array data type: " + value.getClass().getName());
            }

            if (isVariable) {
                out.writeInt(array.length); // variable-length prefix
            }
            for (long data : array) {
                out.writeLong(data);
            }
        }

        @Override
        public String getStringFormat(int len, Object data) {
            if (len == 1) {
                return data.toString();
            }
            long[] arr = (long[]) data;
            return Arrays.stream(arr).mapToObj(Long::toString).collect(Collectors.joining(" "));
        }
    }

    public static class BooleanFieldProcessor implements FieldProcessor {
        @Override
        public Object deSerialize(DataInputStream in, VOTableField field, int length) throws IOException {
            if (length == 1) {
                return in.readBoolean();
            }
            throw new UnsupportedOperationException("Boolean Arrays are not supported");
        }

        @Override
        public void serialize(DataOutputStream out, VOTableField field, Object value) throws IOException {
            if (field.getArraysize() == null) {
                out.writeBoolean((Boolean) value);
                return;
            }
            throw new UnsupportedOperationException("Boolean Arrays are not supported");
        }

        @Override
        public String getStringFormat(int len, Object data) {
            return data.toString();
        }
    }

    public static class ByteFieldProcessor implements FieldProcessor {
        @Override
        public Object deSerialize(DataInputStream in, VOTableField field, int length) throws IOException {
            if (length == 1) {
                return in.readByte();
            }

            byte[] bytes = new byte[length];
            in.readFully(bytes);

            return bytes;
        }

        @Override
        public void serialize(DataOutputStream out, VOTableField field, Object value) throws IOException {
            if (field.getArraysize() == null) {
                out.writeByte(((Number) value).byteValue());
                return;
            }

            boolean isVariable = field.getArraysize().contains("*");
            byte[] array = (byte[]) value;

            if (isVariable) {
                out.writeInt(array.length);
            }
            for (byte data : array) {
                out.writeByte(data);
            }
        }

        @Override
        public String getStringFormat(int len, Object data) {
            if (len == 1) {
                return data.toString();
            }
            byte[] arr = (byte[]) data;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < arr.length; i++) {
                if (i > 0) {
                    sb.append(" ");
                }
                sb.append(Byte.toUnsignedInt(arr[i]));
            }

            return sb.toString().trim();
        }
    }

}
