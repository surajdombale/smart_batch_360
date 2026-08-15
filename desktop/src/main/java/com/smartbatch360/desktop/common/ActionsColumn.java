package com.smartbatch360.desktop.common;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.HBox;

import java.util.function.Consumer;

/** Reusable Edit/Delete row-actions column for the master-data tables. */
public final class ActionsColumn {

    private ActionsColumn() {
    }

    public static <T> TableColumn<T, Void> create(Consumer<T> onEdit, Consumer<T> onDelete) {
        TableColumn<T, Void> column = new TableColumn<>("Actions");
        column.setSortable(false);
        column.setMinWidth(150);

        column.setCellFactory(col -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deleteButton = new Button("Delete");
            private final HBox box = new HBox(6, editButton, deleteButton);

            {
                editButton.getStyleClass().add("button-secondary");
                deleteButton.getStyleClass().add("button-danger");
                editButton.setOnAction(e -> onEdit.accept(rowItem()));
                deleteButton.setOnAction(e -> onDelete.accept(rowItem()));
            }

            private T rowItem() {
                return getTableView().getItems().get(getIndex());
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        return column;
    }
}
