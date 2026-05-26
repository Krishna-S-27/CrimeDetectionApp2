package com.krishna.crimedetection.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.telephony.SmsManager;
import androidx.core.content.ContextCompat;

public class SmsUtils {
    public static boolean hasSmsPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    public static void sendSms(Context context, String phoneNumber, String message) {
        if (hasSmsPermission(context)) {
            try {
                android.telephony.SmsManager smsManager;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    smsManager = context.getSystemService(android.telephony.SmsManager.class);
                } else {
                    smsManager = android.telephony.SmsManager.getDefault();
                }

                if (message.length() > 160) {
                    java.util.ArrayList<String> parts = smsManager.divideMessage(message);
                    smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
                } else {
                    smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
