
package com.rento.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import com.rento.models.Payment;
import com.rento.utils.MongoDBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Payment collection.
 */
public class PaymentDAO {

    private static final String COLLECTION_NAME = "payments";

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = MongoDBConnection.getInstance().getDatabase();
        if (db == null) return null;
        return db.getCollection(COLLECTION_NAME);
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
        doc.append("userId", p.getUserId());
        doc.append("amount", p.getAmount());
        doc.append("taxAmount", p.getTaxAmount());
        doc.append("discountAmount", p.getDiscountAmount());
        doc.append("totalAmount", p.getTotalAmount());
        doc.append("paymentMethod", p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null);
        doc.append("status", p.getStatus() != null ? p.getStatus().name() : null);
        doc.append("cardNumber", p.getCardNumber());
        doc.append("cardHolderName", p.getCardHolderName());
        doc.append("transactionRef", p.getTransactionRef());
        doc.append("currency", p.getCurrency());
        doc.append("paymentDate", p.getPaymentDate());
        doc.append("createdAt", p.getCreatedAt());
        return doc;
    }

    private Payment documentToPayment(Document doc) {
        Payment p = new Payment();
        p.setId(doc.getObjectId("_id"));
        p.setBookingId(doc.getObjectId("bookingId"));
        p.setUserId(doc.getObjectId("userId"));
        p.setAmount(doc.getDouble("amount") != null ? doc.getDouble("amount") : 0);
        p.setTaxAmount(doc.getDouble("taxAmount") != null ? doc.getDouble("taxAmount") : 0);
        p.setDiscountAmount(doc.getDouble("discountAmount") != null ? doc.getDouble("discountAmount") : 0);
        p.setTotalAmount(doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : 0);
        try { p.setPaymentMethod(Payment.PaymentMethod.valueOf(doc.getString("paymentMethod"))); } catch (Exception ignored) {}
        try { p.setStatus(Payment.PaymentStatus.valueOf(doc.getString("status"))); } catch (Exception ignored) {}
        p.setCardNumber(doc.getString("cardNumber"));
        p.setCardHolderName(doc.getString("cardHolderName"));
        p.setTransactionRef(doc.getString("transactionRef"));
        p.setCurrency(doc.getString("currency"));
        p.setPaymentDate(doc.getDate("paymentDate"));
        p.setCreatedAt(doc.getDate("createdAt"));
        return p;
    }
}
