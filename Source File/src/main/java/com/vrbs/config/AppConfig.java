package com.vrbs.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public final class AppConfig {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/application.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException ignored) {
            // defaults used
        }
    }

    private AppConfig() {
    }

    public static String mongoUri() {
        return PROPS.getProperty("vrbs.mongodb.uri", "mongodb://localhost:27017");
    }

    public static String mongoDatabase() {
        return PROPS.getProperty("vrbs.mongodb.database", "vrbs");
    }

    public static int sessionTimeoutMinutes() {
        return Integer.parseInt(PROPS.getProperty("vrbs.session.timeoutMinutes", "15"));
    }

    public static double windowDefaultWidth() {
        return Double.parseDouble(PROPS.getProperty("vrbs.ui.window.defaultWidth", "1400"));
    }

    public static double windowDefaultHeight() {
        return Double.parseDouble(PROPS.getProperty("vrbs.ui.window.defaultHeight", "900"));
    }

    public static double windowMinWidth() {
        return Double.parseDouble(PROPS.getProperty("vrbs.ui.window.minWidth", "1280"));
    }

    public static double windowMinHeight() {
        return Double.parseDouble(PROPS.getProperty("vrbs.ui.window.minHeight", "720"));
    }

    /**
     * Optional filenames to load from classpath /fonts/ or from assets (see ThemeManager).
     */
    public static List<String> fontFilesToLoad() {
        String raw = PROPS.getProperty("vrbs.font.files", "");
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /** Relative to working directory, e.g. {@code assets}. */
    public static String fontAssetsRoot() {
        return PROPS.getProperty("vrbs.font.assetsRoot", "assets");
    }

    public static boolean fontAutoloadFromAssets() {
        return Boolean.parseBoolean(PROPS.getProperty("vrbs.font.autoloadFromAssets", "true"));
    }
}
