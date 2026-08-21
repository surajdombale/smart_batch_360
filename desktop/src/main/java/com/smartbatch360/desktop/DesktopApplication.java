package com.smartbatch360.desktop;

import com.smartbatch360.desktop.customer.CustomerView;
import com.smartbatch360.desktop.dashboard.DashboardView;
import com.smartbatch360.desktop.driver.DriverView;
import com.smartbatch360.desktop.header.HeaderView;
import com.smartbatch360.desktop.navigation.MainShell;
import com.smartbatch360.desktop.navigation.NavItem;
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
 * Phase 1 navigation is Dashboard + the four approved master-data modules
 * (Customer/Site/Vehicle/Driver) plus Header, whose fields were clarified
 * directly by the user on 2026-08-17 (not defined in either source
 * document). Production, Consumption, Batch Reports, Recipes, Analytics,
 * Plant/PLC, Settings and Alarm/Event History remain NOT implemented
 * (docs/06_SCOPE_AND_ROADMAP.md, CLAUDE.md.md).
 */
public class DesktopApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        List<NavItem> navItems = List.of(
                new NavItem("dashboard", "Dashboard", DashboardView::new),
                new NavItem("customers", "Customers", () -> new CustomerView().getView()),
                new NavItem("sites", "Sites", () -> new SiteView().getView()),
                new NavItem("vehicles", "Vehicles", () -> new VehicleView().getView()),
                new NavItem("drivers", "Drivers", () -> new DriverView().getView()),
                new NavItem("headers", "Headers", () -> new HeaderView().getView())
        );

        // Conservative fixed default (rather than a percentage of the detected
        // screen bounds, which is unreliable on virtual/remote displays) so the
        // title bar - and its minimize/maximize/close buttons - never end up
        // pushed off-screen. Still resizable/maximizable up from here.
        final double defaultWidth = 1000;
        final double defaultHeight = 650;

        MainShell shell = new MainShell(navItems);

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

    public static void main(String[] args) {
        launch(args);
    }
}
