#!/bin/zsh

NUM_REQUESTS=${1:-100}

hey -n "$NUM_REQUESTS" -c 25 \
  -m POST \
  -H "Content-Type: application/json" \
  -d '{"code":"import time\n\ntime.sleep(3)\nprint(\"hello world\")\n","language":"PYTHON", "problemId":"two-sum"}' \
  http://localhost:8080/api/submit