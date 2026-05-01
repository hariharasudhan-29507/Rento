package com.rento.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import com.rento.models.Vehicle;
import com.rento.utils.MongoCollections;
import com.rento.utils.MongoDBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data Access Object for Vehicle collection.
 */
public class VehicleDAO {

    public static final String COLLECTION_NAME = MongoCollections.VEHICLES;

    private MongoCollection<Document> getCollection() {
        return MongoDBConnection.getInstance().getCollection(COLLECTION_NAME);
    }

    public boolean insertVehicle(Vehicle vehicle) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            Document doc = vehicleToDocument(vehicle);
            InsertOneResult result = col.insertOne(doc);
            if (result.getInsertedId() != null) {
                vehicle.setId(result.getInsertedId().asObjectId().getValue());
                return true;
            }
        } catch (Exception e) {
            System.err.println("[VehicleDAO] Insert failed: " + e.getMessage());
        }
        return false;
    }

    public Vehicle findById(ObjectId id) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return null;

            Document doc = col.find(Filters.eq("_id", id)).first();
            return doc != null ? documentToVehicle(doc) : null;
        } catch (Exception e) {
            System.err.println("[VehicleDAO] Find by ID failed: " + e.getMessage());
            return null;
        }
    }

    public List<Vehicle> findAvailable() {
        List<Vehicle> vehicles = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return vehicles;

            for (Document doc : col.find(Filters.eq("status", Vehicle.Status.AVAILABLE.name()))) {
                vehicles.add(documentToVehicle(doc));
            }
        } catch (Exception e) {
            System.err.println("[VehicleDAO] Find available failed: " + e.getMessage());
        }
        return vehicles;
    }

    public List<Vehicle> findByOwner(ObjectId ownerId) {
        List<Vehicle> vehicles = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return vehicles;

            for (Document doc : col.find(Filters.eq("ownerId", ownerId))) {
                vehicles.add(documentToVehicle(doc));
            }
        } catch (Exception e) {
            System.err.println("[VehicleDAO] Find by owner failed: " + e.getMessage());
        }
        return vehicles;
    }

    public List<Vehicle> findAll() {
        List<Vehicle> vehicles = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return vehicles;

            for (Document doc : col.find()) {
                vehicles.add(documentToVehicle(doc));
            }
        } catch (Exception e) {
            System.err.println("[VehicleDAO] Find all failed: " + e.getMessage());
        }
        return vehicles;
    }

    public List<Vehicle> findByCategory(Vehicle.Category category) {
        List<Vehicle> vehicles = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return vehicles;

            for (Document doc : col.find(Filters.eq("category", category.name()))) {
                vehicles.add(documentToVehicle(doc));
            }
        } catch (Exception e) {
            System.err.println("[VehicleDAO] Find by category failed: " + e.getMessage());
        }
        return vehicles;
    }

    public boolean updateVehicle(Vehicle vehicle) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            vehicle.setUpdatedAt(new Date());
            Document doc = vehicleToDocument(vehicle);
            doc.remove("_id");
            return col.replaceOne(Filters.eq("_id", vehicle.getId()), doc).getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("[VehicleDAO] Update failed: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStatus(ObjectId id, Vehicle.Status status) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            Document update = new Document("$set", new Document("status", status.name()).append("updatedAt", new Date()));
            return col.updateOne(Filters.eq("_id", id), update).getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("[VehicleDAO] Update status failed: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteVehicle(ObjectId id) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            return col.deleteOne(Filters.eq("_id", id)).getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("[VehicleDAO] Delete failed: " + e.getMessage());
            return false;
        }
    }

    // --- Mapping ---

    private Document vehicleToDocument(Vehicle v) {
        Document doc = new Document();
        if (v.getId() != null) doc.append("_id", v.getId());
        doc.append("vin", v.getVin());
        doc.append("make", v.getMake());
        doc.append("model", v.getModel());
        doc.append("year", v.getYear());
        doc.append("licensePlate", v.getLicensePlate());
        doc.append("category", v.getCategory() != null ? v.getCategory().name() : null);
        doc.append("fuelType", v.getFuelType() != null ? v.getFuelType().name() : null);
        doc.append("status", v.getStatus() != null ? v.getStatus().name() : null);
        doc.append("currentMileage", v.getCurrentMileage());
        doc.append("nextServiceDue", v.getNextServiceDue());
        doc.append("dailyRate", v.getDailyRate());
        doc.append("seats", v.getSeats());
        doc.append("color", v.getColor());
        doc.append("imageUrl", v.getImageUrl());
        doc.append("description", v.getDescription());
        doc.append("ownerId", v.getOwnerId());
        doc.append("branchLocation", v.getBranchLocation());
        doc.append("approvalStatus", v.getApprovalStatus() != null ? v.getApprovalStatus().name() : null);
        doc.append("adminReviewNote", v.getAdminReviewNote());
        doc.append("createdAt", v.getCreatedAt());
        doc.append("updatedAt", v.getUpdatedAt());
        return doc;
    }

    private Vehicle documentToVehicle(Document doc) {
        Vehicle v = new Vehicle();
        v.setId(doc.getObjectId("_id"));
        v.setVin(doc.getString("vin"));
        v.setMake(doc.getString("make"));
        v.setModel(doc.getString("model"));
        v.setYear(doc.getInteger("year", 0));
        v.setLicensePlate(doc.getString("licensePlate"));
        try { v.setCategory(Vehicle.Category.valueOf(doc.getString("category"))); } catch (Exception ignored) {}
        try { v.setFuelType(Vehicle.FuelType.valueOf(doc.getString("fuelType"))); } catch (Exception ignored) {}
        try { v.setStatus(Vehicle.Status.valueOf(doc.getString("status"))); } catch (Exception ignored) {}
        v.setCurrentMileage(readDouble(doc, "currentMileage"));
        v.setNextServiceDue(readDouble(doc, "nextServiceDue"));
        v.setDailyRate(readDouble(doc, "dailyRate"));
        v.setSeats(doc.getInteger("seats", 4));
        v.setColor(doc.getString("color"));
        v.setImageUrl(doc.getString("imageUrl"));
        v.setDescription(doc.getString("description"));
        v.setOwnerId(doc.getObjectId("ownerId"));
        v.setBranchLocation(doc.getString("branchLocation"));
        try { v.setApprovalStatus(Vehicle.ApprovalStatus.valueOf(doc.getString("approvalStatus"))); } catch (Exception ignored) {}
        v.setAdminReviewNote(doc.getString("adminReviewNote"));
        v.setCreatedAt(doc.getDate("createdAt"));
        v.setUpdatedAt(doc.getDate("updatedAt"));
        return v;
    }

    private double readDouble(Document doc, String field) {
        Object value = doc.get(field);
        return value instanceof Number ? ((Number) value).doubleValue() : 0;
    }
}
