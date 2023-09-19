<img src="https://capi.surisoft.io/capi_horizontal.svg" alt="CAPI" width="30%"/>

[![CAPI-LB](https://github.com/rodrigoserracoelho/capi-lb/actions/workflows/main.yml/badge.svg)](https://github.com/rodrigoserracoelho/capi-lb/actions/workflows/main.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Docker Image Version (latest by date)](https://img.shields.io/docker/v/surisoft/capi-lb)

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
* Optional Spring Security OIDC protected CAPI Manager API.
* Distributed tracing system (Zipkin)
* Metrics (Prometheus)
* CAPI Browser user interface for route management.
* Rest API for Route management.
* Load Balancer (Round robin)
* Failover (With and without Round Robin)
* Tenant support (Headers)
* Sticky Session (Cookies and Headers)
* Certificate Manager (using the CAPI Manager API)
* Supports running with no DB, using Consul for service discovery
* Websocket Gateway (Since version 4.0.11)


## CAPI support 2 deployment strategies:
* Hashicorp Consul
* Database (MySQL / Postgres / H2)

### Enable Hashicorp Consul (Currently the Best Option, actively supported)
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


If you enable persistence, you will need to provide a database instance.
CAPI supports out of the box MySQL, PostgreSQL and H2
To enable persistence run CAPI with the following property:
```
capi.persistence.enabled=true
```

CAPI will auto discover your DB vendor based on the provided:
```
spring.datasource.url
```

Examples:
```
(mysql) jdbc:mysql://localhost:3306/capi
```
```
(postgres) jdbc:postgresql://localhost:5432/capi
```
```
(h2) jdbc:h2:mem:db;DB_CLOSE_DELAY=-1;
```

With persistence enabled you will be able to deploy new apis using:
* CAPI Manager endpoint.
If you scale up CAPI, new instances will read the deployments from the database.

If you choose not to enable persistence you will need to provide a Consul instance, CAPI will then read Consul catalog and deploy the available services. 
If you scale up CAPI, new instances will read all the deployments from Consul catalog.

*Keep in mind that you can have both Persistence and Consul strategies enabled.*
## Example of an API definition

    {
        "name": "api-name",
    	"context": "your-api-context",
    	"mappingList": [
    		{
    		    "hostname": "your.app.node1",
    		    "port": 8080,
    		    "rootContext": "your-app-context",
                        "ingress": false
    		},
    		{
    		    "hostname": "your.app.node2",
    		    "port": 8080,
    		    "rootContext": "your-app-context",
                        "ingress": true
    		}
    	],
    	"roundRobinEnabled": true,
    	"failoverEnabled": true,
    	"matchOnUriPrefix": true,
    	"httpMethod": ALL,
    	"httpProtocol": "HTTP",
    	"stickySession": true,
    	"stickySessionParam": "SESSION_ID",
    	"stickySessionParamInCookie": true,
    	"removeMe": false,
    	"connectTimeout": 0,
    	"socketTimeout": 0,
    	"swaggerEndpoint": null
    }
### Field Description

* ```context``` (Mandatory) The context where users will access your api. (Example: https://domain.com/capi/your-api-context).
* ```mappingList``` (1-N Array) - If you specify more than one mapping then: If fail-over and round-robin are enabled, CAPI will round-robin between all healthy endpoints. If only fail-over is enabled, then one node will be used only as backup.
* ```httpProtocol``` (Mandatory) (HTTP, HTTPS) - If you are exposing on HTTPS it is important to add your certificate to CAPI trust store. CAPI Manager exposes an API for managing your certificates.
* ```httpMethod``` (Default ALL) - If no http method is specified, CAPI will expose all standard methods for your API (GET,POST,PUT,DELETE). If you specify POST, only post calls to your API will be load balanced.
* ```matchOnUriPrefix``` (Default true), if true, you don't need to specify a definition (Swagger) for your API. CAPI will allow all paths. (Example: /your-api-context/clients /your-api-context/customer/foo/bar?action=example).
* ```stickySession``` (Default false) - If you enable sticky sessions then you also need to provide ```stickySessionParam``` and ```stickySessionParamInCookie``` (Example: ```stickySession=true```, ```stickySessionParam=X_KEY```,```stickySessionParamInCookie=true```: CAPI will look for a cookie named X_KEY, and associate the value with a random node, subsequent calls with the same cookie value will be forwarded to the same node. If that node becames unavailable CAPI returns a 503 to the client and starts all over again.)
* ```connectTimeout``` (default 2 minutes) - You can specify the timeout for CAPI to try to connect to your endpoint.
* ```socketTimeout``` (default 2 minutes) - You can specify the timeout for CAPI to wait for a response from your endpoint.
* ```removeMe``` (default true) - If false, CAPI will not only remove the node requesting to be removed, but the entire API. (Example: Node 1 joins the _API-X_, Node 2 joins _API-X_, with ```removeMe=false```, if Node 2 exits _API-X_, the entire _API-X_ will be deleted)


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
oidc:
  provider:
    enabled: true
    keys: http://localhost:8080/realms/master/protocol/openid-connect/certs
```
* Use Keycloak! In this case, CAPI provides manager endpoints (see `Manager API`) to create roles and groups directly on Keycloak. In this case you need the following properties:
```yaml
oidc:
  provider:
    enabled: true
    keys: http://localhost:8080/realms/master/protocol/openid-connect/certs
    host: http://localhost:8080
    realm: /realms/your-realm
    clientId: <a client with realm management permissions>
    clientSecret: <the secret used by CAPI to authenticate to Keycloak>
```
### After having Authorization enabled, and if your service is protected (See `Protect your API`), CAPI will only route traffic to your service if the following conditions are met:
* The token was signed by the oauth2 provider configured: `oidc.provider.keys`.
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

#### For CAPI to know that your API is a Websocket, please set `Api.websocket` to true.
(See `How to declare your API to CAPI`)



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
  zipkin:
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

* You need a valid MySQL running instance, with CAPI db created.
* You need Open JDK 17
```
$ mkdir logs
$ git clone this repo
$ mvn clean package
$ java \
     -XX:InitialHeapSize=2g \
     -XX:MaxHeapSize=2g \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath="$PWD/logs/heap-dump.hprof" \
     -Dspring.datasource.url=jdbc:mysql://localhost:3306/capi \
     -Dspring.datasource.username=root \
     -Dspring.datasource.password=root \
     -Dcapi.trust.store.path=/your/path/cacerts \ 
     -Dcapi.trust.store.password=changeit \
     -Dcapi.manager.security.enabled=true \ 
     -Dcapi.manager.security.issuer=https://localhost:8443/auth/realms/master/protocol/openid-connect/certs \
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

