#!/usr/bin/env pwsh
# Script to sync Flyway migrations to Supabase migrations using Supabase CLI
# Usage: pwsh ./tools/sync_migrations.ps1 [VERSION1 VERSION2 ... | all | --all]
# Examples:
#   pwsh ./tools/sync_migrations.ps1           # sync all migrations
#   pwsh ./tools/sync_migrations.ps1 all       # sync all migrations
#   pwsh ./tools/sync_migrations.ps1 --all     # sync all migrations
#   pwsh ./tools/sync_migrations.ps1 V8        # sync only V8
#   pwsh ./tools/sync_migrations.ps1 V8 V9 V10 # sync V8, V9 and V10

param(
    [Parameter(ValueFromRemainingArguments=$true)]
    [string[]]$Versions
)

$ErrorActionPreference = 'Stop'

$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$REPO_ROOT = Split-Path -Parent $SCRIPT_DIR
$SRC_DIR = Join-Path $REPO_ROOT 'backend\src\main\resources\db\migration'
$DEST_DIR = Join-Path $REPO_ROOT 'supabase\migrations'

# Check if supabase CLI is available
if (-not (Get-Command supabase -ErrorAction SilentlyContinue)) {
    Write-Host "ERROR: supabase CLI is not installed or not in PATH" -ForegroundColor Red
    Write-Host "Please install it from: https://supabase.com/docs/guides/cli"
    exit 1
}

# Change to repo root for supabase commands
Push-Location $REPO_ROOT

try {
    # Determine which migrations to process
    $VERSIONS_TO_PROCESS = @()

    if ($Versions.Count -eq 0 -or $Versions[0] -eq 'all' -or $Versions[0] -eq '--all') {
        # Process all migrations
        Write-Host "Processing all migrations..."
        $files = Get-ChildItem -Path $SRC_DIR -Filter 'V*__*.sql' -File | Sort-Object Name
        foreach ($file in $files) {
            $BASE_NAME = $file.Name
            # Extract version (e.g., V1, V10)
            if ($BASE_NAME -match '^(V\d+)__') {
                $VERSIONS_TO_PROCESS += $matches[1]
            }
        }
    } else {
        # Process specified versions
        foreach ($arg in $Versions) {
            # Normalize version (add V prefix if missing)
            if ($arg -match '^\d+$') {
                $VERSIONS_TO_PROCESS += "V$arg"
            } else {
                $VERSIONS_TO_PROCESS += $arg
            }
        }
    }

    if ($VERSIONS_TO_PROCESS.Count -eq 0) {
        Write-Host "No migrations found to process"
        exit 0
    }

    Write-Host "Will process $($VERSIONS_TO_PROCESS.Count) migration(s): $($VERSIONS_TO_PROCESS -join ', ')"
    Write-Host ""

    # Process each version
    $processed = 0
    foreach ($VERSION in $VERSIONS_TO_PROCESS) {
        # Find the corresponding file
        $files = Get-ChildItem -Path $SRC_DIR -Filter "${VERSION}__*.sql" -File

        if ($files.Count -eq 0) {
            Write-Host "WARNING: No file found for version $VERSION, skipping..." -ForegroundColor Yellow
            continue
        }

        $file = $files[0]
        $BASE_NAME = $file.Name

        # Extract description and convert underscores to hyphens
        $DESC = $BASE_NAME -replace "^${VERSION}__", '' -replace '\.sql$', '' -replace '_', '-'

        Write-Host "Processing $VERSION : $BASE_NAME"
        Write-Host "  Creating Supabase migration: $DESC"

        # Create new migration using supabase CLI
        supabase migration new $DESC | Out-Null

        # Find the newly created migration file (it will be the latest one)
        $LATEST_MIGRATION = Get-ChildItem -Path $DEST_DIR -Filter '*.sql' -File |
                           Sort-Object LastWriteTime -Descending |
                           Select-Object -First 1

        if ($LATEST_MIGRATION) {
            # Copy the content from Flyway migration to Supabase migration
            Copy-Item -Path $file.FullName -Destination $LATEST_MIGRATION.FullName -Force

            Write-Host "  ✓ Created: $($LATEST_MIGRATION.Name)" -ForegroundColor Green
            $processed++

            # Small delay to ensure unique timestamps
            Start-Sleep -Seconds 1
        } else {
            Write-Host "  ERROR: Could not find newly created migration file" -ForegroundColor Red
        }
    }

    Write-Host ""
    Write-Host "Migration sync complete! Processed $processed migration(s)."

} finally {
    Pop-Location
}
