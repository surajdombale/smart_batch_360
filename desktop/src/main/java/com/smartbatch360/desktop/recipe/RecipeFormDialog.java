package com.smartbatch360.desktop.recipe;

import com.smartbatch360.desktop.api.ApiException;
import com.smartbatch360.desktop.common.FormDialog;
import com.smartbatch360.desktop.material.MaterialApiClient;
import com.smartbatch360.desktop.material.MaterialDto;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Add/Edit dialog for Recipe. The user names the recipe, then adds material
 * lines - pick a real Material record, type how much of it.
 *
 * The material lines are laid out as line items (a header row, one row per
 * material, an "Add material" link underneath) after the style of Stripe's
 * invoice editor, at the user's request 2026-09-07.
 *
 * That rework also fixed the bug it was reported alongside. The lines used to
 * be an editable TableView, and a JavaFX table cell only writes its value back
 * to the model when the edit is COMMITTED with Enter - clicking Save instead
 * cancels the edit and throws the text away. So typing a quantity and pressing
 * Save discarded it, and the recipe was rejected for having no quantities on
 * rows that visibly had them. Every control here is a real, always-live field
 * bound to its row, so there is no edit mode to commit or lose.
 *
 * The total shown is a live PREVIEW. The stored value is always the one the
 * backend derives on save (Recipe#recalculateTotalBatchQuantity); this mirrors
 * that formula from the same inputs (material density / 1000 L per m3).
 */
public class RecipeFormDialog {

    private static final BigDecimal LITRES_PER_CUBIC_METRE = new BigDecimal("1000");
    private static final int CONVERSION_SCALE = 10;
    private static final int TOTAL_SCALE = 4;

    /** Column widths, shared by the header and every line so they line up. */
    private static final double QUANTITY_WIDTH = 80;
    private static final double UNIT_WIDTH = 46;
    private static final double VOLUME_WIDTH = 88;
    private static final double REMOVE_WIDTH = 30;
    private static final double PICKER_MIN_WIDTH = 170;
    private static final double COLUMN_GAP = 8;
    /** Sum of the columns and their gaps - the width the dialog has to give us. */
    private static final double EDITOR_WIDTH =
            PICKER_MIN_WIDTH + QUANTITY_WIDTH + UNIT_WIDTH + VOLUME_WIDTH + REMOVE_WIDTH + (4 * COLUMN_GAP);

    private final RecipeApiClient apiClient = new RecipeApiClient();
    private final MaterialApiClient materialApiClient = new MaterialApiClient();

    private final FormDialog formDialog;
    private final TextField nameField = new TextField();
    private final TextField descriptionField = new TextField();
    private final ComboBox<RecipeStatus> statusField =
            new ComboBox<>(FXCollections.observableArrayList(RecipeStatus.values()));

    private final ObservableList<MaterialDto> availableMaterials = FXCollections.observableArrayList();
    private final List<RecipeMaterialRow> materialRows = new ArrayList<>();
    /** Each line's model row against the node showing it, so removal can drop both. */
    private final Map<RecipeMaterialRow, Node> lineNodes = new LinkedHashMap<>();
    private final VBox linesBox = new VBox();
    private final Label totalLabel = new Label();
    private final Label totalNoteLabel = new Label();

    private final boolean isEdit;
    private final Long id;
    private final List<RecipeMaterialDto> existingMaterials;
    private RecipeDto saved;

    public RecipeFormDialog(RecipeDto existing) {
        this.isEdit = existing != null;
        this.id = existing != null ? existing.id() : null;
        this.existingMaterials = existing != null ? existing.materials() : List.of();

        formDialog = new FormDialog(isEdit ? "Edit Recipe" : "Add Recipe");
        formDialog.addField("Recipe Name", "name", nameField);
        formDialog.addField("Description", "description", descriptionField);
        formDialog.addField("Status", "status", statusField);
        formDialog.addField("Materials", "materials", buildMaterialsEditor());

        statusField.getSelectionModel().select(RecipeStatus.ACTIVE);
        if (existing != null) {
            nameField.setText(existing.name());
            descriptionField.setText(existing.description());
            statusField.getSelectionModel().select(existing.status());
        }

        loadMaterials();
        refreshTotal();

        formDialog.interceptSaveClose(event -> save());
    }

    // ---------------------------------------------------------------- layout

    private VBox buildMaterialsEditor() {
        Label addLink = new Label("+ Add material");
        addLink.getStyleClass().add("link-action");
        addLink.setOnMouseClicked(e -> {
            RecipeMaterialRow row = addLine(new RecipeMaterialRow());
            focusMaterial(row);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label totalCaption = new Label("Total");
        totalCaption.getStyleClass().add("line-total-caption");
        totalLabel.getStyleClass().add("line-total-value");

        HBox footer = new HBox(8, addLink, spacer, totalCaption, totalLabel);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setMaxWidth(Double.MAX_VALUE);
        footer.getStyleClass().add("line-footer");

        totalNoteLabel.getStyleClass().add("state-message");
        totalNoteLabel.setWrapText(true);
        totalNoteLabel.setManaged(false);
        totalNoteLabel.setVisible(false);

        linesBox.setMaxWidth(Double.MAX_VALUE);
        linesBox.setFillWidth(true);

        VBox editor = new VBox(buildHeader(), linesBox, footer, totalNoteLabel);
        editor.setPrefWidth(EDITOR_WIDTH);
        editor.getStyleClass().add("line-items");
        return editor;
    }

    private HBox buildHeader() {
        HBox header = new HBox(COLUMN_GAP,
                headerLabel("MATERIAL", -1),
                headerLabel("QUANTITY", QUANTITY_WIDTH),
                headerLabel("UNIT", UNIT_WIDTH),
                headerLabel("VOLUME", VOLUME_WIDTH),
                headerLabel("", REMOVE_WIDTH));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMaxWidth(Double.MAX_VALUE);
        header.getStyleClass().add("line-header");
        return header;
    }

    private Label headerLabel(String text, double width) {
        Label label = new Label(text);
        label.getStyleClass().add("line-header-label");
        if (width < 0) {
            HBox.setHgrow(label, Priority.ALWAYS);
            label.setMaxWidth(Double.MAX_VALUE);
        } else {
            label.setMinWidth(width);
            label.setPrefWidth(width);
            label.setMaxWidth(width);
        }
        return label;
    }

    /**
     * One material line. Everything is a live control bound straight to the
     * row - selecting a material or typing a quantity updates the row, its own
     * volume and the total immediately, with nothing to commit.
     */
    private RecipeMaterialRow addLine(RecipeMaterialRow row) {
        ComboBox<MaterialDto> picker = materialPicker();
        HBox.setHgrow(picker, Priority.ALWAYS);
        picker.setMinWidth(PICKER_MIN_WIDTH);
        picker.setMaxWidth(Double.MAX_VALUE);

        TextField quantityField = new TextField(row.getQuantity());
        quantityField.setPromptText("0");
        quantityField.setPrefWidth(QUANTITY_WIDTH);
        quantityField.setMinWidth(QUANTITY_WIDTH);
        quantityField.setMaxWidth(QUANTITY_WIDTH);
        quantityField.setAlignment(Pos.CENTER_RIGHT);

        Label unitLabel = fixedLabel(UNIT_WIDTH, "line-unit");
        Label volumeLabel = fixedLabel(VOLUME_WIDTH, "line-volume");
        volumeLabel.setAlignment(Pos.CENTER_RIGHT);

        Button remove = new Button("✕");
        remove.getStyleClass().add("line-remove");
        remove.setMinWidth(REMOVE_WIDTH);
        remove.setPrefWidth(REMOVE_WIDTH);
        remove.setTooltip(new Tooltip("Remove this material"));

        HBox line = new HBox(COLUMN_GAP, picker, quantityField, unitLabel, volumeLabel, remove);
        line.setAlignment(Pos.CENTER_LEFT);
        line.setMaxWidth(Double.MAX_VALUE);
        line.getStyleClass().add("line-row");

        Runnable refresh = () -> {
            MaterialDto material = row.getMaterial();
            unitLabel.setText(material == null ? "" : material.unit().name());
            volumeLabel.setText(describeVolume(row));
            quantityField.pseudoClassStateChanged(INVALID, isQuantityInvalid(row));
            refreshTotal();
        };

        picker.setValue(row.getMaterial());
        picker.valueProperty().addListener((o, was, is) -> {
            row.materialProperty().set(is);
            refresh.run();
        });
        // No listener on Enter or focus-loss: the row is written on every
        // keystroke, which is the whole point of dropping the editable table.
        quantityField.textProperty().addListener((o, was, is) -> {
            row.quantityProperty().set(is);
            refresh.run();
        });
        remove.setOnAction(e -> removeLine(row));

        refresh.run();

        materialRows.add(row);
        lineNodes.put(row, line);
        linesBox.getChildren().add(line);
        return row;
    }

    private void removeLine(RecipeMaterialRow row) {
        Node node = lineNodes.remove(row);
        if (node != null) {
            linesBox.getChildren().remove(node);
        }
        materialRows.remove(row);
        // Never leave the editor with nothing to type into.
        if (materialRows.isEmpty()) {
            addLine(new RecipeMaterialRow());
        }
        refreshTotal();
    }

    private Label fixedLabel(double width, String styleClass) {
        Label label = new Label();
        label.getStyleClass().add(styleClass);
        label.setMinWidth(width);
        label.setPrefWidth(width);
        label.setMaxWidth(width);
        return label;
    }

    private void focusMaterial(RecipeMaterialRow row) {
        Node node = lineNodes.get(row);
        if (node instanceof HBox line && !line.getChildren().isEmpty()) {
            Platform.runLater(() -> line.getChildren().get(0).requestFocus());
        }
    }

    /**
     * A material picker you can type into to narrow the list, after the
     * reference's "Find or add a product" box. New materials are still created
     * under Materials - they need a unit and a density, which is more than
     * belongs on one line here.
     */
    private ComboBox<MaterialDto> materialPicker() {
        ComboBox<MaterialDto> combo = new ComboBox<>();
        FilteredList<MaterialDto> filtered = new FilteredList<>(availableMaterials, m -> true);
        combo.setItems(filtered);
        combo.setEditable(true);
        combo.setPromptText("Find a material...");
        combo.getStyleClass().add("line-picker");

        combo.setConverter(new StringConverter<>() {
            @Override
            public String toString(MaterialDto material) {
                return material == null ? "" : material.name();
            }

            @Override
            public MaterialDto fromString(String text) {
                if (text == null || text.isBlank()) {
                    return null;
                }
                // Half-typed text must not null out a good selection: the
                // editor here is a search box, not a second source of truth.
                return availableMaterials.stream()
                        .filter(m -> m.name().equalsIgnoreCase(text.trim()))
                        .findFirst()
                        .orElseGet(combo::getValue);
            }
        });

        combo.getEditor().setOnKeyReleased(event -> {
            KeyCode code = event.getCode();
            if (code == KeyCode.UP || code == KeyCode.DOWN || code == KeyCode.ENTER
                    || code == KeyCode.ESCAPE || code == KeyCode.TAB || code.isModifierKey()) {
                return;
            }
            String text = combo.getEditor().getText();
            filtered.setPredicate(m -> text == null || text.isBlank()
                    || m.name().toLowerCase().contains(text.toLowerCase().trim()));
            if (!combo.isShowing()) {
                combo.show();
            }
        });
        // Reopening should offer everything again, not the last search.
        combo.setOnHidden(event -> filtered.setPredicate(m -> true));

        return combo;
    }

    // ------------------------------------------------------------ data / math

    private void loadMaterials() {
        materialApiClient.list().whenComplete((materials, throwable) -> Platform.runLater(() -> {
            List<MaterialDto> items = throwable == null ? materials : List.of();
            availableMaterials.setAll(items);

            // Re-link an edited recipe's saved lines to the loaded material
            // instances, so each picker shows its material as selected.
            if (materialRows.isEmpty() && !existingMaterials.isEmpty()) {
                for (RecipeMaterialDto line : existingMaterials) {
                    items.stream()
                            .filter(m -> m.id().equals(line.materialId()))
                            .findFirst()
                            .ifPresent(m -> addLine(new RecipeMaterialRow(
                                    m, line.quantity() != null ? line.quantity().toPlainString() : "")));
                }
            }
            if (materialRows.isEmpty()) {
                addLine(new RecipeMaterialRow());
            }
            if (items.isEmpty()) {
                formDialog.setFormError("No materials exist yet. Add materials before creating a recipe.");
            }
            refreshTotal();
        }));
    }

    /** This line's contribution in m3, or null when it isn't usable yet. */
    private BigDecimal volumeOf(RecipeMaterialRow row) {
        MaterialDto material = row.getMaterial();
        BigDecimal quantity = row.parsedQuantity();
        if (material == null || quantity == null) {
            return null;
        }
        BigDecimal divisor;
        if (material.unit().requiresDensity()) {
            if (material.densityKgPerM3() == null
                    || material.densityKgPerM3().compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            divisor = material.densityKgPerM3();
        } else {
            divisor = LITRES_PER_CUBIC_METRE;
        }
        return quantity.divide(divisor, CONVERSION_SCALE, RoundingMode.HALF_UP);
    }

    private String describeVolume(RecipeMaterialRow row) {
        BigDecimal volume = volumeOf(row);
        return volume == null ? "—" : volume.setScale(TOTAL_SCALE, RoundingMode.HALF_UP).toPlainString();
    }

    private static boolean isBlank(String text) {
        return text == null || text.isBlank();
    }

    /** True only for text that was actually typed and cannot be used. */
    private boolean isQuantityInvalid(RecipeMaterialRow row) {
        String text = row.getQuantity();
        return text != null && !text.isBlank() && row.parsedQuantity() == null;
    }

    private void refreshTotal() {
        BigDecimal total = BigDecimal.ZERO;
        boolean incomplete = false;
        boolean missingDensity = false;

        for (RecipeMaterialRow row : materialRows) {
            BigDecimal volume = volumeOf(row);
            if (volume != null) {
                total = total.add(volume);
                continue;
            }
            MaterialDto material = row.getMaterial();
            if (material != null && material.unit().requiresDensity() && row.parsedQuantity() != null) {
                missingDensity = true;
            } else if (material != null || !isBlank(row.getQuantity())) {
                // Only half-filled lines are worth mentioning. An untouched
                // one is just the empty line waiting to be used, and saying
                // so on a form nobody has typed in yet reads as an error.
                incomplete = true;
            }
        }

        totalLabel.setText(total.setScale(TOTAL_SCALE, RoundingMode.HALF_UP).toPlainString() + " m³");

        String note = "";
        if (missingDensity) {
            note = "Some materials have no density set, so they add nothing here - fix them under Materials.";
        } else if (incomplete) {
            note = "Lines without both a material and a quantity are not counted.";
        }
        totalNoteLabel.setText(note);
        totalNoteLabel.setVisible(!note.isEmpty());
        totalNoteLabel.setManaged(!note.isEmpty());
    }

    // ----------------------------------------------------------------- saving

    private void save() {
        formDialog.clearErrors();

        List<RecipeMaterialRequestDto> materials = new ArrayList<>();
        for (RecipeMaterialRow row : materialRows) {
            if (row.getMaterial() == null && (row.getQuantity() == null || row.getQuantity().isBlank())) {
                continue; // untouched blank line
            }
            BigDecimal quantity = row.parsedQuantity();
            if (row.getMaterial() == null) {
                formDialog.setFormError("Pick a material on every line, or remove the line.");
                return;
            }
            if (quantity == null) {
                formDialog.setFormError("Enter a quantity greater than zero for "
                        + row.getMaterial().name() + ", or remove the line.");
                return;
            }
            materials.add(new RecipeMaterialRequestDto(row.getMaterial().id(), quantity));
        }
        if (materials.isEmpty()) {
            formDialog.setFormError("At least one material is required.");
            return;
        }

        RecipeRequestDto request = new RecipeRequestDto(
                nameField.getText(), descriptionField.getText(), statusField.getValue(), materials);

        formDialog.setSaving(true);
        CompletableFuture<RecipeDto> future = isEdit ? apiClient.update(id, request) : apiClient.create(request);

        future.whenComplete((result, throwable) -> Platform.runLater(() -> {
            formDialog.setSaving(false);
            if (throwable != null) {
                handleError(throwable);
            } else {
                saved = result;
                formDialog.close();
            }
        }));
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

    public Optional<RecipeDto> showAndWait() {
        formDialog.showAndWait();
        return Optional.ofNullable(saved);
    }

    private static final javafx.css.PseudoClass INVALID = javafx.css.PseudoClass.getPseudoClass("invalid");
}
