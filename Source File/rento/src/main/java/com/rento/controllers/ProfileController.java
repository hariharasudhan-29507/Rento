package com.rento.controllers;

import com.rento.dao.BookingDAO;
import com.rento.dao.RentalDAO;
import com.rento.dao.UserDAO;
import com.rento.models.PaymentMethodProfile;
import com.rento.models.User;
import com.rento.navigation.NavigationManager;
import com.rento.security.SessionManager;
import com.rento.services.AuthService;
import com.rento.services.NotificationService;
import com.rento.services.PaymentMethodService;
import com.rento.utils.AlertUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller for the Profile page.
 */
public class ProfileController implements Initializable {

    @FXML private Label avatarLabel;
    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleLabel;
    @FXML private VBox guestNotice;
    @FXML private VBox userInfoSection;
    @FXML private Label bookingCount;
    @FXML private Label rentalCount;
    @FXML private Label accountAge;
    @FXML private Label detailName;
    @FXML private Label detailEmail;
    @FXML private Label detailPhone;
    @FXML private Label detailRole;
    @FXML private Label detailStatus;
    @FXML private Label detailAge;
    @FXML private Label detailWallet;
    @FXML private Button logoutBtn;
    @FXML private Button roleDashboardBtn;
    @FXML private Label notificationsSummaryLabel;
    @FXML private ComboBox<String> paymentMethodTypeCombo;
    @FXML private TextField paymentProfileNameField;
    @FXML private TextField paymentHolderNameField;
    @FXML private TextField paymentReferenceField;
    @FXML private TextField paymentProviderField;
    @FXML private TextField paymentBillingAddressField;
    @FXML private CheckBox preferredPaymentCheck;
    @FXML private Label paymentMethodStatusLabel;
    @FXML private VBox paymentMethodList;

    private final AuthService authService = new AuthService();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final RentalDAO rentalDAO = new RentalDAO();
    private final UserDAO userDAO = new UserDAO();
    private final NotificationService notificationService = new NotificationService();
    private final PaymentMethodService paymentMethodService = new PaymentMethodService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        paymentMethodTypeCombo.setItems(FXCollections.observableArrayList("Credit Card", "UPI", "Cash on Delivery"));
        paymentMethodTypeCombo.setValue("Credit Card");
        paymentMethodTypeCombo.valueProperty().addListener((obs, oldVal, newVal) -> updatePaymentMethodHints());
        updatePaymentMethodHints();
        if (SessionManager.getInstance().isGuest()) {
            showGuestMode();
        } else {
            showUserProfile();
        }
    }

    private void showGuestMode() {
        guestNotice.setVisible(true);
        guestNotice.setManaged(true);
        userInfoSection.setVisible(false);
        userInfoSection.setManaged(false);

        nameLabel.setText("Guest");
        emailLabel.setText("Not signed in");
        roleLabel.setText("GUEST");
        avatarLabel.setText("G");
        logoutBtn.setVisible(false);
        roleDashboardBtn.setVisible(false);
        roleDashboardBtn.setManaged(false);
    }

    private void showUserProfile() {
        guestNotice.setVisible(false);
        guestNotice.setManaged(false);
        userInfoSection.setVisible(true);
        userInfoSection.setManaged(true);
        logoutBtn.setVisible(true);

        User sessionUser = SessionManager.getInstance().getCurrentUser();
        User user = sessionUser != null ? userDAO.findById(sessionUser.getId()) : null;
        if (user != null) {
            nameLabel.setText(user.getFullName());
            emailLabel.setText(user.getEmail());
            roleLabel.setText(formatRole(user.getRole()));
            avatarLabel.setText(user.getFullName().substring(0, 1).toUpperCase());

            detailName.setText(user.getFullName());
            detailEmail.setText(user.getEmail());
            detailPhone.setText(user.getPhone() != null ? user.getPhone() : "Not set");
            detailRole.setText(formatRole(user.getRole()));
            detailStatus.setText(user.isLocked()
                ? "● Locked"
                : user.isVerified() ? "● Verified" : "● Unverified");
            detailAge.setText(user.getAge() > 0 ? String.valueOf(user.getAge()) : "Not set");
            detailWallet.setText(com.rento.utils.ValidationUtil.formatCurrency(user.getWalletBalance()));

            if (user.getCreatedAt() != null) {
                accountAge.setText(new SimpleDateFormat("MMM yyyy").format(user.getCreatedAt()));
            }

            if (user.getRole() == User.Role.DRIVER) {
                bookingCount.setText(String.valueOf(bookingDAO.findByDriver(user.getId()).size()));
            } else {
                bookingCount.setText(String.valueOf(bookingDAO.findByUser(user.getId()).size()));
            }
            if (user.getRole() == User.Role.SUPPLIER) {
                rentalCount.setText(String.valueOf(rentalDAO.findBySupplier(user.getId()).size()));
            } else {
                rentalCount.setText(String.valueOf(rentalDAO.findByRenter(user.getId()).size()));
            }
            roleDashboardBtn.setVisible(true);
            roleDashboardBtn.setManaged(true);
            roleDashboardBtn.setText(getRoleDashboardLabel(user.getRole()));
            loadNotificationsSummary(user);
            loadPaymentMethods(user);
        }
    }

    private void loadNotificationsSummary(User user) {
        List<org.bson.Document> notifications = notificationService.getNotifications(user.getId());
        notificationsSummaryLabel.setText(notifications.isEmpty()
            ? "No notifications yet."
            : "You have " + notifications.size() + " notification(s). Click Download to save all.");
    }

    @FXML
    private void onLogout() {
        authService.logout();
        NavigationManager.clearHistory();
        NavigationManager.navigateTo("/fxml/landing.fxml");
    }

    @FXML private void onLogin() { NavigationManager.navigateTo("/fxml/login.fxml"); }
    @FXML private void onRegister() { NavigationManager.navigateTo("/fxml/register.fxml"); }
    @FXML private void onNavHome() { NavigationManager.navigateTo("/fxml/landing.fxml"); }
    @FXML private void onNavBook() { NavigationManager.navigateTo("/fxml/booking.fxml"); }
    @FXML private void onNavRent() { NavigationManager.navigateTo("/fxml/rent.fxml"); }
    @FXML private void onNavContact() { NavigationManager.navigateTo("/fxml/contact.fxml"); }
    @FXML private void onOpenRoleDashboard() {
        User.Role role = SessionManager.getInstance().getCurrentRole();
        switch (role) {
            case DRIVER:
                NavigationManager.navigateTo("/fxml/driver_dashboard.fxml");
                break;
            case SUPPLIER:
                NavigationManager.navigateTo("/fxml/supplier_dashboard.fxml");
                break;
            case ADMIN:
                NavigationManager.navigateTo("/fxml/admin_dashboard.fxml");
                break;
            default:
                NavigationManager.navigateTo("/fxml/rent.fxml");
                break;
        }
    }

    @FXML
    private void onShowNotifications() {
        if (SessionManager.getInstance().isGuest() || SessionManager.getInstance().getCurrentUser() == null) {
            AlertUtil.showInfo("Notifications", "Please sign in to view notifications.");
            return;
        }
        List<org.bson.Document> notifications = notificationService.getNotifications(SessionManager.getInstance().getCurrentUser().getId());
        if (notifications.isEmpty()) {
            AlertUtil.showInfo("Notifications", "No notifications yet.");
            return;
        }
        StringBuilder message = new StringBuilder();
        for (org.bson.Document n : notifications) {
            message.append("• ").append(n.getString("title")).append("\n");
        }
        AlertUtil.showInfo("Notifications", message.toString());
    }

    @FXML
    private void onDownloadNotifications() {
        if (SessionManager.getInstance().isGuest() || SessionManager.getInstance().getCurrentUser() == null) {
            AlertUtil.showInfo("Notifications", "Please sign in to download notifications.");
            return;
        }
        try {
            String outputDir = System.getProperty("user.home") + "\\Documents\\RentoNotifications";
            String path = notificationService.exportNotifications(SessionManager.getInstance().getCurrentUser().getId(), outputDir);
            AlertUtil.showSuccess("Notifications downloaded:\n" + path);
        } catch (Exception ex) {
            AlertUtil.showError("Download Failed", ex.getMessage());
        }
    }

    @FXML
    private void onSavePaymentMethod() {
        if (SessionManager.getInstance().isGuest() || SessionManager.getInstance().getCurrentUser() == null) {
            AlertUtil.showInfo("Payment Methods", "Please sign in to save a payment method.");
            return;
        }
        String error = paymentMethodService.savePaymentMethod(
            SessionManager.getInstance().getCurrentUser().getId(),
            mapMethodType(),
            paymentProfileNameField.getText(),
            paymentHolderNameField.getText(),
            paymentReferenceField.getText(),
            paymentProviderField.getText(),
            paymentBillingAddressField.getText(),
            preferredPaymentCheck.isSelected()
        );
        if (error != null) {
            paymentMethodStatusLabel.setText(error);
            return;
        }
        paymentMethodStatusLabel.setText("Payment method saved successfully.");
        paymentProfileNameField.clear();
        paymentHolderNameField.clear();
        paymentReferenceField.clear();
        paymentProviderField.clear();
        paymentBillingAddressField.clear();
        preferredPaymentCheck.setSelected(false);
        if (SessionManager.getInstance().getCurrentUser() != null) {
            User user = userDAO.findById(SessionManager.getInstance().getCurrentUser().getId());
            if (user != null) {
                loadPaymentMethods(user);
            }
        }
    }

    private String formatRole(User.Role role) {
        return role == null ? "Guest" : role.name().replace('_', ' ');
    }

    private String getRoleDashboardLabel(User.Role role) {
        switch (role) {
            case DRIVER:
                return "Driver Dashboard";
            case SUPPLIER:
                return "Supplier Dashboard";
            case ADMIN:
                return "Admin Dashboard";
            default:
                return "Rental Marketplace";
        }
    }

    private void loadPaymentMethods(User user) {
        paymentMethodList.getChildren().clear();
        for (PaymentMethodProfile profile : paymentMethodService.getPaymentMethodsForUser(user.getId())) {
            Label label = new Label(profile.getProfileName() + " • "
                + profile.getMethodType().name().replace('_', ' ') + " • "
                + profile.getMaskedReference()
                + (profile.isPreferred() ? " • Preferred" : ""));
            label.getStyleClass().add("text-body");
            paymentMethodList.getChildren().add(label);
        }
        if (paymentMethodList.getChildren().isEmpty()) {
            Label empty = new Label("No saved payment methods yet.");
            empty.getStyleClass().add("text-muted");
            paymentMethodList.getChildren().add(empty);
        }
    }

    private void updatePaymentMethodHints() {
        String type = paymentMethodTypeCombo.getValue();
        if ("UPI".equals(type)) {
            paymentReferenceField.setPromptText("name@bank");
            paymentProviderField.setPromptText("UPI app or bank");
        } else if ("Cash on Delivery".equals(type)) {
            paymentReferenceField.setPromptText("Cash on delivery");
            paymentProviderField.setPromptText("Collected by driver or supplier");
        } else {
            paymentReferenceField.setPromptText("1234 5678 9012 3456");
            paymentProviderField.setPromptText("Card issuer");
        }
    }

    private PaymentMethodProfile.MethodType mapMethodType() {
        return switch (paymentMethodTypeCombo.getValue()) {
            case "UPI" -> PaymentMethodProfile.MethodType.UPI;
            case "Cash on Delivery" -> PaymentMethodProfile.MethodType.CASH_ON_DELIVERY;
            default -> PaymentMethodProfile.MethodType.CREDIT_CARD;
        };
    }
}
