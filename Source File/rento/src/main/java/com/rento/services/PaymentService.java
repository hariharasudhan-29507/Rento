package com.rento.services;

import com.rento.dao.BookingDAO;
import com.rento.dao.PaymentDAO;
import com.rento.dao.RentalDAO;
import com.rento.models.Booking;
import com.rento.models.Payment;
import com.rento.models.Rental;
import com.rento.utils.OTPGenerator;
import com.rento.utils.ValidationUtil;
import org.bson.types.ObjectId;

import java.util.Date;
import java.util.List;

/**
 * Payment service for simulated transaction processing.
 */
public class PaymentService {

    private final PaymentDAO paymentDAO;
    private final BookingDAO bookingDAO;
    private final RentalDAO rentalDAO;

    public PaymentService() {
        this.paymentDAO = new PaymentDAO();
        this.bookingDAO = new BookingDAO();
        this.rentalDAO = new RentalDAO();
    }

    /**
     * Process a payment (simulated).
     * @return stored payment or null when validation/persistence fails
     */
    public Payment processPayment(ObjectId bookingId, ObjectId userId, double amount, double taxAmount,
                                  double totalAmount, Payment.PaymentMethod method, String accountReference,
                                  String accountHolderName, String expiry, String cvv) {
        if (method == Payment.PaymentMethod.CREDIT_CARD) {
            if (!ValidationUtil.isValidCardNumber(accountReference)) {
                return null;
            }
            if (!ValidationUtil.isValidName(accountHolderName)) {
                return null;
            }
            if (!ValidationUtil.isValidExpiryDate(expiry)) {
                return null;
            }
            if (!ValidationUtil.isValidCVV(cvv)) {
                return null;
            }
        } else if (method == Payment.PaymentMethod.UPI && !ValidationUtil.isValidUpiId(accountReference)) {
            return null;
        }

        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setTaxAmount(taxAmount);
        payment.setTotalAmount(totalAmount);
        payment.setPaymentMethod(method);
        if (method == Payment.PaymentMethod.CREDIT_CARD) {
            payment.setCardNumber(maskReference(method, accountReference));
        } else if (method == Payment.PaymentMethod.UPI) {
            payment.setUpiId(maskReference(method, accountReference));
        } else {
            payment.setPaymentLabel("Cash on delivery");
        }
        payment.setCardHolderName(accountHolderName);
        payment.setTransactionRef(OTPGenerator.generateTransactionRef());
        payment.setPaymentDate(new Date());
        payment.setStatus(method == Payment.PaymentMethod.CASH_ON_DELIVERY
            ? Payment.PaymentStatus.PENDING_CASH_CONFIRMATION
            : Payment.PaymentStatus.COMPLETED);
        payment.setCashVerified(method != Payment.PaymentMethod.CASH_ON_DELIVERY);
        payment.setPaymentLabel(buildLabel(method, accountReference));

        boolean success = paymentDAO.insertPayment(payment);
        if (success && bookingId != null) {
            Booking booking = bookingDAO.findById(bookingId);
            if (booking != null) {
                booking.setPaymentId(payment.getId());
                booking.setPaymentMethod(method.name());
                booking.setPaymentStatus(payment.getStatus().name());
                booking.setCashPaymentPending(method == Payment.PaymentMethod.CASH_ON_DELIVERY);
                booking.setPaidVerified(method != Payment.PaymentMethod.CASH_ON_DELIVERY);
                bookingDAO.updateBooking(booking);
            }
        }
        return success ? payment : null;
    }

    public Payment createRentalPayment(ObjectId rentalId, ObjectId userId, double totalAmount, Payment.PaymentMethod method,
                                       String accountReference, String accountHolderName) {
        if (method == Payment.PaymentMethod.CREDIT_CARD && !ValidationUtil.isValidCardNumber(accountReference)) {
            return null;
        }
        if (method == Payment.PaymentMethod.UPI && !ValidationUtil.isValidUpiId(accountReference)) {
            return null;
        }

        Payment payment = new Payment();
        payment.setRentalId(rentalId);
        payment.setUserId(userId);
        payment.setAmount(totalAmount);
        payment.setTaxAmount(0);
        payment.setTotalAmount(totalAmount);
        payment.setPaymentMethod(method);
        payment.setCardHolderName(accountHolderName);
        payment.setPaymentLabel(buildLabel(method, accountReference));
        if (method == Payment.PaymentMethod.CREDIT_CARD) {
            payment.setCardNumber(maskReference(method, accountReference));
        } else if (method == Payment.PaymentMethod.UPI) {
            payment.setUpiId(maskReference(method, accountReference));
        }
        payment.setTransactionRef(OTPGenerator.generateTransactionRef());
        payment.setPaymentDate(new Date());
        payment.setStatus(method == Payment.PaymentMethod.CASH_ON_DELIVERY
            ? Payment.PaymentStatus.PENDING_CASH_CONFIRMATION
            : Payment.PaymentStatus.COMPLETED);
        payment.setCashVerified(method != Payment.PaymentMethod.CASH_ON_DELIVERY);

        boolean success = paymentDAO.insertPayment(payment);
        if (success && rentalId != null) {
            Rental rental = rentalDAO.findById(rentalId);
            if (rental != null) {
                rental.setPaymentId(payment.getId());
                rental.setPaymentMethod(method.name());
                rental.setPaymentStatus(payment.getStatus().name());
                rental.setCashPaymentPending(method == Payment.PaymentMethod.CASH_ON_DELIVERY);
                rental.setPaidVerified(method != Payment.PaymentMethod.CASH_ON_DELIVERY);
                rentalDAO.updateRental(rental);
            }
        }
        return success ? payment : null;
    }

    public Payment getPaymentByBooking(ObjectId bookingId) {
        return paymentDAO.findByBooking(bookingId);
    }

    public Payment getPaymentByRental(ObjectId rentalId) {
        return paymentDAO.findByRental(rentalId);
    }

    public List<Payment> getPaymentsByUser(ObjectId userId) {
        return paymentDAO.findByUser(userId);
    }

    public Payment getPaymentById(ObjectId id) {
        return paymentDAO.findById(id);
    }

    private String maskReference(Payment.PaymentMethod method, String reference) {
        if (reference == null || reference.isBlank()) {
            return "SIMULATED";
        }
        if (method == Payment.PaymentMethod.CREDIT_CARD) {
            return ValidationUtil.maskCardNumber(reference);
        }
        if (method == Payment.PaymentMethod.UPI) {
            return ValidationUtil.maskUpiId(reference);
        }
        String cleaned = reference.trim();
        if (cleaned.length() <= 4) {
            return cleaned;
        }
        return "*".repeat(cleaned.length() - 4) + cleaned.substring(cleaned.length() - 4);
    }

    private String buildLabel(Payment.PaymentMethod method, String reference) {
        return switch (method) {
            case CREDIT_CARD -> "Credit Card • " + ValidationUtil.maskCardNumber(reference);
            case UPI -> "UPI • " + ValidationUtil.maskUpiId(reference);
            case CASH_ON_DELIVERY -> "Cash on Delivery";
        };
    }
}
