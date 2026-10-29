# Builds a single self-contained SmartBatch360 .exe - the JavaFX UI and the
# Spring Boot backend embedded in one process (bundles its own Java runtime;
# the target machine only needs MySQL, not Java or Maven).
#
# Named/versioned as "SmartBatch360 V8" (app version 8.0.0) so it installs
# into its own folder alongside existing installs without colliding.
# V8 (2026-10-28) carries everything since V7: Production as the plant runs it
# (one batch at a time, driver chosen on screen), Settings > Plant Details, the
# single-batch report as PDF or Excel, the redesigned Dashboard, the cycle grid
# on Batch Reports > View, cycles typed in on Production for the running batch
# (the last planned cycle completes it, Stop Batch beside it), order status
# that follows production, a Status column on Orders, Production listing only
# open orders, and fulfilled orders no longer editable. Rebuilt the same day,
# at the user's request and still as V8, with the batch report PDF redesigned
# after their sample (up to twenty materials, landscape past ten), the View
# dialog fitted to the screen and scrolling both ways, and Delete disabled for
# fulfilled orders. Bump $AppName, $AppVersion and $ZipName here for future
# releases.
#
# Every release is kept. It is written to releases\<AppName> and
# releases\<ZipName>, outside target\ - every build begins with `mvn clean`,
# which wipes target\, and that is how earlier versions were lost when they
# lived there. Rebuilding a version replaces that version only; no other
# release is touched.
#
# Usage: powershell -ExecutionPolicy Bypass -File scripts\package-app.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root "backend"
$desktop = Join-Path $root "desktop"
$AppName = "SmartBatch360 V8"
$AppVersion = "8.0.0"
$ZipName = "SmartBatch360-V8.zip"
$releases = Join-Path $root "releases"
$appImage = Join-Path $releases $AppName
$IconPath = Join-Path $root "desktop\src\main\resources\images\app-icon.ico"

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

    # Only this version's own folder is cleared - jpackage will not write over
    # an existing app image - so every other release stays as it is.
    New-Item -ItemType Directory -Force -Path $releases | Out-Null
    Remove-Item -Recurse -Force $appImage -ErrorAction SilentlyContinue

    Write-Host "Packaging with jpackage as '$AppName' (version $AppVersion)..."
    # Launcher (not DesktopApplication) is required as --main-class: the java
    # launcher refuses to start a packaged app whose main class directly
    # extends javafx.application.Application without JavaFX on the module-path.
    # jdk.unsupported is required in addition to java.se: JavaFX's Marlin
    # rasterizer AND Spring's CGLIB/AOP proxying (@Transactional) both need it -
    # omitting it crashes the UI on first paint, or fails bean creation with
    # "Unable to instantiate proxy using Objenesis", respectively.
    jpackage `
        --type app-image `
        --name $AppName `
        --input target/app-input `
        --main-jar smartbatch360-desktop.jar `
        --main-class com.smartbatch360.desktop.Launcher `
        --add-modules java.se,jdk.unsupported `
        --dest $releases `
        --app-version $AppVersion `
        --vendor "SmartBatch360" `
        --icon $IconPath `
        --description "SmartBatch360 - Industrial Batching Plant Management System (V8: Materials, Recipes with a batch limit, Orders whose status follows production, Production with cycles recorded per batch, Batch Reports with every cycle on screen, in PDF and Excel, Material Consumption with chart, Dashboard, Company and Plant Details - quantities in kg, loads planned in m3)"
    if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

    Copy-Item (Join-Path $root "scripts\SETUP_ON_NEW_PC.md") $appImage -Force

    $zipPath = Join-Path $releases $ZipName
    Remove-Item $zipPath -ErrorAction SilentlyContinue
    Compress-Archive -Path $appImage -DestinationPath $zipPath
    Write-Host "Done: $zipPath"
}
finally {
    Pop-Location
}
