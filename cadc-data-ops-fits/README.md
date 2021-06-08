# OpenCADC FITS file operator (cadc-data-ops-fits) 0.1.2

Library to interface with, and perform operations on, FITS files.  This library was written in Java and supports JDK 8 and 11.
The `cadc-data-ops-fits` library depends on the NASA led NOM TAM FITS library 
(https://github.com/nom-tam-fits/nom-tam-fits) version 1.15.3 (or newer).

## Building it
You may use the provided Gradle Wrapper, or provide your own Gradle (< 7) installation.

```sh
$ ../gradlew -i clean build
```

## Cutout API
This library supports the commonly used cutout syntax to extract a sub-image from an Image HDU.

### Testing it
```sh
$ ../gradlew -i clean test
```

Some tests will be ignored as they require local files that are too big to fit into source control 
(see [CircleCutoutTest](src/test/java/org/opencadc/fits/slice/CircleCutoutTest.java), [PolygonCutoutTest](src/test/java/org/opencadc/fits/slice/PolygonCutoutTest.java))
The contents of https://www.canfar.net/storage/list/CADC/test-data/cutouts can be put into your `System.properties("user.home")/.config/test-data`
to enable them (recommended).

### Examples

#### A 100x100 image from the middle of a 400x400 image

```java
final File myFitsFile = new File("/data/file.fits");
final String myCutoutSpec = "[150:250, 150:250]"; // Will translate to a corner at {150,150} with 100 pixel lengths
final FitsOperations fitsOperations = new FitsOperations(myFitsFile);
fitsOperations.slice(myCutoutSpec, System.out);
```

#### A 100x100 image from a specific HDU

```java
final File myFitsFile = new File("/data/file.fits");

// Will translate to a corner at {0,0} with 100 pixel lengths ONLY for an HDU whose
// Extension name (EXTNAME) is 'SCI' and whose Extension version (EXTVER) is 2.
final String myCutoutSpec = "[SCI,2][0:100, 0:100]";
 
final FitsOperations fitsOperations = new FitsOperations(myFitsFile);
fitsOperations.slice(myCutoutSpec, System.out);
```

#### A Circle cutout using the IVOA SODA API (https://www.ivoa.net/documents/SODA/)

```java
final File myFitsFile = new File("/data/file.fits");

// Use the SODA CIRCLE API
final String myCutoutSpec = "CIRCLE 134.4 7.54 0.1";
final FitsOperations fitsOperations = new FitsOperations(myFitsFile);
fitsOperations.slice(myCutoutSpec, System.out);
```
