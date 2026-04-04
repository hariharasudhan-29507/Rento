package com.vrbs;

import com.vrbs.config.AppConfig;
import com.vrbs.data.IndexSetup;
import com.vrbs.data.MongoConnection;
import com.vrbs.ui.shell.MainShell;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * VRBS — Vehicle Rental and Booking System (JavaFX desktop shell).
 */
public class VrbsApp extends Application {

    @Override
    public void init() {
        try {
            IndexSetup.createAllIndexes(MongoConnection.getDatabase());
        } catch (Exception ignored) {
            // Allow the desktop app to start even if MongoDB is unavailable.
        }
    }

    @Override
    public void start(Stage stage) {
        MainShell shell = new MainShell(stage);
        stage.setTitle("VRBS — Vehicle Rental & Booking");
        stage.setMinWidth(AppConfig.windowMinWidth());
        stage.setMinHeight(AppConfig.windowMinHeight());
        stage.setScene(shell.createScene());
        stage.setOnCloseRequest(event -> {
            MongoConnection.close();
            Platform.exit();
        });
        stage.show();
    }

    @Override
    public void stop() {
        MongoConnection.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
