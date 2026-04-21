package com.rento.services;

import com.rento.dao.UserDAO;
import com.rento.dao.VehicleDAO;
import com.rento.models.User;
import com.rento.models.Vehicle;
import com.rento.security.PasswordHasher;
import com.rento.utils.MongoDBConnection;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Seeds lightweight demo data when the MongoDB collections are empty.
 */
public class DemoDataService {

    private final UserDAO userDAO = new UserDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();

    public void seedIfRequired() {
        if (!MongoDBConnection.getInstance().isConnected()) {
            return;
        }

        seedUsers();
        seedVehicles();
    }

    private void seedUsers() {
        if (!userDAO.findAll().isEmpty()) {
            return;
        }

        List<User> demoUsers = Arrays.asList(
            createUser("Rento Customer", "user@rento.local", "+919876543210", User.Role.USER),
            createUser("Rento Driver", "driver@rento.local", "+919876543211", User.Role.DRIVER),
            createUser("Rento Supplier", "supplier@rento.local", "+919876543212", User.Role.SUPPLIER),
            createUser("Rento Admin", "admin@rento.local", "+919876543213", User.Role.ADMIN)
        );

        for (User user : demoUsers) {
            userDAO.insertUser(user);
        }
    }

    private void seedVehicles() {
        User supplier = userDAO.findByEmail("supplier@rento.local");
        List<Vehicle> existingVehicles = vehicleDAO.findAll();
        if (!existingVehicles.isEmpty()) {
            if (supplier != null) {
                for (Vehicle vehicle : existingVehicles) {
                    if (vehicle.getOwnerId() == null) {
                        vehicle.setOwnerId(supplier.getId());
                    }
                    if (vehicle.getStatus() == null || vehicle.getStatus() == Vehicle.Status.UNAVAILABLE) {
                        vehicle.setStatus(Vehicle.Status.AVAILABLE);
                    }
                    if (vehicle.getApprovalStatus() == null) {
                        vehicle.setApprovalStatus(Vehicle.ApprovalStatus.APPROVED);
                    }
                    vehicleDAO.updateVehicle(vehicle);
                }
                ensureMinimumFleet(existingVehicles, supplier);
            }
            return;
        }

        List<Vehicle> demoVehicles = Arrays.asList(
            createVehicle("Toyota", "Camry", 2024, Vehicle.Category.SEDAN, Vehicle.FuelType.PETROL, 2800, 5, "Chennai"),
            createVehicle("Hyundai", "Creta", 2024, Vehicle.Category.SUV, Vehicle.FuelType.DIESEL, 3600, 5, "Madurai"),
            createVehicle("Tata", "Nexon EV", 2024, Vehicle.Category.SUV, Vehicle.FuelType.ELECTRIC, 3900, 5, "Coimbatore"),
            createVehicle("Royal Enfield", "Classic 350", 2023, Vehicle.Category.BIKE, Vehicle.FuelType.PETROL, 900, 2, "Sivakasi"),
            createVehicle("Mercedes", "V-Class", 2024, Vehicle.Category.VAN, Vehicle.FuelType.DIESEL, 7800, 7, "Bengaluru"),
            createVehicle("Honda", "City", 2024, Vehicle.Category.SEDAN, Vehicle.FuelType.PETROL, 2600, 5, "Salem"),
            createVehicle("Mahindra", "Thar", 2024, Vehicle.Category.SUV, Vehicle.FuelType.DIESEL, 4200, 4, "Erode"),
            createVehicle("TVS", "Ntorq 125", 2024, Vehicle.Category.BIKE, Vehicle.FuelType.PETROL, 700, 2, "Tirunelveli"),
            createVehicle("Ather", "450X", 2025, Vehicle.Category.BIKE, Vehicle.FuelType.ELECTRIC, 850, 2, "Chennai"),
            createVehicle("Kia", "Carens", 2024, Vehicle.Category.VAN, Vehicle.FuelType.PETROL, 4300, 6, "Trichy"),
            createVehicle("Maruti", "Baleno", 2024, Vehicle.Category.HATCHBACK, Vehicle.FuelType.PETROL, 1900, 5, "Pondicherry"),
            createVehicle("BMW", "X1", 2025, Vehicle.Category.SUV, Vehicle.FuelType.DIESEL, 6800, 5, "Bengaluru"),
            createVehicle("Yamaha", "FZ-S", 2024, Vehicle.Category.BIKE, Vehicle.FuelType.PETROL, 750, 2, "Madurai")
        );

        for (Vehicle vehicle : demoVehicles) {
            if (supplier != null) {
                vehicle.setOwnerId(supplier.getId());
            }
            vehicle.setApprovalStatus(Vehicle.ApprovalStatus.APPROVED);
            vehicleDAO.insertVehicle(vehicle);
        }
    }

    private void ensureMinimumFleet(List<Vehicle> existingVehicles, User supplier) {
        List<Vehicle> additionalVehicles = Arrays.asList(
            createVehicle("Skoda", "Slavia", 2024, Vehicle.Category.SEDAN, Vehicle.FuelType.PETROL, 2750, 5, "Chennai"),
            createVehicle("Tata", "Punch EV", 2025, Vehicle.Category.SUV, Vehicle.FuelType.ELECTRIC, 3200, 5, "Coimbatore"),
            createVehicle("Bajaj", "Pulsar N160", 2024, Vehicle.Category.BIKE, Vehicle.FuelType.PETROL, 680, 2, "Madurai"),
            createVehicle("Suzuki", "Access 125", 2024, Vehicle.Category.BIKE, Vehicle.FuelType.PETROL, 620, 2, "Trichy")
        );
        Set<String> existingKeys = existingVehicles.stream()
            .map(vehicle -> vehicle.getMake() + "|" + vehicle.getModel() + "|" + vehicle.getYear())
            .collect(Collectors.toSet());
        for (Vehicle vehicle : additionalVehicles) {
            String key = vehicle.getMake() + "|" + vehicle.getModel() + "|" + vehicle.getYear();
            if (!existingKeys.contains(key)) {
                vehicle.setOwnerId(supplier.getId());
                vehicle.setApprovalStatus(Vehicle.ApprovalStatus.APPROVED);
                vehicleDAO.insertVehicle(vehicle);
            }
        }
    }

    private User createUser(String name, String email, String phone, User.Role role) {
        User user = new User();
        user.setFullName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAddress("Tamil Nadu, India");
        user.setDriverLicenseNumber("DL-" + role.name() + "-2026");
        user.setPassword(PasswordHasher.hashPassword("Rento@123"));
        user.setRole(role);
        user.setVerified(true);
        user.setAge(24);
        switch (role) {
            case DRIVER:
                user.setWalletBalance(15000);
                break;
            case SUPPLIER:
                user.setWalletBalance(85000);
                break;
            case ADMIN:
                user.setWalletBalance(150000);
                break;
            default:
                user.setWalletBalance(32000);
                break;
        }
        return user;
    }

    private Vehicle createVehicle(String make, String model, int year, Vehicle.Category category,
                                  Vehicle.FuelType fuelType, double dailyRate, int seats, String branch) {
        Vehicle vehicle = new Vehicle(make, model, year, category, fuelType, dailyRate);
        vehicle.setSeats(seats);
        vehicle.setColor("Premium");
        vehicle.setVin("RENTO-" + make.substring(0, Math.min(make.length(), 3)).toUpperCase() + "-" + year + "-" + model.substring(0, 1).toUpperCase());
        vehicle.setLicensePlate("TN-" + (10 + seats) + "-" + Math.abs(model.hashCode() % 9000));
        vehicle.setBranchLocation(branch);
        vehicle.setCurrentMileage(12000);
        vehicle.setNextServiceDue(25000);
        vehicle.setStatus(Vehicle.Status.AVAILABLE);
        vehicle.setDescription("Curated demo fleet vehicle for the Rento booking and rental workflow.");
        return vehicle;
    }
}
