package com.rento.controllers;

import com.rento.models.Booking;
import com.rento.models.Payment;
import com.rento.navigation.NavigationManager;
import com.rento.security.SessionManager;
import com.rento.services.BookingService;
import com.rento.services.PaymentService;
import com.rento.utils.DateTimeUtil;
import com.rento.utils.AlertUtil;
import com.rento.utils.OTPGenerator;
import com.rento.utils.ValidationUtil;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the payment page.
 */
public class PaymentController implements Initializable {

    @FXML private ComboBox<String> methodCombo;
    @FXML private Label accountNumberLabel;
    @FXML private Label accountNameLabel;
    @FXML private TextField cardNumberField;
    @FXML private TextField cardNameField;
    @FXML private TextField expiryField;
    @FXML private PasswordField cvvField;
    @FXML private Label errorLabel;
    @FXML private StackPane processingPane;
    @FXML private Button payBtn;

    @FXML private Label summaryVehicle;
    @FXML private Label summaryPickup;
    @FXML private Label summaryDropoff;
    @FXML private Label summaryDays;
    @FXML private Label summarySubtotal;
    @FXML private Label summaryTax;
    @FXML private Label summaryTotal;
    @FXML private VBox otpSection;
    @FXML private Label otpLabel;

    private Booking currentBooking;
    private final PaymentService paymentService = new PaymentService();
    private final BookingService bookingService = new BookingService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        methodCombo.setItems(FXCollections.observableArrayList(
            "Credit Card", "Debit Card", "UPI", "Net Banking", "Wallet"));
        methodCombo.setValue("Credit Card");
        methodCombo.valueProperty().addListener((obs, oldValue, newValue) -> updatePaymentFields());
        updatePaymentFields();
    }

    public void setBooking(Booking booking) {
        this.currentBooking = booking;
        if (booking != null) {
            summaryVehicle.setText(booking.getVehicleName() != null ? booking.getVehicleName() : "Vehicle");
            summaryPickup.setText((booking.getPickupLocation() != null ? booking.getPickupLocation() : "") +
                " • " + DateTimeUtil.formatDateTime(booking.getPickupDateTime()));
            summaryDropoff.setText((booking.getDropoffLocation() != null ? booking.getDropoffLocation() : "") +
                " • " + DateTimeUtil.formatDateTime(booking.getReturnDateTime()));
            summaryDays.setText(booking.getRentalDurationLabel());

            double subtotal = booking.getTotalCost() - booking.getTaxAmount();
            summarySubtotal.setText(ValidationUtil.formatCurrency(subtotal));
            summaryTax.setText(ValidationUtil.formatCurrency(booking.getTaxAmount()));
            summaryTotal.setText(ValidationUtil.formatCurrency(booking.getTotalCost()));
        }
    }

    @FXML
    private void onPay() {
        clearError();

        if (requiresCardDetails()) {
            String cardNum = cardNumberField.getText().replaceAll("[\\s\\-]", "");
            if (cardNum.length() < 13) {
                showError("Please enter a valid card number");
                return;
            }
            if (!ValidationUtil.isNotEmpty(cardNameField.getText())) {
                showError("Please enter cardholder name");
                return;
            }
            if (!ValidationUtil.isValidExpiryDate(expiryField.getText())) {
                showError("Please enter a valid expiry date (MM/YY)");
                return;
            }
            if (!ValidationUtil.isValidCVV(cvvField.getText())) {
                showError("Please enter a valid CVV");
                return;
            }
        } else {
            String accountValue = cardNumberField.getText() != null ? cardNumberField.getText().trim() : "";
            if (!ValidationUtil.isNotEmpty(accountValue)) {
                showError("Please enter payment account details");
                return;
            }
            if (!ValidationUtil.isNotEmpty(cardNameField.getText())) {
                showError("Please enter account holder name");
                return;
            }
            if ("UPI".equals(methodCombo.getValue()) && !accountValue.matches("^[A-Za-z0-9._-]{2,}@[A-Za-z]{2,}$")) {
                showError("Please enter a valid UPI ID");
                return;
            }
            if ("Net Banking".equals(methodCombo.getValue()) && accountValue.length() < 6) {
                showError("Please enter a valid net banking reference");
                return;
            }
            if ("Wallet".equals(methodCombo.getValue()) && accountValue.length() < 8) {
                showError("Please enter a valid wallet number/reference");
                return;
            }
        }

        // Show processing animation
        payBtn.setDisable(true);
        processingPane.setVisible(true);

        // Simulate processing delay
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> completePayment());
        delay.play();
    }

    private void completePayment() {
        processingPane.setVisible(false);
        payBtn.setDisable(false);

        try {
            Payment.PaymentMethod method = methodFromSelection();
            String accountReference = cardNumberField.getText();
            String accountHolder = cardNameField.getText();
            if (!requiresCardDetails()) {
                accountHolder = SessionManager.getInstance().getCurrentUserName();
            }

            Payment payment = paymentService.processPayment(
                currentBooking != null ? currentBooking.getId() : null,
                SessionManager.getInstance().getCurrentUser() != null ? SessionManager.getInstance().getCurrentUser().getId() : null,
                currentBooking != null ? currentBooking.getTotalCost() - currentBooking.getTaxAmount() : 0,
                currentBooking != null ? currentBooking.getTaxAmount() : 0,
                currentBooking != null ? currentBooking.getTotalCost() : 0,
                method,
                accountReference,
                accountHolder,
                expiryField.getText(),
                cvvField.getText()
            );
            if (payment == null) {
                payment = createSimulatedPayment(method, accountReference, accountHolder);
            }

            // Ensure booking is properly persisted with all payment details
            if (currentBooking != null && currentBooking.getId() != null) {
                bookingService.notifyBookingPaymentSuccess(currentBooking, payment);
            }
        } catch (Exception ex) {
            System.out.println("[Payment] Processing in demo mode: " + ex.getMessage());
            if (currentBooking != null) {
                bookingService.notifyBookingPaymentSuccess(currentBooking,
                    createSimulatedPayment(methodFromSelection(), cardNumberField.getText(), cardNameField.getText()));
            }
        }

        // Display OTP to customer
        String otp = currentBooking != null && currentBooking.getOtp() != null ?
            currentBooking.getOtp() : OTPGenerator.generateOTP();
        otpLabel.setText(otp);
        otpSection.setVisible(true);

        // Disable pay button
        payBtn.setText("✓ Payment Successful");
        payBtn.getStyleClass().clear();
        payBtn.getStyleClass().add("btn-accent");
        payBtn.setDisable(true);

        AlertUtil.showSuccess("Payment successful!\n\nYour OTP: " + otp + "\n\nShare this with your driver at pickup.\nReceipt will be generated after ride confirmation.");
    }

    @FXML private void onBack() { NavigationManager.goBack(); }
    @FXML private void onNavHome() { NavigationManager.navigateTo("/fxml/landing.fxml"); }

    private boolean requiresCardDetails() {
        String selectedMethod = methodCombo.getValue();
        return "Credit Card".equals(selectedMethod) || "Debit Card".equals(selectedMethod);
    }

    private Payment.PaymentMethod methodFromSelection() {
        switch (methodCombo.getValue()) {
            case "Debit Card":
                return Payment.PaymentMethod.DEBIT_CARD;
            case "UPI":
                return Payment.PaymentMethod.UPI;
            case "Net Banking":
                return Payment.PaymentMethod.NET_BANKING;
            case "Wallet":
                return Payment.PaymentMethod.WALLET;
            default:
                return Payment.PaymentMethod.CREDIT_CARD;
        }
    }

    private void updatePaymentFields() {
        boolean showCardFields = requiresCardDetails();
        cardNumberField.setDisable(false);
        cardNameField.setDisable(false);
        expiryField.setDisable(!showCardFields);
        cvvField.setDisable(!showCardFields);

        if (!showCardFields) {
            expiryField.clear();
            cvvField.clear();
        }

        switch (methodCombo.getValue()) {
            case "UPI":
                accountNumberLabel.setText("UPI ID");
                accountNameLabel.setText("Account Holder");
                cardNumberField.setPromptText("name@bank");
                cardNameField.setPromptText("UPI account holder");
                break;
            case "Net Banking":
                accountNumberLabel.setText("Bank Ref / Login");
                accountNameLabel.setText("Account Holder");
                cardNumberField.setPromptText("Customer ID or reference");
                cardNameField.setPromptText("Bank account holder");
                break;
            case "Wallet":
                accountNumberLabel.setText("Wallet Number / ID");
                accountNameLabel.setText("Wallet Holder");
                cardNumberField.setPromptText("Wallet mobile or ID");
                cardNameField.setPromptText("Wallet holder");
                break;
            default:
                accountNumberLabel.setText("Card Number");
                accountNameLabel.setText("Cardholder Name");
                cardNumberField.setPromptText("1234 5678 9012 3456");
                cardNameField.setPromptText("Name on card");
                break;
        }
    }

    private void showError(String msg) { errorLabel.setText(msg); errorLabel.setVisible(true); }
    private void clearError() { errorLabel.setText(""); errorLabel.setVisible(false); }

    private Payment createSimulatedPayment(Payment.PaymentMethod method, String accountReference, String accountHolder) {
        Payment payment = new Payment();
        payment.setBookingId(currentBooking != null ? currentBooking.getId() : null);
        payment.setUserId(SessionManager.getInstance().getCurrentUser() != null
            ? SessionManager.getInstance().getCurrentUser().getId() : null);
        payment.setAmount(currentBooking != null ? currentBooking.getTotalCost() - currentBooking.getTaxAmount() : 0);
        payment.setTaxAmount(currentBooking != null ? currentBooking.getTaxAmount() : 0);
        payment.setTotalAmount(currentBooking != null ? currentBooking.getTotalCost() : 0);
        payment.setPaymentMethod(method);
        payment.setCardNumber(accountReference);
        payment.setCardHolderName(accountHolder);
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setTransactionRef(OTPGenerator.generateTransactionRef());
        return payment;
    }
}
