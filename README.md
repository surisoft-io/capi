[![CAPI-LB](https://github.com/rodrigoserracoelho/capi-lb/actions/workflows/main.yml/badge.svg)](https://github.com/rodrigoserracoelho/capi-lb/actions/workflows/main.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Docker Image Version (latest by date)](https://img.shields.io/docker/v/surisoft/capi-lb)

# CAPI-LB

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