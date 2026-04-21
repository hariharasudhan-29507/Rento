package com.rento.utils;

import java.security.SecureRandom;

/**
 * OTP and CAPTCHA generation utilities.
 */
public class OTPGenerator {

    private static final SecureRandom random = new SecureRandom();

    /**
     * Generate a numeric OTP of specified length.
     */
    public static String generateOTP(int length) {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < length; i++) {
            otp.append(random.nextInt(10));
        }
        return otp.toString();
    }

    /**
     * Generate a 6-digit OTP.
     */
    public static String generateOTP() {
        return generateOTP(6);
    }

    /**
     * Generate a simple math CAPTCHA.
     * @return String array: [0] = question, [1] = answer
     */
    public static String[] generateMathCaptcha() {
        int a = random.nextInt(20) + 1;
        int b = random.nextInt(20) + 1;
        int op = random.nextInt(3); // 0=add, 1=subtract, 2=multiply

        String question;
        int answer;

        switch (op) {
            case 0:
                question = a + " + " + b + " = ?";
                answer = a + b;
                break;
            case 1:
                // Ensure positive result
                if (a < b) { int temp = a; a = b; b = temp; }
                question = a + " - " + b + " = ?";
                answer = a - b;
                break;
            case 2:
                a = random.nextInt(10) + 1;
                b = random.nextInt(10) + 1;
                question = a + " × " + b + " = ?";
                answer = a * b;
                break;
            default:
                question = a + " + " + b + " = ?";
                answer = a + b;
        }

        return new String[]{question, String.valueOf(answer)};
    }

    /**
     * Generate a text CAPTCHA with random characters.
     */
    public static String generateTextCaptcha(int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder captcha = new StringBuilder();
        for (int i = 0; i < length; i++) {
            captcha.append(chars.charAt(random.nextInt(chars.length())));
        }
        return captcha.toString();
    }

    /**
     * Generate a transaction reference ID.
     */
    public static String generateTransactionRef() {
        return "TXN" + System.currentTimeMillis() + random.nextInt(1000);
    }
}
