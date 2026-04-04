package com.vrbs.session;

import com.vrbs.model.SessionUser;
import com.vrbs.model.UserRole;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.Optional;
import java.util.function.Consumer;

public final class SessionManager {

    private static final SessionManager INSTANCE = new SessionManager();

    private final ObjectProperty<SessionUser> user = new SimpleObjectProperty<>();
    private Consumer<SessionUser> onLogin;
    private Runnable onLogout;

    private SessionManager() {
    }

    public static SessionManager get() {
        return INSTANCE;
    }

    public ObjectProperty<SessionUser> userProperty() {
        return user;
    }

    public Optional<SessionUser> currentUser() {
        return Optional.ofNullable(user.get());
    }

    public UserRole currentRoleOrGuest() {
        SessionUser u = user.get();
        return u == null ? UserRole.GUEST : u.getRole();
    }

    public boolean isLoggedIn() {
        return user.get() != null;
    }

    public void setOnLogin(Consumer<SessionUser> onLogin) {
        this.onLogin = onLogin;
    }

    public void setOnLogout(Runnable onLogout) {
        this.onLogout = onLogout;
    }

    public void login(SessionUser sessionUser) {
        user.set(sessionUser);
        if (onLogin != null) {
            onLogin.accept(sessionUser);
        }
    }

    public void logout() {
        user.set(null);
        if (onLogout != null) {
            onLogout.run();
        }
    }
}
