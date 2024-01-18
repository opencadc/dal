/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2022.                            (c) 2022.
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

import ca.nrc.cadc.io.ByteCountOutputStream;
import ca.nrc.cadc.log.WebServiceLogInfo;
import ca.nrc.cadc.rest.SyncOutput;
import ca.nrc.cadc.util.StringUtil;
import ca.nrc.cadc.util.ThrowableUtil;
import ca.nrc.cadc.uws.ErrorSummary;
import ca.nrc.cadc.uws.ErrorType;
import ca.nrc.cadc.uws.ExecutionPhase;
import ca.nrc.cadc.uws.Job;
import ca.nrc.cadc.uws.ParameterUtil;
import ca.nrc.cadc.uws.server.JobRunner;
import ca.nrc.cadc.uws.server.JobUpdater;
import ca.nrc.cadc.uws.util.JobLogInfo;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * Abstract class PackageRunner: a JobRunner implementation that provides core
 * functionality for building CADC package files (zip or tar files, for example.)
 * To select a package type, RESPONSEFORMAT=(mime type) can be provided as a job parameter.
 * Default is 'application/x-tar'.
 * Job state management and error reporting back to the Job are provided.
 */
public abstract class PackageRunner implements JobRunner {
    private static final Logger log = Logger.getLogger(PackageRunner.class);

    private JobUpdater jobUpdater;
    private SyncOutput syncOutput;
    private ByteCountOutputStream bcOutputStream;
    private WebServiceLogInfo logInfo;

    protected Job job;
    protected String packageName;

    public PackageRunner() {}

    /**
     * Perform any functions needed to initialize the Package. (Setting the
     * package name and validating inputs for example.) Called before any other methods.
     */
    protected abstract void initPackage() throws IllegalArgumentException;

    /**
     * Build an Iterator of PackageItem. Use the list of files provided
     * in the Job instance (generated in the base JobRunner class.)
     * @return PackageItem Iterator instance - populated with references to the files in Job provided.
     * @throws IOException
     */
    protected abstract Iterator<PackageItem> getItems() throws IOException;

    @Override
    public void setJob(Job job) {
        this.job = job;
    }

    @Override
    public void setJobUpdater(JobUpdater ju) {
        this.jobUpdater = ju;
    }

    @Override
    public void setSyncOutput(SyncOutput so) {
        this.syncOutput = so;
    }

    @Override
    public void run() {
        logInfo = new JobLogInfo(job);
        log.info(logInfo.start());
        long start = System.currentTimeMillis();

        doIt();

        if (bcOutputStream != null) {
            logInfo.setBytes(bcOutputStream.getByteCount());
        }
        logInfo.setElapsedTime(System.currentTimeMillis() - start);
        log.info(logInfo.end());
    }

    private void doIt() {
        ExecutionPhase ep;
        PackageWriter writer = null;
        try {
            ep = jobUpdater.setPhase(job.getID(), ExecutionPhase.SUSPENDED, ExecutionPhase.EXECUTING, new Date());

            if (!ExecutionPhase.EXECUTING.equals(ep)) {
                ep = jobUpdater.getPhase(job.getID());
                log.debug(job.getID() + ": SUSPENDED -> EXECUTING [FAILED] -- DONE");
                logInfo.setSuccess(false);
                logInfo.setMessage("Could not set job phase to executing, was: " + ep);
                return;
            }
            log.debug(job.getID() + ": SUSPENDED -> EXECUTING [OK]");

            // Package name should be set here, and anything else needed for
            // package to be created aside from initializing the output stream.
            initPackage();

            if (!StringUtil.hasText(packageName)) {
                // packageName should have been set to something useful in initPackage()
                // as part of an implementing class
                throw new RuntimeException("BUG: packageName not defined.");
            }

            // Build an iterator of PackageItem from the files named
            // in the local Job instance
            Iterator<PackageItem> packageItems = getItems();

            writer = initWriter();

            while (packageItems.hasNext()) {
                PackageItem pi = packageItems.next();
                writer.write(pi);
            }

            // writer is closed in finally clause

            // set final phase, only sync so no results
            log.debug(job.getID() + ": EXECUTING -> COMPLETED...");
            ep = jobUpdater.setPhase(job.getID(), ExecutionPhase.EXECUTING, ExecutionPhase.COMPLETED, new Date());

            if (!ExecutionPhase.COMPLETED.equals(ep)) {
                ep = jobUpdater.getPhase(job.getID());
                log.debug(job.getID() + ": EXECUTING -> COMPLETED [FAILED], phase was " + ep);
                logInfo.setSuccess(false);
                logInfo.setMessage("Could not set job phase to completed.");
                return;
            }
            log.debug(job.getID() + ": EXECUTING -> COMPLETED [OK]");

        } catch (Throwable t) {
            if (ThrowableUtil.isACause(t, InterruptedException.class)) {
                try {
                    ep = jobUpdater.setPhase(job.getID(), ExecutionPhase.SUSPENDED, ExecutionPhase.EXECUTING, new Date());

                    if (!ExecutionPhase.ABORTED.equals(ep)) {
                        return; // clean exit of aborted job
                    }

                } catch (Exception ex2) {
                    log.error("failed to check job phase after InterruptedException", ex2);

                }
            }
            sendError(t, 500);
        } finally {
            // Finalize and close writer instance
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignore) {
                    log.debug("attempt to close writer when it wasn't open");
                }
            }
        }
    }


    /**
     * Set up the SyncOutput headers. Generate the ByteCountOutputStream using the SyncOutput stream.
     *
     * @param mimeType the stream Content-Type.
     * @param contentDisposition the stream Content_Disposition.
     * @return ByteCountOutputStream
     * @throws IOException if error getting the SyncOutput OutputStream.
     */
    private ByteCountOutputStream initOutputStream(String mimeType, String contentDisposition)
            throws IOException {
        // set up syncOutput response and headers
        syncOutput.setResponseCode(200);
        syncOutput.setHeader("Content-Type", mimeType);
        syncOutput.setHeader("Content-Disposition", contentDisposition);
        return new ByteCountOutputStream(syncOutput.getOutputStream());
    }

    /**
     * Set up PackageWriter and SyncOutput for the requested RESPONSEFORMAT value.
     * Default is 'application/x-tar'. Initialize syncOutput output stream with the correct
     * content type and disposition as provided by the writer class. Call writer ctor.
     *
     * @return PackageWriter instance
     * @throws IOException for an error initializing the OutputStream.
     */
    private PackageWriter initWriter()
            throws IOException {

        // Package type is in RESPONSEFORMAT Job parameter (optional)
        String responseFormat = ParameterUtil.findParameterValue("RESPONSEFORMAT", job.getParameterList());

        if (!StringUtil.hasLength(responseFormat)) {
            // Not provided, set default
            responseFormat = TarWriter.MIME_TYPE;
        }

        StringBuilder cdisp = new StringBuilder();
        cdisp.append("inline;filename=");
        cdisp.append(packageName);

        // Initialize the writer, underlying syncoutput and output streams.
        if (responseFormat.equals(ZipWriter.MIME_TYPE)) {
            cdisp.append(ZipWriter.EXTENSION);
            this.bcOutputStream = initOutputStream(ZipWriter.MIME_TYPE, cdisp.toString());
            return new ZipWriter(this.bcOutputStream);

        } else if (responseFormat.equals(TarWriter.MIME_TYPE)) {
            // Default for RESPONSEFORMAT is 'application/x-tar'
            cdisp.append(TarWriter.EXTENSION);
            this.bcOutputStream = initOutputStream(TarWriter.MIME_TYPE, cdisp.toString());
            return new TarWriter(this.bcOutputStream);

        }

        throw new UnsupportedOperationException("RESPONSEFORMAT not supported: " + responseFormat);

    }

    // ----------- Error handling for Job instance ----------------------
    /*
    * all sendError() functions eventually write the reported error into
    * the Job, setting the Job status accordingly.
    */
    private void sendError(Throwable t, int code) {
        if (code >= 500) {
            log.error("EPIC FAIL", t);
        }
        sendError(t, "unexpected failure: " + t.toString(), code);
    }

    private void sendError(Throwable t, String s, int code) {
        logInfo.setSuccess(false);
        logInfo.setMessage(s);
        log.debug("sendError", t);
        try {
            ErrorSummary err = new ErrorSummary(s, ErrorType.FATAL);
            ExecutionPhase ep = jobUpdater.setPhase(job.getID(), ExecutionPhase.EXECUTING, ExecutionPhase.ERROR, err, new Date());
            if (!ExecutionPhase.ERROR.equals(ep)) {
                ep = jobUpdater.getPhase(job.getID());
                log.debug(job.getID() + ": EXECUTING -> ERROR [FAILED] -- DONE");

            } else {
                log.debug(job.getID() + ": EXECUTING -> ERROR [OK]");

            }
        } catch (Throwable t2) {
            log.error("failed to persist Job ERROR for " + job.getID(), t2);
        }
    }
}
