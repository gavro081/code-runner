#!/bin/zsh

PORTS=(8080 8081 8082 8083 8084 5173 5672)

check_port() {
  local host="localhost"
  local port=$1

  if nc -z "$host" "$port"; then
    echo "$host:$port is reachable"
  else
    echo "$host:$port is NOT reachable"
    return 1
  fi
}

echo "Running port checks..."

FAILED=0

for port in "${PORTS[@]}"; do
  if ! check_port "$port"; then
    FAILED=1
  fi
done

echo "----------------------"

if [ "$FAILED" -eq 0 ]; then
  echo "All services are up"
else
  echo "Some services are not reachable"
  exit 1
fi