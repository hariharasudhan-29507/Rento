package com.rento.utils;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

/**
 * Singleton MongoDB connection manager.
 */
public class MongoDBConnection {

    private static final String CONNECTION_STRING = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "rento_db";

    private static MongoDBConnection instance;
    private MongoClient mongoClient;
    private MongoDatabase database;

    private MongoDBConnection() {
        try {  
            mongoClient = MongoClients.create(CONNECTION_STRING);
            database = mongoClient.getDatabase(DATABASE_NAME);
            System.out.println("[Rento] Connected to MongoDB: " + DATABASE_NAME);
        } catch (Exception e) {
            System.err.println("[Rento] MongoDB connection failed: " + e.getMessage());
            // App can still run in demo mode
        }
    }

    public static synchronized MongoDBConnection getInstance() {
        if (instance == null) {
            instance = new MongoDBConnection();
        }
        return instance;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public MongoClient getClient() {
        return mongoClient;
    }

    public boolean isConnected() {
        try {
            if (mongoClient != null) {
                mongoClient.listDatabaseNames().first();
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static void close() {
        if (instance != null && instance.mongoClient != null) {
            instance.mongoClient.close();
            System.out.println("[Rento] MongoDB connection closed.");
        }
    }
}
