package com.smartbatch360.desktop.order;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.client.ClientApiClient;
import com.smartbatch360.desktop.client.ClientDto;
import com.smartbatch360.desktop.common.FormDialog;
import com.smartbatch360.desktop.recipe.RecipeApiClient;
import com.smartbatch360.desktop.recipe.RecipeDto;
import com.smartbatch360.desktop.site.SiteApiClient;
import com.smartbatch360.desktop.site.SiteDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Create Order: Customer, Site, Recipe, quantity in m³ (docs flow, user
 * request 2026-08-27). Status isn't asked for - a new order is always
 * UNFULFILLED.
 *
 * The Site list narrows to the chosen Customer's sites, since a site belongs
 * to exactly one client and the backend rejects a mismatch anyway.
 */
public class OrderFormDialog {

    private final OrderApiClient apiClient = new OrderApiClient();
    private final ClientApiClient clientApiClient = new ClientApiClient();
    private final SiteApiClient siteApiClient = new SiteApiClient();
    private final RecipeApiClient recipeApiClient = new RecipeApiClient();

    private final FormDialog formDialog = new FormDialog("Create Order");
    private final ComboBox<ClientDto> clientField = new ComboBox<>();
    private final ComboBox<SiteDto> siteField = new ComboBox<>();
    private final ComboBox<RecipeDto> recipeField = new ComboBox<>();
    private final TextField quantityField = new TextField();

    private List<SiteDto> allSites = List.of();
    private OrderDto saved;

    public OrderFormDialog() {
        formDialog.addField("Customer", "clientId", clientField);
        formDialog.addField("Site", "siteId", siteField);
        formDialog.addField("Recipe", "recipeId", recipeField);
        formDialog.addField("Order Quantity (m³)", "quantityM3", quantityField);

        quantityField.setPromptText("e.g. 25");
        clientField.valueProperty().addListener((obs, old, client) -> narrowSitesTo(client));

        loadReferenceLists();
        formDialog.interceptSaveClose(event -> save());
    }

    private void narrowSitesTo(ClientDto client) {
        siteField.getSelectionModel().clearSelection();
        List<SiteDto> visible = client == null
                ? List.of()
                : allSites.stream().filter(s -> client.id().equals(s.clientId())).toList();
        siteField.setItems(FXCollections.observableArrayList(visible));
        siteField.setPromptText(client == null
                ? "Select a customer first"
                : (visible.isEmpty() ? "This customer has no sites" : "Select a site"));
    }

    private void loadReferenceLists() {
        clientField.setDisable(true);
        clientApiClient.list().whenComplete((clients, throwable) -> Platform.runLater(() -> {
            List<ClientDto> items = throwable == null ? clients : List.of();
            clientField.setItems(FXCollections.observableArrayList(items));
            clientField.setDisable(false);
            if (items.isEmpty()) {
                formDialog.setFormError("No customers exist yet. Add one before creating an order.");
            }
        }));

        siteApiClient.list().whenComplete((sites, throwable) -> Platform.runLater(() -> {
            allSites = throwable == null ? sites : List.of();
            narrowSitesTo(clientField.getValue());
        }));

        recipeField.setDisable(true);
        recipeApiClient.list().whenComplete((recipes, throwable) -> Platform.runLater(() -> {
            List<RecipeDto> items = throwable == null ? recipes : List.of();
            recipeField.setItems(FXCollections.observableArrayList(items));
            recipeField.setDisable(false);
            if (items.isEmpty()) {
                formDialog.setFormError("No recipes exist yet. Add one before creating an order.");
            }
        }));
    }

    private void save() {
        formDialog.clearErrors();

        ClientDto client = clientField.getValue();
        SiteDto site = siteField.getValue();
        RecipeDto recipe = recipeField.getValue();
        if (client == null || site == null || recipe == null) {
            formDialog.setFormError("Customer, Site and Recipe are all required.");
            return;
        }

        BigDecimal quantity = parseDecimal(quantityField.getText());
        if (quantity == null) {
            formDialog.setFormError("Order quantity must be a valid number greater than zero.");
            return;
        }

        formDialog.setSaving(true);
        apiClient.create(new OrderRequestDto(client.id(), site.id(), recipe.id(), quantity))
                .whenComplete((result, throwable) -> Platform.runLater(() -> {
                    formDialog.setSaving(false);
                    if (throwable != null) {
                        handleError(throwable);
                    } else {
                        saved = result;
                        formDialog.close();
                    }
                }));
    }

    private BigDecimal parseDecimal(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            BigDecimal value = new BigDecimal(text.trim());
            return value.compareTo(BigDecimal.ZERO) > 0 ? value : null;
        } catch (NumberFormatException e) {
            return null;
        }
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

    public Optional<OrderDto> showAndWait() {
        formDialog.showAndWait();
        return Optional.ofNullable(saved);
    }
}
