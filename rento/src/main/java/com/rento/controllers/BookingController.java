package com.rento.controllers;

import com.rento.dao.VehicleDAO;
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
import java.util.Arrays;
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

        // If no vehicles from DB, show demo vehicles
        if (allVehicles.isEmpty()) {
            allVehicles = createDemoVehicles();
        }
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

    private List<Vehicle> createDemoVehicles() {
        return Arrays.asList(
            createDemo("Toyota", "Camry", 2024, Vehicle.Category.SEDAN, Vehicle.FuelType.PETROL, 2500, 5, "Silver"),
            createDemo("Hyundai", "Creta", 2024, Vehicle.Category.SUV, Vehicle.FuelType.DIESEL, 3500, 5, "White"),
            createDemo("Maruti", "Swift", 2023, Vehicle.Category.HATCHBACK, Vehicle.FuelType.PETROL, 1500, 5, "Red"),
            createDemo("Tesla", "Model 3", 2024, Vehicle.Category.SEDAN, Vehicle.FuelType.ELECTRIC, 5000, 5, "Black"),
            createDemo("Mahindra", "Thar", 2024, Vehicle.Category.SUV, Vehicle.FuelType.DIESEL, 4000, 4, "Green"),
            createDemo("Royal Enfield", "Classic 350", 2023, Vehicle.Category.BIKE, Vehicle.FuelType.PETROL, 800, 2, "Black"),
            createDemo("Tata", "Nexon EV", 2024, Vehicle.Category.SUV, Vehicle.FuelType.ELECTRIC, 3800, 5, "Blue"),
            createDemo("Honda", "City", 2023, Vehicle.Category.SEDAN, Vehicle.FuelType.PETROL, 2800, 5, "Grey"),
            createDemo("BMW", "X5", 2024, Vehicle.Category.SUV, Vehicle.FuelType.DIESEL, 8000, 5, "White"),
            createDemo("Ford", "EcoSport", 2023, Vehicle.Category.SUV, Vehicle.FuelType.PETROL, 3000, 5, "Orange"),
            createDemo("Tata", "Ace", 2022, Vehicle.Category.TRUCK, Vehicle.FuelType.DIESEL, 2000, 2, "White"),
            createDemo("Mercedes", "V-Class", 2024, Vehicle.Category.VAN, Vehicle.FuelType.DIESEL, 7500, 7, "Black"),
            createDemo("Kia", "Seltos", 2024, Vehicle.Category.SUV, Vehicle.FuelType.PETROL, 3600, 5, "Blue"),
            createDemo("Audi", "A4", 2024, Vehicle.Category.SEDAN, Vehicle.FuelType.PETROL, 7200, 5, "Grey"),
            createDemo("TVS", "Ntorq", 2023, Vehicle.Category.BIKE, Vehicle.FuelType.PETROL, 700, 2, "Yellow"),
            createDemo("Ashok Leyland", "Dost", 2023, Vehicle.Category.TRUCK, Vehicle.FuelType.DIESEL, 2600, 2, "White"),
            createDemo("Honda", "City", 2024, Vehicle.Category.SEDAN, Vehicle.FuelType.PETROL, 2700, 5, "Brown"),
            createDemo("Skoda", "Slavia", 2024, Vehicle.Category.SEDAN, Vehicle.FuelType.PETROL, 2900, 5, "Blue"),
            createDemo("Mahindra", "XUV700", 2025, Vehicle.Category.SUV, Vehicle.FuelType.DIESEL, 4700, 7, "Navy"),
            createDemo("Ather", "450X", 2025, Vehicle.Category.BIKE, Vehicle.FuelType.ELECTRIC, 850, 2, "Grey"),
            createDemo("Yamaha", "FZ-S", 2024, Vehicle.Category.BIKE, Vehicle.FuelType.PETROL, 750, 2, "Matte Black"),
            createDemo("Suzuki", "Access 125", 2024, Vehicle.Category.BIKE, Vehicle.FuelType.PETROL, 600, 2, "White"),
            createDemo("Kia", "Carens", 2024, Vehicle.Category.VAN, Vehicle.FuelType.PETROL, 4300, 6, "Silver")
        );
    }

    private Vehicle createDemo(String make, String model, int year, Vehicle.Category cat, Vehicle.FuelType fuel,
                                double rate, int seats, String color) {
        Vehicle v = new Vehicle(make, model, year, cat, fuel, rate);
        v.setSeats(seats);
        v.setColor(color);
        v.setStatus(Vehicle.Status.AVAILABLE);
        return v;
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
}
