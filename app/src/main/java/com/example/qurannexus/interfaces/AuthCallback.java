package com.example.qurannexus.interfaces;

public interface AuthCallback {
    void onSuccess(String message);
    void onError(String error);
}