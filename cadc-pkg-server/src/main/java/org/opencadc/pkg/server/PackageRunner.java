/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2021.                            (c) 2021.
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

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
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
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * Abstract class PackageRunner: a JobRunner implementation that provides core
 * functionality for building CADC package files (zip or tar files, for example.)
 * Job state management and error reporting back to the Job are provided.
 */
public abstract class PackageRunner implements JobRunner {
    private static final Logger log = Logger.getLogger(PackageRunner.class);

    private JobUpdater jobUpdater;
    private SyncOutput syncOutput;
    private ByteCountOutputStream outputStreamCounter;
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
     */
    protected abstract Iterator<PackageItem> getItems() throws IOException;

    /**
     * Get name for package Runner will create.
     * @return String with package name.
     */
    protected abstract String getPackageName();

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

        if (outputStreamCounter != null) {
            logInfo.setBytes(outputStreamCounter.getByteCount());
        }
        logInfo.setElapsedTime(System.currentTimeMillis() - start);
        log.info(logInfo.end());
    }

    private void doIt() {
        ExecutionPhase ep;
        TarWriter writer = null;
        try {
            ep = jobUpdater.setPhase(job.getID(), ExecutionPhase.QUEUED, ExecutionPhase.EXECUTING, new Date());

            if (!ExecutionPhase.EXECUTING.equals(ep)) {
                ep = jobUpdater.getPhase(job.getID());
                log.debug(job.getID() + ": QUEUED -> EXECUTING [FAILED] -- DONE");
                logInfo.setSuccess(false);
                logInfo.setMessage("Could not set job phase to executing, was: " + ep);
                return;
            }
            log.debug(job.getID() + ": QUEUED -> EXECUTING [OK]");

            // Package name should be set here, and anything else needed for
            // package to be created aside from initializing the output stream.
            initPackage();

            // Build an iterator of PackageItem from the files named
            // in the local Job instance
            Iterator<PackageItem> packageItems = getItems();

            // TarWriter needs an output stream instantiated.
            if (StringUtil.hasText(packageName)) {
                initOutputStream(packageName);
            } else {
                throw new IllegalArgumentException("package name not defined.");
            }
            writer = new TarWriter(this.outputStreamCounter);

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
                    ep = jobUpdater.setPhase(job.getID(), ExecutionPhase.QUEUED, ExecutionPhase.EXECUTING, new Date());

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
     * Initialize syncOutput output stream with the correct content type and disposition,
     * as provided by the writer class
     * @param packageName - package name to use
     * @throws IOException
     */
    private void initOutputStream(String packageName) throws IOException {

        if (!StringUtil.hasText(packageName)) {
            throw new RuntimeException("BUG: packageName can't be null");
        }

        syncOutput.setResponseCode(200);

        // TODO: when a mechanism to select the package type
        // is included in PackageRunner, the content type and
        // disposition values will depend on values set on entry to
        // PackageRunner. For now they're hard coded for tar files.
        StringBuilder cdisp = new StringBuilder();
        cdisp.append("inline;filename=");
        cdisp.append(packageName);
        cdisp.append(".tar");

        syncOutput.setHeader("Content-Type", "application/x-tar");
        syncOutput.setHeader("Content-Disposition", cdisp.toString());
        this.outputStreamCounter = new ByteCountOutputStream(syncOutput.getOutputStream());
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
