# Kubernetes Deployment Files

This directory contains cloud-agnostic Kubernetes manifests for deploying the GenAI Text Classifier application across multiple cloud providers.

## Quick Start

### Option 1: Using kubectl with raw manifests (Simple)

```bash
# Create namespace
kubectl create namespace genai-app

# Create ConfigMap
kubectl -n genai-app apply -f configmap.yaml

# Create Secrets (update with actual values first!)
kubectl -n genai-app create secret generic genai-text-classifier-secrets \
  --from-literal=gemini-api-key='YOUR_GEMINI_KEY' \
  --from-literal=openai-api-key='YOUR_OPENAI_KEY'

# Deploy application
kubectl -n genai-app apply -f deployment.yaml

# Deploy ingress
kubectl -n genai-app apply -f ingress.yaml

# Verify deployment
kubectl -n genai-app get all
```

### Option 2: Using Helm (Recommended for multi-cloud)

```bash
# Deploy to Oracle Cloud
helm install genai-classifier ../helm/genai-classifier/ \
  -n genai-app \
  --create-namespace \
  -f ../helm/values-oracle.yaml

# Deploy to AWS
helm install genai-classifier ../helm/genai-classifier/ \
  -n genai-app \
  --create-namespace \
  -f ../helm/values-aws.yaml

# Deploy to GCP
helm install genai-classifier ../helm/genai-classifier/ \
  -n genai-app \
  --create-namespace \
  -f ../helm/values-gcp.yaml

# Deploy to Azure
helm install genai-classifier ../helm/genai-classifier/ \
  -n genai-app \
  --create-namespace \
  -f ../helm/values-azure.yaml
```

### Option 3: Using deployment script (Easiest)

```bash
# Deploy to Oracle Cloud
../scripts/deploy.sh oracle

# Deploy to AWS
../scripts/deploy.sh aws -n production

# Deploy to GCP (dry-run)
../scripts/deploy.sh gcp --dry-run

# Deploy to Azure
../scripts/deploy.sh azure -r my-release
```

## File Structure

```
k8s/
├── README.md                    # This file
├── DEPLOYMENT_GUIDE.md          # Detailed cloud-specific guides
├── deployment.yaml              # Multi-cloud deployment manifest
├── configmap.yaml               # Application configuration
├── secrets.example.yaml         # Secrets template (DO NOT COMMIT!)
├── ingress.yaml                 # Ingress configuration
└── ...
```

## Files Explained

### `deployment.yaml`
Main Kubernetes deployment manifest that includes:
- **Deployment**: 3 replicas with rolling update strategy
- **Service**: ClusterIP for internal communication
- **ServiceAccount**: RBAC configuration
- **Role/RoleBinding**: Minimal permissions for the application

Features:
- Multi-cloud compatible (generic image registry placeholder)
- Health checks (liveness & readiness probes)
- Resource requests and limits
- Security context (non-root, read-only filesystem)
- Pod anti-affinity for high availability
- Graceful termination (30s grace period)

### `configmap.yaml`
Application configuration that can be updated without redeploying:
- Spring profiles (dev, prod)
- Application name
- Logging levels (customize as needed)

### `secrets.example.yaml`
Template for API key management. **NEVER commit actual secrets!**

Options for managing secrets:
1. kubectl create secret (best for dev)
2. Cloud-native secret managers (best for prod)
   - Oracle: OCI Vault
   - AWS: Secrets Manager
   - GCP: Secret Manager
   - Azure: Key Vault

### `ingress.yaml`
Exposes the application to external traffic with:
- TLS/SSL termination
- Multiple host support
- Rate limiting
- CORS configuration
- Works with any ingress controller (nginx, ALB, GCE, etc.)

## Cloud-Specific Configuration

### Image Registry

Update the image field in `deployment.yaml` for your cloud:

```yaml
# Oracle Cloud OCIR
image: iad.ocir.io/YOUR_TENANCY/YOUR_REPOSITORY/genai-text-classifier:latest

# AWS ECR
image: YOUR_ACCOUNT.dkr.ecr.REGION.amazonaws.com/genai-text-classifier:latest

# GCP Artifact Registry
image: REGION-docker.pkg.dev/PROJECT_ID/REPO/genai-text-classifier:latest

# Azure ACR
image: YOUR_REGISTRY.azurecr.io/genai-text-classifier:latest
```

### Ingress Controller

Different clouds use different ingress controllers:

```yaml
ingressClassName:
  oracle: nginx           # OKE comes with nginx
  aws: alb                # AWS ALB Ingress Controller
  gcp: gce                # GCP Cloud Load Balancer
  azure: azure/application-gateway  # Azure Application Gateway
```

### Authentication

Each cloud requires registry authentication:

```bash
# Oracle Cloud
kubectl create secret docker-registry ocir-secret \
  --docker-server=iad.ocir.io \
  --docker-username=TENANCY/USERNAME \
  --docker-password=AUTH_TOKEN

# AWS
kubectl create secret docker-registry ecr-secret \
  --docker-server=ACCOUNT.dkr.ecr.REGION.amazonaws.com \
  --docker-username=AWS \
  --docker-password=$(aws ecr get-login-password --region REGION)

# GCP
kubectl create secret docker-registry gcp-secret \
  --docker-server=REGION-docker.pkg.dev \
  --docker-username=_json_key \
  --docker-password="$(cat ~/path/to/key.json)"

# Azure
kubectl create secret docker-registry acr-secret \
  --docker-server=REGISTRY.azurecr.io \
  --docker-username=USERNAME \
  --docker-password=PASSWORD
```

## Security Features

✅ **Already Configured:**
- Non-root user (UID 65532)
- Read-only filesystem
- No privileged access
- Dropped Linux capabilities
- Resource limits
- Health checks
- RBAC with minimal permissions
- Security context enforced
- Network-ready for NetworkPolicies

### Additional Hardening (Recommended)

1. **Network Policies**
```bash
kubectl apply -f - <<EOF
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: genai-text-classifier
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
      port: 443
EOF
```

2. **Pod Security Standards**
```bash
# Label namespace for pod security
kubectl label namespace genai-app pod-security.kubernetes.io/enforce=restricted
```

3. **Sealed Secrets (for GitOps)**
```bash
kubectl apply -f https://github.com/bitnami-labs/sealed-secrets/releases/download/v0.24.0/controller.yaml
```

## Scaling

### Manual Scaling
```bash
kubectl scale deployment genai-text-classifier -n genai-app --replicas=5
```

### Horizontal Pod Autoscaler (HPA)
```bash
kubectl autoscale deployment genai-text-classifier \
  -n genai-app \
  --min=3 \
  --max=10 \
  --cpu-percent=80
```

Or with Helm (recommended):
```yaml
autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80
```

## Monitoring

### Health Checks
The deployment includes:
- **Liveness probe**: `/actuator/health/liveness` (restarts unhealthy pods)
- **Readiness probe**: `/actuator/health/readiness` (removes from traffic)

### Metrics
Prometheus metrics available at: `/actuator/prometheus`

```bash
# Install Prometheus
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm install prometheus prometheus-community/kube-prometheus-stack -n monitoring
```

### Logs
```bash
# View logs
kubectl logs -f -n genai-app -l app=genai-text-classifier

# Stream logs from all pods
kubectl logs -f -n genai-app -l app=genai-text-classifier --all-containers=true
```

## Troubleshooting

### Deployment Issues
```bash
# Check deployment status
kubectl get deployment -n genai-app
kubectl describe deployment genai-text-classifier -n genai-app

# Check events
kubectl get events -n genai-app --sort-by='.lastTimestamp'

# Check pod logs
kubectl logs <POD_NAME> -n genai-app
kubectl logs <POD_NAME> -n genai-app --previous  # Previous crashed pod
```

### Service/Networking Issues
```bash
# Check service
kubectl get svc -n genai-app
kubectl get endpoints -n genai-app

# Test DNS resolution
kubectl run -it --rm debug --image=busybox --restart=Never -- nslookup genai-text-classifier.genai-app

# Port forward for testing
kubectl port-forward svc/genai-text-classifier 8080:80 -n genai-app
curl http://localhost:8080/actuator/health
```

### Image Pull Issues
```bash
# Check if image pull secret exists
kubectl get secrets -n genai-app

# Check pod events
kubectl describe pod <POD_NAME> -n genai-app
```

## Best Practices

1. **Use Helm** for multi-cloud deployments (easier to manage)
2. **Don't commit secrets** to version control
3. **Use namespace isolation** for different environments
4. **Enable pod autoscaling** for production workloads
5. **Setup monitoring and alerting** from day one
6. **Use resource requests/limits** to prevent node resource exhaustion
7. **Implement network policies** to restrict traffic
8. **Enable RBAC** and use service accounts with minimal permissions
9. **Use health checks** to ensure application availability
10. **Setup log aggregation** for centralized monitoring

## Advanced Topics

### GitOps with ArgoCD
```bash
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
```

### Service Mesh (Istio)
```bash
helm repo add istio https://istio-release.storage.googleapis.com/charts
helm install istio-base istio/base -n istio-system --create-namespace
```

### Backup & Disaster Recovery
```bash
# Use Velero for backups
velero backup create my-backup -n velero
```

## References

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Helm Documentation](https://helm.sh/docs/)
- [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md) - Detailed cloud-specific guides
- Oracle Cloud OKE: https://docs.oracle.com/en-us/iaas/Content/ContEng/home.htm
- AWS EKS: https://docs.aws.amazon.com/eks/
- Google Cloud GKE: https://cloud.google.com/kubernetes-engine/docs
- Azure AKS: https://learn.microsoft.com/en-us/azure/aks/

## Support

For issues or questions:
1. Check the [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)
2. Review Kubernetes official documentation
3. Check cloud provider documentation
4. Review application logs: `kubectl logs -f -n genai-app <POD_NAME>`
