package com.rento.controllers;

import com.rento.navigation.NavigationManager;
import com.rento.security.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the landing page.
 */
public class LandingController implements Initializable {

    @FXML private Button profileBtn;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        updateProfileButton();
    }

    private void updateProfileButton() {
        if (SessionManager.getInstance().isLoggedIn()) {
            profileBtn.setText("⬤ " + SessionManager.getInstance().getCurrentUserName());
        } else {
            profileBtn.setText("⬤ Sign In");
        }
    }

    @FXML
    private void onNavHome() {
        NavigationManager.navigateTo("/fxml/landing.fxml");
    }

    @FXML
    private void onNavAbout() {
        NavigationManager.navigateTo("/fxml/about.fxml");
    }

    @FXML
    private void onNavBook() {
        if (SessionManager.getInstance().isGuest()) {
            NavigationManager.navigateTo("/fxml/login.fxml");
        } else {
            NavigationManager.navigateTo("/fxml/booking.fxml");
        }
    }

    @FXML
    private void onNavRent() {
        if (SessionManager.getInstance().isGuest()) {
            NavigationManager.navigateTo("/fxml/login.fxml");
        } else {
            NavigationManager.navigateTo("/fxml/rent.fxml");
        }
    }

    @FXML
    private void onNavContact() {
        NavigationManager.navigateTo("/fxml/contact.fxml");
    }

    @FXML
    private void onNavProfile() {
        if (SessionManager.getInstance().isGuest()) {
            NavigationManager.navigateTo("/fxml/login.fxml");
        } else {
            NavigationManager.navigateTo("/fxml/profile.fxml");
        }
    }
}
