package com.smartbatch360.desktop.common;

import com.smartbatch360.desktop.api.ApiErrorDto;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Reusable Add/Edit dialog scaffold for the master-data forms
 * (docs/02_UI_REFERENCE.md "UI implementation rule"): field grid, inline
 * per-field validation messages, a top-level form error banner, and a Save
 * button whose default close behaviour is suppressed until the async save
 * callback (supplied by the caller) completes successfully.
 */
public class FormDialog {

    private final Dialog<ButtonType> dialog = new Dialog<>();
    private final GridPane grid = new GridPane();
    private final Label formError = new Label();
    private final Map<String, Label> fieldErrorLabels = new LinkedHashMap<>();
    private final ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
    private int rowIndex = 0;

    public FormDialog(String title) {
        dialog.setTitle(title);
        dialog.setResizable(false);

        grid.getStyleClass().add("form-grid");
        grid.setMaxWidth(Double.MAX_VALUE);

        formError.getStyleClass().add("field-error");
        formError.setWrapText(true);
        formError.setManaged(false);
        formError.setVisible(false);

        var content = new javafx.scene.layout.VBox(8, formError, grid);

        // Long forms (Batch's 16 fields + materials table being the worst
        // case) could grow taller than the screen with no way to reach the
        // Save button - wrap in a ScrollPane capped to a fraction of the
        // screen's visible height so the dialog always fits on-screen, with
        // scrolling for whatever doesn't. setMaxHeight alone isn't enough:
        // JavaFX sizes the dialog window from the ScrollPane's PREFERRED
        // height (its full, uncapped content height) during the initial
        // sizeToScene() pass, ignoring maxHeight there - setPrefViewportHeight
        // is what actually drives that preferred-size computation down.
        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        double maxHeight = javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() * 0.75;
        scrollPane.setMaxHeight(maxHeight);
        scrollPane.setPrefViewportHeight(maxHeight);

        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/theme.css").toExternalForm());

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.getStyleClass().add("button-primary");
        // Consumed below by the caller's onSave handler so the dialog stays open
        // while the async save request is in flight / on validation failure.
    }

    /** Adds a labeled field row. Returns this for chaining. */
    public FormDialog addField(String labelText, String fieldKey, Node control) {
        Label label = new Label(labelText);
        label.getStyleClass().add("form-label");
        GridPane.setHgrow(control, Priority.ALWAYS);
        if (control instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }

        Label errorLabel = new Label();
        errorLabel.getStyleClass().add("field-error");
        errorLabel.setManaged(false);
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        fieldErrorLabels.put(fieldKey, errorLabel);

        grid.add(label, 0, rowIndex);
        grid.add(control, 1, rowIndex);
        rowIndex++;
        grid.add(errorLabel, 1, rowIndex);
        rowIndex++;

        return this;
    }

    /**
     * Wires the Save button: {@code onSave} performs the (async) save and must
     * return a CompletableFuture-like result via the supplied callbacks - see
     * {@link #wireSave} usage in each feature dialog for the concrete pattern.
     */
    public Button getSaveButton() {
        return (Button) dialog.getDialogPane().lookupButton(saveButtonType);
    }

    /** Suppresses the dialog's default auto-close on Save so callers can control it after their async call completes. */
    public void interceptSaveClose(java.util.function.Consumer<ActionEvent> onSaveClicked) {
        getSaveButton().addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            onSaveClicked.accept(event);
        });
    }

    public void close() {
        dialog.setResult(saveButtonType);
        dialog.close();
    }

    public void showAndWait() {
        dialog.showAndWait();
    }

    public void setFormError(String message) {
        if (message == null) {
            formError.setManaged(false);
            formError.setVisible(false);
            return;
        }
        formError.setText(message);
        formError.setManaged(true);
        formError.setVisible(true);
    }

    public void clearErrors() {
        setFormError(null);
        fieldErrorLabels.values().forEach(label -> {
            label.setManaged(false);
            label.setVisible(false);
            label.setText("");
        });
    }

    public void applyFieldErrors(List<ApiErrorDto.FieldErrorDto> errors) {
        for (ApiErrorDto.FieldErrorDto fieldError : errors) {
            Label label = fieldErrorLabels.get(fieldError.field());
            if (label != null) {
                label.setText(fieldError.message());
                label.setManaged(true);
                label.setVisible(true);
            } else {
                setFormError(fieldError.message());
            }
        }
    }

    public void setSaving(boolean saving) {
        getSaveButton().setDisable(saving);
    }
}
