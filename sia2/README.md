# bifrost

`sia2` is a [Simple Image Access](https://www.ivoa.net/documents/SIA/) service
that should work with any TAP service that provides an ivoa.ObsCore table (or view).

## deployment
The `sia2` war file can be renamed at deployment time in order to support an 
alternate service name, including introducing additional path elements using the
[war-rename.conf](https://github.com/opencadc/docker-base/tree/master/cadc-tomcat) 
feature.

This service instance is expected to have a PostgreSQL database backend to store UWS
job information. This requirement could be removed in future to support a more lightweight
deployment of what is essentially a facade on a TAP service.

## configuration
The following configuration files must be available in the `/config` directory.

### catalina.properties
This file contains java system properties to configure the tomcat server and some of the java 
libraries used in the service.

See <a href="https://github.com/opencadc/docker-base/tree/master/cadc-tomcat">cadc-tomcat</a>
for system properties related to the deployment environment.

See <a href="https://github.com/opencadc/core/tree/master/cadc-util">cadc-util</a>
for common system properties.

`sia2` includes multiple IdentityManager implementations to support authenticated access:
 - See <a href="https://github.com/opencadc/ac/tree/master/cadc-access-control-identity">cadc-access-control-identity</a> for CADC access-control system support.
 - See <a href="https://github.com/opencadc/ac/tree/master/cadc-gms">cadc-gms</a> for OIDC token support.
 
 `sia2` requires one connection pool to store jobs:
```
# database connection pools
org.opencadc.sia2.uws.maxActive={max connections for jobs pool}
org.opencadc.sia2.uws.username={database username for jobs pool}
org.opencadc.sia2.uws.password={database password for jobs pool}
org.opencadc.sia2.uws.url=jdbc:postgresql://{server}/{database}
```
The _uws_ pool manages (create, alter, drop) uws tables and manages the uws content
(creates and modifies jobs in the uws schema when jobs are created and executed by users.

### cadc-registry.properties
See <a href="https://github.com/opencadc/reg/tree/master/cadc-registry">cadc-registry</a>.

### sia2.properties
`sia2` must be configured to use a single TAP service to execute queries.
```
# TAP service
org.opencadc.sia2.queryService = {resourceID or TAP base URL}
```
The _queryService_ is resolved by a registry lookup and that service is used to query
for CAOM content. It is assumed that this service is deployed "locally" since there can
be many calls and low latency is very desireable.

`sia2` will attempt to use the caller's identity to query so that CAOM proprietary metadata
protections are enforced, but the details of this depend on the configured IdentityManager 
and local A&A service configuration.

## building it
```
gradle clean build
docker build -t sia2 -f Dockerfile .
```

## checking it
```
docker run --rm -it sia2:latest /bin/bash
```

## running it
```
docker run --rm --user tomcat:tomcat --volume=/path/to/external/config:/config:ro --name sia2 sia2:latest
```
