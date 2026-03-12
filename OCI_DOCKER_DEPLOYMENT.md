# Docker Deployment to Oracle Cloud Infrastructure (OCI)

This guide walks through deploying the GenAI Text Classifier as a Docker container on Oracle Cloud Infrastructure.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Build Docker Image](#build-docker-image)
3. [Push to OCI Registry](#push-to-oci-registry)
4. [Deploy to OCI Compute](#deploy-to-oci-compute)
5. [Configure Network and Access](#configure-network-and-access)
6. [Testing](#testing)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Local Requirements
- Docker installed and running
- OCI CLI installed and configured
- Maven 3.8+
- Java 17+

### OCI Account Requirements
- OCI Account with permissions to:
  - Create Container Registry repositories
  - Launch Compute instances (VM.Standard.A1.Flex or similar)
  - Configure Virtual Cloud Networks (VCN)
  - Create security rules and network components

### Setup OCI CLI

1. Install OCI CLI:
   ```bash
   curl -L https://raw.githubusercontent.com/oracle/oci-cli/master/scripts/install/install.sh | bash
   ```

2. Configure OCI CLI with your credentials:
   ```bash
   oci setup config
   ```
   This will prompt you to enter:
   - User OCID
   - Tenancy OCID
   - Region
   - API key location

3. Verify configuration:
   ```bash
   oci iam user get --user-id <your-user-ocid>
   ```

---

## Build Docker Image

### 1. Build the Application
```bash
cd gen-ai-text-classifier
mvn clean install -DskipTests
```

### 2. Build Docker Image
```bash
docker build -t genai-text-classifier:1.0 .
```

### 3. Test Image Locally
```bash
# Start MySQL database
docker-compose up -d

# Run the container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/genai_classifier \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  genai-text-classifier:1.0

# Test the API
curl -X POST http://localhost:8080/api/classifiers/classify \
  -H "Content-Type: application/json" \
  -d '{
    "genAIType": "gemini",
    "genAIModel": "gemini-1.5-flash",
    "genAIAPIKey": "your-api-key",
    "textToClassifyList": ["sample text"],
    "attributeList": ["sentiment"]
  }'
```

---

## Push to OCI Registry

### 1. Create an OCI Registry Repository

```bash
# Get your tenancy namespace
TENANCY_NAMESPACE=$(oci os ns get --query data --raw-output)

# Create repository
oci artifacts container repository create \
  --display-name genai-text-classifier \
  --repository-type private

# Get the repository URL
REGISTRY_URL=$(oci artifacts container repository list --query 'data[0]."image-url"' --raw-output)
```

Or use the OCI Console:
- Navigate to **Container Registries** → **Create Repository**
- Set visibility to Private
- Note the repository URL (format: `<region>.ocir.io/<tenancy>/<repo-name>`)

### 2. Authenticate Docker with OCI Registry

```bash
# Get an auth token from OCI Console
# Settings → User Settings → Auth Tokens → Generate Token

docker login <region>.ocir.io

# When prompted:
# Username: <tenancy-name>/<username>
# Password: <auth-token>
```

### 3. Tag and Push Image

```bash
# Set variables
REGION="us-ashburn-1"  # Replace with your region
TENANCY_NAME="your-tenancy-name"
REPO_NAME="genai-text-classifier"
IMAGE_TAG="1.0"

# Tag image
docker tag genai-text-classifier:${IMAGE_TAG} \
  ${REGION}.ocir.io/${TENANCY_NAME}/${REPO_NAME}:${IMAGE_TAG}

# Push to registry
docker push ${REGION}.ocir.io/${TENANCY_NAME}/${REPO_NAME}:${IMAGE_TAG}

# Verify push
oci artifacts container image list \
  --repository-name ${REPO_NAME} \
  --compartment-id <your-compartment-id>
```

---

## Deploy to OCI Compute

### 1. Create Compute Instance

Using OCI Console:
1. Navigate to **Compute** → **Instances** → **Create Instance**
2. **Image and Shape**:
   - Image: Ubuntu 22.04 (Canonical)
   - Shape: VM.Standard.A1.Flex (always free tier eligible)
   - Network bandwidth: 1 Gbps

3. **Networking**:
   - Create new VCN or use existing
   - Create public subnet
   - Assign public IP

4. **Add SSH Key**:
   - Download and save private key file
   - Set permissions: `chmod 600 <key-file>`

Or use OCI CLI:
```bash
oci compute instance launch \
  --compartment-id <compartment-id> \
  --image-id <ubuntu-image-id> \
  --shape VM.Standard.A1.Flex \
  --shape-config '{"memoryInGBs":12,"ocpus":2}' \
  --display-name genai-classifier-instance
```

### 2. Install Docker on Compute Instance

```bash
ssh -i <private-key> ubuntu@<instance-public-ip>

# Update system
sudo apt update && sudo apt upgrade -y

# Install Docker
sudo apt install docker.io -y
sudo usermod -aG docker $USER

# Install Docker Compose
sudo apt install docker-compose -y

# Verify installation
docker --version
docker-compose --version
```

### 3. Pull and Run Image

```bash
# Authenticate with OCI Registry
echo <auth-token> | docker login -u <tenancy>/<username> \
  --password-stdin <region>.ocir.io

# Pull image
docker pull <region>.ocir.io/<tenancy>/<repo>:1.0

# Create environment file
cat > .env << EOF
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/genai_classifier
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=secure_password_here
SPRING_DATASOURCE_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
EOF

# Create docker-compose.yaml for production
cat > docker-compose.yaml << 'EOF'
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
      MYSQL_DATABASE: genai_classifier
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    networks:
      - app_network

  genai-classifier:
    image: <region>.ocir.io/<tenancy>/<repo>:1.0
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}
      SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
    depends_on:
      - mysql
    networks:
      - app_network
    restart: unless-stopped

volumes:
  mysql_data:

networks:
  app_network:
    driver: bridge
EOF

# Start services
docker-compose up -d

# Check status
docker-compose ps
docker-compose logs -f genai-classifier
```

---

## Configure Network and Access

### 1. Update Security List Rules

In OCI Console:
1. Navigate to **Networking** → **Virtual Cloud Networks** → Your VCN
2. Select **Security Lists** → **Default Security List**
3. **Add Ingress Rules**:

   | Protocol | Port | Source | Description |
   |----------|------|--------|-------------|
   | TCP | 8080 | 0.0.0.0/0 | API access |
   | TCP | 3306 | 10.0.0.0/8 | MySQL (internal only) |

### 2. Test Access

```bash
# From local machine
curl -X POST http://<instance-public-ip>:8080/api/classifiers/classify \
  -H "Content-Type: application/json" \
  -d '{
    "genAIType": "gemini",
    "genAIModel": "gemini-1.5-flash",
    "genAIAPIKey": "your-api-key",
    "textToClassifyList": ["test"],
    "attributeList": ["sentiment"]
  }'
```

---

## Testing

### 1. API Health Check
```bash
curl -X GET http://<instance-public-ip>:8080/api/classifiers/classify \
  -H "Content-Type: application/json"
```

### 2. Classification Test
```bash
curl -X POST http://<instance-public-ip>:8080/api/classifiers/classify \
  -H "Content-Type: application/json" \
  -d '{
    "genAIType": "gemini",
    "genAIModel": "gemini-1.5-flash",
    "genAIAPIKey": "your-gemini-api-key",
    "textToClassifyList": [
      "I love this product!",
      "This is terrible",
      "It is okay"
    ],
    "attributeList": ["sentiment"]
  }'
```

### 3. Container Logs
```bash
# SSH into instance
ssh -i <key> ubuntu@<instance-ip>

# Check application logs
docker-compose logs -f genai-classifier

# Check database logs
docker-compose logs -f mysql
```

---

## Troubleshooting

### Issue: Docker Image Pull Fails
**Solution**: Verify authentication and image exists
```bash
docker logout <region>.ocir.io
echo <auth-token> | docker login -u <tenancy>/<username> \
  --password-stdin <region>.ocir.io
docker pull <full-image-path>:tag
```

### Issue: Connection Refused on Port 8080
**Solution**: Check security list and docker-compose status
```bash
# On instance
docker-compose ps
docker-compose logs genai-classifier

# Check port is listening
sudo netstat -tlnp | grep 8080
```

### Issue: Database Connection Error
**Solution**: Verify MySQL is running and credentials match
```bash
docker-compose logs mysql

# Check from application container
docker exec <container-id> curl localhost:3306
```

### Issue: OutOfMemory Exceptions
**Solution**: Increase JVM heap memory in docker-compose
```bash
environment:
  JAVA_OPTS: "-Xmx512m -Xms256m"
```

### Issue: Image Registry Access Denied
**Solution**: Recreate auth token and re-login
```bash
docker logout <region>.ocir.io
# Generate new token in OCI Console
echo <new-token> | docker login -u <tenancy>/<username> \
  --password-stdin <region>.ocir.io
```

---

## Maintenance

### Update Application

1. Build new Docker image locally
2. Push to OCI Registry
3. On instance: pull and redeploy
   ```bash
   docker-compose pull
   docker-compose down
   docker-compose up -d
   ```

### Database Backups

Backup MySQL data:
```bash
docker-compose exec mysql mysqldump -u root -p$MYSQL_ROOT_PASSWORD genai_classifier \
  > backup_$(date +%Y%m%d_%H%M%S).sql
```

### Monitor Resources

```bash
# Check resource usage
docker stats

# View all Docker events
docker events --filter type=container
```

---

## Cost Optimization

- **Always Free Resources**: Use VM.Standard.A1.Flex (eligible for always free tier)
- **Auto-shutdown**: Configure compute instance to auto-shutdown during non-working hours
- **Container Registry**: 20GB always free storage
- **Network**: 1TB/month always free data transfer

---

## Security Considerations

1. **API Keys**: Store in environment variables or OCI Vault
2. **Database**: Use internal security list rules (not exposed to internet)
3. **Registry Access**: Use private repository with appropriate IAM policies
4. **HTTPS**: Consider using OCI Load Balancer with SSL certificate
5. **Secrets Management**: Use OCI Vault for sensitive credentials

---

## Next Steps

- Set up OCI Load Balancer for HTTPS
- Configure OCI Monitoring and Logging
- Implement CI/CD pipeline with GitHub Actions to auto-deploy
- Add application performance monitoring (APM)
