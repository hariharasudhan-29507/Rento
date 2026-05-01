package com.rento.controllers;

import com.rento.dao.VehicleDAO;
import com.rento.models.User;
import com.rento.models.Vehicle;
import com.rento.navigation.NavigationManager;
import com.rento.security.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller for the vehicle booking page.
 */
public class BookingController implements Initializable {

    @FXML private FlowPane vehicleGrid;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private ComboBox<String> fuelFilter;
    @FXML private Slider priceSlider;
    @FXML private Label priceLabel;
    @FXML private Button profileBtn;
    @FXML private VBox emptyState;

    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private List<Vehicle> allVehicles;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        if (isRestrictedRole()) {
            NavigationManager.navigateTo(resolveRoleDashboard());
            return;
        }
        updateProfileButton();

        // Setup filters
        categoryFilter.setItems(FXCollections.observableArrayList(
            "All Categories", "SEDAN", "SUV", "HATCHBACK", "COUPE", "TRUCK", "VAN", "BIKE", "BUS"));
        categoryFilter.setValue("All Categories");

        fuelFilter.setItems(FXCollections.observableArrayList(
            "All Fuel Types", "PETROL", "DIESEL", "ELECTRIC", "HYBRID", "CNG"));
        fuelFilter.setValue("All Fuel Types");

        // Price slider listener
        priceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            priceLabel.setText("Max: ₹" + newVal.intValue() + "/day");
            filterVehicles();
        });

        categoryFilter.setOnAction(e -> filterVehicles());
        fuelFilter.setOnAction(e -> filterVehicles());

        loadVehicles();
    }

    private void updateProfileButton() {
        if (SessionManager.getInstance().isLoggedIn()) {
            profileBtn.setText("⬤ " + SessionManager.getInstance().getCurrentUserName());
        } else {
            profileBtn.setText("⬤ Sign In");
        }
    }

    private void loadVehicles() {
        allVehicles = vehicleDAO.findAvailable();
        filterVehicles();
    }

    private void filterVehicles() {
        String category = categoryFilter.getValue();
        String fuel = fuelFilter.getValue();
        double maxPrice = priceSlider.getValue();

        List<Vehicle> filtered = allVehicles.stream()
            .filter(v -> ("All Categories".equals(category) || (v.getCategory() != null && v.getCategory().name().equals(category))))
            .filter(v -> ("All Fuel Types".equals(fuel) || (v.getFuelType() != null && v.getFuelType().name().equals(fuel))))
            .filter(v -> v.getDailyRate() <= maxPrice)
            .collect(Collectors.toList());

        vehicleGrid.getChildren().clear();

        if (filtered.isEmpty()) {
            emptyState.setVisible(true);
        } else {
            emptyState.setVisible(false);
            for (Vehicle v : filtered) {
                vehicleGrid.getChildren().add(createVehicleCard(v));
            }
        }
    }

    private VBox createVehicleCard(Vehicle vehicle) {
        VBox card = new VBox(0);
        card.getStyleClass().add("vehicle-card");
        card.setPrefWidth(280);
        card.setMaxWidth(280);

        // Image placeholder with gradient
        StackPane imagePane = new StackPane();
        imagePane.setMinHeight(180);
        imagePane.setPrefHeight(180);

        String[] gradients = {
            "linear-gradient(to bottom right, #2d1b69, #1a1040)",
            "linear-gradient(to bottom right, #1b3a4b, #0d2137)",
            "linear-gradient(to bottom right, #3b1d4a, #1a0d2e)",
            "linear-gradient(to bottom right, #1b4b3a, #0d3721)"
        };
        String gradient = gradients[(int)(Math.random() * gradients.length)];
        imagePane.setStyle("-fx-background-color: " + gradient + "; -fx-background-radius: 16 16 0 0;");

        Label carEmoji = new Label(getVehicleEmoji(vehicle.getCategory()));
        carEmoji.setStyle("-fx-font-size: 64px;");
        imagePane.getChildren().add(carEmoji);

        // Status badge
        Label statusBadge = new Label("● Available");
        statusBadge.getStyleClass().addAll("badge", "badge-success");
        StackPane.setAlignment(statusBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(statusBadge, new Insets(12, 12, 0, 0));
        imagePane.getChildren().add(statusBadge);

        // Body
        VBox body = new VBox(8);
        body.getStyleClass().add("vehicle-card-body");

        Label name = new Label(vehicle.getDisplayName());
        name.getStyleClass().add("vehicle-name");

        HBox tags = new HBox(8);
        Label catTag = new Label(vehicle.getCategory() != null ? vehicle.getCategory().name() : "N/A");
        catTag.getStyleClass().add("vehicle-tag");
        Label fuelTag = new Label(vehicle.getFuelType() != null ? vehicle.getFuelType().name() : "N/A");
        fuelTag.getStyleClass().add("vehicle-tag");
        Label seatTag = new Label(vehicle.getSeats() + " seats");
        seatTag.getStyleClass().add("vehicle-tag");
        tags.getChildren().addAll(catTag, fuelTag, seatTag);

        HBox priceRow = new HBox(8);
        priceRow.setAlignment(Pos.CENTER_LEFT);
        Label price = new Label("₹" + String.format("%.0f", vehicle.getDailyRate()));
        price.getStyleClass().add("vehicle-price");
        Label perDay = new Label("/day");
        perDay.getStyleClass().add("text-muted");
        priceRow.getChildren().addAll(price, perDay);

        Button bookBtn = new Button("Book Now →");
        bookBtn.getStyleClass().add("btn-primary");
        bookBtn.setMaxWidth(Double.MAX_VALUE);
        bookBtn.setOnAction(e -> onBookVehicle(vehicle));

        body.getChildren().addAll(name, tags, priceRow, bookBtn);
        card.getChildren().addAll(imagePane, body);

        return card;
    }

    private String getVehicleEmoji(Vehicle.Category category) {
        if (category == null) return "🚗";
        switch (category) {
            case SUV: return "🚙";
            case BIKE: return "🏍";
            case TRUCK: return "🚛";
            case BUS: return "🚌";
            case VAN: return "🚐";
            default: return "🚗";
        }
    }

    private void onBookVehicle(Vehicle vehicle) {
        if (SessionManager.getInstance().isGuest()) {
            NavigationManager.navigateTo("/fxml/login.fxml");
            return;
        }

        NavigationManager.navigateTo("/fxml/booking_detail.fxml", controller -> {
            if (controller instanceof BookingDetailController) {
                ((BookingDetailController) controller).setVehicle(vehicle);
            }
        });
    }

    @FXML private void onRefresh() { loadVehicles(); }
    @FXML private void onNavHome() { NavigationManager.navigateTo("/fxml/landing.fxml"); }
    @FXML private void onNavAbout() { NavigationManager.navigateTo("/fxml/about.fxml"); }
    @FXML private void onNavRent() { NavigationManager.navigateTo("/fxml/rent.fxml"); }
    @FXML private void onNavContact() { NavigationManager.navigateTo("/fxml/contact.fxml"); }
    @FXML private void onNavProfile() {
        if (SessionManager.getInstance().isGuest()) {
            NavigationManager.navigateTo("/fxml/login.fxml");
        } else {
            NavigationManager.navigateTo("/fxml/profile.fxml");
        }
    }

    private boolean isRestrictedRole() {
        User.Role role = SessionManager.getInstance().getCurrentRole();
        return role == User.Role.ADMIN || role == User.Role.DRIVER || role == User.Role.SUPPLIER;
    }

    private String resolveRoleDashboard() {
        return switch (SessionManager.getInstance().getCurrentRole()) {
            case ADMIN -> "/fxml/admin_dashboard.fxml";
            case DRIVER -> "/fxml/driver_dashboard.fxml";
            case SUPPLIER -> "/fxml/supplier_dashboard.fxml";
            default -> "/fxml/landing.fxml";
        };
    }
}
