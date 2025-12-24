#!/usr/bin/env zsh
set -e
set -m

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"

# config
PORTS_TO_KILL=(8080 8081 8082 8083 5173)
DOCKER_COMPOSE_FILE="$ROOT_DIR/docker-compose.yml"

SPRING_APPS=(
  "gateway-service"
  "api-server"
  "code-execution-service"
)

API_PORTS=(8081 8082 8083)
FRONTEND_DIR="$ROOT_DIR/frontend"
FRONTEND_PORT=5173

PIDS=()  # track JVM / frontend PIDs

# help functions

kill_port() {
  local port=$1
  lsof -ti tcp:"$port" | xargs -r kill -9
}

cleanup() {
  # disable recursive traps
  trap '' INT TERM EXIT
  echo ""
  echo "Stopping all services..."

  # kill Spring Boot / frontend PIDs
  if [ ${#PIDS[@]} -gt 0 ]; then
    echo "Killing background JVM / frontend processes..."
    kill "${PIDS[@]}" 2>/dev/null || true
  fi

  # stop docker containers
  if [ -f "$DOCKER_COMPOSE_FILE" ]; then
    echo "Stopping Docker Compose..."
    docker-compose down || true
  fi

  echo "Cleanup complete."
  exit 0
}

trap cleanup INT TERM EXIT

# free necessary ports
for port in "${PORTS_TO_KILL[@]}"; do
  kill_port "$port"
done

# start docker
echo "Starting Docker Compose..."
docker-compose up --build -d

# wait for RabbitMQ
echo "Waiting for RabbitMQ on port 5672..."
until nc -z localhost 5672; do
  sleep 0.5
done
echo "RabbitMQ is ready"

# start spring boot apps

# 1. Gateway
cd "$ROOT_DIR/gateway-service"
mvn clean install -DskipTests
mvn spring-boot:run &
PIDS+=($!)
echo "Started gateway-service (PID ${PIDS[-1]})"

# 2. API server (3 instances)
cd "$ROOT_DIR/api-server"
mvn clean install -DskipTests
for port in "${API_PORTS[@]}"; do
  mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=$port" &
  PIDS+=($!)
  echo "Started api-server on port $port (PID ${PIDS[-1]})"
done

# 3. Code execution service
cd "$ROOT_DIR/code-execution-service"
mvn clean install -DskipTests
mvn spring-boot:run &
PIDS+=($!)
echo "Started code-execution-service (PID ${PIDS[-1]})"

# frontend
cd "$FRONTEND_DIR"
npm install
npm run dev &
PIDS+=($!)
echo "Started frontend (PID ${PIDS[-1]})"

# check ports
PORTS_TO_CHECK=(8080 8081 8082 8083 5173 5672)
echo "Waiting for services to be ready..."
for port in "${PORTS_TO_CHECK[@]}"; do
  until nc -z localhost "$port"; do
    sleep 0.5
  done
  echo "localhost:$port is up"
done

echo "----------------------------------------"
echo "All services started successfully"
echo "----------------------------------------"

# Keep script alive to trap signals
wait