package com.rento.controllers;

import com.rento.navigation.NavigationManager;
import com.rento.security.SessionManager;
import com.rento.services.AuthService;
import com.rento.utils.CaptchaGenerator;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the login page.
 */
public class LoginController implements Initializable {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField captchaAnswer;
    @FXML private Label captchaQuestion;
    @FXML private Label errorLabel;

    private final AuthService authService = new AuthService();
    private String[] currentCaptcha;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        generateCaptcha();
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
    private void onLogin() {
        clearError();

        // Validate CAPTCHA first
        String answer = captchaAnswer.getText().trim();
        if (answer.isEmpty() || !answer.equals(currentCaptcha[1])) {
            showError("Incorrect CAPTCHA answer. Try again.");
            generateCaptcha();
            captchaAnswer.clear();
            return;
        }

        String email = emailField.getText();
        String password = passwordField.getText();

        String error = authService.login(email, password);
        if (error != null) {
            showError(error);
            generateCaptcha();
            captchaAnswer.clear();
        } else {
            // Navigate based on role
            switch (SessionManager.getInstance().getCurrentRole()) {
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
                    NavigationManager.navigateTo("/fxml/booking.fxml");
                    break;
            }
        }
    }

    @FXML
    private void onGoToRegister() {
        NavigationManager.navigateTo("/fxml/register.fxml");
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
}
