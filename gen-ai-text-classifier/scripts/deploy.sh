#!/bin/bash

# Oracle Cloud Kubernetes Deployment Script for GenAI Text Classifier
# Usage: ./deploy.sh [options]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default values
CLOUD_PROVIDER="oracle"
NAMESPACE=${NAMESPACE:-genai-app}
RELEASE_NAME=${RELEASE_NAME:-genai-classifier}
HELM_CHART_PATH="helm/genai-classifier"
VALUES_FILE="helm/values-oracle.yaml"
DRY_RUN=${DRY_RUN:-false}

# Functions
print_usage() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Deploys GenAI Text Classifier to Oracle Cloud OKE"
    echo ""
    echo "Options:"
    echo "  -n, --namespace NAMESPACE      Kubernetes namespace (default: genai-app)"
    echo "  -r, --release-name NAME        Helm release name (default: genai-classifier)"
    echo "  --dry-run                      Show what would be deployed without actually deploying"
    echo "  --help                         Show this help message"
    echo ""
    echo "Examples:"
    echo "  ./deploy.sh                              # Deploy to Oracle Cloud OKE"
    echo "  ./deploy.sh -n production                # Deploy to production namespace"
    echo "  ./deploy.sh --dry-run                    # Preview deployment"
}

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

check_prerequisites() {
    log_info "Checking prerequisites..."

    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl is not installed. Please install kubectl first."
    fi
    log_info "✓ kubectl found"

    # Check helm
    if ! command -v helm &> /dev/null; then
        log_error "Helm is not installed. Please install Helm 3+ first."
    fi
    log_info "✓ helm found"

    # Check cluster connection
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster. Please configure kubectl."
    fi
    log_info "✓ Connected to Kubernetes cluster"

    # Check OCI CLI
    if ! command -v oci &> /dev/null; then
        log_warn "OCI CLI not found (optional, but recommended)"
    else
        log_info "✓ OCI CLI found"
    fi
}

verify_values_file() {
    log_info "Using Oracle Cloud (OKE) configuration"
    if [[ ! -f "$VALUES_FILE" ]]; then
        log_error "Values file not found: $VALUES_FILE"
    fi
}

create_namespace() {
    if kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log_info "Namespace '$NAMESPACE' already exists"
    else
        log_info "Creating namespace '$NAMESPACE'..."
        kubectl create namespace "$NAMESPACE"
        log_success "Namespace '$NAMESPACE' created"
    fi
}

deploy_with_helm() {
    log_info "Deploying application with Helm..."

    HELM_CMD="helm upgrade --install $RELEASE_NAME $HELM_CHART_PATH \
        -n $NAMESPACE \
        -f $VALUES_FILE"

    if [[ "$DRY_RUN" == "true" ]]; then
        log_warn "DRY RUN MODE - No actual changes will be made"
        $HELM_CMD --dry-run --debug
    else
        $HELM_CMD
        log_success "Helm deployment completed"
    fi
}

verify_deployment() {
    if [[ "$DRY_RUN" == "true" ]]; then
        log_info "Skipping verification in dry-run mode"
        return
    fi

    log_info "Verifying deployment..."

    # Wait for deployment
    log_info "Waiting for deployment to be ready (timeout: 5 minutes)..."
    if kubectl rollout status deployment/$RELEASE_NAME -n $NAMESPACE --timeout=5m; then
        log_success "Deployment is ready"
    else
        log_warn "Deployment did not reach ready state within timeout"
    fi

    # Show deployment info
    log_info "Deployment status:"
    kubectl get deployment -n $NAMESPACE -l app.kubernetes.io/name=genai-text-classifier
    kubectl get pods -n $NAMESPACE -l app.kubernetes.io/name=genai-text-classifier
    kubectl get svc -n $NAMESPACE

    # Get ingress info
    if kubectl get ingress -n $NAMESPACE &> /dev/null; then
        log_info "Ingress status:"
        kubectl get ingress -n $NAMESPACE
    fi
}

print_next_steps() {
    echo ""
    echo -e "${BLUE}===================================${NC}"
    echo -e "${BLUE}   Deployment Complete!${NC}"
    echo -e "${BLUE}===================================${NC}"
    echo ""
    echo "Next steps:"
    echo ""
    echo "1. Check pod logs:"
    echo "   kubectl logs -f -n $NAMESPACE -l app.kubernetes.io/name=genai-text-classifier"
    echo ""
    echo "2. Port forward for testing:"
    echo "   kubectl port-forward -n $NAMESPACE svc/$RELEASE_NAME 8080:80"
    echo ""
    echo "3. Test the API:"
    echo "   curl http://localhost:8080/api/classifiers/classify"
    echo ""
    echo "4. View deployment:"
    echo "   kubectl get all -n $NAMESPACE"
    echo ""
    echo "For more information, see ../k8s/DEPLOYMENT_GUIDE.md"
}

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -n|--namespace)
            NAMESPACE="$2"
            shift 2
            ;;
        -r|--release-name)
            RELEASE_NAME="$2"
            shift 2
            ;;
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        --help)
            print_usage
            exit 0
            ;;
        *)
            log_error "Unknown option: $1"
            print_usage
            exit 1
            ;;
    esac
done

# Main execution
log_info "Starting deployment to Oracle Cloud OKE..."
echo ""

check_prerequisites
verify_values_file
create_namespace
deploy_with_helm
verify_deployment
print_next_steps

log_success "All done!"