package com.example.qurannexus.fragments

import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
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
    private lateinit var termsCheckBox: CheckBox
    private lateinit var registerButton: Button
    private lateinit var authService: AuthService
    private lateinit var backButton : ImageView

    private var isPasswordVisible = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_register, container, false)

        authService = AuthService()

        backButton = view.findViewById(R.id.registerBackButton)
        registerNameInput = view.findViewById(R.id.registerNameInput)
        registerEmailInput = view.findViewById(R.id.registerEmailInput)
        registerPasswordInput = view.findViewById(R.id.registerPasswordInput)

        termsCheckBox = view.findViewById(R.id.termsCheckBox)
        registerButton = view.findViewById(R.id.registerButton)
        registerButton.isEnabled = false

        backButton.setOnClickListener {
            backButtonNavigation()
        }

        termsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            handleTermsCheckbox(isChecked)
        }
        setupPasswordVisibilityToggle()
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
        val deviceName = "mobile"
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || passwordConfirmation.isEmpty()) {
            Toast.makeText(activity, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val request = RegisterRequest(name, email, password, passwordConfirmation, deviceName)

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

    private fun setupPasswordVisibilityToggle() {
        // Set the "eye" icon toggle logic
        registerPasswordInput.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (registerPasswordInput.right - registerPasswordInput.compoundDrawables[2].bounds.width())) {
                    // Toggle password visibility
                    isPasswordVisible = !isPasswordVisible
                    updatePasswordVisibility()
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun updatePasswordVisibility() {
        if (isPasswordVisible) {
            registerPasswordInput.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            registerPasswordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_opened, 0) // "Eye open" icon
        } else {
            registerPasswordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            registerPasswordInput.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye_closed, 0) // "Eye closed" icon
        }
        // Move cursor to the end
        registerPasswordInput.setSelection(registerPasswordInput.text.length)
    }
    private fun handleTermsCheckbox(isChecked: Boolean) {
        // Enable or disable the register button based on whether the checkbox is checked
        registerButton.isEnabled = isChecked
    }

    private fun backButtonNavigation() {
        parentFragmentManager.popBackStack()
    }
}