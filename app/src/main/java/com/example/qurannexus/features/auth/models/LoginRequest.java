package com.example.qurannexus.features.auth.models;

import com.google.gson.annotations.SerializedName;

public class LoginRequest {

    private String email;
    private String password;
    @SerializedName("device_name")
    private String deviceName;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LoginRequest(String email, String password, String deviceName) {
        this.email = email;
        this.password = password;
        this.deviceName = deviceName; // Initialize new field
    }

    // Getters and setters for the new field
    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
}

