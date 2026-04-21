package com.rento.models;

import org.bson.types.ObjectId;
import java.util.Date;

/**
 * Vehicle model for the fleet.
 */
public class Vehicle {

    public enum Category {
        SEDAN, SUV, HATCHBACK, COUPE, TRUCK, VAN, BIKE, BUS
    }

    public enum FuelType {
        PETROL, DIESEL, ELECTRIC, HYBRID, CNG
    }

    public enum Status {
        AVAILABLE, RESERVED, IN_USE, UNDER_INSPECTION, MAINTENANCE, UNAVAILABLE
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }

    private ObjectId id;
    private String vin;
    private String make;
    private String model;
    private int year;
    private String licensePlate;
    private Category category;
    private FuelType fuelType;
    private Status status;
    private double currentMileage;
    private double nextServiceDue;
    private double dailyRate;
    private int seats;
    private String color;
    private String imageUrl;
    private String description;
    private ObjectId ownerId; // Supplier who owns the vehicle
    private String branchLocation;
    private ApprovalStatus approvalStatus;
    private String adminReviewNote;
    private Date createdAt;
    private Date updatedAt;

    public Vehicle() {
        this.status = Status.AVAILABLE;
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public Vehicle(String make, String model, int year, Category category, FuelType fuelType, double dailyRate) {
        this();
        this.make = make;
        this.model = model;
        this.year = year;
        this.category = category;
        this.fuelType = fuelType;
        this.dailyRate = dailyRate;
    }

    // Getters and Setters
    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getLicensePlate() { return licensePlate; }
    public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public FuelType getFuelType() { return fuelType; }
    public void setFuelType(FuelType fuelType) { this.fuelType = fuelType; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public double getCurrentMileage() { return currentMileage; }
    public void setCurrentMileage(double currentMileage) { this.currentMileage = currentMileage; }

    public double getNextServiceDue() { return nextServiceDue; }
    public void setNextServiceDue(double nextServiceDue) { this.nextServiceDue = nextServiceDue; }

    public double getDailyRate() { return dailyRate; }
    public void setDailyRate(double dailyRate) { this.dailyRate = dailyRate; }

    public int getSeats() { return seats; }
    public void setSeats(int seats) { this.seats = seats; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ObjectId getOwnerId() { return ownerId; }
    public void setOwnerId(ObjectId ownerId) { this.ownerId = ownerId; }

    public String getBranchLocation() { return branchLocation; }
    public void setBranchLocation(String branchLocation) { this.branchLocation = branchLocation; }

    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getAdminReviewNote() { return adminReviewNote; }
    public void setAdminReviewNote(String adminReviewNote) { this.adminReviewNote = adminReviewNote; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    /**
     * Check if vehicle needs maintenance based on mileage.
     */
    public boolean needsMaintenance() {
        return nextServiceDue > 0 && currentMileage >= nextServiceDue;
    }

    /**
     * Get display name for the vehicle.
     */
    public String getDisplayName() {
        return year + " " + make + " " + model;
    }

    @Override
    public String toString() {
        return "Vehicle{" + getDisplayName() + ", category=" + category + ", status=" + status + ", rate=$" + dailyRate + "/day}";
    }
}
