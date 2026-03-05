# Oracle Cloud OKE Setup Guide - Beginner Edition

This guide walks you through setting up Oracle Cloud and deploying your GenAI Text Classifier step-by-step.

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Step 1: Create Oracle Cloud Account](#step-1-create-oracle-cloud-account)
3. [Step 2: Install Oracle Cloud CLI (OCI)](#step-2-install-oracle-cloud-cli-oci)
4. [Step 3: Configure OCI CLI](#step-3-configure-oci-cli)
5. [Step 4: Create OKE Cluster](#step-4-create-oke-cluster)
6. [Step 5: Configure kubectl](#step-5-configure-kubectl)
7. [Step 6: Verify Connection](#step-6-verify-connection)
8. [Step 7: Deploy Your Application](#step-7-deploy-your-application)

---

## Prerequisites

Before starting, make sure you have:
- A Mac/Linux/Windows machine with internet access
- About 20-30 minutes for the complete setup
- A credit card (Oracle offers $300 free credits for new accounts)

---

## Step 1: Create Oracle Cloud Account

### 1.1 Sign Up
1. Go to https://www.oracle.com/cloud/free/
2. Click **"Start for free"** button
3. Enter your email address
4. Click **"Verify your email"**
5. Check your email for verification link
6. Complete the registration form with:
   - Full name
   - Company name
   - Country
   - Phone number
7. Click **"Continue"**

### 1.2 Provide Payment Information
1. Enter your credit card details (won't be charged for free tier)
2. Verify your phone number
3. Complete the setup wizard

### 1.3 Access Oracle Cloud Console
1. Sign in to https://cloud.oracle.com
2. You should see the **Oracle Cloud Dashboard**
3. Note your **Tenancy Name** (displayed in top right) - you'll need this later

**✅ You now have an Oracle Cloud account!**

---

## Step 2: Install Oracle Cloud CLI (OCI)

The OCI CLI allows you to interact with Oracle Cloud from your terminal.

### On macOS

```bash
# Install Homebrew (if not already installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install OCI CLI
brew install oci-cli

# Verify installation
oci --version
```

### On Linux

```bash
# Install Python 3.7+ (if not already installed)
sudo apt-get update
sudo apt-get install python3 python3-pip

# Install OCI CLI
pip3 install oci-cli

# Add to PATH
export PATH=$PATH:/home/$USER/.local/bin
echo 'export PATH=$PATH:/home/$USER/.local/bin' >> ~/.bashrc
source ~/.bashrc

# Verify installation
oci --version
```

### On Windows

```bash
# Install Python 3.7+ from https://www.python.org/
# Open PowerShell and run:
pip install oci-cli

# Verify installation
oci --version
```

**✅ OCI CLI is installed!**

---

## Step 3: Configure OCI CLI

### 3.1 Generate API Signing Key

```bash
# Create .oci directory
mkdir -p ~/.oci

# Generate private key
openssl genrsa -out ~/.oci/oci_api_key.pem 2048

# Set proper permissions
chmod 600 ~/.oci/oci_api_key.pem

# Generate public key
openssl rsa -pubout -in ~/.oci/oci_api_key.pem -out ~/.oci/oci_api_key_public.pem

# Display public key (you'll need to copy this)
cat ~/.oci/oci_api_key_public.pem
```

### 3.2 Add Public Key to Oracle Cloud Console

1. Log in to https://cloud.oracle.com
2. Click your **profile icon** (top right)
3. Click **"User Settings"**
4. Scroll down to **"API Keys"**
5. Click **"Add API Key"**
6. Click **"Paste Public Key"**
7. Paste the public key output from Step 3.1
8. Click **"Add"**

### 3.3 Configure OCI CLI

```bash
# Run the interactive configuration
oci setup config

# When prompted, answer as follows:
# Location of config file: [/Users/YOUR_USER/.oci/config] - Press Enter (default)
# User OCID: (You'll get this from Oracle Cloud console, see below)
# Tenancy OCID: (You'll get this from Oracle Cloud console, see below)
# Region: (Choose your region, e.g., us-ashburn-1, us-phoenix-1)
# Do you want to generate a new API Signing RSA key pair? (y/N): n
# Path to your private key: ~/.oci/oci_api_key.pem
```

### 3.4 Get Your User and Tenancy OCIDs

1. Log in to https://cloud.oracle.com
2. For **User OCID**:
   - Click your profile icon (top right)
   - Click **"User Settings"**
   - Copy the **OCID** value
3. For **Tenancy OCID**:
   - Go to **"Administration"** → **"Tenancy Details"**
   - Copy the **OCID** value

### 3.5 Update OCI Config File

```bash
# Edit your config file
nano ~/.oci/config

# It should look like this:
# [DEFAULT]
# user=ocid1.user.oc1..aaaaaaaa...
# fingerprint=aa:bb:cc:dd:ee:ff:...
# tenancy=ocid1.tenancy.oc1..aaaaaaaa...
# region=us-ashburn-1
# key_file=~/.oci/oci_api_key.pem
```

### 3.6 Verify Configuration

```bash
# Test OCI CLI connection
oci iam user list

# If successful, you'll see your user information
```

**✅ OCI CLI is configured!**

---

## Step 4: Create OKE Cluster

You can create a cluster via Oracle Cloud Console (easiest for beginners) or OCI CLI.

### Option A: Using Oracle Cloud Console (Recommended for Beginners)

#### Step 1: Navigate to Kubernetes Clusters

1. Log in to https://cloud.oracle.com
2. Click **"Menu"** (top left)
3. Go to **"Developer Services"** → **"Kubernetes Clusters (OKE)"**
4. Click **"Create Cluster"**

#### Step 2: Configure Cluster

Fill in the form:

**Cluster Information**
- Name: `genai-classifier-cluster`
- Kubernetes Version: `v1.28` or latest
- Visibility: `Public` (for simplicity)

**Networking**
- VCN: Click **"Create New VCN"** (for beginners)
  - VCN Name: `genai-vcn`
  - Click **"Create"**
- Kubernetes Service LB Subnet: Auto-generated
- Pods CIDR Block: `10.244.0.0/16` (default)
- Services CIDR Block: `10.96.0.0/16` (default)

**Node Pool**
- Click **"Add Node Pool"**
- Name: `worker-pool`
- Number of Nodes: `3`
- Image: Select **"Oracle Linux 8"**
- Shape: Select **"VM.Standard.A1.Flex"** (Always Free eligible)
  - OCPUs: `2`
  - RAM: `12` GB
- Click **"Create"**

#### Step 3: Review and Create

1. Review all settings
2. Click **"Create Cluster"**
3. Wait for cluster creation (5-10 minutes)
4. You'll see a green checkmark when complete

#### Step 4: Get Your Cluster ID

1. Go to **"Kubernetes Clusters (OKE)"**
2. Click your cluster name
3. Copy the **OCID** (Cluster ID) - you'll need this for kubectl

**Note**: Save your Cluster ID!
```
Cluster ID: ocid1.cluster.oc1.xxxxxx
```

### Option B: Using OCI CLI (Advanced)

```bash
# List available VCNs
oci network vcn list

# Create a new cluster (replace VCN_ID with actual ID)
oci ce cluster create \
  --name genai-classifier-cluster \
  --kubernetes-version v1.28 \
  --vcn-id <VCN_ID>
```

**✅ OKE Cluster is created!**

---

## Step 5: Configure kubectl

### 5.1 Install kubectl

```bash
# On macOS
brew install kubectl

# On Linux
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
chmod +x kubectl
sudo mv kubectl /usr/local/bin/

# On Windows
brew install kubectl
# Or download from: https://kubernetes.io/docs/tasks/tools/

# Verify installation
kubectl version --client
```

### 5.2 Download kubeconfig

```bash
# Get your cluster OCID (from Step 4)
CLUSTER_ID="ocid1.cluster.oc1.xxxxx"  # Replace with your cluster ID

# Download kubeconfig
oci ce cluster create-kubeconfig --cluster-id $CLUSTER_ID --file ~/.kube/config

# Set correct permissions
chmod 600 ~/.kube/config

# Verify connection
kubectl cluster-info
```

**If you get an error:**
```bash
# Create .kube directory if it doesn't exist
mkdir -p ~/.kube

# Try again
oci ce cluster create-kubeconfig --cluster-id <YOUR_CLUSTER_ID> --file ~/.kube/config
```

**✅ kubectl is configured!**

---

## Step 6: Verify Connection

### 6.1 Test kubectl Connection

```bash
# Check cluster info
kubectl cluster-info

# Check nodes
kubectl get nodes

# You should see 3 worker nodes listed
```

### 6.2 Example Output

```
NAME                     STATUS   ROLES   AGE   VERSION
oke-xxxxx-node-xxxxx     Ready    node    5m    v1.28.0
oke-xxxxx-node-xxxxx     Ready    node    5m    v1.28.0
oke-xxxxx-node-xxxxx     Ready    node    5m    v1.28.0
```

**✅ kubectl is connected to your cluster!**

---

## Step 7: Deploy Your Application

Now that kubectl is connected, you can deploy!

### 7.1 Prepare for Deployment

Before deploying, you need to:

1. **Build and push Docker image to OCIR**

```bash
# Navigate to your app directory
cd GenAITextClassifierBackend/gen-ai-text-classifier

# Build Docker image
docker build -t genai-text-classifier:v1.0.0 .

# Login to OCIR (Oracle Container Image Registry)
# Get your auth token from: Oracle Cloud Console → User Settings → Auth Tokens
docker login -u <TENANCY>/<USERNAME> <REGION>.ocir.io
# When prompted, enter your auth token as password

# Tag image for OCIR
docker tag genai-text-classifier:v1.0.0 \
  <REGION>.ocir.io/<TENANCY>/genai-text-classifier:v1.0.0

# Push to OCIR
docker push <REGION>.ocir.io/<TENANCY>/genai-text-classifier:v1.0.0
```

**Get Your Auth Token:**
1. Log in to https://cloud.oracle.com
2. Click your profile icon → **"User Settings"**
3. Scroll to **"Auth Tokens"**
4. Click **"Generate Token"**
5. Copy the token (you can only see it once!)

2. **Update Helm values for Oracle**

```bash
# Edit the values file
nano helm/values-oracle.yaml

# Update the registry line:
# registry: <REGION>.ocir.io/<TENANCY>/genai-repo

# Example:
# registry: iad.ocir.io/mytenancy/genai-repo
```

### 7.2 Deploy Using Your Script

```bash
# Navigate to your project
cd GenAITextClassifierBackend

# Make sure script is executable
chmod +x scripts/deploy.sh

# Deploy!
./scripts/deploy.sh oracle

# The script will:
# ✓ Check prerequisites
# ✓ Create namespace
# ✓ Deploy with Helm
# ✓ Wait for pods to be ready
# ✓ Show deployment status
```

### 7.3 Verify Deployment

```bash
# Check deployment status
kubectl -n genai-app get pods

# View logs
kubectl -n genai-app logs -f deployment/genai-text-classifier

# Test API (port forward)
kubectl port-forward -n genai-app svc/genai-text-classifier 8080:80

# In another terminal, test:
curl http://localhost:8080/api/classifiers/classify \
  -H "Content-Type: application/json" \
  -d '{
    "genAIType": "gemini",
    "genAIModel": "gemini-1.5-flash",
    "genAIAPIKey": "YOUR_API_KEY",
    "textToClassifyList": ["Hello World"],
    "attributeList": ["sentiment"]
  }'
```

**✅ Your application is deployed!**

---

## Troubleshooting

### Issue: "kubectl: command not found"
```bash
# Install kubectl
brew install kubectl  # macOS
# or follow installation steps above for your OS
```

### Issue: "Cannot connect to cluster"
```bash
# Verify kubeconfig
cat ~/.kube/config

# Regenerate kubeconfig
oci ce cluster create-kubeconfig --cluster-id <CLUSTER_ID> --file ~/.kube/config --region <REGION>
```

### Issue: "Image pull errors"
```bash
# Create image pull secret
kubectl create secret docker-registry ocir-secret \
  -n genai-app \
  --docker-server=<REGION>.ocir.io \
  --docker-username=<TENANCY>/<USERNAME> \
  --docker-password=<AUTH_TOKEN>
```

### Issue: "Pods not reaching ready state"
```bash
# Check pod events
kubectl describe pod -n genai-app <POD_NAME>

# Check logs
kubectl logs -n genai-app <POD_NAME>

# Check resource availability
kubectl top nodes
```

---

## Cost Management

### Always Free Resources on Oracle Cloud
- 2 VM.Standard.A1.Flex instances (Ampere)
- 4GB of memory
- 1 OKE cluster
- Enough for development/testing

### Monitor Costs
1. Go to **"Billing"** in Oracle Cloud Console
2. Check your **"Cost Analysis"**
3. Set up **"Budget Alerts"** (free tier stays within limits)

### Avoid Unexpected Charges
- Don't create additional VM instances beyond Always Free
- Monitor your usage regularly
- Delete unused resources

---

## Next Steps After Deployment

1. **Setup Ingress** (make API publicly accessible)
2. **Configure TLS/SSL** (HTTPS)
3. **Setup Monitoring** (Prometheus + Grafana)
4. **Setup Logging** (centralized logs)
5. **Configure Auto-scaling** (HPA)

See `k8s/DEPLOYMENT_GUIDE.md` for advanced topics.

---

## Summary

You've successfully:
1. ✅ Created an Oracle Cloud account
2. ✅ Installed and configured OCI CLI
3. ✅ Created an OKE cluster
4. ✅ Configured kubectl
5. ✅ Deployed your GenAI Text Classifier

Your application is now running on Oracle Cloud Kubernetes! 🎉

---

## Quick Reference

```bash
# Check cluster status
kubectl cluster-info
kubectl get nodes
kubectl get all -n genai-app

# View logs
kubectl logs -f -n genai-app deployment/genai-text-classifier

# Scale deployment
kubectl scale deployment genai-text-classifier -n genai-app --replicas=5

# Delete deployment
kubectl delete namespace genai-app
```

---

## Helpful Resources

- [Oracle Cloud Documentation](https://docs.oracle.com/en-us/iaas/)
- [OKE Documentation](https://docs.oracle.com/en-us/iaas/Content/ContEng/home.htm)
- [OCI CLI Documentation](https://docs.oracle.com/en-us/iaas/tools/oci-cli/latest/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Oracle Cloud Always Free](https://www.oracle.com/cloud/free/)

---

## Still Need Help?

Check:
1. `k8s/README.md` - Kubernetes quick start
2. `k8s/DEPLOYMENT_GUIDE.md` - Detailed deployment guide
3. `KUBERNETES_DEPLOYMENT_SUMMARY.md` - Complete overview
4. Oracle Cloud support: https://www.oracle.com/cloud/support/