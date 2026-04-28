package com.rento.controllers;

import com.rento.dao.BookingDAO;
import com.rento.dao.PaymentDAO;
import com.rento.dao.UserDAO;
import com.rento.dao.VehicleDAO;
import com.rento.models.Booking;
import com.rento.models.Payment;
import com.rento.models.Rental;
import com.rento.models.User;
import com.rento.models.Vehicle;
import com.rento.navigation.NavigationManager;
import com.rento.security.SessionManager;
import com.rento.services.AuthService;
import com.rento.services.AdminExportService;
import com.rento.services.SystemCollectionBootstrapService;
import com.rento.utils.AlertUtil;
import com.rento.services.RentalService;
import com.rento.utils.ValidationUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Administrative dashboard for monitoring all actors and platform activity.
 */
public class AdminDashboardController implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private Label totalUsers;
    @FXML private Label totalDrivers;
    @FXML private Label totalSuppliers;
    @FXML private Label totalCollections;
    @FXML private Label totalVehicles;
    @FXML private Label totalBookings;
    @FXML private Label totalRentals;
    @FXML private Label overduePenaltyTotal;
    @FXML private PieChart actorMixChart;
    @FXML private BarChart<String, Number> operationsChart;
    @FXML private VBox actorList;
    @FXML private VBox userManagementList;
    @FXML private VBox driverManagementList;
    @FXML private VBox supplierManagementList;
    @FXML private VBox incidentList;
    @FXML private VBox supplierChangeList;
    @FXML private Label noSupplierChangesLabel;
    @FXML private ComboBox<String> mailRecipientCombo;
    @FXML private TextField mailSubjectField;
    @FXML private TextArea mailBodyArea;
    @FXML private TextArea systemDataArea;

    private final UserDAO userDAO = new UserDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final RentalService rentalService = new RentalService();
    private final AuthService authService = new AuthService();
    private final AdminExportService adminExportService = new AdminExportService();
    private final SystemCollectionBootstrapService collectionBootstrapService = new SystemCollectionBootstrapService();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        welcomeLabel.setText("Welcome, " + SessionManager.getInstance().getCurrentUserName() + " (Administrator)");
        loadDashboard();
    }

    private void loadDashboard() {
        List<User> users = userDAO.findAll();
        List<Vehicle> vehicles = vehicleDAO.findAll();
        List<Booking> bookings = bookingDAO.findAll();
        List<Rental> rentals = rentalService.getAllRentals();
        List<Payment> payments = paymentDAO.findAll();

        totalUsers.setText(String.valueOf(users.stream().filter(u -> u.getRole() == User.Role.USER).count()));
        totalDrivers.setText(String.valueOf(users.stream().filter(u -> u.getRole() == User.Role.DRIVER).count()));
        totalSuppliers.setText(String.valueOf(users.stream().filter(u -> u.getRole() == User.Role.SUPPLIER).count()));
        totalCollections.setText(String.valueOf(collectionBootstrapService.getCollectionCounts().size()));
        totalVehicles.setText(String.valueOf(vehicles.size()));
        totalBookings.setText(String.valueOf(bookings.size()));
        totalRentals.setText(String.valueOf(rentals.size()));
        overduePenaltyTotal.setText(ValidationUtil.formatCurrency(
            rentals.stream().mapToDouble(Rental::getPenaltyAmount).sum()
        ));

        updateCharts(users, bookings, rentals, payments);
        updateActorList(users);
        updateManagedActorLists(users);
        updateIncidentList(rentals, vehicles);
        updateSupplierChangeList(vehicles);
        populateMailRecipients(users);
        updateSystemDataArea(users, vehicles, bookings, rentals, payments);
    }

    private void updateCharts(List<User> users, List<Booking> bookings, List<Rental> rentals, List<Payment> payments) {
        long customers = users.stream().filter(u -> u.getRole() == User.Role.USER).count();
        long suppliers = users.stream().filter(u -> u.getRole() == User.Role.SUPPLIER).count();
        long drivers = users.stream().filter(u -> u.getRole() == User.Role.DRIVER).count();
        long admins = users.stream().filter(u -> u.getRole() == User.Role.ADMIN).count();

        actorMixChart.getData().setAll(
            new PieChart.Data("Customers", customers),
            new PieChart.Data("Suppliers", suppliers),
            new PieChart.Data("Drivers", drivers),
            new PieChart.Data("Admins", admins)
        );

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Platform Volume");
        series.getData().add(new XYChart.Data<>("Bookings", bookings.size()));
        series.getData().add(new XYChart.Data<>("Rentals", rentals.size()));
        series.getData().add(new XYChart.Data<>("Payments", payments.size()));
        series.getData().add(new XYChart.Data<>("Overdue", rentals.stream().filter(r -> r.getStatus() == Rental.RentalStatus.OVERDUE).count()));
        operationsChart.getData().clear();
        operationsChart.getData().add(series);
    }

    private void updateActorList(List<User> users) {
        actorList.getChildren().clear();
        for (User user : users) {
            Label item = new Label(user.getFullName() + " • " + user.getRole().name() + " • "
                + user.getEmail() + " • Wallet " + ValidationUtil.formatCurrency(user.getWalletBalance())
                + (user.isLocked() ? " • LOCKED" : " • ACTIVE"));
            item.getStyleClass().add("text-body");
            actorList.getChildren().add(item);
        }
    }

    private void updateManagedActorLists(List<User> users) {
        userManagementList.getChildren().clear();
        driverManagementList.getChildren().clear();
        supplierManagementList.getChildren().clear();

        for (User user : users) {
            if (user.getRole() == User.Role.USER) {
                userManagementList.getChildren().add(createActorAdminCard(user));
            } else if (user.getRole() == User.Role.DRIVER) {
                driverManagementList.getChildren().add(createActorAdminCard(user));
            } else if (user.getRole() == User.Role.SUPPLIER) {
                supplierManagementList.getChildren().add(createActorAdminCard(user));
            }
        }
    }

    private VBox createActorAdminCard(User user) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");

        Label name = new Label(user.getFullName() + " • " + user.getEmail());
        name.getStyleClass().add("card-title");

        Label meta = new Label("Phone: " + user.getPhone() + " • Wallet: " + ValidationUtil.formatCurrency(user.getWalletBalance())
            + " • Status: " + (user.isLocked() ? "Locked" : "Active"));
        meta.getStyleClass().add("text-muted");

        Button actionBtn = new Button(user.isLocked() ? "Unlock Account" : "Lock Account");
        actionBtn.getStyleClass().add(user.isLocked() ? "btn-accent" : "btn-danger");
        actionBtn.setOnAction(event -> {
            boolean updated = userDAO.updateLockState(user.getId(), !user.isLocked(),
                user.isLocked() ? null : "Locked by admin");
            if (updated) {
                loadDashboard();
            }
        });

        card.getChildren().addAll(name, meta, actionBtn);
        return card;
    }

    private void updateIncidentList(List<Rental> rentals, List<Vehicle> vehicles) {
        incidentList.getChildren().clear();

        rentals.stream()
            .filter(r -> r.getStatus() == Rental.RentalStatus.OVERDUE)
            .forEach(r -> {
                Label item = new Label("Overdue rental: " + r.getVehicleName() + " • penalty " + ValidationUtil.formatCurrency(r.getPenaltyAmount()));
                item.getStyleClass().add("text-error");
                incidentList.getChildren().add(item);
            });

        vehicles.stream()
            .filter(v -> v.getStatus() == Vehicle.Status.MAINTENANCE)
            .forEach(v -> {
                Label item = new Label("Maintenance alert: " + v.getDisplayName());
                item.getStyleClass().add("text-warning");
                incidentList.getChildren().add(item);
            });

        if (incidentList.getChildren().isEmpty()) {
            Label item = new Label("No critical incidents right now.");
            item.getStyleClass().add("text-muted");
            incidentList.getChildren().add(item);
        }
    }

    private void updateSupplierChangeList(List<Vehicle> vehicles) {
        supplierChangeList.getChildren().clear();
        List<Vehicle> pendingVehicles = vehicles.stream()
            .filter(vehicle -> vehicle.getOwnerId() != null)
            .filter(vehicle -> vehicle.getApprovalStatus() == Vehicle.ApprovalStatus.PENDING)
            .toList();

        noSupplierChangesLabel.setVisible(pendingVehicles.isEmpty());
        for (Vehicle vehicle : pendingVehicles) {
            VBox card = new VBox(8);
            card.getStyleClass().add("card");

            Label title = new Label(vehicle.getDisplayName() + " • " + ValidationUtil.formatCurrency(vehicle.getDailyRate()) + "/day");
            title.getStyleClass().add("card-title");

            Label meta = new Label("Supplier change waiting for admin approval");
            meta.getStyleClass().add("text-muted");

            Button approveButton = new Button("Approve Vehicle");
            approveButton.getStyleClass().add("btn-accent");
            approveButton.setOnAction(event -> {
                vehicle.setApprovalStatus(Vehicle.ApprovalStatus.APPROVED);
                vehicle.setAdminReviewNote("Approved by admin");
                vehicleDAO.updateVehicle(vehicle);
                loadDashboard();
            });

            Button rejectButton = new Button("Reject Vehicle");
            rejectButton.getStyleClass().add("btn-danger");
            rejectButton.setOnAction(event -> {
                vehicle.setApprovalStatus(Vehicle.ApprovalStatus.REJECTED);
                vehicle.setAdminReviewNote("Rejected by admin");
                vehicleDAO.updateVehicle(vehicle);
                loadDashboard();
            });

            card.getChildren().addAll(title, meta, approveButton, rejectButton);
            supplierChangeList.getChildren().add(card);
        }
    }

    private void populateMailRecipients(List<User> users) {
        String currentSelection = mailRecipientCombo.getValue();
        mailRecipientCombo.getItems().clear();
        for (User user : users) {
            mailRecipientCombo.getItems().add(user.getFullName() + " <" + user.getEmail() + ">");
        }
        if (currentSelection != null && mailRecipientCombo.getItems().contains(currentSelection)) {
            mailRecipientCombo.setValue(currentSelection);
        } else if (!mailRecipientCombo.getItems().isEmpty()) {
            mailRecipientCombo.setValue(mailRecipientCombo.getItems().get(0));
        }
    }

    private void updateSystemDataArea(List<User> users, List<Vehicle> vehicles, List<Booking> bookings,
                                      List<Rental> rentals, List<Payment> payments) {
        StringBuilder builder = new StringBuilder();
        builder.append("Collection Summary\n");
        collectionBootstrapService.getCollectionCounts().forEach((name, count) ->
            builder.append("- ").append(name).append(": ").append(count).append('\n')
        );
        builder.append("\nUsers\n");
        users.forEach(user -> builder.append(user.getFullName()).append(" | ").append(user.getRole()).append(" | ")
            .append(user.isLocked() ? "LOCKED" : "ACTIVE").append('\n'));
        builder.append("\nVehicles\n");
        vehicles.forEach(vehicle -> builder.append(vehicle.getDisplayName()).append(" | ").append(vehicle.getStatus()).append(" | ")
            .append(vehicle.getApprovalStatus()).append('\n'));
        builder.append("\nBookings\n");
        bookings.forEach(booking -> builder.append(booking.getVehicleName()).append(" | ").append(booking.getStatus()).append(" | ")
            .append(booking.getPaymentStatus()).append('\n'));
        builder.append("\nRentals\n");
        rentals.forEach(rental -> builder.append(rental.getVehicleName()).append(" | ").append(rental.getStatus()).append(" | ")
            .append(rental.getPaymentStatus()).append('\n'));
        builder.append("\nPayments\n");
        payments.forEach(payment -> builder.append(payment.getTransactionRef()).append(" | ")
            .append(payment.getPaymentMethod()).append(" | ").append(payment.getStatus()).append('\n'));
        systemDataArea.setText(builder.toString());
    }

    @FXML
    private void onSendMail() {
        String recipient = mailRecipientCombo.getValue();
        String subject = mailSubjectField.getText() != null ? mailSubjectField.getText().trim() : "";
        String body = mailBodyArea.getText() != null ? mailBodyArea.getText().trim() : "";

        if (recipient == null || recipient.isBlank() || subject.isBlank() || body.isBlank()) {
            System.out.println("[Admin Mail] Missing recipient, subject, or body.");
            return;
        }

        System.out.println("[Admin Mail] To: " + recipient);
        System.out.println("[Admin Mail] Subject: " + subject);
        System.out.println("[Admin Mail] Body: " + body);

        mailSubjectField.clear();
        mailBodyArea.clear();
    }

    @FXML private void onRefresh() { loadDashboard(); }
    @FXML
    private void onDownloadSystemExport() {
        try {
            String outputDir = System.getProperty("user.home") + "\\Documents\\RentoAdminExports";
            String path = adminExportService.exportAllData(outputDir);
            AlertUtil.showSuccess("System export generated:\n" + path);
        } catch (Exception ex) {
            AlertUtil.showError("Export Failed", ex.getMessage());
        }
    }
    @FXML private void onNavHome() { NavigationManager.navigateTo("/fxml/landing.fxml"); }
    @FXML private void onNavProfile() { NavigationManager.navigateTo("/fxml/profile.fxml"); }
    @FXML private void onLogout() {
        authService.logout();
        NavigationManager.clearHistory();
        NavigationManager.navigateTo("/fxml/landing.fxml");
    }
}
