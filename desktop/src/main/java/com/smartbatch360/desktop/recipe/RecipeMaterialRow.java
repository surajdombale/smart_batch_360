package com.smartbatch360.desktop.recipe;

import com.smartbatch360.desktop.material.MaterialDto;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.math.BigDecimal;

/**
 * One "Add Material" line in RecipeFormDialog: which Material was picked and
 * how much of it.
 *
 * As of 2026-08-27 the material is a real MaterialDto reference rather than a
 * typed-in name/unit pair - the unit now comes from the material itself, so it
 * can't be entered inconsistently. Quantity stays a String (parsed/validated on
 * save) to keep it a plain text cell.
 */
public class RecipeMaterialRow {

    private final ObjectProperty<MaterialDto> material = new SimpleObjectProperty<>();
    private final StringProperty quantity = new SimpleStringProperty("");

    public RecipeMaterialRow() {
    }

    public RecipeMaterialRow(MaterialDto material, String quantity) {
        this.material.set(material);
        this.quantity.set(quantity);
    }

    public ObjectProperty<MaterialDto> materialProperty() {
        return material;
    }

    public StringProperty quantityProperty() {
        return quantity;
    }

    public MaterialDto getMaterial() {
        return material.get();
    }

    public String getQuantity() {
        return quantity.get();
    }

    /** The unit label shown alongside the quantity - always the material's own unit. */
    public String getUnitLabel() {
        return material.get() != null ? material.get().unit().name() : "";
    }

    /** Parsed quantity, or null when blank/invalid/not positive. */
    public BigDecimal parsedQuantity() {
        String text = quantity.get();
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
}
