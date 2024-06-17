# dap

`dap` is a prototype [Data Access Protocol](https://github.com/ivoa-std/DAP) service
that should work with any TAP service that provides an ivoa.ObsCore table (or view). It
also supports a mode that makes it operate as a compliant 
[Simple Image Access 2.0](https://www.ivoa.net/documents/SIA/) service.

## deployment
The `dap` war file can be renamed at deployment time in order to support an 
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

`dap` includes multiple IdentityManager implementations to support authenticated access:
 - See <a href="https://github.com/opencadc/ac/tree/master/cadc-access-control-identity">cadc-access-control-identity</a> for CADC access-control system support.
 - See <a href="https://github.com/opencadc/ac/tree/master/cadc-gms">cadc-gms</a> for OIDC token support.
 
 `dap` requires one connection pool to store jobs:
```
# database connection pools
org.opencadc.dap.uws.maxActive={max connections for jobs pool}
org.opencadc.dap.uws.username={database username for jobs pool}
org.opencadc.dap.uws.password={database password for jobs pool}
org.opencadc.dap.uws.url=jdbc:postgresql://{server}/{database}
```
The _uws_ pool manages (create, alter, drop) uws tables and manages the uws content
(creates and modifies jobs in the uws schema when jobs are created and executed by users.

### cadc-registry.properties
See <a href="https://github.com/opencadc/reg/tree/master/cadc-registry">cadc-registry</a>.

### dap.properties
`dap` must be configured to use a single TAP service to execute queries.
```
# TAP service
org.opencadc.dap.queryService = {resourceID or TAP base URL}

# run in backwards compatible SIAv2 mode (optional)
org.opencadc.dap.sia2mode = true | false
```
The _queryService_ is resolved by a registry lookup and that service is used to query
for CAOM content. It is assumed that this service is deployed "locally" since there can
be many calls and low latency is very desireable.

The _sia2mode_ can be set to make the service behave as an SIA-2.0 service: this causes
the generated query to restrict the ObsCore.dataproduct_type values to `cube` and `image`.
TODO: the `/capabilities` endpoint is currently hard-coded to advertise the `SIA#query-2.0` 
standardID so that _sia2mode_ is fully correct; as a result "DAP" mode is not really correct
right now.

`dap` will attempt to use the caller's identity to query, but the details of this depend 
on the configured IdentityManager and local A&A service configuration.

## building it
```
gradle clean build
docker build -t dap -f Dockerfile .
```

## checking it
```
docker run --rm -it dap:latest /bin/bash
```

## running it
```
docker run --rm --user tomcat:tomcat --volume=/path/to/external/config:/config:ro --name dap dap:latest
```
