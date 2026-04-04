package com.vrbs.model;

import java.util.Objects;

public final class SessionUser {

    private final String id;
    private final String username;
    private final String displayName;
    private final UserRole role;
    private final String email;

    public SessionUser(String id, String username, String displayName, UserRole role, String email) {
        this.id = Objects.requireNonNull(id);
        this.username = Objects.requireNonNull(username);
        this.displayName = displayName != null ? displayName : username;
        this.role = Objects.requireNonNull(role);
        this.email = email != null ? email : "";
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }
}
