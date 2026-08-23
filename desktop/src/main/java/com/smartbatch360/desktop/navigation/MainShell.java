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
 * Consumption, Reports, Recipes, Analytics, PLC and Alarms are
 * intentionally absent (docs/06_SCOPE_AND_ROADMAP.md).
 *
 * Entries can be flat (NavItem) or grouped under a heading (NavGroup) - e.g.
 * Client/Site/Vehicle/Driver under "Resources", requested by the user
 * 2026-08-23 "to make it look good". Selection/highlighting works
 * identically either way; grouping is purely visual organization.
 */
public class MainShell extends BorderPane {

    private final VBox sidebar = new VBox();
    private final ScrollPane contentScroll = new ScrollPane();
    private final List<Button> allNavButtons = new ArrayList<>();

    public MainShell(List<NavEntry> navEntries) {
        getStyleClass().add("app-shell");

        Label brand = new Label("SmartBatch360");
        brand.getStyleClass().add("nav-brand");

        sidebar.getStyleClass().add("nav-sidebar");
        sidebar.getChildren().add(brand);

        contentScroll.setFitToWidth(true);
        contentScroll.setFitToHeight(true);

        for (NavEntry entry : navEntries) {
            if (entry instanceof NavItem item) {
                sidebar.getChildren().add(navButton(item, false));
            } else if (entry instanceof NavGroup group) {
                Label groupLabel = new Label(group.label().toUpperCase());
                groupLabel.getStyleClass().add("nav-group-label");
                sidebar.getChildren().add(groupLabel);
                for (NavItem item : group.children()) {
                    sidebar.getChildren().add(navButton(item, true));
                }
            }
        }

        setLeft(sidebar);
        setCenter(contentScroll);

        if (!allNavButtons.isEmpty()) {
            allNavButtons.get(0).fire();
        }
    }

    private Button navButton(NavItem item, boolean nested) {
        Button button = new Button(item.label());
        button.getStyleClass().add("nav-item");
        if (nested) {
            button.getStyleClass().add("nav-item-nested");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        button.setOnAction(e -> select(item, button));
        allNavButtons.add(button);
        return button;
    }

    private void select(NavItem item, Button selectedButton) {
        allNavButtons.forEach(b -> b.getStyleClass().remove("nav-item-selected"));
        selectedButton.getStyleClass().add("nav-item-selected");
        contentScroll.setContent(item.viewFactory().get());
    }
}
