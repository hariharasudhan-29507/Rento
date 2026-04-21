package com.rento.utils;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public final class DateTimeUtil {

    private static final SimpleDateFormat DISPLAY_FORMAT = new SimpleDateFormat("dd MMM yyyy, hh:mm a");

    private DateTimeUtil() {
    }

    public static List<String> buildTimeSlots() {
        List<String> slots = new ArrayList<>();
        LocalTime time = LocalTime.MIDNIGHT;
        while (!time.equals(LocalTime.of(23, 30).plusMinutes(30))) {
            slots.add(formatTime(time));
            time = time.plusMinutes(30);
        }
        return slots;
    }

    public static String formatTime(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        int displayHour = hour % 12 == 0 ? 12 : hour % 12;
        String meridiem = hour < 12 ? "AM" : "PM";
        return String.format("%02d:%02d %s", displayHour, minute, meridiem);
    }

    public static LocalTime parseTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalTime.of(9, 0);
        }
        String[] tokens = value.trim().split(" ");
        String[] parts = tokens[0].split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        String meridiem = tokens.length > 1 ? tokens[1].toUpperCase() : "AM";
        if ("PM".equals(meridiem) && hour != 12) {
            hour += 12;
        } else if ("AM".equals(meridiem) && hour == 12) {
            hour = 0;
        }
        return LocalTime.of(hour, minute);
    }

    public static Date toDate(LocalDate date, String time) {
        LocalDateTime dateTime = LocalDateTime.of(date, parseTime(time));
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static long ceilDaysBetween(Date start, Date end) {
        long minutes = minutesBetween(start, end);
        if (minutes <= 0) {
            return 0;
        }
        return Math.max(1, (minutes + (24L * 60L) - 1) / (24L * 60L));
    }

    public static long ceilHoursBetween(Date start, Date end) {
        long minutes = minutesBetween(start, end);
        if (minutes <= 0) {
            return 0;
        }
        return Math.max(1, (minutes + 59L) / 60L);
    }

    public static long minutesBetween(Date start, Date end) {
        if (start == null || end == null) {
            return 0;
        }
        return Duration.between(start.toInstant(), end.toInstant()).toMinutes();
    }

    public static String formatDateTime(Date value) {
        return value == null ? "" : DISPLAY_FORMAT.format(value);
    }

    public static String formatDuration(Date start, Date end) {
        long minutes = minutesBetween(start, end);
        if (minutes <= 0) {
            return "0 mins";
        }
        long days = minutes / (24L * 60L);
        long remainingMinutes = minutes % (24L * 60L);
        long hours = remainingMinutes / 60L;
        long mins = remainingMinutes % 60L;

        List<String> parts = new ArrayList<>();
        if (days > 0) {
            parts.add(days + (days == 1 ? " day" : " days"));
        }
        if (hours > 0) {
            parts.add(hours + (hours == 1 ? " hr" : " hrs"));
        }
        if (mins > 0) {
            parts.add(mins + (mins == 1 ? " min" : " mins"));
        }
        return String.join(" ", parts);
    }
}
