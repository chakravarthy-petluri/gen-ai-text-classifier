#!/bin/bash

# Oracle Cloud kubectl Configuration Helper
# This script automates kubectl setup after you've created an OKE cluster
# Usage: ./setup-oracle-kubectl.sh <CLUSTER_ID> [REGION] [NAMESPACE]

set -e

# Colors
BLUE='\033[0;34m'
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

# Default values
CLUSTER_ID=${1:-}
REGION=${2:-us-ashburn-1}
NAMESPACE=${3:-genai-app}

print_usage() {
    echo "Usage: $0 <CLUSTER_ID> [REGION] [NAMESPACE]"
    echo ""
    echo "Arguments:"
    echo "  CLUSTER_ID    Oracle OKE cluster OCID (required)"
    echo "  REGION        Oracle Cloud region (default: us-ashburn-1)"
    echo "  NAMESPACE     Kubernetes namespace (default: genai-app)"
    echo ""
    echo "Regions:"
    echo "  us-ashburn-1    US East (Ashburn)"
    echo "  us-phoenix-1    US West (Phoenix)"
    echo "  eu-london-1     UK London"
    echo "  ap-tokyo-1      Japan Tokyo"
    echo "  ap-sydney-1     Australia Sydney"
    echo ""
    echo "Example:"
    echo "  ./setup-oracle-kubectl.sh ocid1.cluster.oc1.iad.xxxxx us-ashburn-1 genai-app"
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

# Validate inputs
if [[ -z "$CLUSTER_ID" ]]; then
    log_error "Cluster ID is required"
    print_usage
fi

# Check prerequisites
log_info "Checking prerequisites..."

if ! command -v oci &> /dev/null; then
    log_error "OCI CLI is not installed. Please install it first:"
    echo "  brew install oci-cli  (macOS)"
    echo "  pip3 install oci-cli  (Linux)"
fi
log_info "✓ OCI CLI found"

if ! command -v kubectl &> /dev/null; then
    log_error "kubectl is not installed. Please install it first:"
    echo "  brew install kubectl  (macOS)"
    echo "  https://kubernetes.io/docs/tasks/tools/  (other OS)"
fi
log_info "✓ kubectl found"

# Verify cluster exists
log_info "Verifying cluster..."
if ! oci ce cluster get --cluster-id "$CLUSTER_ID" --region "$REGION" &> /dev/null; then
    log_error "Cannot find cluster with ID: $CLUSTER_ID in region: $REGION"
    echo "Please verify your Cluster ID and Region"
fi
log_info "✓ Cluster found"

# Create .kube directory
log_info "Setting up kubectl configuration..."
mkdir -p ~/.kube

# Download kubeconfig
KUBECONFIG_FILE="$HOME/.kube/config"

if [[ -f "$KUBECONFIG_FILE" ]]; then
    log_warn "Existing kubeconfig found. Creating backup..."
    cp "$KUBECONFIG_FILE" "$KUBECONFIG_FILE.backup.$(date +%s)"
fi

log_info "Downloading kubeconfig from Oracle Cloud..."
oci ce cluster create-kubeconfig \
    --cluster-id "$CLUSTER_ID" \
    --file "$KUBECONFIG_FILE" \
    --region "$REGION"

# Set proper permissions
chmod 600 "$KUBECONFIG_FILE"
log_success "kubeconfig saved to $KUBECONFIG_FILE"

# Verify connection
log_info "Verifying kubectl connection..."
if ! kubectl cluster-info &> /dev/null; then
    log_error "Cannot connect to cluster. Check your kubeconfig."
fi
log_success "Successfully connected to Kubernetes cluster"

# Get cluster info
log_info "Cluster Information:"
kubectl cluster-info | grep -E "Kubernetes|server"

# Get nodes
log_info "Worker Nodes:"
kubectl get nodes -o wide

# Create namespace if it doesn't exist
log_info "Setting up namespace '$NAMESPACE'..."
if kubectl get namespace "$NAMESPACE" &> /dev/null; then
    log_warn "Namespace '$NAMESPACE' already exists"
else
    kubectl create namespace "$NAMESPACE"
    log_success "Namespace '$NAMESPACE' created"
fi

# Summary
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  kubectl Setup Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Next steps:"
echo ""
echo "1. Build and push your Docker image:"
echo "   docker build -t genai-text-classifier:v1.0.0 gen-ai-text-classifier/"
echo "   docker push <REGION>.ocir.io/<TENANCY>/genai-text-classifier:v1.0.0"
echo ""
echo "2. Update Helm values:"
echo "   nano helm/values-oracle.yaml"
echo "   # Update 'registry' field with your OCIR path"
echo ""
echo "3. Deploy your application:"
echo "   ./scripts/deploy.sh oracle"
echo ""
echo "4. Verify deployment:"
echo "   kubectl -n $NAMESPACE get pods"
echo "   kubectl -n $NAMESPACE logs -f deployment/genai-text-classifier"
echo ""
echo "Useful commands:"
echo "  kubectl get all -n $NAMESPACE          # View all resources"
echo "  kubectl describe pod <POD_NAME> -n $NAMESPACE  # Debug pod"
echo "  kubectl port-forward svc/genai-text-classifier 8080:80 -n $NAMESPACE  # Local testing"
echo ""

log_success "Ready to deploy!"