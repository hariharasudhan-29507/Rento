package com.rento.controllers;

import com.rento.dao.VehicleDAO;
import com.rento.models.Payment;
import com.rento.models.Rental;
import com.rento.models.User;
import com.rento.models.Vehicle;
import com.rento.navigation.NavigationManager;
import com.rento.security.SessionManager;
import com.rento.services.PaymentService;
import com.rento.services.RentalService;
import com.rento.utils.AlertUtil;
import com.rento.utils.DateTimeUtil;
import com.rento.utils.ValidationUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Role-aware marketplace for supplier listings and user rental requests.
 */
public class RentController implements Initializable {

    @FXML private TextField makeField;
    @FXML private TextField modelField;
    @FXML private TextField yearField;
    @FXML private TextField plateField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private ComboBox<String> fuelCombo;
    @FXML private TextField seatsField;
    @FXML private TextField priceField;
    @FXML private TextArea descField;
    @FXML private Label errorLabel;
    @FXML private Button profileBtn;
    @FXML private Label pageSubtitle;

    @FXML private VBox supplierListingSection;
    @FXML private FlowPane listingGrid;
    @FXML private Label noListingsLabel;
    @FXML private Label supplierActionLabel;

    @FXML private VBox marketplaceSection;
    @FXML private FlowPane marketplaceGrid;
    @FXML private Label noMarketplaceLabel;
    @FXML private Label selectedVehicleLabel;
    @FXML private Label selectedSupplierLabel;
    @FXML private Label selectedRateLabel;
    @FXML private DatePicker requestStartDate;
    @FXML private DatePicker requestEndDate;
    @FXML private ComboBox<String> requestStartTimeCombo;
    @FXML private ComboBox<String> requestEndTimeCombo;
    @FXML private ComboBox<String> requestUnitCombo;
    @FXML private TextField requestHoursField;
    @FXML private ComboBox<String> rentalPaymentMethodCombo;
    @FXML private TextField rentalPaymentReferenceField;
    @FXML private TextField rentalPaymentHolderField;
    @FXML private Label requestInfoLabel;
    @FXML private Button requestRentalBtn;

    @FXML private VBox myRentalsSection;
    @FXML private FlowPane myRentalsGrid;
    @FXML private Label noUserRentalsLabel;

    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final RentalService rentalService = new RentalService();
    private final PaymentService paymentService = new PaymentService();
    private Vehicle selectedVehicle;
    private Vehicle editingVehicle;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        updateProfileButton();

        categoryCombo.setItems(FXCollections.observableArrayList(
            "SEDAN", "SUV", "HATCHBACK", "COUPE", "TRUCK", "VAN", "BIKE", "BUS"));
        categoryCombo.setValue("SEDAN");

        fuelCombo.setItems(FXCollections.observableArrayList(
            "PETROL", "DIESEL", "ELECTRIC", "HYBRID", "CNG"));
        fuelCombo.setValue("PETROL");

        requestStartDate.setValue(LocalDate.now().plusDays(1));
        requestEndDate.setValue(LocalDate.now().plusDays(1));
        requestStartTimeCombo.setItems(FXCollections.observableArrayList(DateTimeUtil.buildTimeSlots()));
        requestEndTimeCombo.setItems(FXCollections.observableArrayList(DateTimeUtil.buildTimeSlots()));
        requestStartTimeCombo.setValue("09:00 AM");
        requestEndTimeCombo.setValue("06:00 PM");
        requestUnitCombo.setItems(FXCollections.observableArrayList("DAYS", "HOURS"));
        requestUnitCombo.setValue("DAYS");
        requestUnitCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateRentalUnitState());
        rentalPaymentMethodCombo.setItems(FXCollections.observableArrayList("Credit Card", "UPI", "Cash on Delivery"));
        rentalPaymentMethodCombo.setValue("Credit Card");
        rentalPaymentMethodCombo.valueProperty().addListener((obs, oldValue, newValue) -> updateRentalPaymentFields());
        updateRentalPaymentFields();
        updateRentalUnitState();

        configureSectionsByRole();
        loadSupplierListings();
        loadMarketplace();
        loadMyRentals();
    }

    private void configureSectionsByRole() {
        User.Role role = SessionManager.getInstance().getCurrentRole();
        boolean supplierMode = role == User.Role.SUPPLIER || role == User.Role.ADMIN;
        boolean renterMode = role == User.Role.USER || role == User.Role.ADMIN;

        supplierListingSection.setManaged(supplierMode);
        supplierListingSection.setVisible(supplierMode);

        marketplaceSection.setManaged(renterMode);
        marketplaceSection.setVisible(renterMode);

        myRentalsSection.setManaged(renterMode);
        myRentalsSection.setVisible(renterMode);

        if (role == User.Role.SUPPLIER) {
            pageSubtitle.setText("Publish vehicles for rent and monitor customer requests from your supplier dashboard.");
        } else if (role == User.Role.ADMIN) {
            pageSubtitle.setText("Review the supplier marketplace and simulate the full rental lifecycle.");
        } else {
            pageSubtitle.setText("Choose a supplier vehicle, request a rental period, and finish on time to avoid penalties.");
        }
    }

    private void updateProfileButton() {
        if (SessionManager.getInstance().isLoggedIn()) {
            profileBtn.setText("⬤ " + SessionManager.getInstance().getCurrentUserName());
        }
    }

    @FXML
    private void onSubmitVehicle() {
        clearError();

        if (!ValidationUtil.isNotEmpty(makeField.getText())) { showError("Make is required"); return; }
        if (!ValidationUtil.isNotEmpty(modelField.getText())) { showError("Model is required"); return; }
        if (!ValidationUtil.isNotEmpty(yearField.getText())) { showError("Year is required"); return; }
        if (!ValidationUtil.isNotEmpty(priceField.getText())) { showError("Price is required"); return; }
        if (makeField.getText().trim().length() < 2 || modelField.getText().trim().length() < 2) { showError("Make and model should each be at least 2 characters"); return; }
        if (!plateField.getText().trim().isEmpty() && plateField.getText().trim().length() < 6) { showError("License plate must be at least 6 characters"); return; }

        try {
            int year = Integer.parseInt(yearField.getText().trim());
            double price = Double.parseDouble(priceField.getText().trim());
            int seats = seatsField.getText().isEmpty() ? 5 : Integer.parseInt(seatsField.getText().trim());
            if (year < 2000 || year > 2035) { showError("Year must be between 2000 and 2035"); return; }
            if (price <= 0) { showError("Price per day must be greater than zero"); return; }
            if (seats <= 0 || seats > 60) { showError("Seats must be between 1 and 60"); return; }
            if (descField.getText() != null && descField.getText().trim().length() < 10) { showError("Description should be at least 10 characters"); return; }

            Vehicle vehicle = new Vehicle();
            vehicle.setMake(makeField.getText().trim());
            vehicle.setModel(modelField.getText().trim());
            vehicle.setYear(year);
            vehicle.setLicensePlate(plateField.getText().trim());
            vehicle.setCategory(Vehicle.Category.valueOf(categoryCombo.getValue()));
            vehicle.setFuelType(Vehicle.FuelType.valueOf(fuelCombo.getValue()));
            vehicle.setSeats(seats);
            vehicle.setDailyRate(price);
            vehicle.setDescription(descField.getText().trim());
            vehicle.setBranchLocation("Supplier Hub");
            vehicle.setOwnerId(SessionManager.getInstance().getCurrentUser().getId());
            vehicle.setApprovalStatus(Vehicle.ApprovalStatus.PENDING);

            if (editingVehicle != null) {
                vehicle.setId(editingVehicle.getId());
                vehicle.setCreatedAt(editingVehicle.getCreatedAt());
                vehicleDAO.updateVehicle(vehicle);
                AlertUtil.showSuccess("Vehicle update sent to admin for approval.");
            } else {
                rentalService.addVehicleForRent(vehicle);
                AlertUtil.showSuccess("Vehicle listing sent to admin for approval.");
            }
            clearForm();
            loadSupplierListings();
            loadMarketplace();
        } catch (NumberFormatException e) {
            showError("Please enter valid numbers for year, seats, and price");
        } catch (Exception e) {
            showError(e.getMessage());
        }
    }

    private void loadSupplierListings() {
        listingGrid.getChildren().clear();
        noListingsLabel.setVisible(true);

        if (SessionManager.getInstance().getCurrentUser() == null) {
            return;
        }

        List<Vehicle> vehicles = vehicleDAO.findByOwner(SessionManager.getInstance().getCurrentUser().getId());
        if (!vehicles.isEmpty()) {
            noListingsLabel.setVisible(false);
            for (Vehicle vehicle : vehicles) {
                listingGrid.getChildren().add(createListingCard(vehicle));
            }
        }
    }

    private void loadMarketplace() {
        marketplaceGrid.getChildren().clear();
        List<Vehicle> vehicles = rentalService.getMarketplaceVehicles();
        noMarketplaceLabel.setVisible(vehicles.isEmpty());
        for (Vehicle vehicle : vehicles) {
            marketplaceGrid.getChildren().add(createMarketplaceCard(vehicle));
        }
    }

    private void loadMyRentals() {
        myRentalsGrid.getChildren().clear();
        noUserRentalsLabel.setVisible(true);

        if (SessionManager.getInstance().getCurrentUser() == null) {
            return;
        }

        List<Rental> rentals = rentalService.getRentalsByRenter(SessionManager.getInstance().getCurrentUser().getId());
        if (!rentals.isEmpty()) {
            noUserRentalsLabel.setVisible(false);
            for (Rental rental : rentals) {
                myRentalsGrid.getChildren().add(createUserRentalCard(rental));
            }
        }
    }

    private VBox createListingCard(Vehicle vehicle) {
        VBox card = new VBox(8);
        card.getStyleClass().add("card");
        card.setPrefWidth(250);

        Label name = new Label(vehicle.getDisplayName());
        name.getStyleClass().add("card-title");

        Label status = new Label("● " + vehicle.getStatus().name());
        status.getStyleClass().addAll("badge", vehicle.getStatus() == Vehicle.Status.AVAILABLE ? "badge-success" : "badge-warning");

        Vehicle.ApprovalStatus approvalStatus = vehicle.getApprovalStatus() != null
            ? vehicle.getApprovalStatus()
            : Vehicle.ApprovalStatus.APPROVED;
        Label approval = new Label("Approval: " + approvalStatus.name());
        approval.getStyleClass().addAll("badge",
            approvalStatus == Vehicle.ApprovalStatus.APPROVED ? "badge-success"
                : approvalStatus == Vehicle.ApprovalStatus.REJECTED ? "badge-danger" : "badge-warning");

        Label rate = new Label("₹" + String.format("%.0f", vehicle.getDailyRate()) + "/day");
        rate.getStyleClass().add("vehicle-price");

        Label details = new Label((vehicle.getCategory() != null ? vehicle.getCategory().name() : "N/A") + " • "
            + vehicle.getSeats() + " seats");
        details.getStyleClass().add("text-muted");

        Button editBtn = new Button("Edit & Resubmit");
        editBtn.getStyleClass().add("btn-secondary");
        editBtn.setOnAction(e -> beginEdit(vehicle));

        card.getChildren().addAll(name, status, approval, rate, details, editBtn);
        return card;
    }

    private VBox createMarketplaceCard(Vehicle vehicle) {
        VBox card = new VBox(10);
        card.getStyleClass().add("vehicle-card");
        card.setPrefWidth(260);

        VBox body = new VBox(8);
        body.getStyleClass().add("vehicle-card-body");

        Label name = new Label(vehicle.getDisplayName());
        name.getStyleClass().add("vehicle-name");

        Label rate = new Label("₹" + String.format("%.0f", vehicle.getDailyRate()) + "/day");
        rate.getStyleClass().add("vehicle-price");

        Label details = new Label((vehicle.getCategory() != null ? vehicle.getCategory().name() : "N/A") + " • "
            + vehicle.getFuelType() + " • " + vehicle.getSeats() + " seats");
        details.getStyleClass().add("text-muted");

        Button selectBtn = new Button("Select Rental");
        selectBtn.getStyleClass().add("btn-primary");
        selectBtn.setMaxWidth(Double.MAX_VALUE);
        selectBtn.setOnAction(e -> selectVehicle(vehicle));

        body.getChildren().addAll(name, rate, details, selectBtn);
        card.getChildren().add(body);
        return card;
    }

    private VBox createUserRentalCard(Rental rental) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPrefWidth(280);

        Label name = new Label(rental.getVehicleName());
        name.getStyleClass().add("card-title");

        String badgeStyle = rental.getStatus() == Rental.RentalStatus.COMPLETED ? "badge-success"
            : rental.getStatus() == Rental.RentalStatus.OVERDUE ? "badge-danger"
            : rental.getStatus() == Rental.RentalStatus.APPROVED ? "badge-primary"
            : rental.getStatus() == Rental.RentalStatus.REJECTED ? "badge-warning"
            : "badge-primary";
        Label status = new Label("● " + rental.getStatus().name().replace('_', ' '));
        status.getStyleClass().addAll("badge", badgeStyle);

        Label schedule = new Label("Period: " + rental.getRentalDurationLabel() + " • " + rental.getRentalDays() + " billable day(s)");
        schedule.getStyleClass().add("text-muted");

        Label total = new Label("Base: " + ValidationUtil.formatCurrency(rental.getTotalAmount()));
        total.getStyleClass().add("text-body");

        Label penalty = new Label("Penalty: " + ValidationUtil.formatCurrency(rental.getPenaltyAmount()));
        penalty.getStyleClass().add(rental.getPenaltyAmount() > 0 ? "text-error" : "text-muted");

        card.getChildren().addAll(name, status, schedule, total, penalty);

        if (rental.getStatus() == Rental.RentalStatus.ACTIVE || rental.getStatus() == Rental.RentalStatus.OVERDUE) {
            Button finishBtn = new Button("Mark Rental Finished");
            finishBtn.getStyleClass().add(rental.getStatus() == Rental.RentalStatus.OVERDUE ? "btn-danger" : "btn-accent");
            finishBtn.setOnAction(e -> {
                boolean success = rentalService.completeRental(rental.getId());
                if (success) {
                    String message = rental.getPenaltyAmount() > 0
                        ? "Rental completed. Penalty charged: " + ValidationUtil.formatCurrency(rental.getPenaltyAmount())
                        : "Rental completed successfully.";
                    AlertUtil.showSuccess(message);
                    loadMarketplace();
                    loadMyRentals();
                }
            });
            card.getChildren().add(finishBtn);
        }

        return card;
    }

    private void selectVehicle(Vehicle vehicle) {
        this.selectedVehicle = vehicle;
        selectedVehicleLabel.setText(vehicle.getDisplayName());
        selectedSupplierLabel.setText(vehicle.getOwnerId() != null ? "Approved supplier listing" : "Direct listing");
        selectedRateLabel.setText(ValidationUtil.formatCurrency(vehicle.getDailyRate()) + "/day");
        requestInfoLabel.setText("Choose your rental period and submit a request.");
        requestInfoLabel.getStyleClass().remove("text-error");
    }

    @FXML
    private void onRequestRental() {
        if (selectedVehicle == null) {
            requestInfoLabel.setText("Please select a vehicle from the marketplace first.");
            return;
        }
        if (SessionManager.getInstance().isGuest()) {
            NavigationManager.navigateTo("/fxml/login.fxml");
            return;
        }
        if (SessionManager.getInstance().getCurrentRole() == User.Role.SUPPLIER) {
            requestInfoLabel.setText("Supplier accounts cannot request rentals.");
            return;
        }

        LocalDate start = requestStartDate.getValue();
        LocalDate end = requestEndDate.getValue();
        if (start == null || end == null) {
            requestInfoLabel.setText("Please choose a valid rental period.");
            return;
        }
        if (!validateRentalPaymentInputs()) {
            return;
        }

        try {
            Date startDate = DateTimeUtil.toDate(start, requestStartTimeCombo.getValue());
            Date endDate = DateTimeUtil.toDate(end, requestEndTimeCombo.getValue());
            if ("HOURS".equals(requestUnitCombo.getValue())) {
                int hours = 0;
                try {
                    hours = Integer.parseInt(requestHoursField.getText().trim());
                } catch (Exception ignored) {
                }
                if (hours <= 0) {
                    requestInfoLabel.setText("Please enter valid rental hours.");
                    return;
                }
                endDate = new Date(startDate.getTime() + (hours * 60L * 60L * 1000L));
            } else if (!endDate.after(startDate)) {
                requestInfoLabel.setText("Drop date/time must be after pickup date/time.");
                return;
            }
            Rental rental = rentalService.requestRental(
                selectedVehicle.getId(),
                SessionManager.getInstance().getCurrentUser().getId(),
                SessionManager.getInstance().getCurrentUserName(),
                startDate,
                endDate
            );
            Payment payment = paymentService.createRentalPayment(
                rental.getId(),
                SessionManager.getInstance().getCurrentUser().getId(),
                rental.getTotalAmount(),
                rentalMethodFromSelection(),
                rentalPaymentReferenceField.getText(),
                rentalPaymentHolderField.getText().isBlank()
                    ? SessionManager.getInstance().getCurrentUserName()
                    : rentalPaymentHolderField.getText().trim()
            );
            if (payment == null) {
                requestInfoLabel.setText("Rental request created, but payment validation failed. Please retry with a valid payment method.");
                return;
            }
            rentalService.attachPaymentToRental(rental.getId(), payment);
            AlertUtil.showSuccess("Rental request sent to supplier for approval.");
            requestInfoLabel.setText("Request submitted for " + rental.getVehicleName() + ".");
            loadMyRentals();
            loadMarketplace();
        } catch (Exception e) {
            requestInfoLabel.setText(e.getMessage());
        }
    }

    private void clearForm() {
        editingVehicle = null;
        makeField.clear();
        modelField.clear();
        yearField.clear();
        plateField.clear();
        seatsField.clear();
        priceField.clear();
        descField.clear();
        if (supplierActionLabel != null) {
            supplierActionLabel.setText("Publish a brand new supplier vehicle");
        }
    }

    @FXML private void onRefreshMarketplace() { loadMarketplace(); loadMyRentals(); }
    @FXML private void onNavHome() { NavigationManager.navigateTo("/fxml/landing.fxml"); }
    @FXML private void onNavBook() { NavigationManager.navigateTo("/fxml/booking.fxml"); }
    @FXML private void onNavContact() { NavigationManager.navigateTo("/fxml/contact.fxml"); }
    @FXML private void onNavProfile() {
        if (SessionManager.getInstance().isGuest()) NavigationManager.navigateTo("/fxml/login.fxml");
        else NavigationManager.navigateTo("/fxml/profile.fxml");
    }

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    private void clearError() { errorLabel.setText(""); errorLabel.setVisible(false); }

    private void beginEdit(Vehicle vehicle) {
        editingVehicle = vehicle;
        makeField.setText(vehicle.getMake());
        modelField.setText(vehicle.getModel());
        yearField.setText(String.valueOf(vehicle.getYear()));
        plateField.setText(vehicle.getLicensePlate());
        if (vehicle.getCategory() != null) {
            categoryCombo.setValue(vehicle.getCategory().name());
        }
        if (vehicle.getFuelType() != null) {
            fuelCombo.setValue(vehicle.getFuelType().name());
        }
        seatsField.setText(String.valueOf(vehicle.getSeats()));
        priceField.setText(String.valueOf((int) vehicle.getDailyRate()));
        descField.setText(vehicle.getDescription() != null ? vehicle.getDescription() : "");
        if (supplierActionLabel != null) {
            supplierActionLabel.setText("Editing existing vehicle. Save to request admin approval.");
        }
    }

    private void updateRentalUnitState() {
        boolean hourly = "HOURS".equals(requestUnitCombo.getValue());
        requestHoursField.setDisable(!hourly);
        requestEndDate.setDisable(hourly);
        requestEndTimeCombo.setDisable(hourly);
        if (!hourly) {
            requestHoursField.clear();
        }
    }

    private boolean validateRentalPaymentInputs() {
        String method = rentalPaymentMethodCombo.getValue();
        String reference = rentalPaymentReferenceField.getText() != null ? rentalPaymentReferenceField.getText().trim() : "";
        String holder = rentalPaymentHolderField.getText() != null ? rentalPaymentHolderField.getText().trim() : "";
        if ("Credit Card".equals(method)) {
            if (!ValidationUtil.isValidCardNumber(reference)) {
                requestInfoLabel.setText("Enter a valid credit card number for the rental payment.");
                return false;
            }
            if (!ValidationUtil.isNotEmpty(holder)) {
                requestInfoLabel.setText("Enter the credit card holder name.");
                return false;
            }
        } else if ("UPI".equals(method)) {
            if (!ValidationUtil.isValidUpiId(reference)) {
                requestInfoLabel.setText("Enter a valid UPI ID for the rental payment.");
                return false;
            }
            if (!ValidationUtil.isNotEmpty(holder)) {
                requestInfoLabel.setText("Enter the UPI account holder name.");
                return false;
            }
        }
        return true;
    }

    private void updateRentalPaymentFields() {
        String method = rentalPaymentMethodCombo.getValue();
        boolean cash = "Cash on Delivery".equals(method);
        rentalPaymentReferenceField.setDisable(cash);
        rentalPaymentHolderField.setDisable(cash);
        if (cash) {
            rentalPaymentReferenceField.setText("Cash on delivery");
            rentalPaymentHolderField.clear();
            rentalPaymentReferenceField.setPromptText("Cash will be collected by supplier");
            rentalPaymentHolderField.setPromptText("Verified during handover");
        } else if ("UPI".equals(method)) {
            rentalPaymentReferenceField.clear();
            rentalPaymentHolderField.clear();
            rentalPaymentReferenceField.setPromptText("name@bank");
            rentalPaymentHolderField.setPromptText("UPI holder name");
        } else {
            rentalPaymentReferenceField.clear();
            rentalPaymentHolderField.clear();
            rentalPaymentReferenceField.setPromptText("1234 5678 9012 3456");
            rentalPaymentHolderField.setPromptText("Card holder name");
        }
    }

    private Payment.PaymentMethod rentalMethodFromSelection() {
        return switch (rentalPaymentMethodCombo.getValue()) {
            case "UPI" -> Payment.PaymentMethod.UPI;
            case "Cash on Delivery" -> Payment.PaymentMethod.CASH_ON_DELIVERY;
            default -> Payment.PaymentMethod.CREDIT_CARD;
        };
    }
}
