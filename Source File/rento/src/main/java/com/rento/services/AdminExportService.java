package com.rento.services;

import com.rento.dao.BookingDAO;
import com.rento.dao.PaymentDAO;
import com.rento.dao.PaymentMethodDAO;
import com.rento.dao.RentalDAO;
import com.rento.dao.UserDAO;
import com.rento.dao.VehicleDAO;
import com.rento.models.Booking;
import com.rento.models.Payment;
import com.rento.models.PaymentMethodProfile;
import com.rento.models.Rental;
import com.rento.models.User;
import com.rento.models.Vehicle;
import com.rento.utils.MongoCollections;
import com.rento.utils.MongoDBConnection;

import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class AdminExportService {
    private final UserDAO userDAO = new UserDAO();
    private final VehicleDAO vehicleDAO = new VehicleDAO();
    private final BookingDAO bookingDAO = new BookingDAO();
    private final RentalDAO rentalDAO = new RentalDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final PaymentMethodDAO paymentMethodDAO = new PaymentMethodDAO();

    public String exportAllData(String outputDir) throws Exception {
        new File(outputDir).mkdirs();
        String path = outputDir + File.separator + "rento_full_export_" + System.currentTimeMillis() + ".txt";
        List<User> users = userDAO.findAll();
        List<Vehicle> vehicles = vehicleDAO.findAll();
        List<Booking> bookings = bookingDAO.findAll();
        List<Rental> rentals = rentalDAO.findAll();
        List<Payment> payments = paymentDAO.findAll();
        List<PaymentMethodProfile> paymentMethods = paymentMethodDAO.findAll();

        try (FileWriter writer = new FileWriter(path)) {
            writer.write("RENTO FULL SYSTEM EXPORT\n\n");
            writer.write("COLLECTION COUNTS\n");
            MongoDBConnection.getInstance().getCollectionCounts(MongoCollections.ALL_COLLECTIONS).forEach((name, count) -> {
                try {
                    writer.write(name + " : " + count + "\n");
                } catch (Exception ignored) {
                }
            });
            writer.write("\n");
            writer.write("USERS (" + users.size() + ")\n");
            for (User u : users) {
                writer.write(u.getFullName() + " | " + u.getEmail() + " | " + u.getRole() + " | " + (u.isLocked() ? "LOCKED" : "ACTIVE") + "\n");
            }
            writer.write("\nVEHICLES (" + vehicles.size() + ")\n");
            for (Vehicle v : vehicles) {
                writer.write(v.getDisplayName() + " | " + v.getStatus() + " | " + v.getApprovalStatus() + "\n");
            }
            writer.write("\nBOOKINGS (" + bookings.size() + ")\n");
            for (Booking b : bookings) {
                writer.write((b.getVehicleName() != null ? b.getVehicleName() : "Vehicle")
                    + " | " + b.getStatus()
                    + " | " + b.getPickupLocation()
                    + " -> " + b.getDropoffLocation() + "\n");
            }
            writer.write("\nRENTALS (" + rentals.size() + ")\n");
            for (Rental r : rentals) {
                writer.write((r.getVehicleName() != null ? r.getVehicleName() : "Vehicle")
                    + " | " + r.getStatus()
                    + " | " + r.getRenterName()
                    + " | " + r.getSupplierName() + "\n");
            }
            writer.write("\nPAYMENTS (" + payments.size() + ")\n");
            for (Payment payment : payments) {
                writer.write(payment.getTransactionRef() + " | " + payment.getPaymentMethod() + " | " + payment.getStatus() + "\n");
            }
            writer.write("\nPAYMENT METHODS (" + paymentMethods.size() + ")\n");
            for (PaymentMethodProfile profile : paymentMethods) {
                writer.write(profile.getProfileName() + " | " + profile.getMethodType() + " | " + profile.getMaskedReference() + "\n");
            }
        }
        return path;
    }
}
