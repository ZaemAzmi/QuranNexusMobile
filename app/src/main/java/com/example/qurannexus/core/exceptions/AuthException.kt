package com.example.qurannexus.core.exceptions

class AuthException(message: String) : Exception(message) {
    companion object {
        const val NOT_LOGGED_IN = "Please login to continue"
        const val TOKEN_EXPIRED = "Session expired. Please login again"
        const val INVALID_TOKEN = "Invalid authentication token"
    }
}