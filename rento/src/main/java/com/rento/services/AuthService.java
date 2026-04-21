package com.rento.services;

import com.rento.dao.UserDAO;
import com.rento.models.User;
import com.rento.security.PasswordHasher;
import com.rento.security.SessionManager;
import com.rento.utils.ValidationUtil;

/**
 * Authentication service handling registration, login, and session management.
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    private double getStartingWallet(User.Role role) {
        switch (role) {
            case DRIVER:
                return 12000;
            case SUPPLIER:
                return 60000;
            case ADMIN:
                return 100000;
            default:
                return 25000;
        }
    }

    /**
     * Register a new user.
     * @return error message or null on success
     */
    public String register(String fullName, String email, String phone, int age, String password, String confirmPassword, User.Role role) {
        // Validate name
        if (!ValidationUtil.isValidName(fullName)) {
            return "Please enter a valid name (2-50 letters)";
        }

        // Validate email
        if (!ValidationUtil.isValidEmail(email)) {
            return "Please enter a valid email address";
        }

        // Check duplicate email
        if (userDAO.emailExists(email)) {
            return "An account with this email already exists";
        }

        // Validate phone
        if (!ValidationUtil.isValidPhone(phone)) {
            return "Please enter a valid phone number";
        }

        if (!ValidationUtil.isValidAge(age)) {
            return "Age must be between 18 and 90";
        }

        // Validate password
        String passwordError = ValidationUtil.validatePassword(password);
        if (passwordError != null) {
            return passwordError;
        }

        // Confirm password match
        if (!password.equals(confirmPassword)) {
            return "Passwords do not match";
        }

        // Create user
        User user = new User();
        user.setFullName(fullName.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPhone(phone.trim());
        user.setPassword(PasswordHasher.hashPassword(password));
        user.setRole(role != null ? role : User.Role.USER);
        user.setVerified(true);
        user.setAge(age);
        user.setWalletBalance(getStartingWallet(user.getRole()));

        boolean success = userDAO.insertUser(user);
        if (success) {
            return null; // success
        } else {
            return "Registration failed. Please try again.";
        }
    }

    /**
     * Login with email and password.
     * @return error message or null on success
     */
    public String login(String email, String password) {
        if (!ValidationUtil.isNotEmpty(email)) {
            return "Email is required";
        }
        if (!ValidationUtil.isNotEmpty(password)) {
            return "Password is required";
        }

        User user = userDAO.findByEmail(email.trim().toLowerCase());
        if (user == null) {
            return "No account found with this email";
        }

        if (!PasswordHasher.verifyPassword(password, user.getPassword())) {
            return "Incorrect password";
        }

        // Set session
        SessionManager.getInstance().login(user);
        return null; // success
    }

    /**
     * Logout current user.
     */
    public void logout() {
        SessionManager.getInstance().logout();
    }

    /**
     * Check if user is authenticated.
     */
    public boolean isAuthenticated() {
        return SessionManager.getInstance().isLoggedIn();
    }

    /**
     * Get current user role.
     */
    public User.Role getCurrentRole() {
        return SessionManager.getInstance().getCurrentRole();
    }
}
