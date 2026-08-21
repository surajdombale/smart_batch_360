# Builds a single self-contained SmartBatch360 .exe - the JavaFX UI and the
# Spring Boot backend embedded in one process (bundles its own Java runtime;
# the target machine only needs MySQL, not Java or Maven).
#
# Usage: powershell -ExecutionPolicy Bypass -File scripts\package-app.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
$desktop = Join-Path $root "desktop"

Push-Location $backend
try {
    Write-Host "Installing smartbatch360-api to the local repo (desktop depends on it)..."
    mvn -q clean install "-DskipTests"
    if ($LASTEXITCODE -ne 0) { throw "Backend build failed" }
}
finally {
    Pop-Location
}

Push-Location $desktop
try {
    Write-Host "Building smartbatch360-desktop.jar (embeds the backend)..."
    mvn -q clean package "-DskipTests"
    if ($LASTEXITCODE -ne 0) { throw "Desktop build failed" }

    Write-Host "Collecting runtime dependencies (JavaFX, Spring Boot, Hibernate, MySQL driver, Flyway, Jackson)..."
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
    # rasterizer AND Spring's CGLIB/AOP proxying (@Transactional) both need it -
    # omitting it crashes the UI on first paint, or fails bean creation with
    # "Unable to instantiate proxy using Objenesis", respectively.
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

    Copy-Item (Join-Path $root "scripts\SETUP_ON_NEW_PC.md") "target/dist/SmartBatch360/" -Force

    $zipPath = Join-Path $desktop "target\SmartBatch360.zip"
    Remove-Item $zipPath -ErrorAction SilentlyContinue
    Compress-Archive -Path "target/dist/SmartBatch360" -DestinationPath $zipPath
    Write-Host "Done: $zipPath"
}
finally {
    Pop-Location
}
