#!/bin/zsh
while true; do
  hey -n 200 -c 50 \
  -m POST \
  -H "Content-Type: application/json" \
  -d '{"code":"import time\n\ntime.sleep(1)\nprint(\"hello world\")\n","language":"PYTHON", "problemId":"two-sum"}' \
  http://localhost:8080/api/submit
  sleep 1
done