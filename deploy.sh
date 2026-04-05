#!/bin/bash
# TransitPulse AI - Kubernetes Deployment Script
# Prerequisites: minikube, kubectl, docker

set -e

echo "============================================"
echo "  TransitPulse AI - Kubernetes Deployment"
echo "============================================"

# Step 1: Start Minikube (if not running)
echo ""
echo "[1/7] Checking Minikube..."
if ! minikube status | grep -q "Running"; then
    echo "Starting Minikube..."
    minikube start --cpus=4 --memory=8192 --driver=docker
fi
minikube addons enable ingress
minikube addons enable metrics-server

# Step 2: Point Docker to Minikube's daemon
echo ""
echo "[2/7] Configuring Docker for Minikube..."
eval $(minikube docker-env)

# Step 3: Build Java services
echo ""
echo "[3/7] Building Java services..."
cd "$(dirname "$0")"
mvn clean package -DskipTests -q

# Step 4: Build Docker images
echo ""
echo "[4/7] Building Docker images..."
docker build -t transitpulse/bus-service:latest ./bus-service
docker build -t transitpulse/occupancy-service:latest ./occupancy-service
docker build -t transitpulse/commuter-service:latest ./commuter-service
docker build -t transitpulse/ai-prediction-service:latest ./ai-prediction-service
docker build -t transitpulse/gps-simulator:latest ./gps-simulator
docker build -t transitpulse/stream-processor:latest ./stream-processor
docker build -t transitpulse/notification-service:latest ./notification-service
docker build -t transitpulse/frontend:latest ./frontend

# Step 5: Create namespace and config
echo ""
echo "[5/7] Deploying infrastructure..."
kubectl apply -f k8s/namespace.yml
kubectl apply -f k8s/configmaps/
kubectl apply -f k8s/secrets/

# Step 6: Deploy stateful services first
echo ""
echo "[6/7] Deploying stateful services..."
kubectl apply -f k8s/statefulsets/postgres.yml
kubectl apply -f k8s/statefulsets/redis.yml
kubectl apply -f k8s/statefulsets/kafka.yml

echo "Waiting for infrastructure pods to be ready..."
kubectl wait --for=condition=ready pod -l app=postgres -n bus-tracking --timeout=120s
kubectl wait --for=condition=ready pod -l app=redis -n bus-tracking --timeout=60s
kubectl wait --for=condition=ready pod -l app=kafka -n bus-tracking --timeout=120s

# Step 7: Deploy application services
echo ""
echo "[7/7] Deploying application services..."
kubectl apply -f k8s/deployments/
kubectl apply -f k8s/hpa/
kubectl apply -f k8s/ingress/

echo ""
echo "============================================"
echo "  Deployment Complete!"
echo "============================================"
echo ""
echo "Waiting for pods to start..."
sleep 10
kubectl get pods -n bus-tracking

echo ""
echo "Access the application:"
echo "  Frontend:  $(minikube service frontend -n bus-tracking --url 2>/dev/null || echo 'Run: minikube service frontend -n bus-tracking')"
echo "  Ingress:   $(minikube ip)"
echo ""
echo "Useful commands:"
echo "  kubectl get pods -n bus-tracking"
echo "  kubectl logs -f deployment/commuter-service -n bus-tracking"
echo "  minikube dashboard"
