package com.krishna.crimedetection.network.models;

public class FcmTokenRequest {
    private String token;
    private String device_name;
    private String device_type;
    
    public FcmTokenRequest(String token, String deviceName, String deviceType) {
        this.token = token;
        this.device_name = deviceName;
        this.device_type = deviceType;
    }

    public String getToken() { return token; }
    public String getDeviceName() { return device_name; }
    public String getDeviceType() { return device_type; }
}
