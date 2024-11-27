package com.example.qurannexus.features.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.qurannexus.R
import com.example.qurannexus.core.activities.MainActivity
import com.example.qurannexus.core.interfaces.AuthCallback
import com.example.qurannexus.features.auth.models.LoginRequest

class LoginFragment : Fragment() {

    private lateinit var loginEmailInput: EditText
    private lateinit var loginPasswordInput: EditText
    private lateinit var loginButton: Button
    private lateinit var authService: AuthService
    val deviceName = "My Android Device"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        loginEmailInput = view.findViewById(R.id.loginEmailInput)
        loginPasswordInput = view.findViewById(R.id.loginPasswordInput)
        loginButton = view.findViewById(R.id.loginButton)

        authService = AuthService()

        loginButton.setOnClickListener {
            login()
        }

        return view
    }

    private fun login() {
        val email = loginEmailInput.text.toString()
        val password = loginPasswordInput.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(activity, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val request = LoginRequest(
            email,
            password,
            deviceName
        )

        authService.login(requireContext(), request, object : AuthCallback {
            override fun onSuccess(token: String) {
                val sharedPreferences = requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                sharedPreferences.edit().putString("token", token).apply()

                // Fetch user profile to get the username
                authService.getUserProfile(token) { user ->
                    if (user?.name != null) {
                        sharedPreferences.edit().putString("username", user.name).apply()
                        Log.d("AuthDebug", "Username saved: ${user.name}")
                    } else {
                        Log.e("AuthDebug", "Failed to fetch username.")
                    }

                    Toast.makeText(activity, "Login successful! Token: $token", Toast.LENGTH_SHORT).show()
                    val intent = Intent(activity, MainActivity::class.java)
                    startActivity(intent)
                    activity?.finish()
                }
            }

            override fun onError(error: String) {
                Toast.makeText(activity, error, Toast.LENGTH_SHORT).show()
            }
        })

    }
}