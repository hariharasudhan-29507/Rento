package com.vrbs.data;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

public final class IndexSetup {

    private IndexSetup() {
    }

    public static void createAllIndexes(MongoDatabase db) {
        db.getCollection(DBCollections.USERS)
                .createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));

        db.getCollection(DBCollections.DRIVERS)
                .createIndex(Indexes.ascending("email"), new IndexOptions().unique(true));
        db.getCollection(DBCollections.DRIVERS)
                .createIndex(Indexes.geo2dsphere("currentLocation"));

        db.getCollection(DBCollections.VEHICLES)
                .createIndex(Indexes.compoundIndex(
                        Indexes.ascending("supplierId"),
                        Indexes.ascending("isAvailable")
                ));

        db.getCollection(DBCollections.BOOKINGS)
                .createIndex(Indexes.compoundIndex(
                                Indexes.ascending("userId"),
                                Indexes.ascending("vehicleId"),
                                Indexes.ascending("startTime")
                        ),
                        new IndexOptions().unique(true));
        db.getCollection(DBCollections.BOOKINGS)
                .createIndex(Indexes.compoundIndex(
                        Indexes.ascending("vehicleId"),
                        Indexes.ascending("status")
                ));
        db.getCollection(DBCollections.BOOKINGS)
                .createIndex(new Document("userId", 1).append("createdAt", -1));

        db.getCollection(DBCollections.CAB_RIDES)
                .createIndex(Indexes.geo2dsphere("pickupLocation"));
        db.getCollection(DBCollections.CAB_RIDES)
                .createIndex(Indexes.compoundIndex(
                        Indexes.ascending("driverId"),
                        Indexes.ascending("status")
                ));
        db.getCollection(DBCollections.CAB_RIDES)
                .createIndex(new Document("userId", 1).append("createdAt", -1));

        db.getCollection(DBCollections.NOTIFICATIONS)
                .createIndex(Indexes.compoundIndex(
                        Indexes.ascending("userId"),
                        Indexes.ascending("isRead")
                ));
    }
}
