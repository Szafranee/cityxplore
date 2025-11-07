#!/usr/bin/env bash
# Script to sync Flyway migrations to Supabase migrations using Supabase CLI
# Usage: ./tools/sync_migrations.sh [VERSION1 VERSION2 ... | all | --all]
# Examples:
#   ./tools/sync_migrations.sh           # sync all migrations
#   ./tools/sync_migrations.sh all       # sync all migrations
#   ./tools/sync_migrations.sh --all     # sync all migrations
#   ./tools/sync_migrations.sh V8        # sync only V8
#   ./tools/sync_migrations.sh V8 V9 V10 # sync V8, V9 and V10

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

SRC_DIR="$REPO_ROOT/backend/src/main/resources/db/migration"

# Check if supabase CLI is available
if ! command -v supabase &> /dev/null; then
  echo "ERROR: supabase CLI is not installed or not in PATH"
  echo "Please install it from: https://supabase.com/docs/guides/cli"
  exit 1
fi

# Change to repo root for supabase commands
cd "$REPO_ROOT"

# Determine which migrations to process
VERSIONS_TO_PROCESS=()

if [ $# -eq 0 ] || [ "$1" = "all" ] || [ "$1" = "--all" ]; then
  # Process all migrations
  echo "Processing all migrations..."
  shopt -s nullglob
  for FILE in "$SRC_DIR"/V*__*.sql; do
    BASE_NAME="$(basename "$FILE")"
    VERSION="${BASE_NAME%%__*}"
    VERSIONS_TO_PROCESS+=("$VERSION")
  done
  shopt -u nullglob
else
  # Process specified versions
  for ARG in "$@"; do
    # Normalize version (add V prefix if missing)
    if [[ "$ARG" =~ ^[0-9]+$ ]]; then
      VERSION="V$ARG"
    else
      VERSION="$ARG"
    fi
    VERSIONS_TO_PROCESS+=("$VERSION")
  done
fi

if [ ${#VERSIONS_TO_PROCESS[@]} -eq 0 ]; then
  echo "No migrations found to process"
  exit 0
fi

echo "Will process ${#VERSIONS_TO_PROCESS[@]} migration(s): ${VERSIONS_TO_PROCESS[*]}"
echo ""

# Process each version
processed=0
for VERSION in "${VERSIONS_TO_PROCESS[@]}"; do
  # Find the corresponding file
  shopt -s nullglob
  FILES=("$SRC_DIR/${VERSION}__"*.sql)
  shopt -u nullglob

  if [ ${#FILES[@]} -eq 0 ]; then
    echo "WARNING: No file found for version $VERSION, skipping..."
    continue
  fi

  FILE="${FILES[0]}"
  BASE_NAME="$(basename "$FILE")"

  # Extract description and convert underscores to hyphens
  DESC="${BASE_NAME#"${VERSION}"__}"
  DESC="${DESC%.sql}"
  DESC="${DESC//_/-}"

  echo "Processing $VERSION: $BASE_NAME"
  echo "  Creating Supabase migration: $DESC"

  # Create new migration using supabase CLI
  supabase migration new "$DESC" > /dev/null

  # Find the newly created migration file (it will be the latest one)
  LATEST_MIGRATION=$(find "$REPO_ROOT/supabase/migrations" -maxdepth 1 -type f -printf '%T@ %p\n' | sort -n | tail -n 1 | cut -d' ' -f2-)
  DEST_FILE="$REPO_ROOT/supabase/migrations/$(basename "$LATEST_MIGRATION")"

  # Copy the content from Flyway migration to Supabase migration
  cp "$FILE" "$DEST_FILE"

  echo "  ✓ Created: $LATEST_MIGRATION"
  processed=$((processed+1))

  # Small delay to ensure unique timestamps
  sleep 1
done

echo ""
echo "Migration sync complete! Processed $processed migration(s)."
