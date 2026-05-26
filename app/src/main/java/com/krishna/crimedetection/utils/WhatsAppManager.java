package com.krishna.crimedetection.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;
import java.net.URLEncoder;
import java.util.List;

public class WhatsAppManager {

    public static String getInstalledWhatsAppPackage(Context context) {
        PackageManager pm = context.getPackageManager();
        String[] packages = {"com.whatsapp", "com.whatsapp.w4b"};
        for (String pkg : packages) {
            try {
                pm.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
                return pkg;
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }
        return null;
    }

    public static boolean isWhatsAppInstalled(Context context) {
        return getInstalledWhatsAppPackage(context) != null;
    }

    public static void sendWhatsAppMessage(Context context, String phoneNumber, String message, Callback callback) {
        String whatsappPackage = getInstalledWhatsAppPackage(context);
        if (whatsappPackage == null) {
            if (callback != null) callback.onNotInstalled();
            return;
        }

        try {
            String formattedNumber = formatPhoneNumberForWhatsApp(phoneNumber);
            // Remove + if present for the URI
            if (formattedNumber.startsWith("+")) {
                formattedNumber = formattedNumber.substring(1);
            }
            
            Intent intent = new Intent(Intent.ACTION_VIEW);
            String url = "https://wa.me/" + formattedNumber + "?text=" + URLEncoder.encode(message, "UTF-8");
            intent.setPackage(whatsappPackage);
            intent.setData(Uri.parse(url));
            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
            if (callback != null) callback.onSuccess(phoneNumber);
        } catch (Exception e) {
            if (callback != null) callback.onFailure(phoneNumber, e.getMessage());
        }
    }

    public static void sendWhatsAppVideo(Context context, String phoneNumber, String message, Uri videoUri, Callback callback) {
        String whatsappPackage = getInstalledWhatsAppPackage(context);
        if (whatsappPackage == null) {
            if (callback != null) callback.onNotInstalled();
            return;
        }

        try {
            String formattedNumber = formatPhoneNumberForWhatsApp(phoneNumber);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("video/*");
            intent.putExtra("jid", formattedNumber + "@s.whatsapp.net");
            intent.putExtra(Intent.EXTRA_TEXT, message);
            intent.putExtra(Intent.EXTRA_STREAM, videoUri);
            intent.setPackage(whatsappPackage);
            if (!(context instanceof android.app.Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
            if (callback != null) callback.onSuccess(phoneNumber);
        } catch (Exception e) {
            if (callback != null) callback.onFailure(phoneNumber, e.getMessage());
        }
    }

    public static String formatPhoneNumberForWhatsApp(String phoneNumber) {
        return phoneNumber.replaceAll("[^0-9+]", "");
    }

    public interface Callback {
        void onSuccess(String phoneNumber);
        void onFailure(String phoneNumber, String error);
        void onNotInstalled();
    }
}
