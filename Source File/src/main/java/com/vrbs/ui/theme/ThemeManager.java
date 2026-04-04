package com.vrbs.ui.theme;

import com.vrbs.config.AppConfig;
import javafx.scene.Scene;
import javafx.scene.text.Font;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Loads fonts from the {@code assets} folder on disk (primary for development) and from the classpath
 * {@code /fonts/} (when copied by Maven for packaged runs).
 */
public final class ThemeManager {

    private static final Logger LOG = Logger.getLogger(ThemeManager.class.getName());

    public static final String DARK = "/css/theme-dark.css";
    public static final String LIGHT = "/css/theme-light.css";

    private boolean dark = true;

    public void tryLoadBundledFonts() {
        Set<String> loaded = new HashSet<>();
        Path assetsRoot = Path.of(System.getProperty("user.dir")).resolve(AppConfig.fontAssetsRoot()).normalize();

        if (AppConfig.fontAutoloadFromAssets()) {
            Path fontsDir = assetsRoot.resolve("fonts");
            if (Files.isDirectory(fontsDir)) {
                try (Stream<Path> walk = Files.walk(fontsDir)) {
                    walk.filter(ThemeManager::isFontFile)
                            .forEach(p -> {
                                if (loadFontFromPath(p)) {
                                    loaded.add(p.getFileName().toString());
                                }
                            });
                } catch (IOException e) {
                    LOG.log(Level.FINE, "Could not scan {0}: {1}", new Object[]{fontsDir, e.getMessage()});
                }
            }
            try (Stream<Path> stream = Files.list(assetsRoot)) {
                stream.filter(ThemeManager::isFontFile)
                        .forEach(p -> {
                            if (loadFontFromPath(p)) {
                                loaded.add(p.getFileName().toString());
                            }
                        });
            } catch (IOException e) {
                LOG.log(Level.FINE, "Could not scan {0}: {1}", new Object[]{assetsRoot, e.getMessage()});
            }
        }

        for (String name : AppConfig.fontFilesToLoad()) {
            if (loaded.contains(name)) {
                continue;
            }
            if (tryLoadClasspath("/fonts/" + name)) {
                loaded.add(name);
                continue;
            }
            Path underFonts = assetsRoot.resolve("fonts").resolve(name);
            if (Files.isRegularFile(underFonts) && loadFontFromPath(underFonts)) {
                loaded.add(name);
                continue;
            }
            Path underAssets = assetsRoot.resolve(name);
            if (Files.isRegularFile(underAssets) && loadFontFromPath(underAssets)) {
                loaded.add(name);
            }
        }

        if (loaded.isEmpty()) {
            LOG.log(Level.INFO,
                    "No fonts found under {0}/fonts — add .ttf/.otf files or set vrbs.font.files for classpath.",
                    assetsRoot);
        }
    }

    private static boolean isFontFile(Path p) {
        if (!Files.isRegularFile(p)) {
            return false;
        }
        String n = p.getFileName().toString().toLowerCase();
        return n.endsWith(".ttf") || n.endsWith(".otf");
    }

    private boolean loadFontFromPath(Path file) {
        try {
            try (InputStream in = Files.newInputStream(file)) {
                Font.loadFont(in, 13);
            }
            try (InputStream in = Files.newInputStream(file)) {
                Font.loadFont(in, 18);
            }
            LOG.log(Level.INFO, "Loaded font from assets: {0}", file);
            return true;
        } catch (IOException e) {
            LOG.log(Level.FINE, "Font load failed {0}: {1}", new Object[]{file, e.getMessage()});
            return false;
        }
    }

    private boolean tryLoadClasspath(String resourcePath) {
        try (InputStream in = ThemeManager.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return false;
            }
            Font.loadFont(in, 13);
        } catch (Exception e) {
            return false;
        }
        try (InputStream in = ThemeManager.class.getResourceAsStream(resourcePath)) {
            if (in != null) {
                Font.loadFont(in, 18);
            }
        } catch (Exception e) {
            return false;
        }
        LOG.log(Level.INFO, "Loaded font from classpath: {0}", resourcePath);
        return true;
    }

    public boolean isDark() {
        return dark;
    }

    public void setDark(boolean dark) {
        this.dark = dark;
    }

    public void applyTo(Scene scene) {
        var sheets = scene.getStylesheets();
        sheets.clear();
        sheets.add(getClass().getResource(dark ? DARK : LIGHT).toExternalForm());
    }

    public void toggle(Scene scene) {
        dark = !dark;
        applyTo(scene);
    }
}
