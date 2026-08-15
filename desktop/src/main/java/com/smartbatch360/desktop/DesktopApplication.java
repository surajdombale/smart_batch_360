package com.smartbatch360.desktop;

import com.smartbatch360.desktop.customer.CustomerView;
import com.smartbatch360.desktop.dashboard.DashboardView;
import com.smartbatch360.desktop.driver.DriverView;
import com.smartbatch360.desktop.navigation.MainShell;
import com.smartbatch360.desktop.navigation.NavItem;
import com.smartbatch360.desktop.site.SiteView;
import com.smartbatch360.desktop.vehicle.VehicleView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;

/**
 * SmartBatch360 desktop client entry point.
 *
 * Phase 1 navigation is intentionally limited to Dashboard + the four
 * approved master-data modules. Production, Consumption, Batch Reports,
 * Recipes, Analytics, Plant/PLC, Settings, Alarm/Event History and Header are
 * NOT implemented in this phase (docs/06_SCOPE_AND_ROADMAP.md, CLAUDE.md.md).
 */
public class DesktopApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
        List<NavItem> navItems = List.of(
                new NavItem("dashboard", "Dashboard", DashboardView::new),
                new NavItem("customers", "Customers", () -> new CustomerView().getView()),
                new NavItem("sites", "Sites", () -> new SiteView().getView()),
                new NavItem("vehicles", "Vehicles", () -> new VehicleView().getView()),
                new NavItem("drivers", "Drivers", () -> new DriverView().getView())
        );

        MainShell shell = new MainShell(navItems);

        Scene scene = new Scene(shell, 1200, 800);
        scene.getStylesheets().add(Objects.requireNonNull(
                getClass().getResource("/css/theme.css"),
                "css/theme.css not found on classpath").toExternalForm());

        primaryStage.setTitle("SmartBatch360 - Industrial Batching Plant Management System");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
