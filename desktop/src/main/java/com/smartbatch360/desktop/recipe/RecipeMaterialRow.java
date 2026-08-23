package com.smartbatch360.desktop.recipe;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Mutable, editable-TableView-friendly row backing the Material Proportion
 * editor in RecipeFormDialog. Quantity stays a String here (parsed/validated
 * on save) rather than a numeric property - simpler than wiring a
 * NumberStringConverter for what's ultimately just a text cell.
 */
public class RecipeMaterialRow {

    private final StringProperty materialName = new SimpleStringProperty("");
    private final StringProperty quantity = new SimpleStringProperty("");
    private final StringProperty unit = new SimpleStringProperty("");

    public RecipeMaterialRow() {
    }

    public RecipeMaterialRow(String materialName, String quantity, String unit) {
        this.materialName.set(materialName);
        this.quantity.set(quantity);
        this.unit.set(unit);
    }

    public StringProperty materialNameProperty() {
        return materialName;
    }

    public StringProperty quantityProperty() {
        return quantity;
    }

    public StringProperty unitProperty() {
        return unit;
    }

    public String getMaterialName() {
        return materialName.get();
    }

    public String getQuantity() {
        return quantity.get();
    }

    public String getUnit() {
        return unit.get();
    }
}
