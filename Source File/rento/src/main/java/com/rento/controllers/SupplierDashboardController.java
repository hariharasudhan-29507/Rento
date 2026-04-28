package com.rento.controllers;

import com.rento.dao.VehicleDAO;
import com.rento.dao.UserDAO;
import com.rento.models.Rental;
import com.rento.models.Vehicle;
import com.rento.navigation.NavigationManager;
import com.rento.security.SessionManager;
import com.rento.services.AuthService;
import com.rento.services.RentalService;
import com.rento.utils.DateTimeUtil;
import com.rento.utils.AlertUtil;
import com.rento.utils.ValidationUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Supplier Dashboard.
 */
public class SupplierDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label totalVehicles;
    @FXML private Label activeRentals;
    @FXML private Label pendingApprovals;
    @FXML private Label totalRevenue;
    @FXML private Label walletBalance;
    @FXML private VBox approvalList;
    @FXML private Label noApprovalsLabel;
    @FXML private FlowPane vehicleFleet;
    @FXML private Label noVehiclesLabel;
    @FXML private PieChart fleetStatusChart;
    @FXML private BarChart<String, Number> supplierTrendChart;

    private final RentalService rentalService = new RentalService();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final AuthService authService = new AuthService();
    private final UserDAO userDAO = new UserDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        welcomeLabel.setText("Welcome, " + SessionManager.getInstance().getCurrentUserName() + " (Supplier)");
        loadDashboard();
    }

    private void loadDashboard() {
        if (SessionManager.getInstance().getCurrentUser() == null) {
            return;
        }

        List<Rental> requested = rentalService.getRentalsBySupplier(SessionManager.getInstance().getCurrentUser().getId()).stream()
            .filter(r -> r.getStatus() == Rental.RentalStatus.REQUESTED)
            .toList();
        List<Rental> approved = rentalService.getAwaitingOtpBySupplier(SessionManager.getInstance().getCurrentUser().getId());
        List<Rental> active = rentalService.getActiveRentalsBySupplier(SessionManager.getInstance().getCurrentUser().getId());
        List<Rental> allSupplierRentals = rentalService.getRentalsBySupplier(SessionManager.getInstance().getCurrentUser().getId());

        int totalPending = requested.size() + approved.size();
        pendingApprovals.setText(String.valueOf(totalPending));
        activeRentals.setText(String.valueOf(active.size()));
        totalRevenue.setText(ValidationUtil.formatCurrency(
            allSupplierRentals.stream().mapToDouble(r -> r.getTotalAmount() + r.getPenaltyAmount()).sum()
        ));
        if (SessionManager.getInstance().getCurrentUser() != null) {
            com.rento.models.User freshUser = userDAO.findById(SessionManager.getInstance().getCurrentUser().getId());
            walletBalance.setText(ValidationUtil.formatCurrency(freshUser != null ? freshUser.getWalletBalance() : 0));
        }

        approvalList.getChildren().clear();
        if (requested.isEmpty() && approved.isEmpty()) {
            noApprovalsLabel.setVisible(true);
        } else {
            noApprovalsLabel.setVisible(false);
            // Show requested rentals first (need approval)
            for (Rental r : requested) {
                approvalList.getChildren().add(createRequestCard(r));
            }
            // Then show approved rentals (awaiting OTP confirmation)
            for (Rental r : approved) {
                approvalList.getChildren().add(createApprovedCard(r));
            }
        }

        try {
            List<Vehicle> vehicles = vehicleDAO.findByOwner(SessionManager.getInstance().getCurrentUser().getId());
            totalVehicles.setText(String.valueOf(vehicles.size()));
            vehicleFleet.getChildren().clear();
            if (!vehicles.isEmpty()) {
                noVehiclesLabel.setVisible(false);
                for (Vehicle v : vehicles) {
                    vehicleFleet.getChildren().add(createVehicleCard(v));
                }
            }
        } catch (Exception ignored) {}

        updateCharts();
    }

    private HBox createRequestCard(Rental rental) {
        HBox card = new HBox(16);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 16; -fx-background-radius: 12px;");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(rental.getVehicleName() != null ? rental.getVehicleName() : "Vehicle");
        nameLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: white; -fx-font-size: 15px;");

        Label priceLabel = new Label("Customer: " + (rental.getRenterName() != null ? rental.getRenterName() : "Unknown"));
        priceLabel.getStyleClass().add("vehicle-price");

        Label periodLabel = new Label("Period: " + rental.getRentalDurationLabel() + " • " + ValidationUtil.formatCurrency(rental.getTotalAmount()));
        periodLabel.getStyleClass().add("text-muted");

        Label statusLabel = new Label("● " + Rental.RentalStatus.REQUESTED.name());
        statusLabel.getStyleClass().addAll("badge", "badge-warning");

        info.getChildren().addAll(nameLabel, priceLabel, periodLabel, statusLabel);

        VBox actions = new VBox(8);
        Button approveBtn = new Button("✓ Approve");
        approveBtn.getStyleClass().add("btn-accent");
        approveBtn.setStyle("-fx-padding: 8 20;");
        approveBtn.setOnAction(e -> {
            boolean ok = rentalService.approveRentalRequest(rental.getId());
            if (ok) {
                AlertUtil.showSuccess("Rental approved. OTP: " + rental.getApprovalOtp() + "\nShare with renter for activation.");
            }
            loadDashboard();
        });

        Button rejectBtn = new Button("✗ Reject");
        rejectBtn.getStyleClass().addAll("btn-danger", "btn-small");
        rejectBtn.setOnAction(e -> {
            rentalService.rejectRentalRequest(rental.getId());
            AlertUtil.showInfo("Rejected", "Rental request rejected and the vehicle remains available.");
            loadDashboard();
        });

        actions.getChildren().addAll(approveBtn, rejectBtn);
        card.getChildren().addAll(info, actions);
        return card;
    }

    private HBox createApprovedCard(Rental rental) {
        HBox card = new HBox(16);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 16; -fx-background-radius: 12px; -fx-border-color: #06d6a0; -fx-border-width: 2;");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(4);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label nameLabel = new Label(rental.getVehicleName() != null ? rental.getVehicleName() : "Vehicle");
        nameLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #06d6a0; -fx-font-size: 15px;");

        Label priceLabel = new Label("Customer: " + (rental.getRenterName() != null ? rental.getRenterName() : "Unknown"));
        priceLabel.getStyleClass().add("vehicle-price");

        Label periodLabel = new Label("Period: " + rental.getRentalDurationLabel() + " • " + ValidationUtil.formatCurrency(rental.getTotalAmount()));
        periodLabel.getStyleClass().add("text-muted");

        Label statusLabel = new Label("● Awaiting OTP Confirmation");
        statusLabel.getStyleClass().addAll("badge", "badge-success");

        Label paymentLabel = new Label("Payment: " + (rental.getPaymentMethod() != null ? rental.getPaymentMethod().replace('_', ' ') : "Pending"));
        paymentLabel.getStyleClass().add("text-muted");

        info.getChildren().addAll(nameLabel, priceLabel, periodLabel, statusLabel, paymentLabel);

        VBox actions = new VBox(8);
        TextField otpField = new TextField();
        otpField.setPromptText("OTP");
        otpField.setText(rental.getApprovalOtp() != null ? rental.getApprovalOtp() : "");
        otpField.setStyle("-fx-padding: 8;");

        Button confirmOtpBtn = new Button("Confirm OTP & Activate");
        confirmOtpBtn.getStyleClass().add("btn-secondary");
        confirmOtpBtn.setStyle("-fx-padding: 8 20;");
        confirmOtpBtn.setOnAction(e -> {
            boolean ok = rentalService.confirmSupplierOtp(rental.getId(), otpField.getText());
            if (ok) {
                AlertUtil.showSuccess("Rental confirmed and activated.");
            } else {
                AlertUtil.showWarning("Invalid OTP", "Please enter the correct OTP to activate rental.");
            }
            loadDashboard();
        });

        actions.getChildren().addAll(otpField, confirmOtpBtn);
        if (rental.isCashPaymentPending() && !rental.isPaidVerified()) {
            Button paidBtn = new Button("Mark Cash Paid");
            paidBtn.getStyleClass().add("btn-accent");
            paidBtn.setOnAction(e -> {
                boolean ok = rentalService.verifyCashPaymentForRental(rental.getId(), SessionManager.getInstance().getCurrentUserName());
                if (ok) {
                    AlertUtil.showSuccess("Cash payment verified. Receipt is available for the renter.");
                }
                loadDashboard();
            });
            actions.getChildren().add(paidBtn);
        } else if (rental.isPaidVerified()) {
            Label paidBadge = new Label("Paid Verified");
            paidBadge.getStyleClass().addAll("badge", "badge-success");
            actions.getChildren().add(paidBadge);
        }
        card.getChildren().addAll(info, actions);
        return card;
    }

    private VBox createVehicleCard(Vehicle vehicle) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPrefWidth(230);

        Label name = new Label(vehicle.getDisplayName());
        name.getStyleClass().add("card-title");

        String statusText = vehicle.getStatus() != null ? vehicle.getStatus().name().replace("_", " ") : "UNKNOWN";
        Label status = new Label("● " + statusText);
        String statusStyle = "badge-primary";
        if (vehicle.getStatus() == Vehicle.Status.AVAILABLE) statusStyle = "badge-success";
        else if (vehicle.getStatus() == Vehicle.Status.IN_USE) statusStyle = "badge-warning";
        else if (vehicle.getStatus() == Vehicle.Status.MAINTENANCE) statusStyle = "badge-danger";
        status.getStyleClass().addAll("badge", statusStyle);

        Label price = new Label("₹" + String.format("%.0f", vehicle.getDailyRate()) + "/day");
        price.getStyleClass().add("text-body");

        card.getChildren().addAll(name, status, price);
        return card;
    }

    private void updateCharts() {
        if (SessionManager.getInstance().getCurrentUser() == null) {
            return;
        }

        List<Vehicle> vehicles = vehicleDAO.findByOwner(SessionManager.getInstance().getCurrentUser().getId());
        long available = vehicles.stream().filter(v -> v.getStatus() == Vehicle.Status.AVAILABLE).count();
        long inUse = vehicles.stream().filter(v -> v.getStatus() == Vehicle.Status.IN_USE).count();
        long maintenance = vehicles.stream().filter(v -> v.getStatus() == Vehicle.Status.MAINTENANCE).count();

        fleetStatusChart.getData().setAll(
            new PieChart.Data("Available", available),
            new PieChart.Data("In Use", inUse),
            new PieChart.Data("Maintenance", maintenance)
        );

        List<Rental> rentals = rentalService.getRentalsBySupplier(SessionManager.getInstance().getCurrentUser().getId());
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Supplier Metrics");
        series.getData().add(new XYChart.Data<>("Requested", rentals.stream().filter(r -> r.getStatus() == Rental.RentalStatus.REQUESTED).count()));
        series.getData().add(new XYChart.Data<>("Active", rentals.stream().filter(r -> r.getStatus() == Rental.RentalStatus.ACTIVE).count()));
        series.getData().add(new XYChart.Data<>("Overdue", rentals.stream().filter(r -> r.getStatus() == Rental.RentalStatus.OVERDUE).count()));
        series.getData().add(new XYChart.Data<>("Completed", rentals.stream().filter(r -> r.getStatus() == Rental.RentalStatus.COMPLETED).count()));
        supplierTrendChart.getData().clear();
        supplierTrendChart.getData().add(series);
    }

    @FXML private void onRefresh() { loadDashboard(); }
    @FXML private void onAddVehicle() { NavigationManager.navigateTo("/fxml/rent.fxml"); }
    @FXML private void onNavHome() { NavigationManager.navigateTo("/fxml/landing.fxml"); }
    @FXML private void onNavProfile() { NavigationManager.navigateTo("/fxml/profile.fxml"); }
    @FXML private void onLogout() {
        authService.logout();
        NavigationManager.clearHistory();
        NavigationManager.navigateTo("/fxml/landing.fxml");
    }
}
