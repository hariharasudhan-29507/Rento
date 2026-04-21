package com.rento.utils;

import java.security.SecureRandom;

/**
 * CAPTCHA generator utility for lightweight auth protection.
 */
public final class CaptchaGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    private CaptchaGenerator() {
    }

    /**
     * Generates a simple arithmetic CAPTCHA.
     * @return [question, answer]
     */
    public static String[] generateMathCaptcha() {
        int left = RANDOM.nextInt(20) + 1;
        int right = RANDOM.nextInt(20) + 1;
        int operation = RANDOM.nextInt(3);

        switch (operation) {
            case 1:
                if (left < right) {
                    int temp = left;
                    left = right;
                    right = temp;
                }
                return new String[]{left + " - " + right + " = ?", String.valueOf(left - right)};
            case 2:
                left = RANDOM.nextInt(10) + 1;
                right = RANDOM.nextInt(10) + 1;
                return new String[]{left + " x " + right + " = ?", String.valueOf(left * right)};
            default:
                return new String[]{left + " + " + right + " = ?", String.valueOf(left + right)};
        }
    }
}
