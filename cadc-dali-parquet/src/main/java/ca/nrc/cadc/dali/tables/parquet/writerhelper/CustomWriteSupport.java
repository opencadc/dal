/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2025.                            (c) 2025.
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

package ca.nrc.cadc.dali.tables.parquet.writerhelper;

import ca.nrc.cadc.dali.tables.votable.VOTableField;

import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.log4j.Logger;
import org.apache.parquet.hadoop.api.WriteSupport;
import org.apache.parquet.io.api.Binary;
import org.apache.parquet.io.api.RecordConsumer;
import org.apache.parquet.schema.MessageType;

public class CustomWriteSupport extends WriteSupport<List<Object>> {

    private static final Logger log = Logger.getLogger(CustomWriteSupport.class);

    private final List<VOTableField> voTableFields;
    private RecordConsumer recordConsumer;
    private final MessageType schema;
    private final Map<String, String> extraMetaData;

    CustomWriteSupport(MessageType schema, List<VOTableField> voTableFields, Map<String, String> extraMetaData) {
        super();
        this.voTableFields = voTableFields;
        this.schema = schema;
        this.extraMetaData = extraMetaData;
    }

    @Override
    public WriteContext init(Configuration configuration) {
        return new WriteContext(schema, extraMetaData);
    }

    @Override
    public void prepareForWrite(RecordConsumer recordConsumer) {
        this.recordConsumer = recordConsumer;
    }

    @Override
    public void write(List<Object> dataList) {
        recordConsumer.startMessage();

        VOTableField voTableField;
        for (int i = 0; i < dataList.size(); i++) {
            voTableField = voTableFields.get(i);
            Object data = dataList.get(i);
            if (data != null) {
                recordConsumer.startField(voTableField.getName(), i);
                try {
                    if (data.getClass().isArray()) {
                        fillUpArrayData(recordConsumer, data);
                    } else {
                        fillUpPrimitiveData(recordConsumer, data);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Error writing field '" + voTableField.getName() + "' (index " + i + ")", e);
                }
                recordConsumer.endField(voTableField.getName(), i);
            }
        }
        recordConsumer.endMessage();
    }

    private void fillUpPrimitiveData(RecordConsumer recordConsumer, Object data) {
        if (data instanceof String) {
            recordConsumer.addBinary(Binary.fromString((String) data));
        } else if (data instanceof Integer) {
            recordConsumer.addInteger((Integer) data);
        } else if (data instanceof Short) {
            recordConsumer.addInteger((int)(Short) data);
        } else if (data instanceof Long) {
            recordConsumer.addLong((Long) data);
        } else if (data instanceof Double) {
            recordConsumer.addDouble((Double) data);
        } else if (data instanceof Float) {
            recordConsumer.addFloat((Float) data);
        } else if (data instanceof Boolean) {
            recordConsumer.addBoolean((Boolean) data);
        } else {
            log.debug("Unsupported data type: " + data.getClass().getName());
            throw new UnsupportedOperationException("Unsupported data type: " + data.getClass().getName());
        }
    }

    private void fillUpArrayData(RecordConsumer recordConsumer, Object data) {
        if (data instanceof byte[]) {
            recordConsumer.addBinary(Binary.fromConstantByteArray((byte[]) data));
            return;
        }

        recordConsumer.startGroup();
        recordConsumer.startField("element", 0);
        if (data instanceof int[]) {
            int[] array = (int[]) data;
            for (int value : array) {
                recordConsumer.addInteger(value);
            }
        } else if (data instanceof long[]) {
            long[] array = (long[]) data;
            for (long value : array) {
                recordConsumer.addLong(value);
            }
        } else if (data instanceof double[]) {
            double[] array = (double[]) data;
            for (double value : array) {
                recordConsumer.addDouble(value);
            }
        } else if (data instanceof float[]) {
            float[] array = (float[]) data;
            for (float value : array) {
                recordConsumer.addFloat(value);
            }
        } else if (data instanceof Long[]) {
            Long[] array = (Long[]) data;
            for (Long value : array) {
                recordConsumer.addLong(value);
            }
        } else if (data instanceof Double[]) {
            Double[] array = (Double[]) data;
            for (Double value : array) {
                recordConsumer.addDouble(value);
            }
        } else if (data instanceof short[]) {
            short[] array = (short[]) data;
            for (short value : array) {
                recordConsumer.addInteger(value); // Parquet does not have a short type, so we use int
            }
        } else {
            log.debug("Unsupported array data type: " + data.getClass().getName());
            throw new UnsupportedOperationException("Unsupported array data type: " + data.getClass().getName());
        }

        recordConsumer.endField("element", 0);
        recordConsumer.endGroup();
    }
}
