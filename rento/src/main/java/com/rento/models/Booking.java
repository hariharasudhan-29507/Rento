package com.rento.models;

import org.bson.types.ObjectId;
import com.rento.utils.DateTimeUtil;
import java.util.Date;

/**
 * Booking model for vehicle reservations.
 */
public class Booking {

    public enum BookingStatus {
        PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
    }

    private ObjectId id;
    private ObjectId userId;
    private ObjectId vehicleId;
    private ObjectId driverId;
    private ObjectId preferredDriverId;
    private String pickupLocation;
    private String dropoffLocation;
    private Date pickupDateTime;
    private Date returnDateTime;
    private double totalCost;
    private double depositAmount;
    private double taxAmount;
    private double discountApplied;
    private BookingStatus status;
    private String otp;
    private boolean otpVerified;
    private Date createdAt;
    private Date updatedAt;

    // Display-only fields (not stored, populated from joins)
    private String vehicleName;
    private String userName;
    private String driverName;
    private String preferredDriverName;

    public Booking() {
        this.status = BookingStatus.PENDING;
        this.otpVerified = false;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    // Getters and Setters
    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public ObjectId getUserId() { return userId; }
    public void setUserId(ObjectId userId) { this.userId = userId; }

    public ObjectId getVehicleId() { return vehicleId; }
    public void setVehicleId(ObjectId vehicleId) { this.vehicleId = vehicleId; }

    public ObjectId getDriverId() { return driverId; }
    public void setDriverId(ObjectId driverId) { this.driverId = driverId; }

    public ObjectId getPreferredDriverId() { return preferredDriverId; }
    public void setPreferredDriverId(ObjectId preferredDriverId) { this.preferredDriverId = preferredDriverId; }

    public String getPickupLocation() { return pickupLocation; }
    public void setPickupLocation(String pickupLocation) { this.pickupLocation = pickupLocation; }

    public String getDropoffLocation() { return dropoffLocation; }
    public void setDropoffLocation(String dropoffLocation) { this.dropoffLocation = dropoffLocation; }

    public Date getPickupDateTime() { return pickupDateTime; }
    public void setPickupDateTime(Date pickupDateTime) { this.pickupDateTime = pickupDateTime; }

    public Date getReturnDateTime() { return returnDateTime; }
    public void setReturnDateTime(Date returnDateTime) { this.returnDateTime = returnDateTime; }

    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }

    public double getDepositAmount() { return depositAmount; }
    public void setDepositAmount(double depositAmount) { this.depositAmount = depositAmount; }

    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }

    public double getDiscountApplied() { return discountApplied; }
    public void setDiscountApplied(double discountApplied) { this.discountApplied = discountApplied; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public boolean isOtpVerified() { return otpVerified; }
    public void setOtpVerified(boolean otpVerified) { this.otpVerified = otpVerified; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }

    public String getPreferredDriverName() { return preferredDriverName; }
    public void setPreferredDriverName(String preferredDriverName) { this.preferredDriverName = preferredDriverName; }

    /**
     * Calculate rental duration in days.
     */
    public long getRentalDays() {
        return DateTimeUtil.ceilDaysBetween(pickupDateTime, returnDateTime);
    }

    public String getRentalDurationLabel() {
        return DateTimeUtil.formatDuration(pickupDateTime, returnDateTime);
    }
}
