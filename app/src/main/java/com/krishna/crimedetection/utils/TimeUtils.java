package com.krishna.crimedetection.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TimeUtils {
    private TimeUtils() {}

    public static String getCurrentTimestamp() {
        return formatTimestamp(System.currentTimeMillis());
    }

    public static String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(timestamp));
    }
}