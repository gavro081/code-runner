#!/bin/zsh
JSON_FILE="cmd1_data.json"
ab -n 100 -c 20 -p "$JSON_FILE" -T application/json http://localhost:8080/api/submit