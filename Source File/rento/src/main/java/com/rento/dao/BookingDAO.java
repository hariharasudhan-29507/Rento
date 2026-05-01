package com.rento.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import com.rento.models.Booking;
import com.rento.utils.MongoCollections;
import com.rento.utils.MongoDBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data Access Object for Booking collection.
 */
public class BookingDAO {

    public static final String COLLECTION_NAME = MongoCollections.BOOKINGS;

    private MongoCollection<Document> getCollection() {
        return MongoDBConnection.getInstance().getCollection(COLLECTION_NAME);
    }

    public boolean insertBooking(Booking booking) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            Document doc = bookingToDocument(booking);
            InsertOneResult result = col.insertOne(doc);
            if (result.getInsertedId() != null) {
                booking.setId(result.getInsertedId().asObjectId().getValue());
                return true;
            }
        } catch (Exception e) {
            System.err.println("[BookingDAO] Insert failed: " + e.getMessage());
        }
        return false;
    }

    public Booking findById(ObjectId id) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return null;

            Document doc = col.find(Filters.eq("_id", id)).first();
            return doc != null ? documentToBooking(doc) : null;
        } catch (Exception e) {
            System.err.println("[BookingDAO] Find by ID failed: " + e.getMessage());
            return null;
        }
    }

    public List<Booking> findByUser(ObjectId userId) {
        List<Booking> bookings = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return bookings;

            for (Document doc : col.find(Filters.eq("userId", userId))) {
                bookings.add(documentToBooking(doc));
            }
        } catch (Exception e) {
            System.err.println("[BookingDAO] Find by user failed: " + e.getMessage());
        }
        return bookings;
    }

    public List<Booking> findByDriver(ObjectId driverId) {
        List<Booking> bookings = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return bookings;

            for (Document doc : col.find(Filters.eq("driverId", driverId))) {
                bookings.add(documentToBooking(doc));
            }
        } catch (Exception e) {
            System.err.println("[BookingDAO] Find by driver failed: " + e.getMessage());
        }
        return bookings;
    }

    public List<Booking> findPending() {
        List<Booking> bookings = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return bookings;

            for (Document doc : col.find(Filters.eq("status", Booking.BookingStatus.PENDING.name()))) {
                bookings.add(documentToBooking(doc));
            }
        } catch (Exception e) {
            System.err.println("[BookingDAO] Find pending failed: " + e.getMessage());
        }
        return bookings;
    }

    public List<Booking> findAll() {
        List<Booking> bookings = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return bookings;

            for (Document doc : col.find()) {
                bookings.add(documentToBooking(doc));
            }
        } catch (Exception e) {
            System.err.println("[BookingDAO] Find all failed: " + e.getMessage());
        }
        return bookings;
    }

    public boolean updateBooking(Booking booking) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            booking.setUpdatedAt(new Date());
            Document doc = bookingToDocument(booking);
            doc.remove("_id");
            return col.replaceOne(Filters.eq("_id", booking.getId()), doc).getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("[BookingDAO] Update failed: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(ObjectId id, Booking.BookingStatus status) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            Document update = new Document("$set",
                new Document("status", status.name()).append("updatedAt", new Date()));
            return col.updateOne(Filters.eq("_id", id), update).getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("[BookingDAO] Update status failed: " + e.getMessage());
            return false;
        }
    }

    private Document bookingToDocument(Booking b) {
        Document doc = new Document();
        if (b.getId() != null) doc.append("_id", b.getId());
        doc.append("userId", b.getUserId());
        doc.append("vehicleId", b.getVehicleId());
        doc.append("driverId", b.getDriverId());
        doc.append("preferredDriverId", b.getPreferredDriverId());
        doc.append("pickupLocation", b.getPickupLocation());
        doc.append("dropoffLocation", b.getDropoffLocation());
        doc.append("pickupDateTime", b.getPickupDateTime());
        doc.append("returnDateTime", b.getReturnDateTime());
        doc.append("totalCost", b.getTotalCost());
        doc.append("depositAmount", b.getDepositAmount());
        doc.append("taxAmount", b.getTaxAmount());
        doc.append("discountApplied", b.getDiscountApplied());
        doc.append("paymentId", b.getPaymentId());
        doc.append("paymentMethod", b.getPaymentMethod());
        doc.append("paymentStatus", b.getPaymentStatus());
        doc.append("cashPaymentPending", b.isCashPaymentPending());
        doc.append("paidVerified", b.isPaidVerified());
        doc.append("paidVerifiedBy", b.getPaidVerifiedBy());
        doc.append("paidVerifiedAt", b.getPaidVerifiedAt());
        doc.append("receiptPath", b.getReceiptPath());
        doc.append("status", b.getStatus() != null ? b.getStatus().name() : null);
        doc.append("otp", b.getOtp());
        doc.append("otpVerified", b.isOtpVerified());
        doc.append("vehicleName", b.getVehicleName());
        doc.append("userName", b.getUserName());
        doc.append("driverName", b.getDriverName());
        doc.append("preferredDriverName", b.getPreferredDriverName());
        doc.append("createdAt", b.getCreatedAt());
        doc.append("updatedAt", b.getUpdatedAt());
        return doc;
    }

    private Booking documentToBooking(Document doc) {
        Booking b = new Booking();
        b.setId(doc.getObjectId("_id"));
        b.setUserId(doc.getObjectId("userId"));
        b.setVehicleId(doc.getObjectId("vehicleId"));
        b.setDriverId(doc.getObjectId("driverId"));
        b.setPreferredDriverId(doc.getObjectId("preferredDriverId"));
        b.setPickupLocation(doc.getString("pickupLocation"));
        b.setDropoffLocation(doc.getString("dropoffLocation"));
        b.setPickupDateTime(doc.getDate("pickupDateTime"));
        b.setReturnDateTime(doc.getDate("returnDateTime"));
        b.setTotalCost(readDouble(doc, "totalCost"));
        b.setDepositAmount(readDouble(doc, "depositAmount"));
        b.setTaxAmount(readDouble(doc, "taxAmount"));
        b.setDiscountApplied(readDouble(doc, "discountApplied"));
        b.setPaymentId(doc.getObjectId("paymentId"));
        b.setPaymentMethod(doc.getString("paymentMethod"));
        b.setPaymentStatus(doc.getString("paymentStatus"));
        b.setCashPaymentPending(doc.getBoolean("cashPaymentPending", false));
        b.setPaidVerified(doc.getBoolean("paidVerified", false));
        b.setPaidVerifiedBy(doc.getString("paidVerifiedBy"));
        b.setPaidVerifiedAt(doc.getDate("paidVerifiedAt"));
        b.setReceiptPath(doc.getString("receiptPath"));
        try { b.setStatus(Booking.BookingStatus.valueOf(doc.getString("status"))); } catch (Exception ignored) {}
        b.setOtp(doc.getString("otp"));
        b.setOtpVerified(doc.getBoolean("otpVerified", false));
        b.setVehicleName(doc.getString("vehicleName"));
        b.setUserName(doc.getString("userName"));
        b.setDriverName(doc.getString("driverName"));
        b.setPreferredDriverName(doc.getString("preferredDriverName"));
        b.setCreatedAt(doc.getDate("createdAt"));
        b.setUpdatedAt(doc.getDate("updatedAt"));
        return b;
    }

    private double readDouble(Document doc, String field) {
        Object value = doc.get(field);
        return value instanceof Number ? ((Number) value).doubleValue() : 0;
    }
}
