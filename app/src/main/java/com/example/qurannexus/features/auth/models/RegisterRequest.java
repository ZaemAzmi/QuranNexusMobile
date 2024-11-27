package com.example.qurannexus.features.auth.models;

import com.google.gson.annotations.SerializedName;

public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    @SerializedName("password_confirmation")
    private String passwordConfirmation;
    @SerializedName("device_name")
    private String deviceName;
    private String role;

    public RegisterRequest(String name, String email, String password, String passwordConfirmation, String deviceName, String role) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.passwordConfirmation = passwordConfirmation;
        this.deviceName = deviceName;
        this.role = role;
    }

    // Constructor without role (default role "USER" used)
    public RegisterRequest(String name, String email, String password, String passwordConfirmation, String deviceName) {
        this(name, email, password, passwordConfirmation, deviceName, "USER");
    }
    // Getters and Setters for the fields
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public String getPasswordConfirmation() {
        return passwordConfirmation;
    }

    public void setPasswordConfirmation(String passwordConfirmation) {
        this.passwordConfirmation = passwordConfirmation;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
