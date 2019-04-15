# cadc-sia (1.1.4)

This is a simple SIA-2.0 implementation that constructs an ADQL query of the ivoa.ObsCore table and submist it to
a TAP service. It assumes the TAP `/sync` resource follows the POST-redirect-GET (PrG) pattern (`SyncServlet` in cadcUWS
follows this pattern) so it can submit the TAP job, capture the redirect, and then redirect the caller to get the
query result directly from the TAP service.

The TAP service URL is found by looking in a config file (`SiaRunner.properties`) that is found in the classpath. The sample
war file includes `src/resources/SiaRunner.properties` and thus looks up the CADC TAP service. The lookup itself is done
via the RegistryClient class in cadcResgitry; this is a simple config-file based "fake" registry lookup. If you change
the `tapURI` property in `SiaRunner.properties` you will also need to make sure the URI is listed in the RegistryClient
.properties config file.  Alternatively, you may set the `tapURI` property to be an absolute URL to specify an 
unregistered TAP service.

The default config for that library is included in the jar file; you might be able to 
include a new `RegistryClient.properties` in `WEB-INF/classes` or you might have to replace the one in the jar file. This sort of thing
can all be done in the ant build file that constructs the war.

The sample war file built uses the MemoryJobPersistence implementation. You would need to implement your own JobManager and
configure it in the web.xml file to change that. 

Intended use: We deploy the JAR artifact to the [OpenCADC Bintray (Maven)](https://bintray.com/opencadc/software) repository.  
A base Web Service (WAR file) can be implemented in Gradle or Maven from that and deployed to a Java Servlet container.

The default configuration has our preferred JobManager implementation and config files.


IMPORTANT NOTE: The implementation of "POS=RANGE 12/14 13/15" makes use of a custom function we have implemented in the CADC
TAP service to query by coordinate range: RANGE_S2D(ra1, ra2, dec1, dec2). The implementation uses the pg_sphere sbox type
directly. We took this approach in our TAP service after several discussions concerning ADQL showed that there is general
agreement that the geometry functions in ADQL-2.0 got it wrong in trying to map directly to STC regions, specificially
in including coordinate system metadata in the geometry but also using STC BOX instead of a simple coordinate range that 
users want and is easier to implement. So RANGE_S2D is a simple range with no embedded coordinate metadata.

