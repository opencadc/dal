# cadc-conesearch

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

Implementors will want to use the `TAPQueryGenerator` class to create parameters that can be sent to a TAP service.
