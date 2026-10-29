package com.smartbatch360.desktop.common;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/** Reusable destructive-action confirmation (docs/05_CRUD_SPECIFICATION.md: "Do not silently delete records."). */
public final class ConfirmDialogs {

    private ConfirmDialogs() {
    }

    public static boolean confirmDelete(String entityLabel, String recordDescription) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm delete");
        alert.setHeaderText("Delete " + entityLabel + "?");
        alert.setContentText("This will permanently delete \"" + recordDescription + "\". This action cannot be undone.");
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(result -> result == ButtonType.OK).isPresent();
    }

    /** Any other action that cannot be taken back, such as stopping a running batch. */
    public static boolean confirm(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        return alert.showAndWait().filter(result -> result == ButtonType.OK).isPresent();
    }
}
