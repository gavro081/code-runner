#!/bin/zsh
NUM_REQUESTS=${1:-100}
JSON_FILE="cmd1_data.json"
ab -n "$NUM_REQUESTS" -c 20 -p "$JSON_FILE" -T application/json http://localhost:8080/api/submit