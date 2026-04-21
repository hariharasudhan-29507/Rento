package com.rento.controllers;

import com.rento.models.Booking;
import com.rento.models.User;
import com.rento.models.Vehicle;
import com.rento.navigation.NavigationManager;
import com.rento.security.SessionManager;
import com.rento.services.BookingService;
import com.rento.utils.DateTimeUtil;
import com.rento.utils.MongoDBConnection;
import com.rento.utils.ValidationUtil;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the booking detail/confirmation page.
 */
public class BookingDetailController implements Initializable {

    @FXML private Label vehicleName;
    @FXML private Label vehicleCategory;
    @FXML private Label vehicleFuel;
    @FXML private Label vehicleSeats;
    @FXML private TextField pickupField;
    @FXML private TextField dropoffField;
    @FXML private ComboBox<String> driverCombo;
    @FXML private DatePicker pickupDate;
    @FXML private DatePicker returnDate;
    @FXML private ComboBox<String> pickupTimeCombo;
    @FXML private ComboBox<String> returnTimeCombo;
    @FXML private Label dailyRateLabel;
    @FXML private Label daysLabel;
    @FXML private Label subtotalLabel;
    @FXML private Label surchargeLabel;
    @FXML private Label discountLabel;
    @FXML private Label taxLabel;
    @FXML private Label totalLabel;
    @FXML private Label depositLabel;
    @FXML private HBox surchargeRow;
    @FXML private HBox discountRow;
    @FXML private Label errorLabel;

    private Vehicle currentVehicle;
    private final BookingService bookingService = new BookingService();
    private double[] pricingResult;
    private List<User> availableDrivers = List.of();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        pickupDate.setValue(LocalDate.now().plusDays(1));
        returnDate.setValue(LocalDate.now().plusDays(1));
        pickupTimeCombo.getItems().setAll(DateTimeUtil.buildTimeSlots());
        returnTimeCombo.getItems().setAll(DateTimeUtil.buildTimeSlots());
        pickupTimeCombo.setValue("09:00 AM");
        returnTimeCombo.setValue("06:00 PM");
        loadDrivers();
        pickupField.textProperty().addListener((obs, oldVal, newVal) -> loadDrivers());
        pickupDate.valueProperty().addListener((obs, oldVal, newVal) -> onCalculatePrice());
        returnDate.valueProperty().addListener((obs, oldVal, newVal) -> onCalculatePrice());
        pickupTimeCombo.valueProperty().addListener((obs, oldVal, newVal) -> onCalculatePrice());
        returnTimeCombo.valueProperty().addListener((obs, oldVal, newVal) -> onCalculatePrice());
    }

    public void setVehicle(Vehicle vehicle) {
        this.currentVehicle = vehicle;
        if (vehicle != null) {
            vehicleName.setText(vehicle.getDisplayName());
            vehicleCategory.setText(vehicle.getCategory() != null ? vehicle.getCategory().name() : "");
            vehicleFuel.setText(vehicle.getFuelType() != null ? vehicle.getFuelType().name() : "");
            vehicleSeats.setText(vehicle.getSeats() + " seats");
            dailyRateLabel.setText(ValidationUtil.formatCurrency(vehicle.getDailyRate()));
            onCalculatePrice();
        }
    }

    private void loadDrivers() {
        List<User> allDrivers = bookingService.getAvailableDrivers();
        String pickup = pickupField != null && pickupField.getText() != null ? pickupField.getText().trim().toLowerCase() : "";
        if (!pickup.isBlank()) {
            availableDrivers = allDrivers.stream().filter(driver -> {
                String address = driver.getAddress() != null ? driver.getAddress().toLowerCase() : "";
                return !address.isBlank() && (address.contains(pickup) || pickup.contains(address));
            }).toList();
            if (availableDrivers.isEmpty()) {
                availableDrivers = allDrivers;
            }
        } else {
            availableDrivers = allDrivers;
        }
        driverCombo.getItems().clear();
        driverCombo.getItems().add("Any Available Driver");
        for (User driver : availableDrivers) {
            driverCombo.getItems().add(driver.getFullName() + " • " + driver.getEmail());
        }
        driverCombo.setValue("Any Available Driver");
    }

    @FXML
    private void onCalculatePrice() {
        if (currentVehicle == null) return;

        LocalDate pickup = pickupDate.getValue();
        LocalDate ret = returnDate.getValue();
        if (pickup == null || ret == null) {
            showError("Please choose both pickup and drop date/time");
            return;
        }
        Date pickupD = DateTimeUtil.toDate(pickup, pickupTimeCombo.getValue());
        Date returnD = DateTimeUtil.toDate(ret, returnTimeCombo.getValue());
        if (!returnD.after(pickupD)) {
            showError("Drop date/time must be after pickup date/time");
            return;
        }
        clearError();

        pricingResult = bookingService.calculatePricing(currentVehicle.getDailyRate(), pickupD, returnD);
        // [0]=days, [1]=subtotal, [2]=surcharge, [3]=discount, [4]=tax, [5]=total, [6]=deposit

        daysLabel.setText(DateTimeUtil.formatDuration(pickupD, returnD) + " (" + (int) pricingResult[0] + " billable day(s))");
        subtotalLabel.setText(ValidationUtil.formatCurrency(pricingResult[1]));

        if (pricingResult[2] > 0) {
            surchargeRow.setVisible(true);
            surchargeRow.setManaged(true);
            surchargeLabel.setText("+" + ValidationUtil.formatCurrency(pricingResult[2]));
        } else {
            surchargeRow.setVisible(false);
            surchargeRow.setManaged(false);
        }

        if (pricingResult[3] > 0) {
            discountRow.setVisible(true);
            discountRow.setManaged(true);
            discountLabel.setText("-" + ValidationUtil.formatCurrency(pricingResult[3]));
        } else {
            discountRow.setVisible(false);
            discountRow.setManaged(false);
        }

        taxLabel.setText(ValidationUtil.formatCurrency(pricingResult[4]));
        totalLabel.setText(ValidationUtil.formatCurrency(pricingResult[5]));
        depositLabel.setText(ValidationUtil.formatCurrency(pricingResult[6]));
    }

    @FXML
    private void onProceedToPayment() {
        clearError();

        if (!ValidationUtil.isNotEmpty(pickupField.getText())) {
            showError("Please enter a pickup location");
            return;
        }
        if (!ValidationUtil.isNotEmpty(dropoffField.getText())) {
            showError("Please enter a drop-off location");
            return;
        }

        LocalDate pickup = pickupDate.getValue();
        LocalDate ret = returnDate.getValue();
        if (pickup == null || ret == null) {
            showError("Please select valid pickup and drop date/time");
            return;
        }

        try {
            Date pickupD = DateTimeUtil.toDate(pickup, pickupTimeCombo.getValue());
            Date returnD = DateTimeUtil.toDate(ret, returnTimeCombo.getValue());
            if (!returnD.after(pickupD)) {
                showError("Drop date/time must be after pickup date/time");
                return;
            }

            Booking booking = bookingService.createBooking(
                SessionManager.getInstance().getCurrentUser().getId(),
                currentVehicle.getId(),
                getSelectedDriverId(),
                pickupField.getText().trim(),
                dropoffField.getText().trim(),
                pickupD,
                returnD
            );

            // Navigate to payment
            NavigationManager.navigateTo("/fxml/payment.fxml", controller -> {
                if (controller instanceof PaymentController) {
                    ((PaymentController) controller).setBooking(booking);
                }
            });
        } catch (Exception e) {
            if (MongoDBConnection.getInstance().isConnected()) {
                showError(e.getMessage());
                return;
            }

            Booking mockBooking = new Booking();
            mockBooking.setPickupLocation(pickupField.getText().trim());
            mockBooking.setDropoffLocation(dropoffField.getText().trim());
            mockBooking.setPickupDateTime(DateTimeUtil.toDate(pickup, pickupTimeCombo.getValue()));
            mockBooking.setReturnDateTime(DateTimeUtil.toDate(ret, returnTimeCombo.getValue()));
            mockBooking.setVehicleName(currentVehicle.getDisplayName());
            mockBooking.setPreferredDriverId(getSelectedDriverId());
            if (getSelectedDriverId() != null && !availableDrivers.isEmpty()) {
                int selectedIndex = driverCombo.getSelectionModel().getSelectedIndex();
                if (selectedIndex > 0 && selectedIndex - 1 < availableDrivers.size()) {
                    mockBooking.setPreferredDriverName(availableDrivers.get(selectedIndex - 1).getFullName());
                }
            }
            if (pricingResult != null) {
                mockBooking.setTotalCost(pricingResult[5]);
                mockBooking.setTaxAmount(pricingResult[4]);
                mockBooking.setDepositAmount(pricingResult[6]);
                mockBooking.setDiscountApplied(pricingResult[3]);
            }

            NavigationManager.navigateTo("/fxml/payment.fxml", controller -> {
                if (controller instanceof PaymentController) {
                    ((PaymentController) controller).setBooking(mockBooking);
                }
            });
        }
    }

    @FXML private void onBack() { NavigationManager.goBack(); }
    @FXML private void onNavHome() { NavigationManager.navigateTo("/fxml/landing.fxml"); }

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    private void clearError() { errorLabel.setText(""); errorLabel.setVisible(false); }

    private org.bson.types.ObjectId getSelectedDriverId() {
        int selectedIndex = driverCombo.getSelectionModel().getSelectedIndex();
        if (selectedIndex <= 0 || selectedIndex - 1 >= availableDrivers.size()) {
            return null;
        }
        return availableDrivers.get(selectedIndex - 1).getId();
    }
}
