package com.rento.models;

import com.rento.utils.DateTimeUtil;
import org.bson.types.ObjectId;
import java.util.Date;

/**
 * Rental model for supplier-approved rental requests.
 */
public class Rental {

    public enum RentalStatus {
        REQUESTED, APPROVED, ACTIVE, COMPLETED, OVERDUE, REJECTED, CANCELLED
    }

    private ObjectId id;
    private ObjectId vehicleId;
    private ObjectId supplierId;
    private ObjectId renterId;
    private String vehicleName;
    private String supplierName;
    private String renterName;
    private double pricePerDay;
    private Date startDate;
    private Date endDate;
    private RentalStatus status;
    private String approvalOtp;
    private boolean otpVerified;
    private String notes;
    private double totalAmount;
    private double penaltyAmount;
    private Date requestedAt;
    private Date approvedAt;
    private Date completedAt;
    private Date createdAt;
    private Date updatedAt;

    public Rental() {
        this.status = RentalStatus.REQUESTED;
        this.otpVerified = false;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.requestedAt = new Date();
    }

    // Getters and Setters
    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public ObjectId getVehicleId() { return vehicleId; }
    public void setVehicleId(ObjectId vehicleId) { this.vehicleId = vehicleId; }

    public ObjectId getSupplierId() { return supplierId; }
    public void setSupplierId(ObjectId supplierId) { this.supplierId = supplierId; }

    public ObjectId getRenterId() { return renterId; }
    public void setRenterId(ObjectId renterId) { this.renterId = renterId; }

    public String getVehicleName() { return vehicleName; }
    public void setVehicleName(String vehicleName) { this.vehicleName = vehicleName; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public String getRenterName() { return renterName; }
    public void setRenterName(String renterName) { this.renterName = renterName; }

    public double getPricePerDay() { return pricePerDay; }
    public void setPricePerDay(double pricePerDay) { this.pricePerDay = pricePerDay; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    public RentalStatus getStatus() { return status; }
    public void setStatus(RentalStatus status) { this.status = status; }

    public String getApprovalOtp() { return approvalOtp; }
    public void setApprovalOtp(String otp) { this.approvalOtp = otp; }

    public boolean isOtpVerified() { return otpVerified; }
    public void setOtpVerified(boolean otpVerified) { this.otpVerified = otpVerified; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getPenaltyAmount() { return penaltyAmount; }
    public void setPenaltyAmount(double penaltyAmount) { this.penaltyAmount = penaltyAmount; }

    public Date getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Date requestedAt) { this.requestedAt = requestedAt; }

    public Date getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Date approvedAt) { this.approvedAt = approvedAt; }

    public Date getCompletedAt() { return completedAt; }
    public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public long getRentalDays() {
        return DateTimeUtil.ceilDaysBetween(startDate, endDate);
    }

    public long getRentalHours() {
        return DateTimeUtil.ceilHoursBetween(startDate, endDate);
    }

    public String getRentalDurationLabel() {
        return DateTimeUtil.formatDuration(startDate, endDate);
    }

    public boolean isOverdue() {
        return (status == RentalStatus.ACTIVE || status == RentalStatus.OVERDUE)
            && endDate != null
            && completedAt == null
            && new Date().after(endDate);
    }
}
