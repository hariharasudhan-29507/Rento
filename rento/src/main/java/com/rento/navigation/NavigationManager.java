
package com.rento.navigation;

import javafx.animation.FadeTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Stack;

/**
 * Stack-based navigation manager for forward/backward page transitions.
 */
public class NavigationManager {

    private static StackPane rootContainer;
    private static Scene scene;
    private static final Stack<String> backStack = new Stack<>();
    private static final Stack<String> forwardStack = new Stack<>();
    private static String currentPage = null;

    public static void initialize(StackPane root, Scene scn) {
        rootContainer = root;
        scene = scn;

        // Keyboard navigation support
        scn.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case BACK_SPACE:
                    goBack();
                    break;
                case F5:
                    refresh();
                    break;
                default:
                    break;
            }
            if (event.isAltDown()) {
                switch (event.getCode()) {
                    case LEFT:
                        goBack();
                        break;
                    case RIGHT:
                        goForward();
                        break;
                    default:
                        break;
                }
            }
        });
    }

    /**
     * Navigate to a new FXML page with fade animation.
     */
    public static void navigateTo(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource(fxmlPath));
            Parent page = loader.load();

            if (currentPage != null) {
                backStack.push(currentPage);
            }
            forwardStack.clear();
            currentPage = fxmlPath;

            transitionTo(page);
        } catch (IOException e) {
            System.err.println("[Navigation] Failed to load: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Navigate to a page, passing a controller configurator.
     */
    public static void navigateTo(String fxmlPath, ControllerConfigurator configurator) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource(fxmlPath));
            Parent page = loader.load();

            if (configurator != null) {
                Object controller = loader.getController();
                configurator.configure(controller);
            }

            if (currentPage != null) {
                backStack.push(currentPage);
            }
            forwardStack.clear();
            currentPage = fxmlPath;

            transitionTo(page);
        } catch (IOException e) {
            System.err.println("[Navigation] Failed to load: " + fxmlPath);
            e.printStackTrace();
        }
    }

    public static void goBack() {
        if (!backStack.isEmpty()) {
            forwardStack.push(currentPage);
            currentPage = backStack.pop();
            loadPage(currentPage);
        }
    }

    public static void goForward() {
        if (!forwardStack.isEmpty()) {
            backStack.push(currentPage);
            currentPage = forwardStack.pop();
            loadPage(currentPage);
        }
    }

    public static void refresh() {
        if (currentPage != null) {
            loadPage(currentPage);
        }
    }

    public static boolean canGoBack() {
        return !backStack.isEmpty();
    }

    public static boolean canGoForward() {
        return !forwardStack.isEmpty();
    }

    public static String getCurrentPage() {
        return currentPage;
    }

    public static Scene getScene() {
        return scene;
    }

    public static Stage getStage() {
        return scene != null ? (Stage) scene.getWindow() : null;
    }

    public static void minimizeWindow() {
        Stage stage = getStage();
        if (stage != null) {
            stage.setIconified(true);
        }
    }

    public static void toggleMaximizeWindow() {
        Stage stage = getStage();
        if (stage != null) {
            stage.setMaximized(!stage.isMaximized());
        }
    }

    public static void clearHistory() {
        backStack.clear();
        forwardStack.clear();
    }

    private static void loadPage(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(NavigationManager.class.getResource(fxmlPath));
            Parent page = loader.load();
            transitionTo(page);
        } catch (IOException e) {
            System.err.println("[Navigation] Failed to reload: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private static void transitionTo(Parent newPage) {
        newPage.setOpacity(0);
        rootContainer.getChildren().setAll(newPage);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), newPage);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    @FunctionalInterface
    public interface ControllerConfigurator {
        void configure(Object controller);
    }
}
