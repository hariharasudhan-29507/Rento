package com.rento.services;

import com.rento.dao.PaymentDAO;
import com.rento.models.Payment;
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

    public PaymentService() {
        this.paymentDAO = new PaymentDAO();
    }

    /**
     * Process a payment (simulated).
     * @return stored payment or null when validation/persistence fails
     */
    public Payment processPayment(ObjectId bookingId, ObjectId userId, double amount, double taxAmount,
                                  double totalAmount, Payment.PaymentMethod method, String accountReference,
                                  String accountHolderName, String expiry, String cvv) {
        if (method == Payment.PaymentMethod.CREDIT_CARD || method == Payment.PaymentMethod.DEBIT_CARD) {
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
        }

        Payment payment = new Payment();
        payment.setBookingId(bookingId);
        payment.setUserId(userId);
        payment.setAmount(amount);
        payment.setTaxAmount(taxAmount);
        payment.setTotalAmount(totalAmount);
        payment.setPaymentMethod(method);
        payment.setCardNumber(maskReference(method, accountReference));
        payment.setCardHolderName(accountHolderName);
        payment.setTransactionRef(OTPGenerator.generateTransactionRef());
        payment.setPaymentDate(new Date());
        payment.setStatus(Payment.PaymentStatus.COMPLETED);

        boolean success = paymentDAO.insertPayment(payment);
        return success ? payment : null;
    }

    public Payment getPaymentByBooking(ObjectId bookingId) {
        return paymentDAO.findByBooking(bookingId);
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
        if (method == Payment.PaymentMethod.CREDIT_CARD || method == Payment.PaymentMethod.DEBIT_CARD) {
            return ValidationUtil.maskCardNumber(reference);
        }
        String cleaned = reference.trim();
        if (cleaned.length() <= 4) {
            return cleaned;
        }
        return "*".repeat(cleaned.length() - 4) + cleaned.substring(cleaned.length() - 4);
    }
}
