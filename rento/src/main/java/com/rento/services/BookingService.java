package com.rento.services;

import com.rento.dao.BookingDAO;
import com.rento.dao.UserDAO;
import com.rento.dao.VehicleDAO;
import com.rento.models.Booking;
import com.rento.models.Payment;
import com.rento.models.User;
import com.rento.models.Vehicle;
import com.rento.utils.DateTimeUtil;
import com.rento.utils.OTPGenerator;
import org.bson.types.ObjectId;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Booking service for vehicle reservation management.
 */
public class BookingService {

    private final BookingDAO bookingDAO;
    private final VehicleDAO vehicleDAO;
    private final UserDAO userDAO;
    private final ReceiptService receiptService;
    private final NotificationService notificationService;

    // Pricing constants
    private static final double TAX_RATE = 0.18; // 18% GST
    private static final double WEEKEND_SURCHARGE = 0.20; // 20% extra
    private static final double LONG_RENTAL_DISCOUNT = 0.10; // 10% off for 7+ days
    private static final double DEPOSIT_RATE = 0.25; // 25% deposit

    public BookingService() {
        this.bookingDAO = new BookingDAO();
        this.vehicleDAO = new VehicleDAO();
        this.userDAO = new UserDAO();
        this.receiptService = new ReceiptService();
        this.notificationService = new NotificationService();
    }

    /**
     * Create a new booking with dynamic pricing.
     */
    public Booking createBooking(ObjectId userId, ObjectId vehicleId, ObjectId preferredDriverId, String pickup,
                                 String dropoff, Date pickupDate, Date returnDate) throws Exception {
        // Validate vehicle availability
        Vehicle vehicle = vehicleDAO.findById(vehicleId);
        if (vehicle == null) {
            throw new Exception("Vehicle not found");
        }
        if (vehicle.getStatus() != Vehicle.Status.AVAILABLE) {
            throw new Exception("Vehicle is not available for booking");
        }
        if (vehicle.needsMaintenance()) {
            throw new Exception("Vehicle is under maintenance");
        }

        // Calculate cost
        long days = calculateDays(pickupDate, returnDate);
        if (days <= 0) {
            throw new Exception("Drop time must be after pickup time");
        }

        double baseRate = vehicle.getDailyRate();
        double subtotal = baseRate * days;

        // Weekend surcharge
        if (isWeekend(pickupDate)) {
            subtotal *= (1 + WEEKEND_SURCHARGE);
        }

        // Duration discount
        double discount = 0;
        if (days >= 7) {
            discount = subtotal * LONG_RENTAL_DISCOUNT;
            subtotal -= discount;
        }

        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;
        double deposit = total * DEPOSIT_RATE;

        User customer = userDAO.findById(userId);
        if (customer == null) {
            throw new Exception("Customer account not found");
        }
        if (customer.getWalletBalance() < deposit) {
            throw new Exception("Insufficient wallet balance for the booking deposit");
        }

        // Create booking
        Booking booking = new Booking();
        booking.setUserId(userId);
        booking.setVehicleId(vehicleId);
        ObjectId resolvedDriverId = resolveNearbyDriver(preferredDriverId, pickup);
        booking.setPreferredDriverId(resolvedDriverId);
        booking.setPickupLocation(pickup);
        booking.setDropoffLocation(dropoff);
        booking.setPickupDateTime(pickupDate);
        booking.setReturnDateTime(returnDate);
        booking.setTotalCost(total);
        booking.setDepositAmount(deposit);
        booking.setTaxAmount(tax);
        booking.setDiscountApplied(discount);
        booking.setStatus(Booking.BookingStatus.PENDING);
        booking.setOtp(OTPGenerator.generateOTP());
        booking.setVehicleName(vehicle.getDisplayName());
        booking.setUserName(customer.getFullName());
        if (resolvedDriverId != null) {
            User preferredDriver = userDAO.findById(resolvedDriverId);
            booking.setPreferredDriverName(preferredDriver != null ? preferredDriver.getFullName() : "Preferred Driver");
        }

        boolean success = bookingDAO.insertBooking(booking);
        if (!success) {
            throw new Exception("Failed to create booking");
        }

        userDAO.adjustWalletBalance(userId, -deposit);
        vehicleDAO.updateStatus(vehicleId, Vehicle.Status.RESERVED);
        notificationService.addNotification(
            userId,
            "Booking created",
            "Your booking for " + vehicle.getDisplayName() + " is waiting for payment and driver confirmation.",
            null
        );
        if (resolvedDriverId != null) {
            notificationService.addNotification(
                resolvedDriverId,
                "New booking request assigned",
                customer.getFullName() + " requested " + vehicle.getDisplayName() + " from " + pickup + " to " + dropoff + ".",
                null
            );
        }

        return booking;
    }

    /**
     * Confirm a booking (driver accepts).
     */
    public boolean confirmBooking(ObjectId bookingId, ObjectId driverId) {
        Booking booking = bookingDAO.findById(bookingId);
        if (booking == null) return false;
        if (booking.getPreferredDriverId() != null && !booking.getPreferredDriverId().equals(driverId)) {
            return false;
        }

        booking.setDriverId(driverId);
        User driver = userDAO.findById(driverId);
        booking.setDriverName(driver != null ? driver.getFullName() : "Assigned Driver");
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        // Do NOT regenerate OTP - use the one generated during booking creation
        boolean updated = bookingDAO.updateBooking(booking);
        if (updated && booking.getUserId() != null) {
            notificationService.addNotification(
                booking.getUserId(),
                "Driver confirmed your ride",
                "Booking confirmed by " + booking.getDriverName() + ". OTP: " + booking.getOtp() + " - Share with driver when pickup begins.",
                null
            );
        }
        if (updated && driverId != null) {
            notificationService.addNotification(
                driverId,
                "Ride confirmed",
                "You accepted the booking. Use the OTP displayed by customer to verify ride start.",
                null
            );
        }
        return updated;
    }

    /**
     * Complete a booking.
     */
    public boolean completeBooking(ObjectId bookingId) {
        Booking booking = bookingDAO.findById(bookingId);
        if (booking == null) return false;

        booking.setStatus(Booking.BookingStatus.COMPLETED);

        // Release vehicle
        if (booking.getVehicleId() != null) {
            vehicleDAO.updateStatus(booking.getVehicleId(), Vehicle.Status.UNDER_INSPECTION);
        }

        if (booking.getDriverId() != null) {
            userDAO.adjustWalletBalance(booking.getDriverId(), booking.getTotalCost() * 0.15);
        }

        boolean updated = bookingDAO.updateBooking(booking);
        if (updated && booking.getUserId() != null) {
            try {
                String outDir = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "RentoReceipts";
                Payment payment = new Payment();
                payment.setTransactionRef(OTPGenerator.generateTransactionRef());
                payment.setTotalAmount(booking.getTotalCost());
                String path = receiptService.generatePDFReceipt(booking, payment, outDir);
                notificationService.addNotification(booking.getUserId(), "Ride receipt generated", "Your completed ride receipt is ready.", path);
            } catch (Exception ignored) {
            }
        }
        return updated;
    }

    /**
     * Cancel a booking.
     */
    public boolean cancelBooking(ObjectId bookingId) {
        Booking booking = bookingDAO.findById(bookingId);
        if (booking == null) return false;

        booking.setStatus(Booking.BookingStatus.CANCELLED);

        // Release vehicle
        if (booking.getVehicleId() != null) {
            vehicleDAO.updateStatus(booking.getVehicleId(), Vehicle.Status.AVAILABLE);
        }

        return bookingDAO.updateBooking(booking);
    }

    public List<Booking> getBookingsByUser(ObjectId userId) {
        return bookingDAO.findByUser(userId);
    }

    public List<Booking> getBookingsByDriver(ObjectId driverId) {
        return bookingDAO.findByDriver(driverId);
    }

    public List<Booking> getPendingBookings() {
        return bookingDAO.findPending();
    }

    public List<Booking> getPendingBookingsForDriver(ObjectId driverId) {
        return bookingDAO.findPending().stream()
            .filter(booking -> booking.getPreferredDriverId() == null || booking.getPreferredDriverId().equals(driverId))
            .toList();
    }

    public List<Booking> getConfirmedBookingsForDriver(ObjectId driverId) {
        return bookingDAO.findAll().stream()
            .filter(booking -> booking.getStatus() == Booking.BookingStatus.CONFIRMED && booking.getDriverId() != null && booking.getDriverId().equals(driverId))
            .toList();
    }

    public List<User> getAvailableDrivers() {
        return userDAO.findByRole(User.Role.DRIVER);
    }

    public List<Booking> getAllBookings() {
        return bookingDAO.findAll();
    }

    public Booking getBookingById(ObjectId id) {
        return bookingDAO.findById(id);
    }

    public boolean verifyRideOtp(ObjectId driverId, String otp) {
        if (otp == null || otp.isBlank()) return false;
        for (Booking booking : getBookingsByDriver(driverId)) {
            if (booking.getStatus() == Booking.BookingStatus.CONFIRMED
                && booking.getOtp() != null
                && booking.getOtp().equals(otp.trim())) {
                booking.setOtpVerified(true);
                booking.setStatus(Booking.BookingStatus.IN_PROGRESS);
                return bookingDAO.updateBooking(booking);
            }
        }
        return false;
    }

    /**
     * Calculate pricing breakdown for display.
     */
    public double[] calculatePricing(double dailyRate, Date pickupDate, Date returnDate) {
        long days = calculateDays(pickupDate, returnDate);
        if (days <= 0) days = 1;

        double subtotal = dailyRate * days;
        double surcharge = 0;
        double discount = 0;

        if (isWeekend(pickupDate)) {
            surcharge = subtotal * WEEKEND_SURCHARGE;
            subtotal += surcharge;
        }
        if (days >= 7) {
            discount = subtotal * LONG_RENTAL_DISCOUNT;
            subtotal -= discount;
        }

        double tax = subtotal * TAX_RATE;
        double total = subtotal + tax;
        double deposit = total * DEPOSIT_RATE;

        return new double[]{days, subtotal, surcharge, discount, tax, total, deposit};
    }

    private long calculateDays(Date start, Date end) {
        return DateTimeUtil.ceilDaysBetween(start, end);
    }

    private boolean isWeekend(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int day = cal.get(Calendar.DAY_OF_WEEK);
        return day == Calendar.SATURDAY || day == Calendar.SUNDAY;
    }

    private ObjectId resolveNearbyDriver(ObjectId preferredDriverId, String pickupLocation) {
        if (preferredDriverId != null) {
            return preferredDriverId;
        }
        List<User> drivers = userDAO.findByRole(User.Role.DRIVER);
        if (drivers.isEmpty()) return null;
        String pickup = pickupLocation == null ? "" : pickupLocation.toLowerCase();
        for (User driver : drivers) {
            String address = driver.getAddress() == null ? "" : driver.getAddress().toLowerCase();
            if (!pickup.isBlank() && !address.isBlank() && (pickup.contains(address) || address.contains(pickup))) {
                return driver.getId();
            }
        }
        return drivers.get(0).getId();
    }

    public void notifyBookingPaymentSuccess(Booking booking, Payment payment) {
        if (booking == null) {
            return;
        }
        if (booking.getUserId() != null) {
            notificationService.addNotification(
                booking.getUserId(),
                "Payment received",
                "Payment completed for " + booking.getVehicleName() + ". Booking OTP is ready for pickup.",
                null
            );
        }
        if (booking.getPreferredDriverId() != null) {
            notificationService.addNotification(
                booking.getPreferredDriverId(),
                "Customer payment completed",
                "Booking for " + booking.getVehicleName() + " is paid and ready for your confirmation. OTP: " + booking.getOtp(),
                null
            );
        }
        if (payment != null && booking.getId() != null) {
            System.out.println("[BookingService] Payment recorded for booking " + booking.getId() + " with ref " + payment.getTransactionRef());
        }
    }
}
