
package com.rento.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import com.rento.models.Payment;
import com.rento.utils.MongoCollections;
import com.rento.utils.MongoDBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Payment collection.
 */
public class PaymentDAO {

    public static final String COLLECTION_NAME = MongoCollections.PAYMENTS;

    private MongoCollection<Document> getCollection() {
        return MongoDBConnection.getInstance().getCollection(COLLECTION_NAME);
    }

    public boolean insertPayment(Payment payment) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            Document doc = paymentToDocument(payment);
            InsertOneResult result = col.insertOne(doc);
            if (result.getInsertedId() != null) {
                payment.setId(result.getInsertedId().asObjectId().getValue());
                return true;
            }
        } catch (Exception e) {
            System.err.println("[PaymentDAO] Insert failed: " + e.getMessage());
        }
        return false;
    }

    public Payment findById(ObjectId id) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return null;

            Document doc = col.find(Filters.eq("_id", id)).first();
            return doc != null ? documentToPayment(doc) : null;
        } catch (Exception e) {
            System.err.println("[PaymentDAO] Find by ID failed: " + e.getMessage());
            return null;
        }
    }

    public Payment findByBooking(ObjectId bookingId) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return null;

            Document doc = col.find(Filters.eq("bookingId", bookingId)).first();
            return doc != null ? documentToPayment(doc) : null;
        } catch (Exception e) {
            System.err.println("[PaymentDAO] Find by booking failed: " + e.getMessage());
            return null;
        }
    }

    public Payment findByRental(ObjectId rentalId) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return null;

            Document doc = col.find(Filters.eq("rentalId", rentalId)).first();
            return doc != null ? documentToPayment(doc) : null;
        } catch (Exception e) {
            System.err.println("[PaymentDAO] Find by rental failed: " + e.getMessage());
            return null;
        }
    }

    public List<Payment> findByUser(ObjectId userId) {
        List<Payment> payments = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return payments;

            for (Document doc : col.find(Filters.eq("userId", userId))) {
                payments.add(documentToPayment(doc));
            }
        } catch (Exception e) {
            System.err.println("[PaymentDAO] Find by user failed: " + e.getMessage());
        }
        return payments;
    }

    public List<Payment> findAll() {
        List<Payment> payments = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return payments;

            for (Document doc : col.find()) {
                payments.add(documentToPayment(doc));
            }
        } catch (Exception e) {
            System.err.println("[PaymentDAO] Find all failed: " + e.getMessage());
        }
        return payments;
    }

    public boolean updatePayment(Payment payment) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            Document doc = paymentToDocument(payment);
            doc.remove("_id");
            return col.replaceOne(Filters.eq("_id", payment.getId()), doc).getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("[PaymentDAO] Update failed: " + e.getMessage());
            return false;
        }
    }

    private Document paymentToDocument(Payment p) {
        Document doc = new Document();
        if (p.getId() != null) doc.append("_id", p.getId());
        doc.append("bookingId", p.getBookingId());
        doc.append("rentalId", p.getRentalId());
        doc.append("userId", p.getUserId());
        doc.append("paymentMethodProfileId", p.getPaymentMethodProfileId());
        doc.append("amount", p.getAmount());
        doc.append("taxAmount", p.getTaxAmount());
        doc.append("discountAmount", p.getDiscountAmount());
        doc.append("totalAmount", p.getTotalAmount());
        doc.append("paymentMethod", p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null);
        doc.append("status", p.getStatus() != null ? p.getStatus().name() : null);
        doc.append("cardNumber", p.getCardNumber());
        doc.append("cardHolderName", p.getCardHolderName());
        doc.append("upiId", p.getUpiId());
        doc.append("paymentLabel", p.getPaymentLabel());
        doc.append("transactionRef", p.getTransactionRef());
        doc.append("currency", p.getCurrency());
        doc.append("cashVerified", p.isCashVerified());
        doc.append("cashVerifiedBy", p.getCashVerifiedBy());
        doc.append("cashVerifiedAt", p.getCashVerifiedAt());
        doc.append("fundsDistributed", p.isFundsDistributed());
        doc.append("distributedBy", p.getDistributedBy());
        doc.append("distributedAt", p.getDistributedAt());
        doc.append("receiptPath", p.getReceiptPath());
        doc.append("paymentDate", p.getPaymentDate());
        doc.append("createdAt", p.getCreatedAt());
        return doc;
    }

    private Payment documentToPayment(Document doc) {
        Payment p = new Payment();
        p.setId(doc.getObjectId("_id"));
        p.setBookingId(doc.getObjectId("bookingId"));
        p.setRentalId(doc.getObjectId("rentalId"));
        p.setUserId(doc.getObjectId("userId"));
        p.setPaymentMethodProfileId(doc.getObjectId("paymentMethodProfileId"));
        p.setAmount(readDouble(doc, "amount"));
        p.setTaxAmount(readDouble(doc, "taxAmount"));
        p.setDiscountAmount(readDouble(doc, "discountAmount"));
        p.setTotalAmount(readDouble(doc, "totalAmount"));
        try { p.setPaymentMethod(Payment.PaymentMethod.valueOf(doc.getString("paymentMethod"))); } catch (Exception ignored) {}
        try { p.setStatus(Payment.PaymentStatus.valueOf(doc.getString("status"))); } catch (Exception ignored) {}
        p.setCardNumber(doc.getString("cardNumber"));
        p.setCardHolderName(doc.getString("cardHolderName"));
        p.setUpiId(doc.getString("upiId"));
        p.setPaymentLabel(doc.getString("paymentLabel"));
        p.setTransactionRef(doc.getString("transactionRef"));
        p.setCurrency(doc.getString("currency"));
        p.setCashVerified(doc.getBoolean("cashVerified", false));
        p.setCashVerifiedBy(doc.getString("cashVerifiedBy"));
        p.setCashVerifiedAt(doc.getDate("cashVerifiedAt"));
        p.setFundsDistributed(doc.getBoolean("fundsDistributed", false));
        p.setDistributedBy(doc.getString("distributedBy"));
        p.setDistributedAt(doc.getDate("distributedAt"));
        p.setReceiptPath(doc.getString("receiptPath"));
        p.setPaymentDate(doc.getDate("paymentDate"));
        p.setCreatedAt(doc.getDate("createdAt"));
        return p;
    }

    private double readDouble(Document doc, String field) {
        Object value = doc.get(field);
        return value instanceof Number ? ((Number) value).doubleValue() : 0;
    }
}
