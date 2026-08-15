package com.smartbatch360.desktop.common;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Reusable page title/subtitle header used at the top of every screen. */
public class PageHeader extends VBox {

    public PageHeader(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-header-title");

        getChildren().add(titleLabel);

        if (subtitle != null && !subtitle.isBlank()) {
            Label subtitleLabel = new Label(subtitle);
            subtitleLabel.getStyleClass().add("page-header-subtitle");
            getChildren().add(subtitleLabel);
        }

        setSpacing(2);
        setStyle("-fx-padding: 0 0 16 0;");
    }
}
