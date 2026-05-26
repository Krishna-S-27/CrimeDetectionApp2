package com.krishna.crimedetection.utils;

public class Constants {
    public static final float EMERGENCY_RESPONSE_THRESHOLD = 0.85f;
    public static final int MIN_EMERGENCY_CONTACTS = 1;
    public static final boolean SMS_FALLBACK_ENABLED = true;
    public static final String DRIVE_FOLDER_PREFIX = "Violence_Detection_Evidence";

    public static final String EMERGENCY_MESSAGE_TEMPLATE = 
        "🚨 EMERGENCY ALERT 🚨\n\n" +
        "Violence Detected!\n\n" +
        "👤 User: {userName}\n" +
        "📞 Phone: {userPhone}\n" +
        "📍 Location: {address}\n" +
        "Coordinates: {latitude}, {longitude}\n\n" +
        "👮 Nearest Police Station: {policeStation}\n" +
        "📞 Police Number: {policeNumber}\n\n" +
        "📊 Confidence: {confidence}%\n" +
        "⏰ Time: {timestamp}\n\n" +
        "📹 Video Evidence: {driveLink}\n\n" +
        "🚨 Emergency services notified.";
}
