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

package ca.nrc.cadc.sia2.impl;

import ca.nrc.cadc.auth.AuthMethod;
import ca.nrc.cadc.auth.AuthenticationUtil;
import ca.nrc.cadc.reg.Standards;
import ca.nrc.cadc.reg.client.RegistryClient;
import ca.nrc.cadc.sia2.SiaRunner;
import ca.nrc.cadc.util.FileUtil;
import ca.nrc.cadc.util.PropertiesReader;
import ca.nrc.cadc.vosi.AvailabilityPlugin;
import ca.nrc.cadc.vosi.Availability;
import ca.nrc.cadc.vosi.avail.CheckException;
import ca.nrc.cadc.vosi.avail.CheckWebService;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Properties;

import org.apache.log4j.Logger;


/**
 * @author Sailor Zhang
 */
public class ServiceAvailability implements AvailabilityPlugin {

    private static final Logger log = Logger.getLogger(ServiceAvailability.class);
    private static final String CONFIG_FILE_NAME = SiaRunner.class.getSimpleName() + ".properties";
    private static final String CONFIG_TAP_URI_KEY = "tapURI";

    private String applicationName;
    private static String tapURI;

    public ServiceAvailability() {
    }

    /**
     * Set application name. The appName is a string unique to this
     * application.
     *
     * @param appName unique application name
     */
    @Override
    public void setAppName(String appName) {
        this.applicationName = appName;
    }

    @Override
    public boolean heartbeat() {
        return true;
    }

    public Availability getStatus() {
        boolean isGood = true;
        String note = "service is accepting queries";

        try {
            // Test the TAP service
            URL tapBaseURL = ServiceAvailability.getTapBaseURL();
            String availURLstr = tapBaseURL.toExternalForm() + "/availability";
            URL availURL = new URL(availURLstr);
            CheckWebService checkWebService = new CheckWebService(availURL);
            checkWebService.check();

            // TODO: use CheckDataSource if the JobPersistence impl is changed to use a database
        } catch (CheckException ce) {
            // tests determined that the resource is not working
            isGood = false;
            note = ce.getMessage();
        } catch (Throwable t) {
            // the test itself failed
            isGood = false;
            note = String.format("%s test failed, reason: %s", applicationName, t);
        }
        return new Availability(isGood, note);
    }

    public void setState(String string) {
        //no-op
    }

    /**
     * Obtain the base TAP URL to use. This method will read in an optionally configured 'tapURI' property and if it
     * appears to be a URL, then assume an unregistered TAP service was configured and treat it as the base URL,
     * otherwise attempt to use it as a URI and look the URL up in the Registry.
     *
     * @return URL Base URL of the TAP service to use.
     *
     * @throws MalformedURLException If a URL cannot be created from the specified string.
     */
    public static URL getTapBaseURL() throws MalformedURLException {
        RegistryClient regClient = new RegistryClient();
        URI configuredTapURI = URI.create(ServiceAvailability.getTapURI());

        AuthMethod am = AuthenticationUtil.getAuthMethod(AuthenticationUtil.getCurrentSubject());
        if (configuredTapURI.getScheme().equals("ivo")) {
            // Attempt to load the URI as a resource URI from the Registry.
            return regClient.getServiceURL(configuredTapURI, Standards.TAP_10,
                                           (am == null) ? AuthMethod.ANON : am);
        } else {
            // Fallback and assume the URI is an absolute one.
            return configuredTapURI.toURL();
        }
    }

    public static String getTapURI() {
        if (tapURI == null) {
            try {
                tapURI = ServiceAvailability.getTapURIProperty();

                if (tapURI == null) {
                    throw new RuntimeException("config error: failed to find tapURI in classpath or external file.");
                }
            } catch (IOException ex) {
                final String message = String.format("failed to read config file '%s' from classpath.",
                                                     CONFIG_FILE_NAME);
                log.error(message, ex);
                throw new RuntimeException(message, ex);
            }
        }
        return tapURI;
    }

    private static String getTapURIProperty() throws IOException {
        // Try to get from a file on disk first.
        final PropertiesReader propertiesReader = new PropertiesReader(CONFIG_FILE_NAME);

        if (propertiesReader.canRead()) {
            return propertiesReader.getFirstPropertyValue(CONFIG_TAP_URI_KEY);
        } else {
            URL url = FileUtil.getURLFromResource(CONFIG_FILE_NAME, ServiceAvailability.class);
            Properties props = new Properties();
            props.load(url.openStream());
            return props.getProperty(CONFIG_TAP_URI_KEY);
        }
    }
}
