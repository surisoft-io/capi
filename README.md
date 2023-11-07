<img src="https://capi.surisoft.io/capi_horizontal.svg" alt="CAPI" width="20%"/>

[![CAPI-LB](https://github.com/rodrigoserracoelho/capi-lb/actions/workflows/main.yml/badge.svg)](https://github.com/rodrigoserracoelho/capi-lb/actions/workflows/main.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=surisoft-io_capi-lb&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=surisoft-io_capi-lb)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Docker Image Version (latest by date)](https://img.shields.io/docker/v/surisoft/capi)
![Known Vulnerabilities](https://snyk.io/test/github/surisoft-io/capi-lb/badge.svg)

<h5 align="center">
  <br>
  <a href="https://github.com/surisoft-io/capi-lb/issues/new?assignees=&labels=use+case&template=use_case.md&title=%5BUSECASE%5D+">
    <img src="https://dummyimage.com/1000x80/15273c/ffffff.png&text=If+you+are+using+CAPI,+please+let+us+know+by+clicking+here" alt="Share your use case with us">
  </a>
  <br>
</h5>

# CAPI Gateway Documentation
## _Light Apache Camel API Gateway_

## Supports:
* Light API Gateway / Load Balancer powered by Apache Camel dynamics routes.
* Optional Spring Security OAUTH2 protected CAPI Manager API.
* Distributed tracing system (OpenTelemetry Collector / Jaeger / Zipkin)
* Metrics (Prometheus)
* CAPI Browser user interface for route management.
* Rest API for Route management.
* Load Balancer (Round robin)
* Failover (With and without Round Robin)
* Tenant support (Headers)
* Sticky Session (Cookies and Headers)
* Certificate Manager (using the CAPI Manager API)
* No DB is needed, CAPI uses Hashicorp Consul for service discovery
* Websocket Gateway (Since version 4.0.11)

### Enable Hashicorp Consul
```yaml
  consul:
    host: http://localhost:8500
    discovery:
      enabled: true
      timer:
        interval: 10000
```
The interval will determine how often CAPI will pull Consul for changes.
CAPI consumes the Catalog API from Consul to discover new services.
Here is an example of how to declare your service to be discovered by CAPI (we will use a Spring Boot application):
You need to include all the required Consul dependencies on your project:
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-consul</artifactId>
  <version></version>
</dependency>
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-consul-discovery</artifactId>
  <version></version>
</dependency>
```

## Example of an Service definition
```json
  {
    "name": "test-service",
    "context": "/test-service/dev",
    "mappingList": [
      {
        "rootContext": "/",
        "hostname": "domain1",
        "port": 8080,
        "ingress": false
      },
      {
        "rootContext": "/",
        "hostname": "domain2",
        "port": 8080,
        "ingress": false
      }
    ],
    "serviceMeta": {
      "group": "dev",
      "root-context": null,
      "schema": null,
      "secured": false,
      "tenant_aware": false,
      "tenant_id": null,
      "X-B3-TraceId": false,
      "ingress": null,
      "sticky_session_enabled": true,
      "sticky_session_type": "cookie",
      "sticky_session_key": "smkSession",
      "type": "rest",
      "subscription-group": null,
      "keep-group": true
    },
    "roundRobinEnabled": false,
    "failOverEnabled": false,
    "matchOnUriPrefix": true,
    "forwardPrefix": false,
    "registeredBy": "io.surisoft.capi.service.ConsulNodeDiscovery"
}
```
### Field Description

* ```context``` (Mandatory) The context where users will access your api. (Example: https://domain.com/capi/your-api-context).
* ```mappingList``` (1-N Array) - If you specify more than one mapping then: If fail-over and round-robin are enabled, CAPI will round-robin between all healthy endpoints. If only fail-over is enabled, then one node will be used only as backup.
* ```httpProtocol``` (Mandatory) (HTTP, HTTPS) - If you are exposing on HTTPS it is important to add your certificate to CAPI trust store. CAPI Manager exposes an API for managing your certificates.
* ```httpMethod``` (Default ALL) - If no http method is specified, CAPI will expose all standard methods for your API (GET,POST,PUT,DELETE). If you specify POST, only post calls to your API will be load balanced.
* ```matchOnUriPrefix``` (Default true), if true, you don't need to specify a definition (Swagger) for your API. CAPI will allow all paths. (Example: /your-api-context/clients /your-api-context/customer/foo/bar?action=example).
* ```stickySession``` (Default false) - If you enable sticky sessions then you also need to provide ```stickySessionParam``` and ```stickySessionParamInCookie``` (Example: ```stickySession=true```, ```stickySessionParam=X_KEY```,```stickySessionParamInCookie=true```: CAPI will look for a cookie named X_KEY, and associate the value with a random node, subsequent calls with the same cookie value will be forwarded to the same node. If that node becames unavailable CAPI returns a 503 to the client and starts all over again.)
* ```ingress``` (default false) - If one of your mapping is pointing to a Kubernetes ingress, ```ingress``` should be true. This is because Ingress Controller needs to evaluate the Host header to determine to which service to forward the request. Check the documentation here: https://kubernetes.io/docs/concepts/services-networking/ingress/#ingress-rules

### Manager API
CAPI Manager is available on http://localhost:8380/swagger-ui.html
Security to this API is disabled by default, if you need to enable security you need to provide the following properties:
```yaml
capi:
  manager:
    security:
      enabled: true
      issuer: https://localhost:8443/auth/realms/master/protocol/openid-connect/certs
```
If security (oauth2) is enabled, CAPI needs the endpoint of your identity provider Public keys.
###### Read only run time info
* Get info about the running CAPI instance: `/manager/info`
* Get all cached (running) API's `/manager/cached`
* Get statistics about the routes. `/manager/stats/routes`

###### Manage your trust store
Certificate management is disabled by default, to enable it you need to provide a valid path to a truststore. 
CAPI will not change JVM default certificate.
To enable start CAPI with the following attributes:
```yaml
capi:
  trust:
    store:
      enabled: true
      path: /your/path/cacerts 
      password: changeit
```
With the Certificate Management enabled you can:
* Get all the certificates in the trust store
* Add a certificate to the trust store providing the certificate
* Remove a certificate from the trust store.

###### Manage your Clients (Authorization) Only available if your oauth2 provider is Keycloak
Client management is disabled by default, to enable it you need to enable `oidc.provider.enabled`.
See `Authorization`
With the Client Management enabled you can:
* Get all the clients (Client on Keycloak) managed by CAPI (all clients managed by CAPI, are prefixed with `capi_` in the name)
* Add a new Client (this operation will return a consumer `key` and `secret`)
* Subscribe a Client to an API. (CAPI will create a `role` on Keycloak and associate it to your client).
* Add a client to a group (CAPI will add an existing group to your client). (See `Configure Keycloak to support subscriptions)

## Authorization (REST and Websocket)
There are 2 ways to work with Authorization on CAPI.
* You have control on your oauth2 provider, and you are able to manage your token claims, so they can be interpreted by CAPI. In this case you only need to provide CAPI with the Public Keys endpoint of your oauth2 provider using the property `oidc.provider.keys`.
```yaml
#example
oauth2:
  provider:
    enabled: true
    keys: http://localhost:8080/realms/master/protocol/openid-connect/certs
```
* Use Keycloak! In this case, CAPI provides manager endpoints (see `Manager API`) to create roles and groups directly on Keycloak. In this case you need the following properties:
```yaml
oauth2:
  provider:
    enabled: true
    keys: http://localhost:8080/realms/master/protocol/openid-connect/certs
    host: http://localhost:8080
    realm: /realms/your-realm
    clientId: <a client with realm management permissions>
    clientSecret: <the secret used by CAPI to authenticate to Keycloak>
```
### After having Authorization enabled, and if your service is protected (See `Protect your API`), CAPI will only route traffic to your service if the following conditions are met:
* The token was signed by the oauth2 provider configured: `oauth2.provider.keys`.
* The token is not expired.
* The token azp (authorized party) has a role with the same as your service.
Example: 
```
TODO: get a token to put here
```
* If the third step fails, CAPI will check if the claim `subscription` is present and if so, if any group in the subscription list matches the subscriptions-groups of your service. `see Api.subscription-groups`
Example:
```
TODO: get a token to put here
```

## Protect your API (REST and Websocket)
If you want CAPI to perform authorization before routing the traffic to your API, you will have to do the following:
* Enable Authorization (See `Authorization`)
* Declare your API as protected (`Api.secured`) (This is performed during route creation) (See `How to declare your API to CAPI`)
* When you declare your API, you can specify a list of groups (subscriptions) allowed to consume your api. (`Api.subscriptionGroup`)

## CAPI Websocket Support.
You can have CAPI acting as a Websocket Gateway.
The main features of the Websocket Support are:
* Reverse Proxy (Hide your websocket server endpoint)
* Authorization (Supports JWT Access tokens)
* Load Balancing. (Distribute the traffic to your websocket server nodes)

Websocket is disabled by default, to enable, just run CAPI with the following configuration:
```
  websocket:
    enabled: true
    server:
      host: localhost
      port: 8381      
```
With the following configuration, CAPI will be listening for Websocket requests on localhost port 8381.
#### Important information regarding Websockets.
CAPI will only look into the initial HTTP request, for authorization if needed. After the protocol update, you should manage the connection between your websocket client and your websocket server.
If your client or server drops the connection, you will need to start a new request.

### Example of a happy path using an anonymous (unprotected) web native connection request to CAPI.
```
websocket: WebSocket | undefined;
endpoint: string = "ws://localhost:8381/capi/your-websocket-server/your-version/your-path";
this.websocket = new WebSocket(this.endpoint);
this.websocket.onopen = (event: any) => {
   console.log("Connected to Your Websocket Server via CAPI");
}
```
### Example of a happy path using a protected web native connection request to CAPI. An access token needs to be sent.
```
websocket: WebSocket | undefined;
endpoint: string = "ws://localhost:8381/capi/your-websocket-server/your-version/your-path?access_token=<your JWT access token>";
this.websocket = new WebSocket(this.endpoint);
this.websocket.onopen = (event: any) => {
   console.log("Connected to Your Websocket Server via CAPI");
}
```
#### For authentication CAPI supports the standard Authorization header, or a query parameter with the key `access_token`.
*Important info* about Authorization: CAPI actively supports Keycloak as an oauth2 provider, but you should still be able to use any oauth2 compliant provider. See `Authorization` section to know how CAPI authorizes a request.

#### For CAPI to know that your API is a Websocket, please set `Service.serviceMeta.type` to `websocket`.
(See `How to declare your Service to CAPI`)



# Installing and Operating CAPI


## Run CAPI behind a reverse proxy? Enable the following:
```yaml
capi:
  reverse:
      proxy:
        enable: true
        host: https://your.host
```

## Enable Tracing (Tested with Zipkin, OpenTelemetry Collector)
```yaml
capi:
  traces:
    enabled: true
    endpoint: http://localhost:9411/api/v2/spans
```

## Running CAPI on HTTPS
```yaml
server:
  ssl:
    enabled: true
    key-store-type: PKCS12
    key-store: /your/path/capi.p12
    key-store-password: capi-lb
    key-alias: capi
```
   
### Install CAPI on Kubernetes

To create your cluster and resources, please check the documentation on your Kubernetes service.
The following charts were tested on Minikube, EKS and OpenShift.

#### CAPI Helm charts available here: [CAPI-LB Helm Charts](https://github.com/surisoft-io/capi-charts)


Install CAPI Gateway Helm Charts
```
$ helm install "capi" ./capi-lb-charts
```

Delete the helm charts
```
$ helm delete capi
$ eksctl delete cluster --name capi-demo-1
```


### Install CAPI fat jar on a VM 
#### The example below has the following dependencies:
* You need Open JDK 17
* Hashicorp Consul
* Keycloak (or a compatible oauth2 provide: See `Authorization`)
* Any traces collector (Open Telemetry Collector or Zipkin)
* A certificate for SSL configuration
* A custom trust store.

```bash
$ mkdir logs
$ git clone this repo
$ mvn clean package
$ java \
     -XX:InitialHeapSize=2g \
     -XX:MaxHeapSize=2g \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath="$PWD/logs/heap-dump.hprof" \
     -Dspring.profiles.active=prod \
     -Dcapi.consul.discovery.enabled=true \
     -Dcapi.consul.host=http://localhost:8500 \
     -Dcapi.consul.discovery.time.interval=20 \
     -Dcapi.manager.security.enabled=false \
     -Dcapi.disable.redirect=true \
     -Dcapi.trust.store.enabled=true \
     -Dcapi.trust.store.path=$CAPI_HOME/client-truststore.jks \
     -Dcapi.trust.store.password=changeit \
     -Dlog-dir=$LOGS_DIR \
     -Dserver.ssl.key-store-type=JKS \
     -Dserver.ssl.key-store=$CAPI_HOME/capi.jks \
     -Dserver.ssl.key-store-password=changeit \
     -Dserver.ssl.key-alias=capi \
     -Dcapi.traces.enabled=true \
     -Dcapi.traces.endpoint=http://localhost:9411/api/v2/spans \
     -Dserver.ssl.enabled=true \
     -Dserver.port=$CAPI_PORT \
     -Doauth2.cookieName=Authorization-Cookie-Name \
     -Doauth2.provider.enabled=true \
     -Doauth2.provider.keys=https://some-auth-server/.well-known/jwks.json \
     -jar <CAPI_JAR> > $PWD/logs/capi.log 2>&1 & echo $! > capi.pid
   ```

In the example above CAPI will be available with CAPI Manager secured and certificate management enabled.

### Install CAPI on Docker (with docker-compose)
Create an _init.sql_ file, for CAPI database to be created on start, with the following script:
```
CREATE DATABASE IF NOT EXISTS capi;
```
You will see this file mapped in the docker-compose.yml below.
```
version: "3"
services:
  capi:
    container_name: capi
    image: surisoft/capi-lb:3.0.20
    ports:
      - "8380:8380"
    environment:
      - spring.datasource.url=jdbc:mysql://capi-db:3306/capi
      - spring.datasource.username=root
      - spring.datasource.password=secret
      - capi.manager.security.enabled=false
    volumes:
      - ./logs:/capi/logs
    depends_on:
      - capi-db
    networks:
      capi-network:
  capi-db:
    container_name: capi-db
    image: mysql:latest
    ports:
      - "3306:3306"
    command: --init-file /data/application/init.sql
    volumes:
      - ./init.sql:/data/application/init.sql
    environment:
      - MYSQL_ROOT_USER=root
      - MYSQL_ROOT_PASSWORD=secret
    networks:
      capi-network:
networks:
  capi-network:
```

[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/summary/new_code?id=surisoft-io_capi-lb)