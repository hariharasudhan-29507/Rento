package com.vrbs.session;

import com.vrbs.config.AppConfig;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Resets idle timer on scene events; fires session expired after timeout.
 */
public final class SessionWatchdog {

    private final int timeoutMinutes;
    private final Consumer<String> onSessionExpired;
    private final AtomicLong lastActivityNanos = new AtomicLong(System.nanoTime());
    private Timeline ticker;
    private Scene boundScene;

    public SessionWatchdog(Consumer<String> onSessionExpired) {
        this(AppConfig.sessionTimeoutMinutes(), onSessionExpired);
    }

    public SessionWatchdog(int timeoutMinutes, Consumer<String> onSessionExpired) {
        this.timeoutMinutes = timeoutMinutes;
        this.onSessionExpired = onSessionExpired;
    }

    public void bind(Scene scene) {
        if (boundScene == scene) {
            return;
        }
        stop();
        boundScene = scene;
        if (scene == null) {
            return;
        }
        javafx.event.EventHandler<javafx.event.Event> filter = e -> touch();
        scene.addEventFilter(javafx.scene.input.KeyEvent.ANY, filter);
        scene.addEventFilter(javafx.scene.input.MouseEvent.ANY, filter);
        scene.addEventFilter(javafx.scene.input.ScrollEvent.ANY, filter);

        ticker = new Timeline(new KeyFrame(Duration.seconds(10), e -> checkIdle()));
        ticker.setCycleCount(Timeline.INDEFINITE);
        ticker.play();
    }

    public void touch() {
        lastActivityNanos.set(System.nanoTime());
    }

    public void stop() {
        if (ticker != null) {
            ticker.stop();
            ticker = null;
        }
        boundScene = null;
    }

    private void checkIdle() {
        if (!SessionManager.get().isLoggedIn()) {
            return;
        }
        long idleNanos = System.nanoTime() - lastActivityNanos.get();
        long limitNanos = java.util.concurrent.TimeUnit.MINUTES.toNanos(timeoutMinutes);
        if (idleNanos > limitNanos) {
            Platform.runLater(() -> {
                SessionManager.get().logout();
                if (onSessionExpired != null) {
                    onSessionExpired.accept("Session expired after " + timeoutMinutes + " minutes of inactivity.");
                }
            });
        }
    }
}
