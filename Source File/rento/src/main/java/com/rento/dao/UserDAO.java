package com.rento.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.rento.models.User;
import com.rento.utils.MongoCollections;
import com.rento.utils.MongoDBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Data Access Object for User collection.
 */
public class UserDAO {

    public static final String COLLECTION_NAME = MongoCollections.USERS;

    private MongoCollection<Document> getCollection() {
        return MongoDBConnection.getInstance().getCollection(COLLECTION_NAME);
    }

    public boolean insertUser(User user) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            Document doc = userToDocument(user);
            InsertOneResult result = col.insertOne(doc);
            if (result.getInsertedId() != null) {
                user.setId(result.getInsertedId().asObjectId().getValue());
                return true;
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] Insert failed: " + e.getMessage());
        }
        return false;
    }

    public User findByEmail(String email) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return null;

            Document doc = col.find(Filters.eq("email", email)).first();
            return doc != null ? documentToUser(doc) : null;
        } catch (Exception e) {
            System.err.println("[UserDAO] Find by email failed: " + e.getMessage());
            return null;
        }
    }

    public User findById(ObjectId id) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return null;

            Document doc = col.find(Filters.eq("_id", id)).first();
            return doc != null ? documentToUser(doc) : null;
        } catch (Exception e) {
            System.err.println("[UserDAO] Find by ID failed: " + e.getMessage());
            return null;
        }
    }

    public List<User> findByRole(User.Role role) {
        List<User> users = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return users;

            for (Document doc : col.find(Filters.eq("role", role.name()))) {
                users.add(documentToUser(doc));
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] Find by role failed: " + e.getMessage());
        }
        return users;
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return users;

            for (Document doc : col.find()) {
                users.add(documentToUser(doc));
            }
        } catch (Exception e) {
            System.err.println("[UserDAO] Find all failed: " + e.getMessage());
        }
        return users;
    }

    public boolean updateUser(User user) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            user.setUpdatedAt(new Date());
            Document doc = userToDocument(user);
            doc.remove("_id");
            UpdateResult result = col.replaceOne(Filters.eq("_id", user.getId()), doc);
            return result.getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("[UserDAO] Update failed: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(ObjectId id) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;

            DeleteResult result = col.deleteOne(Filters.eq("_id", id));
            return result.getDeletedCount() > 0;
        } catch (Exception e) {
            System.err.println("[UserDAO] Delete failed: " + e.getMessage());
            return false;
        }
    }

    public boolean emailExists(String email) {
        return findByEmail(email) != null;
    }

    public boolean updateWalletBalance(ObjectId userId, double newBalance) {
        User user = findById(userId);
        if (user == null) {
            return false;
        }
        user.setWalletBalance(newBalance);
        return updateUser(user);
    }

    public boolean adjustWalletBalance(ObjectId userId, double delta) {
        User user = findById(userId);
        if (user == null) {
            return false;
        }
        user.setWalletBalance(user.getWalletBalance() + delta);
        return updateUser(user);
    }

    // --- Mapping methods ---

    private Document userToDocument(User user) {
        Document doc = new Document();
        if (user.getId() != null) doc.append("_id", user.getId());
        doc.append("fullName", user.getFullName());
        doc.append("email", user.getEmail());
        doc.append("phone", user.getPhone());
        doc.append("address", user.getAddress());
        doc.append("password", user.getPassword());
        doc.append("driverLicenseNumber", user.getDriverLicenseNumber());
        doc.append("role", user.getRole().name());
        doc.append("verified", user.isVerified());
        doc.append("locked", user.isLocked());
        doc.append("lockReason", user.getLockReason());
        doc.append("lockedAt", user.getLockedAt());
        doc.append("lastLoginAt", user.getLastLoginAt());
        doc.append("age", user.getAge());
        doc.append("walletBalance", user.getWalletBalance());
        doc.append("createdAt", user.getCreatedAt());
        doc.append("updatedAt", user.getUpdatedAt());
        return doc;
    }

    private User documentToUser(Document doc) {
        User user = new User();
        user.setId(doc.getObjectId("_id"));
        user.setFullName(doc.getString("fullName"));
        user.setEmail(doc.getString("email"));
        user.setPhone(doc.getString("phone"));
        user.setAddress(doc.getString("address"));
        user.setPassword(doc.getString("password"));
        user.setDriverLicenseNumber(doc.getString("driverLicenseNumber"));
        String role = doc.getString("role");
        if (role != null) {
            try { user.setRole(User.Role.valueOf(role)); } catch (Exception ignored) {}
        }
        user.setVerified(doc.getBoolean("verified", false));
        user.setLocked(doc.getBoolean("locked", false));
        user.setLockReason(doc.getString("lockReason"));
        user.setLockedAt(doc.getDate("lockedAt"));
        user.setLastLoginAt(doc.getDate("lastLoginAt"));
        user.setAge(doc.getInteger("age", 0));
        user.setWalletBalance(readDouble(doc, "walletBalance"));
        user.setCreatedAt(doc.getDate("createdAt"));
        user.setUpdatedAt(doc.getDate("updatedAt"));
        return user;
    }

    private double readDouble(Document doc, String field) {
        Object value = doc.get(field);
        return value instanceof Number ? ((Number) value).doubleValue() : 0;
    }

    public boolean updateLockState(ObjectId userId, boolean locked, String reason) {
        User user = findById(userId);
        if (user == null) {
            return false;
        }
        user.setLocked(locked);
        user.setLockReason(locked ? reason : null);
        user.setLockedAt(locked ? new Date() : null);
        return updateUser(user);
    }
}
