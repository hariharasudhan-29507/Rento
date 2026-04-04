package com.vrbs.ui.support;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Customer care: predefined Q&amp;A plus docked vs floating presentation.
 */
public final class SupportWidget {

    private final BorderPane dockRoot = new BorderPane();
    private final VBox floatingPanel = new VBox(10);
    private final Runnable onLayoutRequest;
    private ToggleButton dockedToggle;
    private ToggleButton floatingToggle;

    private static final Map<String, String> QA = new LinkedHashMap<>();

    static {
        QA.put("How do I book a cab?", "Choose Book Cab, pick a service area, then select an available driver.");
        QA.put("How does hourly rental work?", "Open Rent, add vehicles to cart (max 5), choose 1–24 hours, then pay.");
        QA.put("Peak pricing?", "Peak hours apply a 1.5× multiplier per PRD.");
        QA.put("Late return?", "After a 1 hour grace period, late fees accrue per policy.");
        QA.put("Refunds?", "Full refund within 15 minutes; 50% after that window.");
        QA.put("Wallet?", "Your wallet balance appears on the user dashboard after sign-in.");
        QA.put("Contact supplier?", "Drivers can message linked suppliers from the driver dashboard.");
        QA.put("GST on invoices?", "Suppliers see GST breakdown in their finance panel.");
        QA.put("Account suspended?", "Contact admin or customer care if you see a suspension notice.");
        QA.put("Session timeout?", "Sessions expire after inactivity; sign in again to continue.");
    }

    public SupportWidget(Runnable onLayoutRequest) {
        this.onLayoutRequest = onLayoutRequest;
        buildDock();
        buildFloating();
    }

    private void buildDock() {
        Label head = new Label("Customer care");
        head.getStyleClass().add("text-primary-accent");
        ListView<String> topics = new ListView<>();
        topics.getItems().addAll(QA.keySet());
        topics.setPrefHeight(120);

        Label answer = new Label("Select a question.");
        answer.setWrapText(true);
        answer.getStyleClass().add("text-muted");
        topics.getSelectionModel().selectedItemProperty().addListener((o, a, q) -> {
            if (q != null) {
                answer.setText(QA.getOrDefault(q, ""));
            }
        });

        TextField quick = new TextField();
        quick.setPromptText("Type a keyword (demo)…");
        quick.setOnAction(e -> {
            String t = quick.getText().toLowerCase();
            String hit = QA.entrySet().stream()
                    .filter(en -> en.getKey().toLowerCase().contains(t) || en.getValue().toLowerCase().contains(t))
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse("No match — pick a topic from the list.");
            answer.setText(hit.startsWith("No match") ? hit : QA.get(hit));
        });

        VBox center = new VBox(10, head, topics, quick, answer);
        center.setPadding(new Insets(12));
        center.getStyleClass().add("card");
        dockRoot.setCenter(center);
    }

    private void buildFloating() {
        floatingPanel.setPadding(new Insets(12));
        floatingPanel.setMaxWidth(320);
        floatingPanel.getStyleClass().addAll("card", "surface-high");
        floatingPanel.setVisible(false);
        floatingPanel.setManaged(false);

        Label fl = new Label("Support (floating)");
        fl.getStyleClass().add("text-primary-accent");
        ListView<String> ft = new ListView<>();
        ft.getItems().addAll(QA.keySet());
        ft.setPrefHeight(140);
        Label fa = new Label();
        fa.setWrapText(true);
        fa.getStyleClass().add("text-muted");
        ft.getSelectionModel().selectedItemProperty().addListener((o, a, q) -> {
            if (q != null) {
                fa.setText(QA.get(q));
            }
        });
        Button close = new Button("Dock panel");
        close.getStyleClass().add("secondary-button");
        close.setOnAction(e -> {
            if (dockedToggle != null) {
                dockedToggle.setSelected(true);
            }
            applyFloatingLayout(false);
        });
        floatingPanel.getChildren().addAll(fl, ft, fa, close);
    }

    public Region dockedPane() {
        return dockRoot;
    }

    public VBox floatingPane() {
        return floatingPanel;
    }

    public HBox buildModeToolbar() {
        ToggleGroup group = new ToggleGroup();
        dockedToggle = new ToggleButton("Docked panel");
        floatingToggle = new ToggleButton("Floating widget");
        dockedToggle.setToggleGroup(group);
        floatingToggle.setToggleGroup(group);
        dockedToggle.setSelected(true);
        dockedToggle.setOnAction(e -> {
            if (dockedToggle.isSelected()) {
                applyFloatingLayout(false);
            }
        });
        floatingToggle.setOnAction(e -> {
            if (floatingToggle.isSelected()) {
                applyFloatingLayout(true);
            }
        });
        HBox bar = new HBox(8, new Label("Support:"), dockedToggle, floatingToggle);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 0, 8, 0));
        return bar;
    }

    private void applyFloatingLayout(boolean floating) {
        dockRoot.setVisible(!floating);
        dockRoot.setManaged(!floating);
        floatingPanel.setVisible(floating);
        floatingPanel.setManaged(floating);
        if (floating) {
            FadeTransition ft = new FadeTransition(Duration.millis(220), floatingPanel);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
        if (onLayoutRequest != null) {
            onLayoutRequest.run();
        }
    }

    public void attachFloatingToStack(javafx.scene.layout.StackPane stack) {
        if (!stack.getChildren().contains(floatingPanel)) {
            javafx.scene.layout.StackPane.setAlignment(floatingPanel, Pos.BOTTOM_RIGHT);
            javafx.scene.layout.StackPane.setMargin(floatingPanel, new Insets(16));
            floatingPanel.setMaxHeight(Region.USE_PREF_SIZE);
            stack.getChildren().add(floatingPanel);
        }
    }
}
