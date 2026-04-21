package com.rento.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.application.Platform;
import java.util.Optional;

/**
 * Modern styled alert dialogs.
 */
public class AlertUtil {

    public static void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    public static void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }

    public static void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    public static void showSuccess(String message) {
        showAlert(Alert.AlertType.INFORMATION, "Success", message);
    }

    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        Optional<ButtonType> result;
        try {
            result = alert.showAndWait();
        } catch (IllegalStateException ex) {
            Platform.runLater(alert::show);
            return false;
        }
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        styleAlert(alert);
        safeShow(alert);
    }

    private static void styleAlert(Alert alert) {
        try {
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle(
                "-fx-background-color: #1a1a2e;" +
                "-fx-font-family: 'Segoe UI';"
            );
            dialogPane.lookup(".content.label").setStyle(
                "-fx-text-fill: #e0e0e0;" +
                "-fx-font-size: 14px;"
            );
        } catch (Exception ignored) {}
    }

    private static void safeShow(Alert alert) {
        try {
            alert.showAndWait();
        } catch (IllegalStateException ex) {
            // JavaFX blocks showAndWait during animation/layout pulses.
            Platform.runLater(alert::show);
        }
    }
}
