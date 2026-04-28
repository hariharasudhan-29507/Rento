package com.rento.models;

import org.bson.types.ObjectId;
import java.util.Date;

/**
 * Payment model for transaction tracking.
 */
public class Payment {

    public enum PaymentStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, REFUNDED, PENDING_CASH_CONFIRMATION, CASH_CONFIRMED
    }

    public enum PaymentMethod {
        CREDIT_CARD, UPI, CASH_ON_DELIVERY
    }

    private ObjectId id;
    private ObjectId bookingId;
    private ObjectId rentalId;
    private ObjectId userId;
    private ObjectId paymentMethodProfileId;
    private double amount;
    private double taxAmount;
    private double discountAmount;
    private double totalAmount;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String cardNumber; // last 4 digits only
    private String cardHolderName;
    private String upiId;
    private String paymentLabel;
    private String transactionRef;
    private String currency;
    private boolean cashVerified;
    private String cashVerifiedBy;
    private Date cashVerifiedAt;
    private String receiptPath;
    private Date paymentDate;
    private Date createdAt;

    public Payment() {
        this.status = PaymentStatus.PENDING;
        this.currency = "INR";
        this.createdAt = new Date();
    }

    // Getters and Setters
    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public ObjectId getBookingId() { return bookingId; }
    public void setBookingId(ObjectId bookingId) { this.bookingId = bookingId; }

    public ObjectId getRentalId() { return rentalId; }
    public void setRentalId(ObjectId rentalId) { this.rentalId = rentalId; }

    public ObjectId getUserId() { return userId; }
    public void setUserId(ObjectId userId) { this.userId = userId; }

    public ObjectId getPaymentMethodProfileId() { return paymentMethodProfileId; }
    public void setPaymentMethodProfileId(ObjectId paymentMethodProfileId) { this.paymentMethodProfileId = paymentMethodProfileId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getPaymentLabel() { return paymentLabel; }
    public void setPaymentLabel(String paymentLabel) { this.paymentLabel = paymentLabel; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public boolean isCashVerified() { return cashVerified; }
    public void setCashVerified(boolean cashVerified) { this.cashVerified = cashVerified; }

    public String getCashVerifiedBy() { return cashVerifiedBy; }
    public void setCashVerifiedBy(String cashVerifiedBy) { this.cashVerifiedBy = cashVerifiedBy; }

    public Date getCashVerifiedAt() { return cashVerifiedAt; }
    public void setCashVerifiedAt(Date cashVerifiedAt) { this.cashVerifiedAt = cashVerifiedAt; }

    public String getReceiptPath() { return receiptPath; }
    public void setReceiptPath(String receiptPath) { this.receiptPath = receiptPath; }

    public Date getPaymentDate() { return paymentDate; }
    public void setPaymentDate(Date paymentDate) { this.paymentDate = paymentDate; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
