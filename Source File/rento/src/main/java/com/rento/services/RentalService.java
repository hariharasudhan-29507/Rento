package com.rento.services;

import com.rento.dao.PaymentDAO;
import com.rento.dao.UserDAO;
import com.rento.dao.RentalDAO;
import com.rento.dao.VehicleDAO;
import com.rento.models.Payment;
import com.rento.models.Rental;
import com.rento.models.User;
import com.rento.models.Vehicle;
import com.rento.utils.DateTimeUtil;
import com.rento.utils.OTPGenerator;
import org.bson.types.ObjectId;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Rental service for vehicle rental management.
 */
public class RentalService {

    private static final double DAILY_PENALTY_RATE = 0.25;

    private final RentalDAO rentalDAO;
    private final VehicleDAO vehicleDAO;
    private final UserDAO userDAO;
    private final PaymentDAO paymentDAO;
    private final NotificationService notificationService;
    private final ReceiptService receiptService;

    public RentalService() {
        this.rentalDAO = new RentalDAO();
        this.vehicleDAO = new VehicleDAO();
        this.userDAO = new UserDAO();
        this.paymentDAO = new PaymentDAO();
        this.notificationService = new NotificationService();
        this.receiptService = new ReceiptService();
    }

    public Vehicle addVehicleForRent(Vehicle vehicle) throws Exception {
        if (vehicle == null) {
            throw new Exception("Vehicle details are required");
        }
        vehicle.setStatus(Vehicle.Status.AVAILABLE);
        vehicle.setApprovalStatus(Vehicle.ApprovalStatus.PENDING);
        boolean success = vehicleDAO.insertVehicle(vehicle);
        if (!success) {
            throw new Exception("Failed to publish vehicle");
        }
        return vehicle;
    }

    public Rental requestRental(ObjectId vehicleId, ObjectId renterId, String renterName,
                                Date startDate, Date endDate) throws Exception {
        Vehicle vehicle = vehicleDAO.findById(vehicleId);
        if (vehicle == null) {
            throw new Exception("Vehicle not found");
        }
        if (vehicle.getOwnerId() == null) {
            throw new Exception("This vehicle is not managed by a supplier");
        }
        if (vehicle.getStatus() != Vehicle.Status.AVAILABLE) {
            throw new Exception("Vehicle is currently unavailable");
        }
        if (startDate == null || endDate == null || !endDate.after(startDate)) {
            throw new Exception("Please choose a valid rental period with a later drop time");
        }

        Rental rental = new Rental();
        rental.setVehicleId(vehicleId);
        rental.setSupplierId(vehicle.getOwnerId());
        rental.setRenterId(renterId);
        rental.setRenterName(renterName);
        rental.setPricePerDay(vehicle.getDailyRate());
        rental.setVehicleName(vehicle.getDisplayName());
        User supplier = userDAO.findById(vehicle.getOwnerId());
        rental.setSupplierName(supplier != null ? supplier.getFullName() : "Supplier");
        rental.setStartDate(startDate);
        rental.setEndDate(endDate);
        rental.setStatus(Rental.RentalStatus.REQUESTED);
        rental.setTotalAmount(calculateBaseAmount(vehicle.getDailyRate(), startDate, endDate));
        rental.setPenaltyAmount(0);
        rental.setRequestedAt(new Date());

        boolean success = rentalDAO.insertRental(rental);
        if (!success) {
            throw new Exception("Failed to submit rental request");
        }
        notificationService.addNotification(
            vehicle.getOwnerId(),
            "New rental request",
            renterName + " requested " + vehicle.getDisplayName() + " for " + rental.getRentalDurationLabel() + ".",
            null
        );
        return rental;
    }

    public boolean approveRentalRequest(ObjectId rentalId) {
        Rental rental = rentalDAO.findById(rentalId);
        if (rental == null || rental.getStatus() != Rental.RentalStatus.REQUESTED) {
            return false;
        }

        rental.setStatus(Rental.RentalStatus.APPROVED);
        rental.setApprovalOtp(OTPGenerator.generateOTP());
        rental.setApprovedAt(new Date());
        rental.setOtpVerified(false);
        rental.setNotes("Supplier approved. Confirm OTP to activate.");
        boolean updated = rentalDAO.updateRental(rental);
        if (updated && rental.getRenterId() != null) {
            notificationService.addNotification(
                rental.getRenterId(),
                "Rental approved",
                "Supplier approved your rental for " + rental.getVehicleName() + ". Use OTP " + rental.getApprovalOtp() + " to activate it.",
                null
            );
        }
        return updated;
    }

    public boolean confirmSupplierOtp(ObjectId rentalId, String otp) {
        Rental rental = rentalDAO.findById(rentalId);
        if (rental == null || rental.getStatus() != Rental.RentalStatus.APPROVED || rental.getApprovalOtp() == null || otp == null) {
            return false;
        }
        if (!rental.getApprovalOtp().equals(otp.trim())) {
            return false;
        }
        rental.setOtpVerified(true);
        rental.setStatus(Rental.RentalStatus.ACTIVE);
        rental.setNotes("Supplier OTP confirmed.");
        if (rental.getVehicleId() != null) {
            vehicleDAO.updateStatus(rental.getVehicleId(), Vehicle.Status.IN_USE);
        }
        boolean updated = rentalDAO.updateRental(rental);
        if (updated && rental.getRenterId() != null) {
            try {
                String outDir = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "RentoReceipts";
                String receiptPath = receiptService.generateRentalReceipt(rental, outDir);
                rental.setReceiptPath(receiptPath);
                rentalDAO.updateRental(rental);
                notificationService.addNotification(
                    rental.getRenterId(),
                    "Rental confirmed by supplier",
                    "Your rental is now active and confirmed for " + rental.getRentalDurationLabel() + ".",
                    receiptPath
                );
            } catch (Exception ignored) {
            }
        }
        return updated;
    }

    public boolean verifyRentalOtp(ObjectId rentalId, String otp) {
        Rental rental = rentalDAO.findById(rentalId);
        if (rental == null || rental.getApprovalOtp() == null) {
            return false;
        }
        rental.setOtpVerified(rental.getApprovalOtp().equals(otp));
        return rentalDAO.updateRental(rental);
    }

    public boolean completeRental(ObjectId rentalId) {
        Rental rental = rentalDAO.findById(rentalId);
        if (rental == null) return false;

        refreshPenalty(rental);

        rental.setStatus(Rental.RentalStatus.COMPLETED);
        rental.setCompletedAt(new Date());

        if (rental.getRenterId() != null) {
            userDAO.adjustWalletBalance(rental.getRenterId(), -(rental.getTotalAmount() + rental.getPenaltyAmount()));
        }
        if (rental.getSupplierId() != null) {
            userDAO.adjustWalletBalance(rental.getSupplierId(), rental.getTotalAmount() + rental.getPenaltyAmount());
        }

        if (rental.getVehicleId() != null) {
            vehicleDAO.updateStatus(rental.getVehicleId(), Vehicle.Status.AVAILABLE);
        }
        boolean updated = rentalDAO.updateRental(rental);
        if (updated && rental.getRenterId() != null) {
            try {
                String outDir = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "RentoReceipts";
                String receiptPath = receiptService.generateRentalReceipt(rental, outDir);
                rental.setReceiptPath(receiptPath);
                rentalDAO.updateRental(rental);
                notificationService.addNotification(
                    rental.getRenterId(),
                    "Rental completed",
                    "Your rental is completed. Receipt is available for download.",
                    receiptPath
                );
            } catch (Exception ignored) {
            }
        }
        return updated;
    }

    public boolean rejectRentalRequest(ObjectId rentalId) {
        Rental rental = rentalDAO.findById(rentalId);
        if (rental == null) {
            return false;
        }
        rental.setStatus(Rental.RentalStatus.REJECTED);
        rental.setPenaltyAmount(0);
        if (rental.getVehicleId() != null) {
            vehicleDAO.updateStatus(rental.getVehicleId(), Vehicle.Status.AVAILABLE);
        }
        return rentalDAO.updateRental(rental);
    }

    public List<Rental> getRentalsBySupplier(ObjectId supplierId) {
        refreshPenalties();
        return rentalDAO.findBySupplier(supplierId);
    }

    public List<Rental> getRentalsByRenter(ObjectId renterId) {
        refreshPenalties();
        return rentalDAO.findByRenter(renterId);
    }

    public List<Rental> getPendingApprovals() {
        refreshPenalties();
        return rentalDAO.findPendingApproval();
    }

    public List<Rental> getPendingRequestsBySupplier(ObjectId supplierId) {
        refreshPenalties();
        List<Rental> rentals = new ArrayList<>();
        rentals.addAll(rentalDAO.findBySupplierAndStatus(supplierId, Rental.RentalStatus.REQUESTED));
        rentals.addAll(rentalDAO.findBySupplierAndStatus(supplierId, Rental.RentalStatus.APPROVED));
        return rentals;
    }

    public List<Rental> getAwaitingOtpBySupplier(ObjectId supplierId) {
        refreshPenalties();
        // Rentals that are approved but awaiting OTP confirmation
        return rentalDAO.findBySupplierAndStatus(supplierId, Rental.RentalStatus.APPROVED);
    }

    public List<Rental> getActiveRentalsBySupplier(ObjectId supplierId) {
        refreshPenalties();
        List<Rental> rentals = new ArrayList<>();
        rentals.addAll(rentalDAO.findBySupplierAndStatus(supplierId, Rental.RentalStatus.ACTIVE));
        rentals.addAll(rentalDAO.findBySupplierAndStatus(supplierId, Rental.RentalStatus.OVERDUE));
        return rentals;
    }

    public List<Rental> getActiveRentalsByRenter(ObjectId renterId) {
        refreshPenalties();
        List<Rental> rentals = new ArrayList<>();
        rentals.addAll(rentalDAO.findByRenterAndStatus(renterId, Rental.RentalStatus.ACTIVE));
        rentals.addAll(rentalDAO.findByRenterAndStatus(renterId, Rental.RentalStatus.OVERDUE));
        return rentals;
    }

    public List<Vehicle> getMarketplaceVehicles() {
        return vehicleDAO.findAll().stream()
            .filter(vehicle -> vehicle.getOwnerId() != null)
            .filter(vehicle -> vehicle.getStatus() == Vehicle.Status.AVAILABLE)
            .filter(vehicle -> vehicle.getApprovalStatus() == Vehicle.ApprovalStatus.APPROVED)
            .collect(Collectors.toList());
    }

    public List<Rental> getAllRentals() {
        refreshPenalties();
        return rentalDAO.findAll();
    }

    public Rental getRentalById(ObjectId id) {
        refreshPenalties();
        return rentalDAO.findById(id);
    }

    public void refreshPenalties() {
        for (Rental rental : rentalDAO.findAll()) {
            refreshPenalty(rental);
        }
    }

    private void refreshPenalty(Rental rental) {
        if (rental == null) {
            return;
        }
        if ((rental.getStatus() == Rental.RentalStatus.ACTIVE || rental.getStatus() == Rental.RentalStatus.OVERDUE)
            && rental.getEndDate() != null
            && new Date().after(rental.getEndDate())) {
            long overdueDays = Math.max(1, ((new Date().getTime() - rental.getEndDate().getTime()) / (1000L * 60 * 60 * 24)));
            double penalty = overdueDays * rental.getPricePerDay() * DAILY_PENALTY_RATE;
            rental.setPenaltyAmount(penalty);
            rental.setStatus(Rental.RentalStatus.OVERDUE);
            rentalDAO.updateRental(rental);
        }
    }

    private double calculateBaseAmount(double pricePerDay, Date startDate, Date endDate) {
        long days = DateTimeUtil.ceilDaysBetween(startDate, endDate);
        return Math.max(1, days) * pricePerDay;
    }

    public boolean attachPaymentToRental(ObjectId rentalId, Payment payment) {
        Rental rental = rentalDAO.findById(rentalId);
        if (rental == null || payment == null) {
            return false;
        }
        rental.setPaymentId(payment.getId());
        rental.setPaymentMethod(payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null);
        rental.setPaymentStatus(payment.getStatus() != null ? payment.getStatus().name() : null);
        rental.setCashPaymentPending(payment.getPaymentMethod() == Payment.PaymentMethod.CASH_ON_DELIVERY);
        rental.setPaidVerified(payment.getPaymentMethod() != Payment.PaymentMethod.CASH_ON_DELIVERY);
        if (payment.getPaymentMethod() != Payment.PaymentMethod.CASH_ON_DELIVERY) {
            try {
                String outDir = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "RentoReceipts";
                String receiptPath = receiptService.generateRentalReceipt(rental, outDir);
                rental.setReceiptPath(receiptPath);
                payment.setReceiptPath(receiptPath);
                paymentDAO.updatePayment(payment);
            } catch (Exception ignored) {
            }
        }
        return rentalDAO.updateRental(rental);
    }

    public boolean verifyCashPaymentForRental(ObjectId rentalId, String verifierName) {
        Rental rental = rentalDAO.findById(rentalId);
        if (rental == null || rental.getPaymentId() == null) {
            return false;
        }
        Payment payment = paymentDAO.findById(rental.getPaymentId());
        if (payment == null || payment.getPaymentMethod() != Payment.PaymentMethod.CASH_ON_DELIVERY) {
            return false;
        }

        payment.setCashVerified(true);
        payment.setCashVerifiedBy(verifierName);
        payment.setCashVerifiedAt(new Date());
        payment.setStatus(Payment.PaymentStatus.CASH_CONFIRMED);
        paymentDAO.updatePayment(payment);

        rental.setCashPaymentPending(false);
        rental.setPaidVerified(true);
        rental.setPaidVerifiedBy(verifierName);
        rental.setPaidVerifiedAt(new Date());
        rental.setPaymentStatus(payment.getStatus().name());

        try {
            String outDir = System.getProperty("user.home") + File.separator + "Documents" + File.separator + "RentoReceipts";
            String receiptPath = receiptService.generateRentalReceipt(rental, outDir);
            rental.setReceiptPath(receiptPath);
            payment.setReceiptPath(receiptPath);
            paymentDAO.updatePayment(payment);
            if (rental.getRenterId() != null) {
                notificationService.addNotification(
                    rental.getRenterId(),
                    "Cash payment verified",
                    "Supplier confirmed cash payment for " + rental.getVehicleName() + ". Receipt is ready.",
                    receiptPath
                );
            }
        } catch (Exception ignored) {
        }

        return rentalDAO.updateRental(rental);
    }
}
