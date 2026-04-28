package com.rento.dao;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import com.rento.models.PaymentMethodProfile;
import com.rento.utils.MongoDBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * DAO for saved payment methods.
 */
public class PaymentMethodDAO {

    private static final String COLLECTION_NAME = "payment_methods";

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = MongoDBConnection.getInstance().getDatabase();
        if (db == null) return null;
        return db.getCollection(COLLECTION_NAME);
    }

    public boolean insert(PaymentMethodProfile profile) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;
            InsertOneResult result = col.insertOne(toDocument(profile));
            if (result.getInsertedId() != null) {
                profile.setId(result.getInsertedId().asObjectId().getValue());
                return true;
            }
        } catch (Exception e) {
            System.err.println("[PaymentMethodDAO] Insert failed: " + e.getMessage());
        }
        return false;
    }

    public List<PaymentMethodProfile> findByUser(ObjectId userId) {
        List<PaymentMethodProfile> profiles = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return profiles;
            for (Document doc : col.find(Filters.eq("userId", userId))) {
                profiles.add(fromDocument(doc));
            }
        } catch (Exception e) {
            System.err.println("[PaymentMethodDAO] Find by user failed: " + e.getMessage());
        }
        return profiles;
    }

    public List<PaymentMethodProfile> findAll() {
        List<PaymentMethodProfile> profiles = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return profiles;
            for (Document doc : col.find()) {
                profiles.add(fromDocument(doc));
            }
        } catch (Exception e) {
            System.err.println("[PaymentMethodDAO] Find all failed: " + e.getMessage());
        }
        return profiles;
    }

    public boolean update(PaymentMethodProfile profile) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null) return false;
            profile.setUpdatedAt(new Date());
            Document doc = toDocument(profile);
            doc.remove("_id");
            return col.replaceOne(Filters.eq("_id", profile.getId()), doc).getModifiedCount() > 0;
        } catch (Exception e) {
            System.err.println("[PaymentMethodDAO] Update failed: " + e.getMessage());
            return false;
        }
    }

    private Document toDocument(PaymentMethodProfile profile) {
        Document doc = new Document();
        if (profile.getId() != null) doc.append("_id", profile.getId());
        doc.append("userId", profile.getUserId());
        doc.append("methodType", profile.getMethodType() != null ? profile.getMethodType().name() : null);
        doc.append("profileName", profile.getProfileName());
        doc.append("holderName", profile.getHolderName());
        doc.append("maskedReference", profile.getMaskedReference());
        doc.append("providerName", profile.getProviderName());
        doc.append("billingAddress", profile.getBillingAddress());
        doc.append("nickname", profile.getNickname());
        doc.append("status", profile.getStatus());
        doc.append("preferred", profile.isPreferred());
        doc.append("active", profile.isActive());
        doc.append("createdAt", profile.getCreatedAt());
        doc.append("updatedAt", profile.getUpdatedAt());
        return doc;
    }

    private PaymentMethodProfile fromDocument(Document doc) {
        PaymentMethodProfile profile = new PaymentMethodProfile();
        profile.setId(doc.getObjectId("_id"));
        profile.setUserId(doc.getObjectId("userId"));
        try { profile.setMethodType(PaymentMethodProfile.MethodType.valueOf(doc.getString("methodType"))); } catch (Exception ignored) {}
        profile.setProfileName(doc.getString("profileName"));
        profile.setHolderName(doc.getString("holderName"));
        profile.setMaskedReference(doc.getString("maskedReference"));
        profile.setProviderName(doc.getString("providerName"));
        profile.setBillingAddress(doc.getString("billingAddress"));
        profile.setNickname(doc.getString("nickname"));
        profile.setStatus(doc.getString("status"));
        profile.setPreferred(doc.getBoolean("preferred", false));
        profile.setActive(doc.getBoolean("active", true));
        profile.setCreatedAt(doc.getDate("createdAt"));
        profile.setUpdatedAt(doc.getDate("updatedAt"));
        return profile;
    }
}
