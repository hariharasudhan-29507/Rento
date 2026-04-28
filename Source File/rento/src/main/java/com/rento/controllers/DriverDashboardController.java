package com.rento.controllers;

import com.rento.models.Booking;
import com.rento.dao.UserDAO;
import com.rento.navigation.NavigationManager;
import com.rento.security.SessionManager;
import com.rento.services.AuthService;
import com.rento.services.BookingService;
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
 * Controller for the Driver Dashboard.
 */
public class DriverDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label activeRides;
    @FXML private Label completedRides;
    @FXML private Label pendingRequests;
    @FXML private Label earnings;
    @FXML private Label walletBalance;
    @FXML private VBox requestList;
    @FXML private Label noRequestsLabel;
    @FXML private TextField otpInput;
    @FXML private Label otpResult;
    @FXML private PieChart rideMixChart;
    @FXML private BarChart<String, Number> rideVolumeChart;

    private final BookingService bookingService = new BookingService();
    private final AuthService authService = new AuthService();
    private final UserDAO userDAO = new UserDAO();
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        welcomeLabel.setText("Welcome, " + SessionManager.getInstance().getCurrentUserName() + " (Driver)");
        loadDashboard();
    }

    private void loadDashboard() {
        List<Booking> pending = SessionManager.getInstance().getCurrentUser() != null
            ? bookingService.getPendingBookingsForDriver(SessionManager.getInstance().getCurrentUser().getId())
            : List.of();
        List<Booking> confirmed = SessionManager.getInstance().getCurrentUser() != null
            ? bookingService.getConfirmedBookingsForDriver(SessionManager.getInstance().getCurrentUser().getId())
            : List.of();
        List<Booking> myBookings = SessionManager.getInstance().getCurrentUser() != null
            ? bookingService.getBookingsByDriver(SessionManager.getInstance().getCurrentUser().getId())
            : List.of();
        pendingRequests.setText(String.valueOf(pending.size()));
        activeRides.setText(String.valueOf(confirmed.size() + myBookings.stream()
            .filter(b -> b.getStatus() == Booking.BookingStatus.IN_PROGRESS)
            .count()));
        completedRides.setText(String.valueOf(myBookings.stream()
            .filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED)
            .count()));
        double driverRevenue = myBookings.stream()
            .filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED)
            .mapToDouble(Booking::getTotalCost)
            .sum() * 0.15;
        earnings.setText(ValidationUtil.formatCurrency(driverRevenue));
        if (SessionManager.getInstance().getCurrentUser() != null) {
            com.rento.models.User freshUser = userDAO.findById(SessionManager.getInstance().getCurrentUser().getId());
            walletBalance.setText(ValidationUtil.formatCurrency(freshUser != null ? freshUser.getWalletBalance() : 0));
        }

        requestList.getChildren().clear();
        if (pending.isEmpty() && confirmed.isEmpty()) {
            noRequestsLabel.setVisible(true);
        } else {
            noRequestsLabel.setVisible(false);
            // Pending requests (for acceptance)
            for (Booking b : pending) {
                requestList.getChildren().add(createRequestCard(b));
            }
            // Confirmed bookings (for OTP verification)
            for (Booking b : confirmed) {
                requestList.getChildren().add(createConfirmedCard(b));
            }
        }

        updateCharts(pending, confirmed, myBookings);
    }

    private HBox createConfirmedCard(Booking booking) {
        HBox card = new HBox(16);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 16; -fx-background-radius: 12px; -fx-border-color: #06d6a0; -fx-border-width: 2;");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(4);
        VBox.setVgrow(info, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        Label vehicleLabel = new Label(booking.getVehicleName() != null ? booking.getVehicleName() : "Vehicle");
        vehicleLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #06d6a0; -fx-font-size: 15px;");

        Label statusLabel = new Label("● Confirmed - Awaiting OTP");
        statusLabel.getStyleClass().addAll("badge", "badge-success");

        Label locationLabel = new Label(
            (booking.getPickupLocation() != null ? booking.getPickupLocation() : "N/A") +
            " → " +
            (booking.getDropoffLocation() != null ? booking.getDropoffLocation() : "N/A"));
        locationLabel.getStyleClass().add("text-muted");

        Label dateLabel = new Label(booking.getPickupDateTime() != null ?
            DateTimeUtil.formatDateTime(booking.getPickupDateTime()) + " • " + booking.getRentalDurationLabel() : "TBD");
        dateLabel.getStyleClass().add("text-muted");

        Label otpLabel = new Label("OTP: " + (booking.getOtp() != null ? booking.getOtp() : "Pending"));
        otpLabel.setStyle("-fx-text-fill: #06d6a0; -fx-font-weight: 700;");

        info.getChildren().addAll(vehicleLabel, statusLabel, locationLabel, dateLabel, otpLabel);

        Label detailLabel = new Label("Payment: " + (booking.getPaymentMethod() != null ? booking.getPaymentMethod().replace('_', ' ') : "Pending"));
        detailLabel.getStyleClass().add("text-muted");
        info.getChildren().add(detailLabel);

        VBox actions = new VBox(8);
        if (booking.isCashPaymentPending() && !booking.isPaidVerified()) {
            Button paidBtn = new Button("Mark Cash Paid");
            paidBtn.getStyleClass().add("btn-accent");
            paidBtn.setOnAction(event -> {
                boolean ok = bookingService.verifyCashPaymentForBooking(booking.getId(), SessionManager.getInstance().getCurrentUserName());
                if (ok) {
                    AlertUtil.showSuccess("Cash payment verified. Receipt is now available to the customer.");
                }
                loadDashboard();
            });
            actions.getChildren().add(paidBtn);
        } else if (booking.isPaidVerified()) {
            Label paidLabel = new Label("Paid Verified");
            paidLabel.getStyleClass().addAll("badge", "badge-success");
            actions.getChildren().add(paidLabel);
        }

        card.getChildren().addAll(info, actions);
        return card;
    }

    private HBox createRequestCard(Booking booking) {
        HBox card = new HBox(16);
        card.getStyleClass().add("card");
        card.setStyle("-fx-padding: 16; -fx-background-radius: 12px;");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox info = new VBox(4);
        VBox.setVgrow(info, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        Label vehicleLabel = new Label(booking.getVehicleName() != null ? booking.getVehicleName() : "Vehicle");
        vehicleLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: white; -fx-font-size: 15px;");

        Label locationLabel = new Label(
            (booking.getPickupLocation() != null ? booking.getPickupLocation() : "N/A") +
            " → " +
            (booking.getDropoffLocation() != null ? booking.getDropoffLocation() : "N/A"));
        locationLabel.getStyleClass().add("text-muted");

        Label dateLabel = new Label(booking.getPickupDateTime() != null ?
            DateTimeUtil.formatDateTime(booking.getPickupDateTime()) + " • " + booking.getRentalDurationLabel() : "TBD");
        dateLabel.getStyleClass().add("text-muted");

        Label preferenceLabel = new Label(
            booking.getPreferredDriverName() != null
                ? "Requested driver: " + booking.getPreferredDriverName()
                : "Driver preference: Any available driver"
        );
        preferenceLabel.getStyleClass().add("text-muted");

        info.getChildren().addAll(vehicleLabel, locationLabel, dateLabel, preferenceLabel);

        Button acceptBtn = new Button("Accept ✓");
        acceptBtn.getStyleClass().add("btn-accent");
        acceptBtn.setStyle("-fx-padding: 8 20;");
        acceptBtn.setOnAction(e -> {
            boolean success = bookingService.confirmBooking(
                booking.getId(),
                SessionManager.getInstance().getCurrentUser() != null ? SessionManager.getInstance().getCurrentUser().getId() : null
            );
            if (success) {
                AlertUtil.showSuccess("Ride accepted. Waiting for customer OTP verification.");
            } else {
                AlertUtil.showError("Request Locked", "This booking is reserved for a different preferred driver.");
            }
            loadDashboard();
        });

        card.getChildren().addAll(info, acceptBtn);
        return card;
    }

    @FXML
    private void onVerifyOTP() {
        String otp = otpInput.getText().trim();
        if (otp.isEmpty()) {
            otpResult.setText("Please enter an OTP");
            otpResult.setStyle("-fx-text-fill: #f72585;");
            otpResult.setVisible(true);
            return;
        }

        if (otp.length() == 6 && otp.matches("\\d+")
            && SessionManager.getInstance().getCurrentUser() != null
            && bookingService.verifyRideOtp(SessionManager.getInstance().getCurrentUser().getId(), otp)) {
            otpResult.setText("✓ OTP Verified! Ride started.");
            otpResult.setStyle("-fx-text-fill: #06d6a0; -fx-font-weight: 700;");
            loadDashboard();
        } else {
            otpResult.setText("✗ Invalid OTP. Please try again.");
            otpResult.setStyle("-fx-text-fill: #f72585;");
        }
        otpResult.setVisible(true);
    }

    @FXML private void onRefresh() { loadDashboard(); }
    @FXML private void onNavHome() { NavigationManager.navigateTo("/fxml/landing.fxml"); }
    @FXML private void onNavProfile() { NavigationManager.navigateTo("/fxml/profile.fxml"); }
    @FXML private void onLogout() {
        authService.logout();
        NavigationManager.clearHistory();
        NavigationManager.navigateTo("/fxml/landing.fxml");
    }

    private void updateCharts(List<Booking> pending, List<Booking> confirmed, List<Booking> myBookings) {
        long inProgress = myBookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.IN_PROGRESS).count();
        long completed = myBookings.stream().filter(b -> b.getStatus() == Booking.BookingStatus.COMPLETED).count();

        rideMixChart.getData().setAll(
            new PieChart.Data("Pending Pool", pending.size()),
            new PieChart.Data("Confirmed", confirmed.size()),
            new PieChart.Data("In Progress", inProgress),
            new PieChart.Data("Completed", completed)
        );

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Driver Bookings");
        series.getData().add(new XYChart.Data<>("Pending", pending.size()));
        series.getData().add(new XYChart.Data<>("Confirmed", confirmed.size()));
        series.getData().add(new XYChart.Data<>("In Progress", inProgress));
        series.getData().add(new XYChart.Data<>("Completed", completed));
        rideVolumeChart.getData().clear();
        rideVolumeChart.getData().add(series);
    }
}
