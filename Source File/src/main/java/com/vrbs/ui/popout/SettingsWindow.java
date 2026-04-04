package com.vrbs.ui.popout;

import com.vrbs.service.AuthService;
import com.vrbs.session.SessionManager;
import com.vrbs.ui.animations.FxAnimations;
import com.vrbs.ui.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Separator;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

public final class SettingsWindow {

    private SettingsWindow() {
    }

    public static void open(
            Stage owner,
            AuthService authService,
            ThemeManager theme,
            Scene mainScene,
            Runnable onLogout) {
        var userOpt = SessionManager.get().currentUser();
        if (userOpt.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Please sign in to open settings.").showAndWait();
            return;
        }
        var user = userOpt.get();

        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Settings");

        Label head = new Label("Settings");
        head.getStyleClass().add("nav-brand");

        PasswordField cur = new PasswordField();
        cur.setPromptText("Current password");
        PasswordField nw = new PasswordField();
        nw.setPromptText("New password (min 6 chars)");
        PasswordField nw2 = new PasswordField();
        nw2.setPromptText("Confirm new password");

        Button changePw = new Button("Update password");
        changePw.getStyleClass().add("primary-button");
        changePw.setOnAction(e -> {
            if (!nw.getText().equals(nw2.getText())) {
                new Alert(Alert.AlertType.WARNING, "New passwords do not match.").showAndWait();
                return;
            }
            boolean ok = authService.changePassword(user.getUsername(), cur.getText(), nw.getText());
            if (ok) {
                new Alert(Alert.AlertType.INFORMATION, "Password updated.").showAndWait();
                cur.clear();
                nw.clear();
                nw2.clear();
            } else {
                new Alert(Alert.AlertType.ERROR, "Could not update — check current password and length.").showAndWait();
            }
        });

        Label themeState = new Label();
        themeState.getStyleClass().add("text-muted");
        Runnable syncThemeLabel = () -> themeState.setText(theme.isDark() ? "Theme: dark" : "Theme: light");
        syncThemeLabel.run();

        Button themeToggle = new Button("Toggle light / dark");
        themeToggle.getStyleClass().add("secondary-button");
        themeToggle.setOnAction(e -> {
            theme.toggle(mainScene);
            syncThemeLabel.run();
        });

        Button logout = new Button("Log out");
        logout.getStyleClass().add("secondary-button");
        logout.setOnAction(e -> {
            SessionManager.get().logout();
            if (onLogout != null) {
                onLogout.run();
            }
            stage.close();
        });

        VBox root = new VBox(14,
                head,
                new Separator(),
                new Label("Change password"),
                cur, nw, nw2, changePw,
                new Separator(),
                new Label("Appearance"),
                themeState,
                themeToggle,
                new Separator(),
                logout
        );
        root.setPadding(new Insets(24));
        root.getStyleClass().add("surface-low");
        root.setPrefWidth(380);

        Scene scene = new Scene(root);
        theme.applyTo(scene);
        stage.setScene(scene);
        stage.show();
        FxAnimations.fadeIn(root, Duration.millis(280)).play();
    }
}
