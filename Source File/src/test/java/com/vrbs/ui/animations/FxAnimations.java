package com.vrbs.ui.animations;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.util.Duration;

public final class FxAnimations {

    private FxAnimations() {
    }

    public static FadeTransition fadeIn(Node node, Duration duration) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(duration, node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_OUT);
        return ft;
    }

    public static FadeTransition fadeOut(Node node, Duration duration, Runnable onFinished) {
        FadeTransition ft = new FadeTransition(duration, node);
        ft.setFromValue(node.getOpacity());
        ft.setToValue(0);
        ft.setInterpolator(Interpolator.EASE_IN);
        ft.setOnFinished(e -> {
            if (onFinished != null) {
                onFinished.run();
            }
        });
        return ft;
    }

    /**
     * Animate a bar region height from 0 to target (for simple dashboard charts).
     */
    public static Timeline growBarHeight(Node bar, double targetHeight, Duration duration) {
        bar.setScaleY(0);
        bar.setTranslateY(targetHeight / 2);
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(bar.scaleYProperty(), 0, Interpolator.EASE_OUT)),
                new KeyFrame(duration,
                        new KeyValue(bar.scaleYProperty(), 1, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.ZERO,
                        new KeyValue(bar.translateYProperty(), targetHeight / 2)),
                new KeyFrame(duration,
                        new KeyValue(bar.translateYProperty(), 0))
        );
        return tl;
    }

    public static void subtlePulseOpacity(Node node) {
        Timeline tl = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(node.opacityProperty(), 1)),
                new KeyFrame(Duration.seconds(1.2), new KeyValue(node.opacityProperty(), 0.85)),
                new KeyFrame(Duration.seconds(2.4), new KeyValue(node.opacityProperty(), 1))
        );
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.setAutoReverse(true);
        tl.play();
    }
}
