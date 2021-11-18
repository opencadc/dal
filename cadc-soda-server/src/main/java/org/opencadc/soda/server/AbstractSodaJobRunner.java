/*
************************************************************************
*******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
**************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
*
*  (c) 2020.                            (c) 2020.
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

package org.opencadc.soda.server;

import ca.nrc.cadc.dali.Circle;
import ca.nrc.cadc.dali.Interval;
import ca.nrc.cadc.dali.ParamExtractor;
import ca.nrc.cadc.dali.Polygon;
import ca.nrc.cadc.dali.Shape;
import ca.nrc.cadc.dali.util.CircleFormat;
import ca.nrc.cadc.dali.util.DoubleIntervalFormat;
import ca.nrc.cadc.dali.util.PolygonFormat;
import ca.nrc.cadc.log.WebServiceLogInfo;
import ca.nrc.cadc.net.TransientException;
import ca.nrc.cadc.rest.SyncOutput;
import ca.nrc.cadc.util.CaseInsensitiveStringComparator;
import ca.nrc.cadc.uws.ErrorSummary;
import ca.nrc.cadc.uws.ErrorType;
import ca.nrc.cadc.uws.ExecutionPhase;
import ca.nrc.cadc.uws.Job;
import ca.nrc.cadc.uws.Result;
import ca.nrc.cadc.uws.server.JobNotFoundException;
import ca.nrc.cadc.uws.server.JobPersistenceException;
import ca.nrc.cadc.uws.server.JobRunner;
import ca.nrc.cadc.uws.server.JobUpdater;
import ca.nrc.cadc.uws.util.JobLogInfo;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import org.opencadc.soda.SodaParamValidator;

/**
 * This JobRunner implements IVOA WD-SODA-1.0 job semantics.
 *
 * @author pdowler
 */
public abstract class AbstractSodaJobRunner implements JobRunner {

    private static Logger log = Logger.getLogger(AbstractSodaJobRunner.class);

    //private static final EnergyConverter conv = new EnergyConverter();
    
    static final String RESULT_OK = "ok";
    static final String RESULT_WARN = "warn";
    static final String RESULT_FAIL = "fail";

    private final Set<String> customCutoutParams = new TreeSet<String>(new CaseInsensitiveStringComparator());
    private final SodaParamValidator spval = new SodaParamValidator();
    
    private JobUpdater jobUpdater;
    protected SyncOutput syncOutput;
    protected Job job;

    private WebServiceLogInfo logInfo;

    public AbstractSodaJobRunner() {
    }

    public abstract SodaPlugin getSodaPlugin();

    public void setJobUpdater(JobUpdater jobUpdater) {
        this.jobUpdater = jobUpdater;
    }

    public void setSyncOutput(SyncOutput syncOutput) {
        this.syncOutput = syncOutput;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    // subclass populates in ctor
    protected final Set<String> getCustomCutoutParams() {
        return customCutoutParams;
    }

    
    public void run() {
        logInfo = new JobLogInfo(job);
        log.info(logInfo.start());
        long start = System.currentTimeMillis();

        try {
            doit();
        } catch (Exception ex) {
            log.error("unexpected exception", ex);
        }

        logInfo.setElapsedTime(System.currentTimeMillis() - start);
        log.info(logInfo.end());
    }

    void doit() throws IOException {
        try {
            // phase->EXECUTING
            ExecutionPhase ep = jobUpdater.setPhase(job.getID(), ExecutionPhase.QUEUED, ExecutionPhase.EXECUTING, new Date());
            if (!ExecutionPhase.EXECUTING.equals(ep)) {
                ep = jobUpdater.getPhase(job.getID());
                log.debug(job.getID() + ": QUEUED -> EXECUTING [FAILED] -- phase is " + ep);
                logInfo.setSuccess(false);
                logInfo.setMessage("Could not set job phase to EXECUTING.");
                return;
            }
            log.debug(job.getID() + ": QUEUED -> EXECUTING [OK]");

            // validate params
            List<String> pnames = new ArrayList<>();
            pnames.addAll(SodaParamValidator.SODA_PARAMS);
            pnames.addAll(customCutoutParams);
            ParamExtractor pex = new ParamExtractor(pnames);
            Map<String, List<String>> params = pex.getParameters(job.getParameterList());
            log.debug("soda params: " + SodaParamValidator.SODA_PARAMS.size() + " map params: " + params.size());
            List<String> idList = spval.validateID(params);

            List<Cutout> posCut = wrapPos(spval.validateAllShapes(params));
            List<Cutout> bandCut = wrapBand(spval.validateBAND(params));
            List<Cutout> timeCut = wrapTime(spval.validateTIME(params));
            Cutout polCut = new Cutout();
            polCut.pol = spval.validatePOL(params);
            List<Cutout> customCuts = new ArrayList<>();
            for (String ccp : customCutoutParams) {
                List<Interval> cis = spval.validateNumericInterval(ccp, params);
                for (Interval ci : cis) {
                    Cutout c = new Cutout();
                    c.customAxis = ccp;
                    c.custom = ci;
                    customCuts.add(c);
                }
            }
            Map<String, List<String>> extraParams = pex.getExtraParameters(job.getParameterList());
            
            
            // check single-valued param limits
            StringBuilder esb = new StringBuilder();
            if (idList.size() != 1) {
                esb.append("found ").append(idList.size()).append(" ID values, expected 1\n");
            }
            if (syncOutput != null) {
                if (posCut.size() > 1) {
                    esb.append("found ").append(posCut.size()).append(" POS/CIRCLE/POLY values, expected 0-1\n");
                }
                if (bandCut.size() > 1) {
                    esb.append("found ").append(bandCut.size()).append(" BAND values, expected 0-1\n");
                }
                if (timeCut.size() > 1) {
                    esb.append("found ").append(timeCut.size()).append(" TIME values, expected 0-1\n");
                }
                if (customCuts.size() > 1) {
                    esb.append("found ").append(customCuts.size()).append(" ");
                    for (Cutout c : customCuts) {
                        esb.append(c.customAxis).append("|");
                        // will leave extra | at end
                    }
                    esb.append(" values, expected 0-1 custom axis");
                }
            }
            
            if (esb.length() > 0) {
                throw new IllegalArgumentException(esb.toString());
            }

            List<URI> ids = new ArrayList<>();
            esb = new StringBuilder();
            for (String i : idList) {
                try {
                    ids.add(new URI(i));
                } catch (URISyntaxException ex) {
                    esb.append("invalid URI: ").append(i).append("\n");
                }
            }
            if (esb.length() > 0) {
                throw new IllegalArgumentException("invalid ID(s) found\n" + esb.toString());
            }

            // add single no-op element to make subsequent loops easier
            if (posCut.isEmpty()) {
                posCut.add(new Cutout());
            }
            if (bandCut.isEmpty()) {
                bandCut.add(new Cutout());
            }
            if (timeCut.isEmpty()) {
                timeCut.add(new Cutout());
            }
            if (customCuts.isEmpty()) {
                customCuts.add(new Cutout());
            }

            String runID = job.getRunID();
            if (runID == null) {
                runID = job.getID();
            }

            SodaPlugin doit = getSodaPlugin();
            List<Result> jobResults = new ArrayList<>();
            int serialNum = 1;
            for (URI id : ids) {
                // async mode: cartesian product of pos+band+time+custom
                // sync mode: each list has 1 entry (possibly no-op for that axis)
                for (Cutout pos : posCut) {
                    for (Cutout band : bandCut) {
                        for (Cutout time : timeCut) {
                            for (Cutout cust : customCuts) {
                                // collect 
                                Cutout cut = new Cutout();
                                cut.pos = pos.pos;
                                cut.band = band.band;
                                cut.time = time.time;
                                cut.pol = polCut.pol;
                                cut.customAxis = cust.customAxis;
                                cut.custom = cust.custom;
                                URL url = doit.toURL(serialNum, id, cut, extraParams);
                                log.debug("cutout URL: " + url.toExternalForm());
                                try {
                                    jobResults.add(new Result(RESULT_OK + "-" + serialNum++, url.toURI()));
                                } catch (URISyntaxException ex) {
                                    throw new RuntimeException("BUG: result URL is invalid URI: " + url.toExternalForm(), ex);
                                }
                            }
                        }
                    }
                }
            }

            // sync: redirect
            if (syncOutput != null) {
                Result r0 = jobResults.get(0);
                syncOutput.setHeader("Location", r0.getURI().toASCIIString());
                syncOutput.setCode(303);
            }

            // phase -> COMPLETED
            ExecutionPhase fep = ExecutionPhase.COMPLETED;
            log.debug("setting ExecutionPhase = " + fep + " with results");
            jobUpdater.setPhase(job.getID(), ExecutionPhase.EXECUTING, fep, jobResults, new Date());
        } catch (IllegalArgumentException ex) {
            handleError(400, ex.getMessage());
        } catch (JobNotFoundException ex) {
            handleError(400, ex.getMessage());
        } catch (IllegalStateException ex) {
            handleError(500, ex.getMessage(), ex);
        } catch (JobPersistenceException ex) {
            handleError(500, ex.getMessage(), ex);
        } catch (TransientException ex) {
            handleError(503, ex.getMessage(), ex);
        } catch (Throwable unexpected) {
            handleError(500, "unexpected failure: " + unexpected, unexpected);
        }
    }
    
    static List<Cutout> wrapPos(List<Shape> inner) {
        ArrayList<Cutout> ret = new ArrayList<>(inner.size());
        for (Shape i : inner) {
            Cutout c = new Cutout();
            c.pos = i;
            ret.add(c);
        }
        return ret;
    }
    
    static List<Cutout> wrapBand(List<Interval> inner) {
        ArrayList<Cutout> ret = new ArrayList<>(inner.size());
        for (Interval i : inner) {
            Cutout c = new Cutout();
            c.band = i;
            ret.add(c);
        }
        return ret;
    }
    
    static List<Cutout> wrapTime(List<Interval> inner) {
        ArrayList<Cutout> ret = new ArrayList<>(inner.size());
        for (Interval i : inner) {
            Cutout c = new Cutout();
            c.time = i;
            ret.add(c);
        }
        return ret;
    }

    private void handleError(int code, String msg) throws IOException {
        handleError(code, msg, null);
    }
    
    private void handleError(int code, String msg, Throwable t) throws IOException {
        logInfo.setMessage(msg);
        if (t != null) {
            log.error("internal exception", t);
        }
        
        if (syncOutput != null) {
            syncOutput.setCode(code);
            syncOutput.setHeader("Content-Type", "text/plain");
            PrintWriter w = new PrintWriter(syncOutput.getOutputStream());
            w.println(msg);
            w.flush();
        }

        ExecutionPhase fep = ExecutionPhase.ERROR;
        ErrorSummary es = new ErrorSummary(msg, ErrorType.FATAL);
        log.debug("setting ExecutionPhase = " + fep + " with results");
        try {
            jobUpdater.setPhase(job.getID(), ExecutionPhase.EXECUTING, fep, es, new Date());
        } catch (JobNotFoundException ex) {
            log.error("oops", ex);
        } catch (JobPersistenceException ex) {
            log.error("oops", ex);
        } catch (TransientException ex) {
            log.error("oops", ex);
        }
    }
}
