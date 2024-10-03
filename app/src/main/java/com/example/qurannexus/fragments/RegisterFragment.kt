package com.example.qurannexus.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.qurannexus.R
import com.example.qurannexus.interfaces.AuthCallback
import com.example.qurannexus.services.AuthService
import com.example.qurannexus.models.RegisterRequest
import org.json.JSONException
import org.json.JSONObject


class RegisterFragment : Fragment() {

    private lateinit var registerNameInput: EditText
    private lateinit var registerEmailInput: EditText
    private lateinit var registerPasswordInput: EditText
    private lateinit var registerButton: Button
    private lateinit var authService: AuthService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        registerNameInput = view.findViewById(R.id.registerNameInput)
        registerEmailInput = view.findViewById(R.id.registerEmailInput)
        registerPasswordInput = view.findViewById(R.id.registerPasswordInput)
        registerButton = view.findViewById(R.id.registerButton)

        authService = AuthService()

        registerButton.setOnClickListener {
            register()
        }

        return view
    }

    private fun register() {
        val name = registerNameInput.text.toString()
        val email = registerEmailInput.text.toString()
        val password = registerPasswordInput.text.toString()
        val passwordConfirmation = password  // You need to add a confirmation input field

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || passwordConfirmation.isEmpty()) {
            Toast.makeText(activity, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val request = RegisterRequest(name, email, password, passwordConfirmation)

        authService.register(requireContext(), request, object : AuthCallback {
            override fun onSuccess(message: String?) {
                if (message.isNullOrEmpty()) {
                    // Handle the case where message is null or empty
                    Toast.makeText(activity, "Registration successful!", Toast.LENGTH_SHORT).show()
                }
                Toast.makeText(activity, "Registration successful! Token: $message", Toast.LENGTH_SHORT).show()
                val fragmentTransaction = parentFragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.authFragmentContainer, LoginFragment())
                fragmentTransaction.addToBackStack(null) // Optional: To add this transaction to the back stack
                fragmentTransaction.commit()
            }

            override fun onError(error: String) {
                Toast.makeText(activity, error, Toast.LENGTH_SHORT).show()
            }
        })
    }
}