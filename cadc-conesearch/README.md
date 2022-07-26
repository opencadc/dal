# cadc-conesearch (1.0.0)

Library containing logic to help implementors support the [Simple Cone Search Working Draft (1.1)](https://www.ivoa.net/documents/ConeSearch/20200828/WD-ConeSearch-1.1-20200828.html).

## Building it

Gradle 6 or higher is required.  The DAL repository comes with a Gradle Wrapper.

```shell
/workdir/dal $ ./gradlew -i -b cadc-conesearch/build.gradle clean build test
```

## Installing it

Gradle 6 or higher is required.  The DAL repository comes with a Gradle Wrapper.

```shell
/workdir/dal $ ./gradlew -i -b cadc-conesearch/build.gradle clean build test install
```

## Implementing it

Your `build.gradle` file should include it as a dependency from Maven Central:

`build.gradle`:
```groovy
repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation 'org.opencadc:cadc-conesearch:1.0.0'
}
```

`${HOME}/config/cadc-conesearch.properties`:
```properties
tapURI = ivo://myhost.com/catalog_tap_service   # Or an absolute URL e.g. https://myhost.com/mytap
positionColumnName = my_spatial_indexed_column  # Used to compare to the Cone's Center position.  Can also be a dynamic spatial object e.g. POINT('ICRS', RA_Source, DEC_Source)
lowVerbositySelectList = obs_id, ra, dec        # Columns for VERB=1
midVerbositySelectList = obs_id, ra, dec, s_region, frequency, bandwidth  # Columns for VERB=2
highVerbositySelectList = *  # Columns for VERB=3
```

Capture the Cone Search parameters and translate to ADQL to be sent to a TAP service.

```java
final ConeSearchConfig coneSearchConfig = new ConeSearchConfig();  // Ensure the cadc-conesearch.properties exists.
final TAPQueryGenerator tapQueryGenerator = new TAPQueryGenerator("mycatalog", coneSearchConfig.getLowVerbositySelectList(), 
                                                                  coneSearchConfig.getMidVerbositySelectList(), 
                                                                  coneSearchConfig.getHighVerbositySelectList());
// Create TAP parameters
final Map<String, Object> parameters =
        tapQueryGenerator.getParameterMap(coneSearchConfig.getPositionColumnName(),
        parameterExtractor.getParameters(job.getParameterList()));

// Issue a Synchronous POST request to a TAP service.
final URL tapSyncURL = new URL(coneSearchConfig.getTapBaseURL().toExternalForm() + "/sync");

// POST ADQL query to TAP but do not follow redirect to execute it.
final HttpPost post = new HttpPost(tapSyncURL, parameters, false);
post.run();

// Do something with the data.
final URL redirectURL = post.getRedirectURL();  // In the case of a redirect
final InputStream inputStream = post.getInputStream();  // Read the data
```
