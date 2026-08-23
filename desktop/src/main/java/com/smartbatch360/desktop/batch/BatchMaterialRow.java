package com.smartbatch360.desktop.batch;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Mutable, editable-TableView-friendly row backing the Material Consumption editor in BatchFormDialog. */
public class BatchMaterialRow {

    private final StringProperty materialName = new SimpleStringProperty("");
    private final StringProperty target = new SimpleStringProperty("");
    private final StringProperty setpoint = new SimpleStringProperty("");
    private final StringProperty achieved = new SimpleStringProperty("");
    private final StringProperty unit = new SimpleStringProperty("");

    public BatchMaterialRow() {
    }

    public BatchMaterialRow(String materialName, String target, String setpoint, String achieved, String unit) {
        this.materialName.set(materialName);
        this.target.set(target);
        this.setpoint.set(setpoint);
        this.achieved.set(achieved);
        this.unit.set(unit);
    }

    public StringProperty materialNameProperty() {
        return materialName;
    }

    public StringProperty targetProperty() {
        return target;
    }

    public StringProperty setpointProperty() {
        return setpoint;
    }

    public StringProperty achievedProperty() {
        return achieved;
    }

    public StringProperty unitProperty() {
        return unit;
    }

    public String getMaterialName() {
        return materialName.get();
    }

    public String getTarget() {
        return target.get();
    }

    public String getSetpoint() {
        return setpoint.get();
    }

    public String getAchieved() {
        return achieved.get();
    }

    public String getUnit() {
        return unit.get();
    }
}
