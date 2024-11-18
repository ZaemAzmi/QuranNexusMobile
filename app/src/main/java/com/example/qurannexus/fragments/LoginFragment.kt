package com.example.qurannexus.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.qurannexus.R
import com.example.qurannexus.activities.MainActivity
import com.example.qurannexus.activities.TestActivity
import com.example.qurannexus.interfaces.AuthCallback
import com.example.qurannexus.models.LoginRequest
import com.example.qurannexus.services.AuthService

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

        val request = LoginRequest(email, password, deviceName)

        authService.login(requireContext(), request, object : AuthCallback {
            override fun onSuccess(message: String) {
                Toast.makeText(activity, "Login successful! Token: $message", Toast.LENGTH_SHORT).show()
                val intent = Intent(activity, MainActivity::class.java)
                startActivity(intent)
                activity?.finish()
            }

            override fun onError(error: String) {
                Toast.makeText(activity, error, Toast.LENGTH_SHORT).show()
            }
        })
    }
}