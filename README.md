[![CAPI-LB](https://github.com/rodrigoserracoelho/capi-lb/actions/workflows/main.yml/badge.svg)](https://github.com/rodrigoserracoelho/capi-lb/actions/workflows/main.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Docker Image Version (latest by date)](https://img.shields.io/docker/v/surisoft/capi-lb)


# CAPI Load Balancer
## _Light Apache Camel Load Balancer_

## Supports:
* Light API Gateway / Load Balancer powered by Apache Camel dynamics routes.
* Deployments supported: Spring Boot Microservices auto discovery, CAPI Manager Endpoints and Hashicorp Consul
* Optional Spring Security OIDC protected CAPI Manager API.
* Distributed tracing system (Zipkin)
* Metrics (Prometheus)
* Hawtio (JVM management console)
* Load Balancer (Round robin)
* Failover (With and without Round Robin)
* Stick Session (Cookies and Headers)
* Certificate Manager (using the CAPI Manager API)

## Example of an API definition

    {
        "name": "api-name",
    	"context": "your-api-context",
    	"mappingList": [
    		{
    		    "hostname": "your.app.node1",
    		    "port": 8080,
    		    "rootContext": "your-app-context" 
    		},
    		{
    		    "hostname": "your.app.node2",
    		    "port": 8080,
    		    "rootContext": "your-app-context" 
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

* ```context``` (Mandatory) The context wehre users will access your api. (Example: https://domain.com/capi/your-api-context).
* ```mappingList``` (1-N Array) - If you specify more than one mapping then: If failover and round robin are enabled, CAPI will round robin between all healthy endpoints. If only failover is enabled, then one node will be used only as backup.
* ```httpProtocol``` (Mandatory) (HTTP, HTTPS) - If you are exposing on HTTPS it is important to add your certificate to CAPI trust store. CAPI Manager exposes an API for managing your certificates.
* ```httpMethod``` (Default ALL) - If no http method is specified, CAPI will expose all standard methods for your API (GET,POST,PUT,DELETE). If you specify POST, only post calls to your API will be load balanced.
* ```matchOnUriPrefix``` (Default true), if true, you don't need to specify a definition (Swagger) for your API. CAPI will allow all paths. (Example: /your-api-context/clients /your-api-context/customer/foo/bar?action=example).
* ```stickySession``` (Default false) - If you enable sticky sessions then you also need to provide ```stickySessionParam``` and ```stickySessionParamInCookie``` (Example: ```stickySession=true```, ```stickySessionParam=X_KEY```,```stickySessionParamInCookie=true```: CAPI will look for a cookie named X_KEY, and associate the value with a random node, subsequent calls with the same cookie value will be forwarded to the same node. If that node becames unavailable CAPI returns a 503 to the client and starts all over again.)
* ```connectTimeout``` (default 2 minutes) - You can specify the timeout for CAPI to try to connect to your endpoint.
* ```socketTimeout``` (default 2 minutes) - You can specify the timeout for CAPI to wait for a response from your endpoint.
* ```removeMe``` (default true) - If false, CAPI will not only remove the node requesting to be removed, but the entire API. (Example: Node 1 joins the _API-X_, Node 2 joins _API-X_, with ```removeMe=false```, if Node 2 exits _API-X_, the entire _API-X_ will be deleted)

### CAPI Manager API
CAPI Manager is available on http://localhost:8380/swagger-ui/
Security to this API is disabled by default, if you need to enable security you need to provide the following properties at run time:
```-Dcapi.manager.security.enabled=true ```
```-Dcapi.manager.security.issuer=https://localhost:8443/auth/realms/master/protocol/openid-connect/certs```
If security (OpenID Connect/oauth2) is enabled CAPI needs the endpoint of your identity provider JWK.
###### With the API you can:
* Get all configured API's
* Get all running API's
* Add/Remove a node to an API
* Get all the certificates in the trust store
* Add a certificate to the trust store providing the certificate
* Add a certificate to the trust store providing the HTTPS endpoint.
* Remove a certificate from the trust store.

## AWS EKS Installation
Create the cluster
```
$ eksctl create cluster -f capi-eks-cluster.yaml 
```
Grab a cup of coffee, it will take a while...
```
$ eksctl utils associate-iam-oidc-provider --region eu-west-1 --cluster capi-demo-1 --approve
```
Apply IAM Policies for Ingress Controller
```
$ kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/aws-alb-ingress-controller/v1.1.8/docs/examples/rbac-role.yaml
```
Apply ALB Ingress Controller
```
$ eksctl create iamserviceaccount \
    --region eu-west-1 \
    --name alb-ingress-controller \
    --namespace kube-system \
    --cluster capi-demo-1 \
    --attach-policy-arn arn:aws:iam::610447901435:policy/ALBIngressControllerIAMPolicy \
    --override-existing-serviceaccounts \
    --approve

$ kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/aws-alb-ingress-controller/v1.1.8/docs/examples/alb-ingress-controller.yaml
```
Install Capi Load Balancer Helm Charts
```
$ helm install "capi" ./capi-lb-charts
```

## Delete
```
$ helm delete capi
$ eksctl delete cluster --name capi-demo-1
```

 