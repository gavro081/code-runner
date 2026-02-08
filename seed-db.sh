#!/bin/bash
set -e

DB_NAME="code_execution_db"
COLLECTION="problems"
JSON_FILE="$(dirname "$0")/code_execution_db.problems.json"

if ! command -v mongoimport &> /dev/null; then
  echo "Error: mongoimport is not installed."
  echo "Install MongoDB Database Tools: https://www.mongodb.com/docs/database-tools/installation/"
  exit 1
fi

if [ ! -f "$JSON_FILE" ]; then
  echo "Error: $JSON_FILE not found."
  exit 1
fi

echo "Dropping existing '$COLLECTION' collection in '$DB_NAME'..."
mongosh --quiet --eval "db.getSiblingDB('$DB_NAME').$COLLECTION.drop()" 2>/dev/null || true

echo "Importing problems from $JSON_FILE..."
mongoimport --db "$DB_NAME" --collection "$COLLECTION" --jsonArray --file "$JSON_FILE"

echo "Done. Imported $(mongosh --quiet --eval "db.getSiblingDB('$DB_NAME').$COLLECTION.countDocuments()" 2>/dev/null) problems into $DB_NAME.$COLLECTION."
