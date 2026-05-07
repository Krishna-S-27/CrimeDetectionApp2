package com.krishna.crimedetection.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

/**
 * Manager for network connectivity status
 * Checks if device is online and determines connection type
 */
public class NetworkStateManager {

    private static final String TAG = "NetworkStateManager";
    private Context context;
    private ConnectivityManager connectivityManager;

    /**
     * Initialize NetworkStateManager
     *
     * @param context Android context
     */
    public NetworkStateManager(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * Check if device is connected to network
     * Supports both old and new Android API levels
     *
     * @return true if connected, false otherwise
     */
    public boolean isNetworkConnected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return isNetworkConnectedModern();
        } else {
            return isNetworkConnectedLegacy();
        }
    }

    /**
     * Get current network connection type
     *
     * @return Connection type (WiFi, Mobile, Ethernet, None)
     */
    public NetworkType getNetworkType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return getNetworkTypeModern();
        } else {
            return getNetworkTypeLegacy();
        }
    }

    /**
     * Check if connected via WiFi
     *
     * @return true if WiFi connected
     */
    public boolean isWiFiConnected() {
        return getNetworkType() == NetworkType.WIFI;
    }

    /**
     * Check if connected via mobile data
     *
     * @return true if mobile data connected
     */
    public boolean isMobileDataConnected() {
        return getNetworkType() == NetworkType.MOBILE;
    }

    // ===================== MODERN API (Android M+) =====================

    private boolean isNetworkConnectedModern() {
        if (connectivityManager == null) return false;

        android.net.Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }

    private NetworkType getNetworkTypeModern() {
        if (connectivityManager == null) return NetworkType.NONE;

        android.net.Network network = connectivityManager.getActiveNetwork();
        if (network == null) return NetworkType.NONE;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        if (capabilities == null) return NetworkType.NONE;

        if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            return NetworkType.WIFI;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
            return NetworkType.MOBILE;
        } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            return NetworkType.ETHERNET;
        }

        return NetworkType.NONE;
    }

    // ===================== LEGACY API (Pre Android M) =====================

    @SuppressWarnings("deprecation")
    private boolean isNetworkConnectedLegacy() {
        if (connectivityManager == null) return false;

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @SuppressWarnings("deprecation")
    private NetworkType getNetworkTypeLegacy() {
        if (connectivityManager == null) return NetworkType.NONE;

        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo == null) return NetworkType.NONE;

        int type = networkInfo.getType();
        if (type == ConnectivityManager.TYPE_WIFI) {
            return NetworkType.WIFI;
        } else if (type == ConnectivityManager.TYPE_MOBILE) {
            return NetworkType.MOBILE;
        } else if (type == ConnectivityManager.TYPE_ETHERNET) {
            return NetworkType.ETHERNET;
        }

        return NetworkType.NONE;
    }

    // ===================== ENUM =====================

    /**
     * Network connection types
     */
    public enum NetworkType {
        WIFI("WiFi"),
        MOBILE("Mobile Data"),
        ETHERNET("Ethernet"),
        NONE("No Connection");

        public final String displayName;

        NetworkType(String displayName) {
            this.displayName = displayName;
        }
    }
}