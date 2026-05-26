package com.krishna.crimedetection.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.location.Location;
import com.krishna.crimedetection.R;
import com.krishna.crimedetection.models.AppDatabase;
import com.krishna.crimedetection.models.CrimeSceneEvidence;
import com.krishna.crimedetection.models.EmergencyContact;
import com.krishna.crimedetection.models.Prediction;
import com.krishna.crimedetection.utils.Constants;
import com.krishna.crimedetection.utils.LocationManager;
import com.krishna.crimedetection.utils.NotificationUtils;
import com.krishna.crimedetection.utils.PreferenceUtils;
import com.krishna.crimedetection.utils.SmsUtils;
import com.krishna.crimedetection.utils.TokenManager;
import com.krishna.crimedetection.utils.WhatsAppManager;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class EmergencyResponseManager {

    public static void triggerEmergencyResponse(
            Context context,
            Prediction prediction,
            Location location,
            String videoPath,
            Bitmap previewFrame) {

        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(context);
            TokenManager tokenManager = new TokenManager(context);
            int userId = tokenManager.getUserId();
            
            if (userId == -1) return;

            List<EmergencyContact> contacts = db.emergencyContactDao().getActiveContacts(userId);
            if (contacts.isEmpty()) return;

            LocationManager locManager = new LocationManager(context);
            String address = "Searching...";
            double lat = 0, lon = 0;
            if (location != null) {
                lat = location.getLatitude();
                lon = location.getLongitude();
                address = locManager.getAddressFromCoordinates(lat, lon);
            }

            // 1. Save Evidence record
            CrimeSceneEvidence evidence = new CrimeSceneEvidence();
            evidence.setLatitude(lat);
            evidence.setLongitude(lon);
            evidence.setAddress(address);
            evidence.setTimestamp(System.currentTimeMillis());
            db.crimeSceneEvidenceDao().insertEvidence(evidence);

            // 2. High Confidence Alert (>85%)
            if (prediction.getConfidence() >= 0.85) {
                String fullAlertMessage = buildEmergencyMessage(context, prediction, location, address, System.currentTimeMillis());
                int smsSentCount = 0;

                for (EmergencyContact contact : contacts) {
                    if (contact.getPhoneNumber() != null && !contact.getPhoneNumber().isEmpty()) {
                        SmsUtils.sendSms(context, contact.getPhoneNumber(), fullAlertMessage);
                        smsSentCount++;
                        android.util.Log.d("EmergencyResponse", "SMS automatically sent to: " + contact.getPhoneNumber());
                    }
                }

                // Show notification for the user to offer manual WhatsApp share
                NotificationUtils.showEmergencyActionNotification(context, "Violence Alert Sent", 
                        "Automated SMS sent to " + smsSentCount + " contacts.", address, fullAlertMessage);
            }

            // 3. WhatsApp Option (User triggered or secondary)
            // Existing WhatsApp logic can remain or be modified to be the "Secondary" option
            // The requirement says "offer WhatsApp option (user can send if they want)"
            // Usually this means launching an intent or showing a dialog.
        });
    }

    private static String buildSmsMessage(Prediction prediction, double lat, double lon, String address) {
        String coords = String.format(Locale.US, "%.4f, %.4f", lat, lon);
        String confidence = String.format(Locale.US, "%.1f%%", prediction.getConfidence() * 100);
        // Evidence link placeholder - usually would be a Drive link if uploaded
        String driveLink = "Evidence captured on device."; 

        return "🚨 EMERGENCY: Violence detected at " + address + 
               ". Confidence: " + confidence + 
               ". Evidence: " + driveLink + 
               ". Location: " + coords;
    }

    private static String buildEmergencyMessage(
            Context context,
            Prediction prediction,
            Location location,
            String address,
            long timestamp) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timeStr = sdf.format(new Date(timestamp));

        String userName = PreferenceUtils.getUserName(context);
        String userPhone = PreferenceUtils.getUserPhone(context);
        
        String policeStation = "Central Police Station"; 
        String policeNumber = context.getString(R.string.num_police);

        String message = Constants.EMERGENCY_MESSAGE_TEMPLATE
                .replace("{userName}", userName != null ? userName : "User")
                .replace("{userPhone}", userPhone != null ? userPhone : "N/A")
                .replace("{address}", address != null ? address : "Unknown")
                .replace("{latitude}", location != null ? String.valueOf(location.getLatitude()) : "N/A")
                .replace("{longitude}", location != null ? String.valueOf(location.getLongitude()) : "N/A")
                .replace("{policeStation}", policeStation)
                .replace("{policeNumber}", policeNumber)
                .replace("{confidence}", String.format(Locale.getDefault(), "%.1f", prediction.getConfidence() * 100))
                .replace("{timestamp}", timeStr);
        
        if (prediction.alertMessage != null && !prediction.alertMessage.isEmpty()) {
            message = "⚠️ ALERT: " + prediction.alertMessage + "\n\n" + message;
        }

        return message.replace("{driveLink}", "Local Evidence Saved");
    }
}
