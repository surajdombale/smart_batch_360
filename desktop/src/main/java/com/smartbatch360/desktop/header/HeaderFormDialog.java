package com.smartbatch360.desktop.header;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.FormDialog;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.scene.control.TextField;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Add/Edit dialog for Header. Fields as clarified directly by the user (no
 * source-document mockup exists for this module): Company Name, Plant/Branch
 * Name, Address, City, Pin code, Phone, Email, GSTIN/Tax ID, Supervisor,
 * Mix time, Discharge time, Logo, Status.
 *
 * The last five arrived with the batch report templates, which print them on
 * every report. Mix and discharge times are durations in seconds.
 */
public class HeaderFormDialog {

    private final HeaderApiClient apiClient = new HeaderApiClient();
    private final FormDialog formDialog;
    private final TextField companyNameField = new TextField();
    private final TextField plantNameField = new TextField();
    private final TextField addressField = new TextField();
    private final TextField cityField = new TextField();
    private final TextField pinCodeField = new TextField();
    private final TextField phoneField = new TextField();
    private final TextField emailField = new TextField();
    private final TextField gstinField = new TextField();
    private final TextField supervisorField = new TextField();
    private final TextField mixTimeField = new TextField();
    private final TextField dischargeTimeField = new TextField();
    private final Label logoLabel = new Label();
    private byte[] pendingLogo;
    private String pendingLogoContentType;
    private final ComboBox<HeaderStatus> statusField = new ComboBox<>(FXCollections.observableArrayList(HeaderStatus.values()));

    private final boolean isEdit;
    private final Long id;
    private HeaderDto saved;

    public HeaderFormDialog(HeaderDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;

        formDialog = new FormDialog(isEdit ? "Edit Company" : "Add Company");
        formDialog.addField("Company Name", "companyName", companyNameField);
        formDialog.addField("Plant/Branch Name", "plantName", plantNameField);
        formDialog.addField("Address", "address", addressField);
        formDialog.addField("City", "city", cityField);
        formDialog.addField("Pin Code", "pinCode", pinCodeField);
        formDialog.addField("Phone", "phone", phoneField);
        formDialog.addField("Email", "email", emailField);
        formDialog.addField("GSTIN / Tax ID", "gstin", gstinField);
        formDialog.addField("Supervisor Name", "supervisorName", supervisorField);
        formDialog.addField("Mix Time (seconds)", "mixTimeSeconds", mixTimeField);
        formDialog.addField("Discharge Time (seconds)", "dischargeTimeSeconds", dischargeTimeField);
        formDialog.addField("Company Logo", "logo", buildLogoPicker());
        formDialog.addField("Status", "status", statusField);

        mixTimeField.setPromptText("e.g. 30");
        dischargeTimeField.setPromptText("e.g. 20");
        pinCodeField.setPromptText("6 digits");

        statusField.getSelectionModel().select(HeaderStatus.ACTIVE);
        if (existing != null) {
            companyNameField.setText(existing.companyName());
            plantNameField.setText(existing.plantName());
            addressField.setText(existing.address());
            phoneField.setText(existing.phone());
            emailField.setText(existing.email());
            cityField.setText(existing.city());
            pinCodeField.setText(existing.pinCode());
            gstinField.setText(existing.gstin());
            supervisorField.setText(existing.supervisorName());
            mixTimeField.setText(existing.mixTimeSeconds() != null ? String.valueOf(existing.mixTimeSeconds()) : "");
            dischargeTimeField.setText(existing.dischargeTimeSeconds() != null
                    ? String.valueOf(existing.dischargeTimeSeconds()) : "");
            logoLabel.setText(existing.hasLogo() ? "A logo is set." : "No logo chosen.");
            statusField.getSelectionModel().select(existing.status());
        }

        formDialog.interceptSaveClose(event -> save());
    }

    private void save() {
        formDialog.clearErrors();
        Integer mixTime = parseSecondsOrNull(mixTimeField.getText());
        Integer dischargeTime = parseSecondsOrNull(dischargeTimeField.getText());
        if (mixTime == null && !mixTimeField.getText().isBlank()) {
            formDialog.setFormError("Mix time must be a whole number of seconds.");
            return;
        }
        if (dischargeTime == null && !dischargeTimeField.getText().isBlank()) {
            formDialog.setFormError("Discharge time must be a whole number of seconds.");
            return;
        }

        HeaderRequestDto request = new HeaderRequestDto(
                companyNameField.getText(), plantNameField.getText(), addressField.getText(),
                cityField.getText(), pinCodeField.getText(),
                phoneField.getText(), emailField.getText(), gstinField.getText(),
                supervisorField.getText(), mixTime, dischargeTime, statusField.getValue());

        formDialog.setSaving(true);
        CompletableFuture<HeaderDto> future = isEdit ? apiClient.update(id, request) : apiClient.create(request);

        future.whenComplete((result, throwable) -> Platform.runLater(() -> {
            formDialog.setSaving(false);
            if (throwable != null) {
                handleError(throwable);
            } else if (pendingLogo != null) {
                uploadLogoThenClose(result);
            } else {
                saved = result;
                formDialog.close();
            }
        }));
    }

    /**
     * The image goes up after the company is saved, because a new company has
     * no id until then. A failure here leaves the company saved and says so,
     * rather than pretending the whole save failed.
     */
    private void uploadLogoThenClose(HeaderDto result) {
        formDialog.setSaving(true);
        apiClient.saveLogo(result.id(), new HeaderLogoDto(pendingLogoContentType, pendingLogo))
                .whenComplete((ignored, throwable) -> Platform.runLater(() -> {
                    formDialog.setSaving(false);
                    if (throwable != null) {
                        pendingLogo = null;
                        formDialog.setFormError("The company was saved, but the logo could not be "
                                + "uploaded: " + rootMessage(throwable));
                    } else {
                        saved = result;
                        formDialog.close();
                    }
                }));
    }

    private HBox buildLogoPicker() {
        logoLabel.setText("No logo chosen.");
        logoLabel.getStyleClass().add("state-message");

        Button choose = new Button("Choose Image...");
        choose.getStyleClass().add("button-secondary");
        choose.setOnAction(e -> chooseLogo());

        HBox box = new HBox(10, choose, logoLabel);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void chooseLogo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a company logo");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG or JPEG image", "*.png", "*.jpg", "*.jpeg"));
        File file = chooser.showOpenDialog(null);
        if (file == null) {
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            pendingLogoContentType = file.getName().toLowerCase().endsWith(".png")
                    ? "image/png" : "image/jpeg";
            pendingLogo = bytes;
            logoLabel.setText(file.getName() + " (" + (bytes.length / 1024) + " KB)");
        } catch (IOException ex) {
            formDialog.setFormError("That image could not be read: " + ex.getMessage());
        }
    }

    private Integer parseSecondsOrNull(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        return cause.getMessage() != null ? cause.getMessage() : "please try again.";
    }

    private void handleError(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        if (cause instanceof ApiException apiEx && !apiEx.fieldErrors().isEmpty()) {
            formDialog.applyFieldErrors(apiEx.fieldErrors());
        } else if (cause instanceof ApiException apiEx) {
            formDialog.setFormError(apiEx.getMessage());
        } else {
            formDialog.setFormError("Something went wrong. Please try again.");
        }
    }

    /** Shows the modal dialog; returns the saved record, or empty if the user cancelled. */
    public Optional<HeaderDto> showAndWait() {
        formDialog.showAndWait();
        return Optional.ofNullable(saved);
    }
}
