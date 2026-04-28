package com.rento.models;

import org.bson.types.ObjectId;
import java.util.Date;

/**
 * User model representing all system actors.
 */
public class User {

    public enum Role {
        GUEST, USER, DRIVER, SUPPLIER, ADMIN
    }

    private ObjectId id;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String password; // hashed
    private String driverLicenseNumber;
    private Role role;
    private boolean verified;
    private boolean locked;
    private String lockReason;
    private Date lockedAt;
    private Date lastLoginAt;
    private Date createdAt;
    private Date updatedAt;
    private int age;
    private double walletBalance;

    public User() {
        this.role = Role.GUEST;
        this.verified = false;
        this.locked = false;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public User(String fullName, String email, String phone, String password, Role role) {
        this();
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
    }

    // Getters and Setters
    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getDriverLicenseNumber() { return driverLicenseNumber; }
    public void setDriverLicenseNumber(String driverLicenseNumber) { this.driverLicenseNumber = driverLicenseNumber; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }

    public String getLockReason() { return lockReason; }
    public void setLockReason(String lockReason) { this.lockReason = lockReason; }

    public Date getLockedAt() { return lockedAt; }
    public void setLockedAt(Date lockedAt) { this.lockedAt = lockedAt; }

    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public double getWalletBalance() { return walletBalance; }
    public void setWalletBalance(double walletBalance) { this.walletBalance = walletBalance; }

    @Override
    public String toString() {
        return "User{name='" + fullName + "', email='" + email + "', role=" + role + "}";
    }
}
