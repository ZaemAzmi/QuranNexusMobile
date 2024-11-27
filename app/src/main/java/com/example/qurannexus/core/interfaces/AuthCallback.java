package com.example.qurannexus.core.interfaces;

public interface AuthCallback {
    void onSuccess(String message);
    void onError(String error);
}