package com.rento.security;

import com.rento.models.User;

/**
 * Session manager to track currently logged-in user.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;
    private boolean isLoggedIn;

    private SessionManager() {
        this.isLoggedIn = false;
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(User user) {
        this.currentUser = user;
        this.isLoggedIn = true;
        if (user != null) {
            System.out.println("[Rento] Active session: " + user.getFullName() + " <" + user.getEmail() + "> as " + user.getRole());
        }
    }

    public void logout() {
        if (currentUser != null) {
            System.out.println("[Rento] Session closed: " + currentUser.getFullName() + " <" + currentUser.getEmail() + ">");
        }
        this.currentUser = null;
        this.isLoggedIn = false;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public boolean isGuest() {
        return !isLoggedIn || currentUser == null;
    }

    public User.Role getCurrentRole() {
        if (currentUser != null) {
            return currentUser.getRole();
        }
        return User.Role.GUEST;
    }

    public String getCurrentUserName() {
        if (currentUser != null && currentUser.getFullName() != null) {
            return currentUser.getFullName();
        }
        return "Guest";
    }

    public String getCurrentUserEmail() {
        if (currentUser != null) {
            return currentUser.getEmail();
        }
        return "";
    }
}
