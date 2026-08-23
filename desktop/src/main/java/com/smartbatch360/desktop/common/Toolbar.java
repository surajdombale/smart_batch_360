package com.smartbatch360.desktop.common;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Reusable list-screen toolbar: a search field on the left, Refresh/Add actions on the right. */
public class Toolbar extends HBox {

    private final TextField searchField = new TextField();
    private final Button refreshButton = new Button("Refresh");
    private final Button addButton;

    public Toolbar(String addLabel) {
        getStyleClass().add("toolbar-bar");
        setAlignment(Pos.CENTER_LEFT);

        searchField.getStyleClass().add("search-field");
        searchField.setPromptText("Search...");
        searchField.setPrefWidth(240);

        addButton = new Button(addLabel);
        addButton.getStyleClass().add("button-primary");
        refreshButton.getStyleClass().add("button-secondary");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(searchField, spacer, refreshButton, addButton);
    }

    public TextField getSearchField() {
        return searchField;
    }

    public Button getAddButton() {
        return addButton;
    }

    public Button getRefreshButton() {
        return refreshButton;
    }
}
