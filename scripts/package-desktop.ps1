# Builds a self-contained SmartBatch360 desktop client .exe (bundles its own
# Java runtime - the target machine does not need Java or Maven installed).
# Run from anywhere; paths are resolved relative to this script's location.
#
# Usage: powershell -ExecutionPolicy Bypass -File scripts\package-desktop.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$desktop = Join-Path $root "desktop"

Push-Location $desktop
try {
    Write-Host "Building smartbatch360-desktop.jar..."
    mvn -q clean package "-DskipTests"
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }

    Write-Host "Collecting runtime dependencies (JavaFX, Jackson)..."
    mvn -q dependency:copy-dependencies "-DoutputDirectory=target/libs" "-DincludeScope=runtime"
    if ($LASTEXITCODE -ne 0) { throw "Dependency collection failed" }

    New-Item -ItemType Directory -Force -Path "target/app-input" | Out-Null
    Copy-Item "target/smartbatch360-desktop.jar" "target/app-input/" -Force
    Copy-Item "target/libs/*.jar" "target/app-input/" -Force

    Remove-Item -Recurse -Force "target/dist" -ErrorAction SilentlyContinue

    Write-Host "Packaging with jpackage..."
    # Launcher (not DesktopApplication) is required as --main-class: the java
    # launcher refuses to start a packaged app whose main class directly
    # extends javafx.application.Application without JavaFX on the module-path.
    # jdk.unsupported is required in addition to java.se: JavaFX's Marlin
    # rasterizer uses sun.misc.Unsafe (from jdk.unsupported) - omitting it
    # crashes on first paint with NoClassDefFoundError.
    jpackage `
        --type app-image `
        --name SmartBatch360 `
        --input target/app-input `
        --main-jar smartbatch360-desktop.jar `
        --main-class com.smartbatch360.desktop.Launcher `
        --add-modules java.se,jdk.unsupported `
        --dest target/dist `
        --app-version 0.1.0 `
        --vendor "SmartBatch360" `
        --description "SmartBatch360 - Industrial Batching Plant Management System (Phase 1)"
    if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

    $zipPath = Join-Path $desktop "target\SmartBatch360-Desktop.zip"
    Remove-Item $zipPath -ErrorAction SilentlyContinue
    Compress-Archive -Path "target/dist/SmartBatch360" -DestinationPath $zipPath
    Write-Host "Done: $zipPath"
}
finally {
    Pop-Location
}
