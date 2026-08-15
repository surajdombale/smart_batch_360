package com.smartbatch360.desktop.navigation;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Application shell: left navigation sidebar + swappable content area.
 * Phase 1 restricts the nav to the approved modules only - Production,
 * Consumption, Reports, Recipes, Analytics, PLC, Settings, Alarms and Header
 * are intentionally absent (docs/06_SCOPE_AND_ROADMAP.md).
 */
public class MainShell extends BorderPane {

    private final VBox sidebar = new VBox();
    private final ScrollPane contentScroll = new ScrollPane();

    public MainShell(List<NavItem> navItems) {
        getStyleClass().add("app-shell");

        Label brand = new Label("SmartBatch360");
        brand.getStyleClass().add("nav-brand");

        sidebar.getStyleClass().add("nav-sidebar");
        sidebar.getChildren().add(brand);

        contentScroll.setFitToWidth(true);
        contentScroll.setFitToHeight(true);

        List<Button> navButtons = new ArrayList<>();
        for (NavItem item : navItems) {
            Button button = new Button(item.label());
            button.getStyleClass().add("nav-item");
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            navButtons.add(button);
        }

        for (int i = 0; i < navItems.size(); i++) {
            NavItem item = navItems.get(i);
            Button button = navButtons.get(i);
            button.setOnAction(e -> select(item, button, navButtons));
        }

        sidebar.getChildren().addAll(navButtons);

        setLeft(sidebar);
        setCenter(contentScroll);

        if (!navItems.isEmpty()) {
            select(navItems.get(0), navButtons.get(0), navButtons);
        }
    }

    private void select(NavItem item, Button selectedButton, List<Button> allButtons) {
        allButtons.forEach(b -> b.getStyleClass().remove("nav-item-selected"));
        selectedButton.getStyleClass().add("nav-item-selected");
        contentScroll.setContent(item.viewFactory().get());
    }
}
