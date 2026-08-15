package com.smartbatch360.desktop.common;

import javafx.animation.PauseTransition;
import javafx.scene.control.Label;
import javafx.util.Duration;

/** Reusable success/error notification banner. Collapses (no layout space) when hidden. */
public class NotificationBanner extends Label {

    private final PauseTransition autoHide = new PauseTransition(Duration.seconds(4));

    public NotificationBanner() {
        getStyleClass().add("notification-banner");
        setWrapText(true);
        setManaged(false);
        setVisible(false);
        autoHide.setOnFinished(e -> hide());
    }

    public void showSuccess(String message) {
        show(message, "notification-success");
    }

    public void showError(String message) {
        show(message, "notification-error");
        autoHide.stop();
    }

    private void show(String message, String styleClass) {
        getStyleClass().removeAll("notification-success", "notification-error");
        getStyleClass().add(styleClass);
        setText(message);
        setManaged(true);
        setVisible(true);
        if (styleClass.equals("notification-success")) {
            autoHide.playFromStart();
        }
    }

    public void hide() {
        setManaged(false);
        setVisible(false);
        autoHide.stop();
    }
}
