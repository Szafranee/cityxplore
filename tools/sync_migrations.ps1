#!/usr/bin/env pwsh
# Script to sync Flyway migrations to Supabase migrations folder
# Usage: pwsh ./tools/sync_migrations.ps1

$ErrorActionPreference = 'Stop'

$SRC_DIR = 'backend/src/main/resources/db/migration'
$DEST_DIR = 'supabase/migrations'

Write-Host "Syncing Flyway migrations from $SRC_DIR to $DEST_DIR ..."

# Ensure destination directory exists
if (!(Test-Path -Path $DEST_DIR)) {
    New-Item -ItemType Directory -Path $DEST_DIR | Out-Null
}

# Collect files
$files = Get-ChildItem -Path $SRC_DIR -Filter 'V*__*.sql' -File | Sort-Object Name

if (-not $files) {
    Write-Host "No matching migrations found."
    exit 0
}

foreach ($file in $files) {
    # Extract descriptive name from file name (remove Vx__ and .sql, replace _ with -)
    $BASE_NAME = $file.Name
    $DESC = $BASE_NAME -replace '^V\d+__', '' -replace '\.sql$', '' -replace '_', '-'

    # Generate timestamp e.g. 20251005121030
    $TS = Get-Date -Format 'yyyyMMddHHmmss'

    # Destination filename
    $DEST_FILE = Join-Path $DEST_DIR "$($TS)_create-$DESC.sql"

    Copy-Item -Path $file.FullName -Destination $DEST_FILE -Force
    Write-Host ("Copied: {0} -> {1}" -f $BASE_NAME, [System.IO.Path]::GetFileName($DEST_FILE))

    # wait a bit to ensure different timestamps
    Start-Sleep -Seconds 1
}

Write-Host "Migration sync complete!"