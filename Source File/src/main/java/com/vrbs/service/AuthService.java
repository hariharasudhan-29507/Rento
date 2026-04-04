package com.vrbs.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.vrbs.model.SessionUser;
import com.vrbs.model.UserRole;
import com.vrbs.state.ApplicationState;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Demo auth with BCrypt hashes in memory. Replace with Mongo-backed DAO.
 * Default accounts for local testing only — never show passwords in UI.
 */
public final class AuthService {

    private static final int BCRYPT_COST = 10;
    private final Map<String, AccountRecord> accounts = new ConcurrentHashMap<>();
    private final Consumer<SessionUser> onAuthenticated;

    public AuthService(Consumer<SessionUser> onAuthenticated) {
        this.onAuthenticated = onAuthenticated;
        seedDemoAccounts();
    }

    private void seedDemoAccounts() {
        putAccount("admin", "admin123", "Ramesh Iyer", UserRole.ADMIN, "admin@vrbs.in");
        putAccount("user", "user123", "Ananya Subramanian", UserRole.USER, "ananya.user@vrbs.in");
        putAccount("driver", "driver123", "Senthil Kumar", UserRole.DRIVER, "senthil.d@vrbs.in");
        putAccount("supplier", "supplier123", "Venkatesh Motors", UserRole.SUPPLIER, "fleet@venkateshmotors.in");
    }

    private void putAccount(String username, String plainPassword, String display, UserRole role, String email) {
        String hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, plainPassword.toCharArray());
        accounts.put(username.toLowerCase(), new AccountRecord(username, hash, display, role, email));
    }

    public Optional<String> register(String username, String plainPassword, String displayName, UserRole role, String email) {
        if (role == UserRole.ADMIN) {
            return Optional.of("Admin registration is not allowed.");
        }
        String key = username.toLowerCase();
        if (accounts.containsKey(key)) {
            return Optional.of("Username already exists.");
        }
        if (plainPassword == null || plainPassword.length() < 6) {
            return Optional.of("Password must be at least 6 characters.");
        }
        String hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, plainPassword.toCharArray());
        accounts.put(key, new AccountRecord(username, hash, displayName != null ? displayName : username, role, email != null ? email : ""));
        return Optional.empty();
    }

    public Optional<String> login(String username, String plainPassword) {
        if (username == null || username.isBlank() || plainPassword == null) {
            return Optional.of("Enter username and password.");
        }
        AccountRecord rec = accounts.get(username.toLowerCase());
        if (rec == null) {
            return Optional.of("Invalid credentials.");
        }
        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), rec.passwordHash);
        if (!result.verified) {
            return Optional.of("Invalid credentials.");
        }
        if (ApplicationState.get().isMaintenanceMode() && rec.role() != UserRole.ADMIN) {
            return Optional.of("System is under maintenance. Please try again later.");
        }
        SessionUser user = new SessionUser(rec.id(), rec.username(), rec.displayName(), rec.role(), rec.email());
        if (rec.role() == UserRole.USER && ApplicationState.get().isUserSuspended(user.getId())) {
            return Optional.of("Your account is suspended. Contact support.");
        }
        if (rec.role() == UserRole.DRIVER && ApplicationState.get().isDriverSuspended(user.getId())) {
            return Optional.of("Your driver account is suspended.");
        }
        if (rec.role() == UserRole.SUPPLIER && ApplicationState.get().isSupplierSuspended(user.getId())) {
            return Optional.of("Your supplier account is suspended.");
        }
        onAuthenticated.accept(user);
        return Optional.empty();
    }

    public Optional<SessionUser> findByUsername(String username) {
        AccountRecord rec = accounts.get(username.toLowerCase());
        if (rec == null) {
            return Optional.empty();
        }
        return Optional.of(new SessionUser(rec.id(), rec.username(), rec.displayName(), rec.role(), rec.email()));
    }

    public boolean changePassword(String username, String currentPlain, String newPlain) {
        AccountRecord rec = accounts.get(username.toLowerCase());
        if (rec == null) {
            return false;
        }
        if (!BCrypt.verifyer().verify(currentPlain.toCharArray(), rec.passwordHash).verified) {
            return false;
        }
        if (newPlain == null || newPlain.length() < 6) {
            return false;
        }
        String hash = BCrypt.withDefaults().hashToString(BCRYPT_COST, newPlain.toCharArray());
        accounts.put(username.toLowerCase(), rec.withNewHash(hash));
        return true;
    }

    private record AccountRecord(String username, String passwordHash, String displayName, UserRole role, String email) {
        String id() {
            return "acct-" + username.toLowerCase();
        }

        AccountRecord withNewHash(String hash) {
            return new AccountRecord(username, hash, displayName, role, email);
        }
    }
}
