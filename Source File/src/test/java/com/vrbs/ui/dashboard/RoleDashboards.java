package com.vrbs.ui.dashboard;

import com.vrbs.model.SessionUser;
import com.vrbs.model.UserRole;
import com.vrbs.service.CabLocationService;
import com.vrbs.state.ApplicationState;
import com.vrbs.ui.animations.FxAnimations;
import com.vrbs.ui.payment.PaymentDemoPanel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;
import java.util.stream.Collectors;

public final class RoleDashboards {

    private RoleDashboards() {
    }

    public static ScrollPane build(SessionUser user, UserRole role, CabLocationService cabService, Stage stage, String searchFilter) {
        return switch (role) {
            case USER -> userView(user, cabService, stage, searchFilter);
            case DRIVER -> driverView(user);
            case SUPPLIER -> supplierView(user);
            case ADMIN -> adminView();
            default -> guestPlaceholder();
        };
    }

    private static ScrollPane wrap(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("scroll-pane");
        return sp;
    }

    private static ScrollPane guestPlaceholder() {
        Label l = new Label("Sign in to see your dashboard.");
        l.getStyleClass().add("text-muted");
        VBox v = new VBox(l);
        v.setPadding(new Insets(40));
        return wrap(v);
    }

    private static ScrollPane userView(SessionUser user, CabLocationService cab, Stage stage, String searchFilter) {
        Label welcome = new Label("Welcome, " + user.getDisplayName());
        welcome.getStyleClass().add("nav-brand");

        Label wallet = new Label("VRBS Wallet: ₹ 2,450.00 (demo) — everyday city transport, prices in ₹");
        wallet.getStyleClass().add("text-primary-accent");

        HBox driverMetricPreview = metricsRow(DashboardDemoData.driverMetrics());
        VBox rideRequests = rideRequestsPanel();
        VBox route = heroPanel(
                "Ongoing transit",
                "Pickup at Grand Central Station. Current route is tracking the next waypoint with a live ETA.",
                "LIVE ROUTE",
                "Progress 65% · Next turn in 1.2 km"
        );

        VBox hero = heroPanel(
                "Move through the city with confidence",
                "Your active Audi Q7 Quattro rental is live, your next pickup is queued, and premium support is one click away.",
                "ACTIVE RENTAL",
                "Audi Q7 Quattro · 42h 15m remaining · Priority support enabled"
        );
        HBox metricRow = metricsRow(DashboardDemoData.userMetrics());
        VBox updates = actionPanel("Updates", DashboardDemoData.userUpdates());
        VBox showroom = showroomPanel();
        VBox timeline = timelinePanel();

        Label payHead = new Label("Checkout (Swiggy/Zomato-style demo)");
        payHead.getStyleClass().add("text-primary-accent");
        VBox payCard = new VBox(10, payHead, PaymentDemoPanel.create(stage, user));
        payCard.getStyleClass().add("card");

        Label bookHead = new Label("Book cab — pick area & driver");
        bookHead.getStyleClass().add("text-primary-accent");
        ComboBox<String> areas = new ComboBox<>();
        areas.getItems().addAll(cab.locations());
        if (!areas.getItems().isEmpty()) {
            areas.getSelectionModel().selectFirst();
        }
        ListView<CabLocationService.DriverCard> drivers = new ListView<>();
        Runnable refreshDrivers = () -> {
            String a = areas.getSelectionModel().getSelectedItem();
            drivers.getItems().setAll(cab.driversInArea(a).stream()
                    .filter(d -> d.available())
                    .collect(Collectors.toList()));
        };
        areas.setOnAction(e -> refreshDrivers.run());
        refreshDrivers.run();
        drivers.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CabLocationService.DriverCard item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.name() + " · " + item.vehicle());
                }
            }
        });
        Button hire = new Button("Hire selected driver");
        hire.getStyleClass().add("primary-button");
        hire.setOnAction(e -> {
            var sel = drivers.getSelectionModel().getSelectedItem();
            if (sel == null) {
                new Alert(Alert.AlertType.WARNING, "Select a driver first.").showAndWait();
                return;
            }
            new Alert(Alert.AlertType.INFORMATION, "Booking request sent to " + sel.name() + " (demo).").showAndWait();
        });

        Label rentHead = new Label("Hourly rental (Indian fleet)");
        rentHead.getStyleClass().add("text-primary-accent");
        Label rentBody = new Label(
                "Browse hatchbacks and SUVs from local partners — Maruti, Tata, Hyundai, Mahindra (not imports/exotics). "
                        + "Add to cart (max 5), 1–24 hours, settle in ₹ via wallet, UPI, card, or cash on vehicle handover."
        );
        rentBody.setWrapText(true);
        rentBody.getStyleClass().add("text-muted");

        VBox bookBox = new VBox(10, bookHead, areas, drivers, hire);
        bookBox.getStyleClass().add("card");
        VBox rentBox = new VBox(10, rentHead, rentBody);
        rentBox.getStyleClass().add("card");

        if (searchFilter != null && !searchFilter.isBlank()) {
            String sf = searchFilter.toLowerCase();
            if (!sf.contains("book") && !sf.contains("cab") && !sf.contains("driver") && !sf.contains("hire")) {
                bookBox.setVisible(false);
            }
            if (!sf.contains("rent") && !sf.contains("vehicle") && !sf.contains("fleet") && !sf.contains("hour")) {
                rentBox.setVisible(false);
            }
            if (!sf.contains("pay") && !sf.contains("payment") && !sf.contains("cod") && !sf.contains("upi")
                    && !sf.contains("checkout") && !sf.contains("wallet")) {
                payCard.setVisible(false);
            }
        }

        VBox root = new VBox(20, welcome, wallet, hero, metricRow, updates, new Separator(), payCard, bookBox, rentBox, showroom, timeline);
        root.setPadding(new Insets(20));
        FxAnimations.fadeIn(root, Duration.millis(320)).play();
        return wrap(root);
    }

    private static ScrollPane driverView(SessionUser user) {
        Label head = new Label("Driver workspace — " + user.getDisplayName());
        head.getStyleClass().add("nav-brand");

        Label wallet = new Label("Wallet: ₹ 890.00 (demo)");
        wallet.getStyleClass().add("text-primary-accent");

        HBox metricRow = metricsRow(DashboardDemoData.driverMetrics());
        VBox rideRequests = rideRequestsPanel();
        VBox route = heroPanel(
                "Ongoing transit",
                "Pickup at Grand Central Station. Current route is tracking the next waypoint with a live ETA.",
                "LIVE ROUTE",
                "Progress 65% · Next turn in 1.2 km"
        );

        Label peersHead = new Label("Other drivers (directory)");
        peersHead.getStyleClass().add("text-primary-accent");
        ListView<String> peers = new ListView<>();
        peers.getItems().addAll(
                "Karthik Venkatesh · Maruti Dzire · Chennai",
                "Deepa Krishnan · Maruti Swift · Bengaluru",
                "Srinivas Reddy · Hyundai Creta · Hyderabad",
                "Suresh Nambiar · Maruti Ertiga · Kochi"
        );
        peers.setPrefHeight(140);

        Label supHead = new Label("Linked suppliers — quick contact");
        supHead.getStyleClass().add("text-primary-accent");
        Button msgSupplier = new Button("Message primary supplier (demo)");
        msgSupplier.getStyleClass().add("secondary-button");
        msgSupplier.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION, "Message queued (integrate chat service).").showAndWait());

        Label perfHead = new Label("Performance (animated bars)");
        perfHead.getStyleClass().add("text-primary-accent");
        HBox chart = buildMiniChart();

        VBox root = new VBox(16, head, wallet, metricRow, rideRequests, route, new Separator(), peersHead, peers, supHead, msgSupplier, perfHead, chart);
        root.setPadding(new Insets(20));
        Platform.runLater(() -> FxAnimations.fadeIn(root, Duration.millis(320)).play());
        return wrap(root);
    }

    private static HBox buildMiniChart() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.BOTTOM_LEFT);
        row.setPadding(new Insets(16, 0, 0, 0));
        double[] targets = {120, 80, 160, 100};
        String[] labels = {"Trips", "Rating", "On-time", "Earnings (₹)"};
        for (int i = 0; i < targets.length; i++) {
            VBox col = new VBox(6);
            col.setAlignment(Pos.BOTTOM_CENTER);
            Region bar = new Region();
            bar.getStyleClass().add("chart-bar");
            bar.setPrefWidth(36);
            bar.setPrefHeight(targets[i]);
            Label lb = new Label(labels[i]);
            lb.getStyleClass().add("label");
            lb.setStyle("-fx-font-size: 10px;");
            col.getChildren().addAll(bar, lb);
            row.getChildren().add(col);
            FxAnimations.growBarHeight(bar, targets[i], Duration.millis(600 + i * 120)).play();
        }
        return row;
    }

    private static ScrollPane supplierView(SessionUser user) {
        Label head = new Label("Supplier — " + user.getDisplayName());
        head.getStyleClass().add("nav-brand");

        VBox operations = heroPanel(
                "Operational overview",
                "Your logistics network is running at 98.4% availability with maintenance concentrated in a small service queue.",
                "FLEET STATUS",
                "Revenue up 12.5% · Safety score stable"
        );
        HBox metrics = metricsRow(DashboardDemoData.supplierMetrics());

        Label stats = new Label(
                "Fleet utilization: 78%\n"
                        + "Active trips today: 14\n"
                        + "Users linked via drivers: 128 (demo)"
        );
        stats.getStyleClass().add("text-muted");
        stats.setStyle("-fx-line-spacing: 6;");

        Label gst = new Label("GST collected (18%): ₹ 42,300 · Net revenue: ₹ 193,400");
        gst.getStyleClass().add("text-primary-accent");

        Button payPlatform = new Button("Pay platform fee (demo)");
        payPlatform.getStyleClass().add("primary-button");
        payPlatform.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION, "Opens payment portal stub.").showAndWait());

        Button ownPortal = new Button("Open supplier billing portal");
        ownPortal.getStyleClass().add("secondary-button");
        ownPortal.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION, "External portal integration point.").showAndWait());

        Label driveStatus = new Label("Drive status: 11 completed · 3 in progress · 0 flagged");
        driveStatus.getStyleClass().add("text-muted");

        VBox root = new VBox(16, head, operations, metrics, stats, actionPanel("Vehicle performance", DashboardDemoData.fleetRows()), gst, driveStatus, payPlatform, ownPortal);
        root.setPadding(new Insets(20));
        FxAnimations.fadeIn(root, Duration.millis(320)).play();
        return wrap(root);
    }

    private static ScrollPane adminView() {
        var state = ApplicationState.get();

        Label head = new Label("Admin control");
        head.getStyleClass().add("nav-brand");

        VBox revenue = heroPanel(
                "Operations overview",
                "Monitor users, logistics health, compliance, and service alerts from one control surface.",
                "NETWORK REVENUE",
                "USD 1.24M aggregated · 14 hubs active"
        );
        HBox metrics = metricsRow(List.of(
                new DashboardDemoData.Metric("Drivers Online", "482", "Shift Alpha"),
                new DashboardDemoData.Metric("Compliance", "98.4%", "Across the fleet"),
                new DashboardDemoData.Metric("Service Downtime", "12h", "Maintenance alerts"),
                new DashboardDemoData.Metric("Alerts", "3 critical", "Berlin-Core latency")
        ));

        Button maintenance = new Button();
        maintenance.getStyleClass().add("secondary-button");
        Runnable syncMaint = () -> maintenance.setText(state.isMaintenanceMode()
                ? "Disable maintenance mode"
                : "Enable maintenance mode (blocks non-admin sign-in)");
        syncMaint.run();
        maintenance.setOnAction(e -> {
            state.setMaintenanceMode(!state.isMaintenanceMode());
            syncMaint.run();
            new Alert(Alert.AlertType.INFORMATION, "Maintenance: " + state.isMaintenanceMode()).showAndWait();
        });

        TextField suspendId = new TextField();
        suspendId.setPromptText("Account id to suspend (e.g. acct-user)");
        ComboBox<String> rolePick = new ComboBox<>();
        rolePick.getItems().addAll("USER", "DRIVER", "SUPPLIER");
        rolePick.getSelectionModel().selectFirst();
        Button suspendBtn = new Button("Suspend / unsuspend toggle");
        suspendBtn.getStyleClass().add("primary-button");
        suspendBtn.setOnAction(e -> {
            String id = suspendId.getText().trim();
            if (id.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Enter an id.").showAndWait();
                return;
            }
            String r = rolePick.getSelectionModel().getSelectedItem();
            if ("USER".equals(r)) {
                boolean now = !state.isUserSuspended(id);
                state.suspendUser(id, now);
                new Alert(Alert.AlertType.INFORMATION, "USER " + id + " suspended=" + now).showAndWait();
            } else if ("DRIVER".equals(r)) {
                boolean now = !state.isDriverSuspended(id);
                state.suspendDriver(id, now);
                new Alert(Alert.AlertType.INFORMATION, "DRIVER " + id + " suspended=" + now).showAndWait();
            } else {
                boolean now = !state.isSupplierSuspended(id);
                state.suspendSupplier(id, now);
                new Alert(Alert.AlertType.INFORMATION, "SUPPLIER " + id + " suspended=" + now).showAndWait();
            }
        });

        Label mon = new Label("Monitoring: bookings, payments, suspensions — wire to Mongo reports.");
        mon.setWrapText(true);
        mon.getStyleClass().add("text-muted");

        VBox root = new VBox(16, head, revenue, metrics, actionPanel("Recent users", DashboardDemoData.adminUsers()), logsPanel(),
                maintenance, new Separator(), suspendId, rolePick, suspendBtn, mon);
        root.setPadding(new Insets(20));
        FxAnimations.fadeIn(root, Duration.millis(320)).play();
        return wrap(root);
    }

    private static VBox heroPanel(String title, String body, String chip, String stat) {
        Label chipLabel = new Label(chip);
        chipLabel.getStyleClass().addAll("chip-label", "text-primary-accent");
        Label heading = new Label(title);
        heading.getStyleClass().add("hero-title");
        Label description = new Label(body);
        description.setWrapText(true);
        description.getStyleClass().add("text-muted");
        Label statLabel = new Label(stat);
        statLabel.getStyleClass().add("kpi-value");
        VBox panel = new VBox(10, chipLabel, heading, description, statLabel);
        panel.getStyleClass().addAll("card", "hero-panel");
        return panel;
    }

    private static HBox metricsRow(List<DashboardDemoData.Metric> metrics) {
        HBox row = new HBox(12);
        for (DashboardDemoData.Metric metric : metrics) {
            Label label = new Label(metric.label());
            label.getStyleClass().add("text-muted");
            Label value = new Label(metric.value());
            value.getStyleClass().add("kpi-value");
            Label note = new Label(metric.note());
            note.getStyleClass().add("text-muted");
            VBox card = new VBox(6, label, value, note);
            card.getStyleClass().addAll("card", "metric-card");
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
            row.getChildren().add(card);
        }
        return row;
    }

    private static VBox actionPanel(String title, List<DashboardDemoData.ActionItem> items) {
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        VBox list = new VBox(8, heading);
        for (DashboardDemoData.ActionItem item : items) {
            Label rowTitle = new Label(item.title());
            rowTitle.getStyleClass().add("text-primary-accent");
            Label detail = new Label(item.detail());
            detail.setWrapText(true);
            detail.getStyleClass().add("text-muted");
            VBox row = new VBox(3, rowTitle, detail);
            row.getStyleClass().addAll("subtle-panel", "info-row");
            list.getChildren().add(row);
        }
        list.getStyleClass().add("card");
        return list;
    }

    private static VBox showroomPanel() {
        Label heading = new Label("Curated showroom");
        heading.getStyleClass().add("section-title");
        HBox row = new HBox(12);
        for (DashboardDemoData.VehicleShowcase vehicle : DashboardDemoData.showroom()) {
            Label tag = new Label(vehicle.tag());
            tag.getStyleClass().addAll("chip-label", "text-primary-accent");
            Label name = new Label(vehicle.name());
            name.getStyleClass().add("text-primary-accent");
            Label price = new Label(vehicle.price());
            price.getStyleClass().add("kpi-value");
            Label spec = new Label(vehicle.spec());
            spec.setWrapText(true);
            spec.getStyleClass().add("text-muted");
            Button button = new Button("Reserve");
            button.getStyleClass().add("primary-button");
            VBox card = new VBox(8, tag, name, price, spec, button);
            card.getStyleClass().addAll("card", "metric-card");
            HBox.setHgrow(card, Priority.ALWAYS);
            card.setMaxWidth(Double.MAX_VALUE);
            row.getChildren().add(card);
        }
        return new VBox(12, heading, row);
    }

    private static VBox timelinePanel() {
        Label heading = new Label("Live trip journey");
        heading.getStyleClass().add("section-title");
        HBox steps = new HBox(18,
                timelineStep("Booking confirmed", "12:45 PM", true),
                timelineStep("Driver en route", "Arriving in 4m", true),
                timelineStep("In transit", "ETA 12:58 PM", false),
                timelineStep("Destination", "Awaiting arrival", false));
        VBox panel = new VBox(12, heading, steps);
        panel.getStyleClass().add("card");
        return panel;
    }

    private static VBox timelineStep(String title, String note, boolean active) {
        Label dot = new Label(active ? "●" : "○");
        dot.getStyleClass().add(active ? "text-primary-accent" : "text-muted");
        Label label = new Label(title);
        label.getStyleClass().add(active ? "text-primary-accent" : "text-muted");
        Label sub = new Label(note);
        sub.getStyleClass().add("text-muted");
        VBox box = new VBox(4, dot, label, sub);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static VBox rideRequestsPanel() {
        Label heading = new Label("New ride requests");
        heading.getStyleClass().add("section-title");
        VBox list = new VBox(10, heading);
        for (DashboardDemoData.RideRequest request : DashboardDemoData.rideRequests()) {
            Label title = new Label(request.category() + " · " + request.rider());
            title.getStyleClass().add("text-primary-accent");
            Label detail = new Label(request.eta() + " · " + request.distance() + " · Fare " + request.fare());
            detail.getStyleClass().add("text-muted");
            HBox actions = new HBox(8);
            Button accept = new Button("Accept");
            accept.getStyleClass().add("primary-button");
            Button decline = new Button("Decline");
            decline.getStyleClass().add("secondary-button");
            actions.getChildren().addAll(accept, decline);
            VBox row = new VBox(4, title, detail, actions);
            row.getStyleClass().addAll("subtle-panel", "info-row");
            list.getChildren().add(row);
        }
        list.getStyleClass().add("card");
        return list;
    }

    private static VBox logsPanel() {
        Label heading = new Label("Global system logs");
        heading.getStyleClass().add("section-title");
        VBox list = new VBox(8, heading);
        for (DashboardDemoData.SystemLog log : DashboardDemoData.logs()) {
            Label line = new Label("[" + log.level() + "] " + log.message());
            line.setWrapText(true);
            line.getStyleClass().add("text-muted");
            list.getChildren().add(line);
        }
        list.getStyleClass().add("card");
        return list;
    }
}
