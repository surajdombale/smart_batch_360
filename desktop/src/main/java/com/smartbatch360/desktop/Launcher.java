package com.smartbatch360.desktop;

/**
 * Separate entry point that does NOT extend javafx.application.Application.
 *
 * When a packaged (jpackage/java -jar) app's main class directly extends
 * Application, the java launcher requires JavaFX on the module-path and
 * refuses to start otherwise ("JavaFX runtime components are missing"), even
 * though the JavaFX jars are present on the classpath. Routing through this
 * plain class avoids that check. Use Launcher as the packaged entry point;
 * DesktopApplication remains the actual JavaFX Application.
 */
public class Launcher {

    public static void main(String[] args) {
        DesktopApplication.main(args);
    }
}
