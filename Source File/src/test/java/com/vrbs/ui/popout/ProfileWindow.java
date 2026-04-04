package com.vrbs.ui.popout;

import com.vrbs.model.SessionUser;
import com.vrbs.ui.animations.FxAnimations;
import com.vrbs.ui.theme.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

public final class ProfileWindow {

    private ProfileWindow() {
    }

    public static void open(Stage owner, SessionUser user, ThemeManager theme) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Profile — " + user.getDisplayName());

        Label title = new Label("Your profile");
        title.getStyleClass().add("nav-brand");

        Label details = new Label(
                "Name: " + user.getDisplayName() + "\n"
                        + "Username: " + user.getUsername() + "\n"
                        + "Email: " + user.getEmail() + "\n"
                        + "Role: " + user.getRole()
        );
        details.getStyleClass().add("text-muted");
        details.setStyle("-fx-line-spacing: 4;");

        Label actTitle = new Label("Recent activity");
        actTitle.getStyleClass().add("text-primary-accent");

        ListView<String> activity = new ListView<>();
        activity.getItems().setAll(List.of(
                "Signed in successfully",
                "Dashboard loaded",
                "Support channel available 24/7"
        ));
        activity.setPrefHeight(180);

        VBox root = new VBox(16, title, details, actTitle, activity);
        root.setPadding(new Insets(24));
        root.getStyleClass().add("surface-low");
        root.setPrefWidth(420);

        Scene scene = new Scene(root);
        theme.applyTo(scene);
        stage.setScene(scene);
        stage.show();
        FxAnimations.fadeIn(root, Duration.millis(280)).play();
    }
}
