package com.rento.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import com.rento.models.Rental;
import com.rento.utils.MongoDBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data Access Object for Rental collection.
 */
public class RentalDAO {

    private static final String COLLECTION_NAME = "rentals";

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = MongoDBConnection.getInstance().getDatabase();
        if (db == null) return null;
        return db.getCollection(COLLECTION_NAME);
    }

    public boolean insertRental(Rental rental) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            Document doc = rentalToDocument(rental);
            InsertOneResult result = col.insertOne(doc);
            if (result.getInsertedId() != null) {
                rental.setId(result.getInsertedId().asObjectId().getValue());
                return true;
            }
        } catch (Exception e) {
            System.err.println("[RentalDAO] Insert failed: " + e.getMessage());
        }
        return false;
    }

    public Rental findById(ObjectId id) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return null;

            Document doc = col.find(Filters.eq("_id", id)).first();
            return doc != null ? documentToRental(doc) : null;
        } catch (Exception e) {
            System.err.println("[RentalDAO] Find failed: " + e.getMessage());
            return null;
        }
    }

    public List<Rental> findBySupplier(ObjectId supplierId) {
        List<Rental> rentals = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return rentals;

            for (Document doc : col.find(Filters.eq("supplierId", supplierId))) {
                rentals.add(documentToRental(doc));
            }
        } catch (Exception e) {
            System.err.println("[RentalDAO] Find by supplier failed: " + e.getMessage());
        }
        return rentals;
    }

    public List<Rental> findByRenter(ObjectId renterId) {
        List<Rental> rentals = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return rentals;

            for (Document doc : col.find(Filters.eq("renterId", renterId))) {
                rentals.add(documentToRental(doc));
            }
        } catch (Exception e) {
            System.err.println("[RentalDAO] Find by renter failed: " + e.getMessage());
        }
        return rentals;
    }

    public List<Rental> findPendingApproval() {
        return findByStatus(Rental.RentalStatus.REQUESTED);
    }

    public List<Rental> findByStatus(Rental.RentalStatus status) {
        List<Rental> rentals = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return rentals;

            for (Document doc : col.find(Filters.eq("status", status.name()))) {
                rentals.add(documentToRental(doc));
            }
        } catch (Exception e) {
            System.err.println("[RentalDAO] Find by status failed: " + e.getMessage());
        }
        return rentals;
    }

    public List<Rental> findBySupplierAndStatus(ObjectId supplierId, Rental.RentalStatus status) {
        List<Rental> rentals = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return rentals;

            for (Document doc : col.find(Filters.and(
                Filters.eq("supplierId", supplierId),
                Filters.eq("status", status.name())
            ))) {
                rentals.add(documentToRental(doc));
            }
        } catch (Exception e) {
            System.err.println("[RentalDAO] Find by supplier and status failed: " + e.getMessage());
        }
        return rentals;
    }

    public List<Rental> findByRenterAndStatus(ObjectId renterId, Rental.RentalStatus status) {
        List<Rental> rentals = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return rentals;

            for (Document doc : col.find(Filters.and(
                Filters.eq("renterId", renterId),
                Filters.eq("status", status.name())
            ))) {
                rentals.add(documentToRental(doc));
            }
        } catch (Exception e) {
            System.err.println("[RentalDAO] Find by renter and status failed: " + e.getMessage());
        }
        return rentals;
    }

    public List<Rental> findAll() {
        List<Rental> rentals = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return rentals;

            for (Document doc : col.find()) {
                rentals.add(documentToRental(doc));
            }
        } catch (Exception e) {
            System.err.println("[RentalDAO] Find all failed: " + e.getMessage());
        }
        return rentals;
    }

    public boolean updateRental(Rental rental) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            rental.setUpdatedAt(new Date());
            Document doc = rentalToDocument(rental);
            doc.remove("_id");
            return col.replaceOne(Filters.eq("_id", rental.getId()), doc).getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("[RentalDAO] Update failed: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(ObjectId id, Rental.RentalStatus status) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            Document update = new Document("$set",
                new Document("status", status.name()).append("updatedAt", new Date()));
            return col.updateOne(Filters.eq("_id", id), update).getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("[RentalDAO] Update status failed: " + e.getMessage());
            return false;
        }
    }

    private Document rentalToDocument(Rental r) {
        Document doc = new Document();
        if (r.getId() != null) doc.append("_id", r.getId());
        doc.append("vehicleId", r.getVehicleId());
        doc.append("supplierId", r.getSupplierId());
        doc.append("renterId", r.getRenterId());
        doc.append("vehicleName", r.getVehicleName());
        doc.append("supplierName", r.getSupplierName());
        doc.append("renterName", r.getRenterName());
        doc.append("pricePerDay", r.getPricePerDay());
        doc.append("startDate", r.getStartDate());
        doc.append("endDate", r.getEndDate());
        doc.append("status", r.getStatus() != null ? r.getStatus().name() : null);
        doc.append("approvalOtp", r.getApprovalOtp());
        doc.append("otpVerified", r.isOtpVerified());
        doc.append("notes", r.getNotes());
        doc.append("paymentId", r.getPaymentId());
        doc.append("paymentMethod", r.getPaymentMethod());
        doc.append("paymentStatus", r.getPaymentStatus());
        doc.append("cashPaymentPending", r.isCashPaymentPending());
        doc.append("paidVerified", r.isPaidVerified());
        doc.append("paidVerifiedBy", r.getPaidVerifiedBy());
        doc.append("paidVerifiedAt", r.getPaidVerifiedAt());
        doc.append("receiptPath", r.getReceiptPath());
        doc.append("totalAmount", r.getTotalAmount());
        doc.append("penaltyAmount", r.getPenaltyAmount());
        doc.append("requestedAt", r.getRequestedAt());
        doc.append("approvedAt", r.getApprovedAt());
        doc.append("completedAt", r.getCompletedAt());
        doc.append("createdAt", r.getCreatedAt());
        doc.append("updatedAt", r.getUpdatedAt());
        return doc;
    }

    private Rental documentToRental(Document doc) {
        Rental r = new Rental();
        r.setId(doc.getObjectId("_id"));
        r.setVehicleId(doc.getObjectId("vehicleId"));
        r.setSupplierId(doc.getObjectId("supplierId"));
        r.setRenterId(doc.getObjectId("renterId"));
        r.setVehicleName(doc.getString("vehicleName"));
        r.setSupplierName(doc.getString("supplierName"));
        r.setRenterName(doc.getString("renterName"));
        r.setPricePerDay(doc.getDouble("pricePerDay") != null ? doc.getDouble("pricePerDay") : 0);
        r.setStartDate(doc.getDate("startDate"));
        r.setEndDate(doc.getDate("endDate"));
        try { r.setStatus(Rental.RentalStatus.valueOf(doc.getString("status"))); } catch (Exception ignored) {}
        r.setApprovalOtp(doc.getString("approvalOtp"));
        r.setOtpVerified(doc.getBoolean("otpVerified", false));
        r.setNotes(doc.getString("notes"));
        r.setPaymentId(doc.getObjectId("paymentId"));
        r.setPaymentMethod(doc.getString("paymentMethod"));
        r.setPaymentStatus(doc.getString("paymentStatus"));
        r.setCashPaymentPending(doc.getBoolean("cashPaymentPending", false));
        r.setPaidVerified(doc.getBoolean("paidVerified", false));
        r.setPaidVerifiedBy(doc.getString("paidVerifiedBy"));
        r.setPaidVerifiedAt(doc.getDate("paidVerifiedAt"));
        r.setReceiptPath(doc.getString("receiptPath"));
        r.setTotalAmount(doc.getDouble("totalAmount") != null ? doc.getDouble("totalAmount") : 0);
        r.setPenaltyAmount(doc.getDouble("penaltyAmount") != null ? doc.getDouble("penaltyAmount") : 0);
        r.setRequestedAt(doc.getDate("requestedAt"));
        r.setApprovedAt(doc.getDate("approvedAt"));
        r.setCompletedAt(doc.getDate("completedAt"));
        r.setCreatedAt(doc.getDate("createdAt"));
        r.setUpdatedAt(doc.getDate("updatedAt"));
        return r;
    }
}
