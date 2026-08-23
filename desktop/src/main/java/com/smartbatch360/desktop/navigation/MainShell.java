package com.smartbatch360.desktop.navigation;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

        ImageView logo = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/images/app-icon.png"),
                "images/app-icon.png not found on classpath")));
        logo.setFitWidth(28);
        logo.setFitHeight(28);
        logo.setPreserveRatio(true);
        logo.getStyleClass().add("nav-brand-logo");

        Label brandText = new Label("SmartBatch");
        brandText.getStyleClass().add("nav-brand");

        Label brandAccent = new Label("360");
        brandAccent.getStyleClass().add("nav-brand-accent");

        HBox brandLabels = new HBox(brandText, brandAccent);
        brandLabels.setAlignment(Pos.CENTER_LEFT);

        HBox brand = new HBox(10, logo, brandLabels);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("nav-brand-row");

        sidebar.getStyleClass().add("nav-sidebar");
        sidebar.getChildren().add(brand);

        contentScroll.setFitToWidth(true);
        contentScroll.setFitToHeight(true);

        for (NavEntry entry : navEntries) {
            if (entry instanceof NavItem item) {
                sidebar.getChildren().add(navButton(item, false));
            } else if (entry instanceof NavGroup group) {
                sidebar.getChildren().add(collapsibleGroup(group));
            }
        }

        setLeft(sidebar);
        setCenter(contentScroll);

        if (!allNavButtons.isEmpty()) {
            allNavButtons.get(0).fire();
        }
    }

    /**
     * A group heading (e.g. "Resources") that collapses/expands its child
     * nav items on click - requested by the user 2026-08-23. Starts expanded
     * so nothing changes on first launch; the chevron flips to reflect state.
     */
    private VBox collapsibleGroup(NavGroup group) {
        VBox items = new VBox();
        for (NavItem item : group.children()) {
            items.getChildren().add(navButton(item, true));
        }

        Label groupText = new Label(group.label().toUpperCase());
        groupText.getStyleClass().add("nav-group-label-text");

        Label chevron = new Label("▾"); // small down-pointing triangle
        chevron.getStyleClass().add("nav-group-chevron");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(groupText, spacer, chevron);
        header.getStyleClass().add("nav-group-label");
        header.setOnMouseClicked(e -> {
            boolean expanding = !items.isVisible();
            items.setVisible(expanding);
            items.setManaged(expanding);
            chevron.setText(expanding ? "▾" : "▸");
        });

        VBox groupBox = new VBox(header, items);
        return groupBox;
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
