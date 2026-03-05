# Multi-Cloud Kubernetes Deployment Guide

This guide covers deploying the GenAI Text Classifier application to various Kubernetes platforms: Oracle Cloud, AWS, GCP, Azure, and on-premises.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Container Registry Setup](#container-registry-setup)
3. [Oracle Cloud (OKE)](#oracle-cloud-oke)
4. [AWS (EKS)](#aws-eks)
5. [Google Cloud (GKE)](#google-cloud-gke)
6. [Azure (AKS)](#azure-aks)
7. [Using Helm Charts](#using-helm-charts)
8. [Security Considerations](#security-considerations)

---

## Prerequisites

- **Kubernetes cluster** (1.20+) with `kubectl` configured
- **Helm 3+** (optional but recommended)
- **Docker** (for building and pushing images)
- **Cloud CLI tools** (cloud-specific)

### Install Required Tools

```bash
# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/darwin/amd64/kubectl"
chmod +x kubectl && sudo mv kubectl /usr/local/bin/

# Helm
curl https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3 | bash

# Cloud CLIs (choose as needed)
# Oracle Cloud
brew install oracle-cloud-infrastructure-cli

# AWS
pip install awscli-v2

# Google Cloud
curl https://sdk.cloud.google.com | bash

# Azure
brew install azure-cli
```

---

## Container Registry Setup

### Build and Push Docker Image

First, build your Docker image locally:

```bash
cd /path/to/GenAITextClassifierBackend/gen-ai-text-classifier
docker build -t genai-text-classifier:v1.0.0 .
```

### Oracle Cloud Container Registry (OCIR)

```bash
# Login to OCIR
docker login -u <TENANCY>/<USERNAME> <REGION>.ocir.io

# Tag image
docker tag genai-text-classifier:v1.0.0 <REGION>.ocir.io/<TENANCY>/<REPOSITORY>/genai-text-classifier:v1.0.0

# Push image
docker push <REGION>.ocir.io/<TENANCY>/<REPOSITORY>/genai-text-classifier:v1.0.0

# Create image pull secret
kubectl create secret docker-registry ocir-secret \
  --docker-server=<REGION>.ocir.io \
  --docker-username=<TENANCY>/<USERNAME> \
  --docker-password=<AUTH_TOKEN> \
  --docker-email=your.email@example.com
```

### AWS ECR (Elastic Container Registry)

```bash
# Login to ECR
aws ecr get-login-password --region <REGION> | docker login --username AWS --password-stdin <ACCOUNT>.dkr.ecr.<REGION>.amazonaws.com

# Create ECR repository
aws ecr create-repository --repository-name genai-text-classifier --region <REGION>

# Tag image
docker tag genai-text-classifier:v1.0.0 <ACCOUNT>.dkr.ecr.<REGION>.amazonaws.com/genai-text-classifier:v1.0.0

# Push image
docker push <ACCOUNT>.dkr.ecr.<REGION>.amazonaws.com/genai-text-classifier:v1.0.0

# Create image pull secret
kubectl create secret docker-registry ecr-secret \
  --docker-server=<ACCOUNT>.dkr.ecr.<REGION>.amazonaws.com \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-login-password --region <REGION>) \
  --docker-email=your.email@example.com
```

### GCP Artifact Registry

```bash
# Configure docker authentication
gcloud auth configure-docker <REGION>-docker.pkg.dev

# Tag image
docker tag genai-text-classifier:v1.0.0 <REGION>-docker.pkg.dev/<PROJECT_ID>/genai-repo/genai-text-classifier:v1.0.0

# Push image
docker push <REGION>-docker.pkg.dev/<PROJECT_ID>/genai-repo/genai-text-classifier:v1.0.0

# Create image pull secret
kubectl create secret docker-registry gcp-secret \
  --docker-server=<REGION>-docker.pkg.dev \
  --docker-username=_json_key \
  --docker-password="$(cat ~/path/to/service-account-key.json)" \
  --docker-email=your.email@example.com
```

### Azure Container Registry (ACR)

```bash
# Login to ACR
az acr login --name <REGISTRY_NAME>

# Tag image
docker tag genai-text-classifier:v1.0.0 <REGISTRY_NAME>.azurecr.io/genai-text-classifier:v1.0.0

# Push image
docker push <REGISTRY_NAME>.azurecr.io/genai-text-classifier:v1.0.0

# Create image pull secret
kubectl create secret docker-registry acr-secret \
  --docker-server=<REGISTRY_NAME>.azurecr.io \
  --docker-username=<USERNAME> \
  --docker-password=<PASSWORD> \
  --docker-email=your.email@example.com
```

---

## Oracle Cloud (OKE)

### 1. Create OKE Cluster

```bash
# Create cluster via OCI Console or CLI
oci ce cluster create \
  --name genai-cluster \
  --kubernetes-version v1.28.0 \
  --vcn-id <VCN_ID>
```

### 2. Configure kubectl

```bash
# Download kubeconfig
oci ce cluster create-kubeconfig --cluster-id <CLUSTER_ID> --file $HOME/.kube/config

# Verify connection
kubectl cluster-info
```

### 3. Create Namespace and Secrets

```bash
kubectl create namespace genai-app
kubectl -n genai-app create secret docker-registry ocir-secret ...

# Create API key secrets
kubectl -n genai-app create secret generic genai-text-classifier-secrets \
  --from-literal=gemini-api-key='YOUR_GEMINI_KEY' \
  --from-literal=openai-api-key='YOUR_OPENAI_KEY'
```

### 4. Update Deployment YAML

Edit `k8s/deployment.yaml` to use your OCIR registry:

```yaml
image: iad.ocir.io/<TENANCY>/<REPOSITORY>/genai-text-classifier:latest
imagePullSecrets:
- name: ocir-secret
```

### 5. Deploy Application

```bash
# Create namespace
kubectl create namespace genai-app

# Apply manifests
kubectl -n genai-app apply -f k8s/configmap.yaml
kubectl -n genai-app apply -f k8s/secrets.example.yaml  # Update with actual values first
kubectl -n genai-app apply -f k8s/deployment.yaml
kubectl -n genai-app apply -f k8s/ingress.yaml

# Verify deployment
kubectl -n genai-app get pods
kubectl -n genai-app get svc
```

### 6. Setup Ingress (OCI Load Balancer)

OKE comes with nginx-ingress by default. Ingress will automatically provision an OCI Load Balancer.

```bash
# Check ingress status
kubectl -n genai-app get ingress
kubectl -n genai-app describe ingress genai-text-classifier
```

---

## AWS (EKS)

### 1. Create EKS Cluster

```bash
# Using eksctl (recommended)
eksctl create cluster --name genai-cluster --region us-east-1 --nodegroup-name standard-nodes

# Or using AWS CLI
aws eks create-cluster \
  --name genai-cluster \
  --version 1.28 \
  --roleArn arn:aws:iam::ACCOUNT_ID:role/eks-service-role
```

### 2. Configure kubectl

```bash
aws eks update-kubeconfig --name genai-cluster --region us-east-1
kubectl cluster-info
```

### 3. Create Namespace and Secrets

```bash
kubectl create namespace genai-app
kubectl -n genai-app create secret docker-registry ecr-secret ...
kubectl -n genai-app create secret generic genai-text-classifier-secrets ...
```

### 4. Install AWS ALB Ingress Controller (Optional)

```bash
# Install controller
helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system \
  --set clusterName=genai-cluster \
  --set serviceAccount.create=true
```

### 5. Update Deployment for AWS

```yaml
image: <ACCOUNT>.dkr.ecr.us-east-1.amazonaws.com/genai-text-classifier:latest
imagePullSecrets:
- name: ecr-secret
```

### 6. Deploy Application

```bash
kubectl create namespace genai-app
kubectl -n genai-app apply -f k8s/
```

---

## Google Cloud (GKE)

### 1. Create GKE Cluster

```bash
gcloud container clusters create genai-cluster \
  --zone us-central1-a \
  --num-nodes 3 \
  --machine-type n1-standard-2 \
  --enable-stackdriver-kubernetes
```

### 2. Configure kubectl

```bash
gcloud container clusters get-credentials genai-cluster --zone us-central1-a
kubectl cluster-info
```

### 3. Create Namespace and Secrets

```bash
kubectl create namespace genai-app
kubectl -n genai-app create secret docker-registry gcp-secret ...
kubectl -n genai-app create secret generic genai-text-classifier-secrets ...
```

### 4. Update Deployment for GCP

```yaml
image: us-central1-docker.pkg.dev/PROJECT_ID/genai-repo/genai-text-classifier:latest
imagePullSecrets:
- name: gcp-secret
```

### 5. Deploy Application

```bash
kubectl create namespace genai-app
kubectl -n genai-app apply -f k8s/
```

### 6. Setup Cloud Load Balancer

GKE Ingress automatically provisions a Google Cloud Load Balancer.

---

## Azure (AKS)

### 1. Create AKS Cluster

```bash
az aks create \
  --resource-group myResourceGroup \
  --name genai-cluster \
  --node-count 3 \
  --vm-set-type VirtualMachineScaleSets \
  --load-balancer-sku standard
```

### 2. Configure kubectl

```bash
az aks get-credentials --resource-group myResourceGroup --name genai-cluster
kubectl cluster-info
```

### 3. Create Namespace and Secrets

```bash
kubectl create namespace genai-app
kubectl -n genai-app create secret docker-registry acr-secret ...
kubectl -n genai-app create secret generic genai-text-classifier-secrets ...
```

### 4. Update Deployment for Azure

```yaml
image: <REGISTRY_NAME>.azurecr.io/genai-text-classifier:latest
imagePullSecrets:
- name: acr-secret
```

### 5. Deploy Application

```bash
kubectl create namespace genai-app
kubectl -n genai-app apply -f k8s/
```

---

## Using Helm Charts

Helm provides a templated, reusable way to deploy across clouds.

### 1. Create Helm Values File for Your Cloud

Create `helm/values-oracle.yaml`:

```yaml
image:
  registry: iad.ocir.io/<TENANCY>/<REPOSITORY>
  repository: genai-text-classifier
  tag: v1.0.0

ingress:
  className: nginx
  hosts:
    - host: genai-classifier.oracle.example.com
      paths:
        - path: /api
          pathType: Prefix

secrets:
  create: true
  geminiApiKey: "YOUR_KEY"
  openaiApiKey: "YOUR_KEY"
```

### 2. Deploy with Helm

```bash
# Add Helm repo (if using community charts)
helm repo add myrepo https://charts.example.com
helm repo update

# Install release
helm install genai-classifier helm/genai-classifier/ \
  -n genai-app \
  --create-namespace \
  -f helm/values-oracle.yaml

# Or upgrade existing
helm upgrade genai-classifier helm/genai-classifier/ \
  -n genai-app \
  -f helm/values-oracle.yaml
```

### 3. Switch Clouds

Simply change the values file:

```bash
# Deploy to AWS
helm upgrade genai-classifier helm/genai-classifier/ \
  -n genai-app \
  -f helm/values-aws.yaml

# Deploy to GCP
helm upgrade genai-classifier helm/genai-classifier/ \
  -n genai-app \
  -f helm/values-gcp.yaml
```

---

## Security Considerations

### 1. API Key Management (DO NOT COMMIT KEYS!)

**Option 1: Cloud Native Secrets Management**

```bash
# Oracle Cloud Vault
oci vault secret create --secret-name genai-gemini-key --secret-content "KEY"

# AWS Secrets Manager
aws secretsmanager create-secret --name genai-secrets --secret-string '{"key":"value"}'

# GCP Secret Manager
echo -n "YOUR_KEY" | gcloud secrets create gemini-api-key --data-file=-

# Azure Key Vault
az keyvault secret set --vault-name MyVault --name gemini-api-key --value "YOUR_KEY"
```

**Option 2: Sealed Secrets (Kubernetes)**

```bash
# Install sealed-secrets controller
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml

# Create and seal secret
echo -n "YOUR_KEY" | kubectl create secret generic genai-secrets --dry-run=client --from-file=gemini-api-key=/dev/stdin -o yaml | kubeseal -f -
```

### 2. Network Policies

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: genai-text-classifier-deny-all
spec:
  podSelector:
    matchLabels:
      app: genai-text-classifier
  policyTypes:
  - Ingress
  - Egress
  ingress:
  - from:
    - namespaceSelector:
        matchLabels:
          name: genai-app
  egress:
  - to:
    - namespaceSelector: {}
    ports:
    - protocol: TCP
      port: 443  # HTTPS for external APIs
```

### 3. Pod Security Policies

The deployment YAML already includes:
- Non-root user (65532)
- Read-only filesystem
- No privileged access
- Dropped capabilities
- Resource limits

### 4. Enable RBAC

Service account and RBAC bindings are already included in deployment.yaml.

---

## Monitoring & Logging

### Prometheus Metrics

The deployment exposes Prometheus metrics at `/actuator/prometheus` on port 8080.

```bash
# Install Prometheus
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack -n monitoring
```

### Centralized Logging

```bash
# ELK Stack (Elasticsearch, Logstash, Kibana)
helm repo add elastic https://helm.elastic.co
helm install elasticsearch elastic/elasticsearch -n logging

# Or cloud-native solutions:
# - Oracle: OCI Logging
# - AWS: CloudWatch Logs
# - GCP: Cloud Logging
# - Azure: Application Insights
```

---

## Troubleshooting

```bash
# Check deployment status
kubectl -n genai-app get deployment
kubectl -n genai-app describe deployment genai-text-classifier

# Check pods
kubectl -n genai-app get pods
kubectl -n genai-app logs -f <POD_NAME>

# Check service
kubectl -n genai-app get svc
kubectl -n genai-app endpoints genai-text-classifier

# Port forward for testing
kubectl -n genai-app port-forward svc/genai-text-classifier 8080:80

# Test API
curl http://localhost:8080/api/classifiers/classify \
  -H "Content-Type: application/json" \
  -d '{
    "genAIType": "gemini",
    "genAIModel": "gemini-1.5-flash",
    "genAIAPIKey": "YOUR_KEY",
    "textToClassifyList": ["Hello World"],
    "attributeList": ["sentiment"]
  }'
```

---

## Production Checklist

- [ ] Configure production container registry
- [ ] Set up image scanning and vulnerability scanning
- [ ] Configure RBAC and network policies
- [ ] Enable pod autoscaling (HPA)
- [ ] Set up monitoring and alerting
- [ ] Configure centralized logging
- [ ] Enable ingress TLS/SSL
- [ ] Setup backup and disaster recovery
- [ ] Document runbooks for common issues
- [ ] Test failover scenarios
- [ ] Configure rate limiting and quota management
- [ ] Setup secrets rotation policy

---

## Additional Resources

- [Kubernetes Official Docs](https://kubernetes.io/docs/)
- [Helm Documentation](https://helm.sh/docs/)
- [Oracle Cloud OKE](https://docs.oracle.com/en-us/iaas/Content/ContEng/home.htm)
- [AWS EKS](https://docs.aws.amazon.com/eks/)
- [Google Cloud GKE](https://cloud.google.com/kubernetes-engine/docs)
- [Azure AKS](https://learn.microsoft.com/en-us/azure/aks/)
