package com.rento.services;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.rento.utils.MongoDBConnection;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class NotificationService {
    private static final String COLLECTION_NAME = "notifications";

    private MongoCollection<Document> getCollection() {
        MongoDatabase db = MongoDBConnection.getInstance().getDatabase();
        if (db == null) return null;
        return db.getCollection(COLLECTION_NAME);
    }

    public void addNotification(ObjectId userId, String title, String message, String downloadablePath) {
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null || userId == null) return;
            Document doc = new Document("userId", userId)
                .append("title", title)
                .append("message", message)
                .append("downloadablePath", downloadablePath)
                .append("createdAt", new Date());
            col.insertOne(doc);
        } catch (Exception ignored) {
        }
    }

    public List<Document> getNotifications(ObjectId userId) {
        List<Document> notifications = new ArrayList<>();
        try {
            MongoCollection<Document> col = getCollection();
            if (col == null || userId == null) return notifications;
            for (Document d : col.find(Filters.eq("userId", userId))) {
                notifications.add(d);
            }
        } catch (Exception ignored) {
        }
        return notifications;
    }

    public String exportNotifications(ObjectId userId, String outputDir) throws Exception {
        List<Document> notifications = getNotifications(userId);
        new File(outputDir).mkdirs();
        String path = outputDir + File.separator + "notifications_" + System.currentTimeMillis() + ".txt";
        try (FileWriter writer = new FileWriter(path)) {
            writer.write("============================================================\n");
            writer.write("                    RENTO NOTIFICATION CENTER               \n");
            writer.write("============================================================\n");
            writer.write("Exported At : " + new Date() + "\n");
            writer.write("Total Items : " + notifications.size() + "\n");
            writer.write("------------------------------------------------------------\n\n");
            for (Document d : notifications) {
                writer.write("[ " + (d.getDate("createdAt") != null ? d.getDate("createdAt") : new Date()) + " ]\n");
                writer.write("Title      : " + d.getString("title") + "\n");
                writer.write("Message    : " + d.getString("message") + "\n");
                String downloadPath = d.getString("downloadablePath");
                if (downloadPath != null && !downloadPath.isBlank()) {
                    writer.write("Attachment : " + downloadPath + "\n");
                }
                writer.write("------------------------------------------------------------\n");
            }
        }
        return path;
    }
}
