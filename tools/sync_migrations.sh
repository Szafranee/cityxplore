#!/usr/bin/env bash
# Script to sync Flyway migrations to Supabase migrations folder
# Usage: ./tools/sync_migrations.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

SRC_DIR="$REPO_ROOT/backend/src/main/resources/db/migration"
DEST_DIR="$REPO_ROOT/supabase/migrations"

echo "Syncing Flyway migrations from $SRC_DIR to $DEST_DIR ..."

shopt -s nullglob

copied=0
for FILE in "$SRC_DIR"/V*__*.sql; do
  BASE_NAME="$(basename "$FILE")"
  DESC="${BASE_NAME#V[0-9]*__}"
  DESC="${DESC%.sql}"
  DESC="${DESC//_/-}"

  TS="$(date +"%Y%m%d%H%M%S")"
  DEST_FILE="$DEST_DIR/${TS}_${DESC}.sql"

  cp "$FILE" "$DEST_FILE"
  echo "Copied: $BASE_NAME → $(basename "$DEST_FILE")"
  copied=$((copied+1))
  sleep 1
done

shopt -u nullglob

if (( copied == 0 )); then
  echo "No files to copy in $SRC_DIR"
fi

echo "Migration sync complete!"