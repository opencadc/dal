# OpenCADC FITS file operator (cadc-data-ops-fits) 0.1.2

Library to interface with, and perform operations on, FITS files.  This library was written in Java and supports JDK 8 and 11.
The `cadc-data-ops-fits` library depends on the NASA led NOM TAM FITS library 
(https://github.com/nom-tam-fits/nom-tam-fits) version 1.15.3 (or newer).

## Building it
Gradle is required to compile and assemble the JAR file.

```
gradle -i clean build
```

## Slicing API (Cutouts)
This library supports the commonly used cutout syntax to extract a sub-image from an HDU:

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

#### A Circle cutout using SODA (https://www.ivoa.net/documents/SODA/)

```java
final File myFitsFile = new File("/data/file.fits");

// Use the SODA CIRCLE API
final String myCutoutSpec = "CIRCLE 134.4 7.54 0.1";
final FitsOperations fitsOperations = new FitsOperations(myFitsFile);
fitsOperations.slice(myCutoutSpec, System.out);
```
