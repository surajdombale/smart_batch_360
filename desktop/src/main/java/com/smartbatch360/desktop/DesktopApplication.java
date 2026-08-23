package com.smartbatch360.desktop;

import com.smartbatch360.desktop.client.ClientView;
import com.smartbatch360.desktop.dashboard.DashboardView;
import com.smartbatch360.desktop.driver.DriverView;
import com.smartbatch360.desktop.header.HeaderView;
import com.smartbatch360.desktop.navigation.MainShell;
import com.smartbatch360.desktop.navigation.NavEntry;
import com.smartbatch360.desktop.navigation.NavGroup;
import com.smartbatch360.desktop.navigation.NavItem;
import com.smartbatch360.desktop.server.EmbeddedServer;
import com.smartbatch360.desktop.settings.SettingsView;
import com.smartbatch360.desktop.site.SiteView;
import com.smartbatch360.desktop.vehicle.VehicleView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.Objects;

/**
 * SmartBatch360 desktop client entry point.
 *
 * A single process/install now carries both the JavaFX UI and the Spring
 * Boot backend (REST/JPA/Flyway, unchanged internally) - see
 * com.smartbatch360.desktop.server.EmbeddedServer. The database connection
 * is configured from Settings > Database Connection rather than a separate
 * server executable; there is no longer a standalone desktop-only mode.
 *
 * Phase 1 navigation is Dashboard + the four approved master-data modules
 * (Client - renamed from "Customer" at the user's request 2026-08-23 -
 * /Site/Vehicle/Driver) plus Header, whose fields were clarified directly by
 * the user on 2026-08-17 (not defined in either source document), plus a
 * minimal Settings screen (Database Connection only - not the full future
 * Settings module). Production, Consumption, Batch Reports, Recipes,
 * Analytics, Plant/PLC and Alarm/Event History remain NOT implemented
 * (docs/06_SCOPE_AND_ROADMAP.md, CLAUDE.md.md).
 */
public class DesktopApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        List<NavEntry> navEntries = List.of(
                new NavItem("dashboard", "Dashboard", DashboardView::new),
                new NavGroup("Resources", List.of(
                        new NavItem("clients", "Clients", () -> new ClientView().getView()),
                        new NavItem("sites", "Sites", () -> new SiteView().getView()),
                        new NavItem("vehicles", "Vehicles", () -> new VehicleView().getView()),
                        new NavItem("drivers", "Drivers", () -> new DriverView().getView())
                )),
                new NavItem("headers", "Headers", () -> new HeaderView().getView()),
                new NavItem("settings", "Settings", () -> new SettingsView().getView())
        );

        // Conservative fixed default (rather than a percentage of the detected
        // screen bounds, which is unreliable on virtual/remote displays) so the
        // title bar - and its minimize/maximize/close buttons - never end up
        // pushed off-screen. Still resizable/maximizable up from here.
        final double defaultWidth = 1000;
        final double defaultHeight = 650;

        MainShell shell = new MainShell(navEntries);

        Scene scene = new Scene(shell, defaultWidth, defaultHeight);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/css/theme.css"),
                "css/theme.css not found on classpath").toExternalForm());

        // Explicit native window chrome: title bar with minimize, maximize/restore
        // and close buttons, and a resizable frame so maximize actually works.
        primaryStage.initStyle(StageStyle.DECORATED);
        primaryStage.setResizable(true);

        primaryStage.setTitle("SmartBatch360 - Industrial Batching Plant Management System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(defaultWidth);
        primaryStage.setMinHeight(defaultHeight);
        primaryStage.setX(20);
        primaryStage.setY(20);
        primaryStage.show();
    }

    @Override
    public void stop() {
        // Shuts embedded Tomcat/Spring down cleanly; without this the JVM can
        // linger after the window closes since Tomcat's threads are non-daemon.
        EmbeddedServer.stop();
        System.exit(0);
    }

    public static void main(String[] args) {
        // Off the JavaFX Application Thread and off main() itself: Spring Boot
        // startup takes several seconds, and the UI must not wait on it - the
        // existing per-screen loading/error/retry states already handle "the
        // backend isn't up yet" gracefully.
        Thread.ofVirtual().name("embedded-server-startup").start(EmbeddedServer::startIfConfigured);
        launch(args);
    }
}
