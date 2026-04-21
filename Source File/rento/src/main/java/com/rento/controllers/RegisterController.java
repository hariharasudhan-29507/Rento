package com.rento.controllers;

import com.rento.models.User;
import com.rento.navigation.NavigationManager;
import com.rento.services.AuthService;
import com.rento.utils.AlertUtil;
import com.rento.utils.CaptchaGenerator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the registration page.
 */
public class RegisterController implements Initializable {

    @FXML private TextField nameField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField ageField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private TextField captchaAnswer;
    @FXML private Label captchaQuestion;
    @FXML private Label errorLabel;
    @FXML private Label passwordStrength;

    private final AuthService authService = new AuthService();
    private String[] currentCaptcha;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Populate roles
        roleCombo.setItems(FXCollections.observableArrayList("User", "Driver", "Supplier"));
        roleCombo.setValue("User");

        generateCaptcha();

        // Live password strength feedback
        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrength(newVal);
        });
    }

    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            passwordStrength.setText("");
            return;
        }

        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[@$!%*?&#^()\\-_+=].*")) score++;

        switch (score) {
            case 0:
            case 1:
                passwordStrength.setText("⬤ Weak");
                passwordStrength.setStyle("-fx-text-fill: #f72585; -fx-font-size: 11px;");
                break;
            case 2:
            case 3:
                passwordStrength.setText("⬤ Fair");
                passwordStrength.setStyle("-fx-text-fill: #ff9f43; -fx-font-size: 11px;");
                break;
            case 4:
                passwordStrength.setText("⬤ Good");
                passwordStrength.setStyle("-fx-text-fill: #a78bfa; -fx-font-size: 11px;");
                break;
            case 5:
                passwordStrength.setText("⬤ Strong");
                passwordStrength.setStyle("-fx-text-fill: #06d6a0; -fx-font-size: 11px;");
                break;
        }
    }

    private void generateCaptcha() {
        currentCaptcha = CaptchaGenerator.generateMathCaptcha();
        captchaQuestion.setText(currentCaptcha[0]);
    }

    @FXML
    private void onRefreshCaptcha() {
        generateCaptcha();
        captchaAnswer.clear();
    }

    @FXML
    private void onRegister() {
        clearError();

        // Validate CAPTCHA
        String answer = captchaAnswer.getText().trim();
        if (answer.isEmpty() || !answer.equals(currentCaptcha[1])) {
            showError("Incorrect CAPTCHA answer. Try again.");
            generateCaptcha();
            captchaAnswer.clear();
            return;
        }

        // Map role
        User.Role role;
        String roleStr = roleCombo.getValue();
        switch (roleStr) {
            case "Driver": role = User.Role.DRIVER; break;
            case "Supplier": role = User.Role.SUPPLIER; break;
            default: role = User.Role.USER; break;
        }

        String error = authService.register(
            nameField.getText(),
            emailField.getText(),
            phoneField.getText(),
            parseAge(),
            passwordField.getText(),
            confirmPasswordField.getText(),
            role
        );

        if (error != null) {
            showError(error);
            generateCaptcha();
            captchaAnswer.clear();
        } else {
            AlertUtil.showSuccess("Account created successfully! Please sign in.");
            NavigationManager.navigateTo("/fxml/login.fxml");
        }
    }

    @FXML
    private void onGoToLogin() {
        NavigationManager.navigateTo("/fxml/login.fxml");
    }

    @FXML
    private void onBack() {
        NavigationManager.navigateTo("/fxml/landing.fxml");
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private int parseAge() {
        try {
            return Integer.parseInt(ageField.getText().trim());
        } catch (Exception e) {
            return -1;
        }
    }
}
