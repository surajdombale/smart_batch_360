# Builds a self-contained SmartBatch360 backend server .exe (bundles its own
# Java runtime - the target machine only needs MySQL, not Java or Maven).
# Run from anywhere; paths are resolved relative to this script's location.
#
# Usage: powershell -ExecutionPolicy Bypass -File scripts\package-backend.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"

Push-Location $backend
try {
    Write-Host "Building smartbatch360-api.jar..."
    mvn -q clean package "-DskipTests"
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }

    New-Item -ItemType Directory -Force -Path "target/app-input" | Out-Null
    Copy-Item "target/smartbatch360-api.jar" "target/app-input/" -Force

    Remove-Item -Recurse -Force "target/dist" -ErrorAction SilentlyContinue

    Write-Host "Packaging with jpackage..."
    # jdk.unsupported is required in addition to java.se: Spring's CGLIB/AOP
    # proxying (used by @Transactional) needs sun.reflect.ReflectionFactory,
    # which lives in jdk.unsupported - omitting it fails at bean-creation time
    # with "Unable to instantiate proxy using Objenesis".
    jpackage `
        --type app-image `
        --name SmartBatch360-Server `
        --input target/app-input `
        --main-jar smartbatch360-api.jar `
        --add-modules java.se,jdk.unsupported `
        --dest target/dist `
        --app-version 0.1.0 `
        --vendor "SmartBatch360" `
        --description "SmartBatch360 REST backend (Phase 1)" `
        --win-console
    if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

    New-Item -ItemType Directory -Force -Path "target/dist/SmartBatch360-Server/db" | Out-Null
    Copy-Item "src/main/resources/db/dev-setup.sql" "target/dist/SmartBatch360-Server/db/" -Force
    Copy-Item (Join-Path $root "scripts\SETUP_ON_NEW_PC.md") "target/dist/SmartBatch360-Server/" -Force

    $zipPath = Join-Path $backend "target\SmartBatch360-Server.zip"
    Remove-Item $zipPath -ErrorAction SilentlyContinue
    Compress-Archive -Path "target/dist/SmartBatch360-Server" -DestinationPath $zipPath
    Write-Host "Done: $zipPath"
}
finally {
    Pop-Location
}
