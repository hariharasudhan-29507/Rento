package com.vrbs.data;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.vrbs.config.AppConfig;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Single shared {@link MongoClient} for the app.
 * <p>
 * <b>You do not download MongoDB JARs by hand</b> — the {@code mongodb-driver-sync}
 * dependency in {@code pom.xml} is resolved by Maven automatically.
 * <p>
 * <b>What you may install separately:</b>
 * <ul>
 *   <li><b>MongoDB Server</b> (local): install MongoDB Community Edition so {@code mongod} listens on
 *       {@code localhost:27017}, or</li>
 *   <li><b>MongoDB Atlas</b> (cloud): create a free cluster and paste the SRV connection string into
 *       {@code application.properties} as {@code vrbs.mongodb.uri}.</li>
 * </ul>
 * Optional: <b>MongoDB Compass</b> (GUI) to browse data — it does not replace a server; you still need
 * Atlas or a local {@code mongod}.
 * <p>
 * If ping fails, this class returns {@link Optional#empty()} and the UI keeps using in-memory demos until
 * you fix the URI or start the server.
 */
public final class MongoConnection {

    private static final Logger LOG = Logger.getLogger(MongoConnection.class.getName());
    private static final String DEFAULT_URI = "mongodb://localhost:27017";
    private static final String DATABASE_NAME = "vrbs_db";
    private static volatile MongoConnection instance;

    private final MongoClient client;
    private final MongoDatabase database;

    private MongoConnection() {
        CodecRegistry pojoCodecRegistry = fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(resolveMongoUri()))
                .codecRegistry(pojoCodecRegistry)
                .build();
        this.client = MongoClients.create(settings);
        this.database = client.getDatabase(DATABASE_NAME).withCodecRegistry(pojoCodecRegistry);
    }

    private static MongoConnection instance() {
        if (instance == null) {
            synchronized (MongoConnection.class) {
                if (instance == null) {
                    instance = new MongoConnection();
                }
            }
        }
        return instance;
    }

    public static Optional<MongoClient> client() {
        try {
            return Optional.of(getClient());
        } catch (Exception e) {
            LOG.log(Level.INFO, "MongoDB not available, using in-memory stubs: {0}", e.getMessage());
            return Optional.empty();
        }
    }

    public static MongoClient getClient() {
        return instance().client;
    }

    public static MongoDatabase getDatabase() {
        return instance().database;
    }

    public static void close() {
        MongoConnection current = instance;
        if (current == null) {
            return;
        }
        synchronized (MongoConnection.class) {
            current = instance;
            if (current != null) {
                current.client.close();
                instance = null;
            }
        }
    }

    private static String resolveMongoUri() {
        String configured = null;
        try {
            configured = AppConfig.mongoUri();
        } catch (Exception ignored) {
            // Fall back to properties lookup below.
        }
        if (configured != null && !configured.isBlank() && !DEFAULT_URI.equals(configured)) {
            return configured.trim();
        }

        Properties props = new Properties();
        try (InputStream in = MongoConnection.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException ignored) {
            // Use defaults if properties are unavailable.
        }

        String direct = props.getProperty("mongo.uri");
        if (direct != null && !direct.isBlank()) {
            return direct.trim();
        }
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }

        String legacy = props.getProperty("vrbs.mongodb.uri");
        if (legacy != null && !legacy.isBlank()) {
            return legacy.trim();
        }
        return DEFAULT_URI;
    }
}
