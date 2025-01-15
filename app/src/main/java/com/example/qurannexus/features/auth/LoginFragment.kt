package com.example.qurannexus.features.auth

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.databinding.FragmentLoginBinding
import com.example.qurannexus.features.auth.models.LoginRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var authService: AuthService
    private val deviceName = "Android Device"
    private var isFragmentActive = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isFragmentActive = true
        authService = AuthService()
        setupInputValidation()
        setupLoginButton()
        setupForgotPasswordButton()
        checkExistingToken()
    }

    override fun onResume() {
        super.onResume()
        isFragmentActive = true
    }

    override fun onPause() {
        super.onPause()
        isFragmentActive = false
    }

    private fun setupLoginButton() {
        binding.loginButton.setOnClickListener {
            if (!isFragmentActive) return@setOnClickListener

            val email = binding.loginEmailInput.text.toString()
            val password = binding.loginPasswordInput.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                showMessage("Please fill in all fields")
                return@setOnClickListener
            }

            setLoadingState(true)

            val request = LoginRequest(email, password, deviceName)
            try {
                context?.let { ctx ->
                    authService.login(ctx, request, object : AuthCallback {
                        override fun onSuccess(token: String) {
                            if (!isFragmentActive) return

                            // Set token in ApiService for other API calls
                            ApiService.setAuthToken(token)

                            // Try to get user profile, but proceed anyway
                            authService.getUserProfile(token) { user ->
                                if (!isFragmentActive) return@getUserProfile

                                activity?.runOnUiThread {
                                    try {
                                        if (!isFragmentActive) return@runOnUiThread

                                        if (user != null) {
                                            context?.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                                                ?.edit()
                                                ?.putString("username", user.name)
                                                ?.apply()
                                        } else {
                                            Log.w("LoginFragment", "Could not fetch user profile, but proceeding with login")
                                        }
                                        setLoadingState(false)
                                        startMainActivity()
                                    } catch (e: Exception) {
                                        Log.e("LoginFragment", "Error in profile success callback", e)
                                    }
                                }
                            }
                        }

                        override fun onError(error: String) {
                            if (!isFragmentActive) return

                            activity?.runOnUiThread {
                                if (!isFragmentActive) return@runOnUiThread
                                setLoadingState(false)
                                showMessage(error)
                            }
                        }
                    })
                }
            } catch (e: Exception) {
                Log.e("LoginFragment", "Error during login", e)
                setLoadingState(false)
                showMessage("An error occurred during login")
            }
        }
    }

    private fun checkExistingToken() {
        try {
            context?.let { ctx ->
                val token = authService.getStoredToken(ctx)
                if (!token.isNullOrEmpty()) {
                    ApiService.setAuthToken(token)

                    // Verify token validity before proceeding
                    authService.getUserProfile(token) { user ->
                        if (!isFragmentActive) return@getUserProfile

                        activity?.runOnUiThread {
                            if (!isFragmentActive) return@runOnUiThread

                            if (user != null) {
                                context?.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                                    ?.edit()
                                    ?.putString("username", user.name)
                                    ?.apply()
                                startMainActivity()
                            }
                            // If user is null, token might be invalid, let user login again
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LoginFragment", "Error checking existing token", e)
        }
    }

    private fun startMainActivity() {
        if (!isFragmentActive) return

        try {
            context?.let { ctx ->
                val intent = Intent(ctx, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                activity?.finish()
            }
        } catch (e: Exception) {
            Log.e("LoginFragment", "Error starting MainActivity", e)
        }
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (!isFragmentActive) return

        try {
            binding.loginButton.isEnabled = !isLoading
            binding.loginProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        } catch (e: Exception) {
            Log.e("LoginFragment", "Error setting loading state", e)
        }
    }

    private fun showMessage(message: String) {
        if (!isFragmentActive) return

        try {
            view?.let { Snackbar.make(it, message, Snackbar.LENGTH_LONG).show() }
        } catch (e: Exception) {
            Log.e("LoginFragment", "Error showing message", e)
        }
    }
    private fun setupForgotPasswordButton() {
        binding.forgotPasswordTextView.setOnClickListener {
            if (!isFragmentActive) return@setOnClickListener

            // Create and show dialog
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle("Forgot Password")
                .setView(R.layout.dialog_forgot_password)
                .setPositiveButton("Send", null) // Set to null initially
                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                .create()

            dialog.show()

            // Get dialog views
            val emailInput = dialog.findViewById<TextInputEditText>(R.id.forgotPasswordEmailInput)

            // Override positive button to prevent automatic dismiss
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val email = emailInput?.text?.toString() ?: ""

                if (email.isEmpty()) {
                    emailInput?.error = "Please enter your email address"
                    return@setOnClickListener
                }

                setLoadingState(true)
                dialog.dismiss()

                try {
                    authService.forgotPassword(requireContext(), email, object : ResetPasswordCallback {
                        override fun onSuccess() {
                            if (!isFragmentActive) return

                            activity?.runOnUiThread {
                                if (!isFragmentActive) return@runOnUiThread
                                setLoadingState(false)
                                showMessage("Password reset instructions have been sent to your email")
                            }
                        }

                        override fun onError(error: String) {
                            if (!isFragmentActive) return

                            activity?.runOnUiThread {
                                if (!isFragmentActive) return@runOnUiThread
                                setLoadingState(false)
                                showMessage(error)
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.e("LoginFragment", "Error during password reset", e)
                    setLoadingState(false)
                    showMessage("An error occurred during password reset")
                }
            }
        }
    }
    private fun setupInputValidation() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (!isFragmentActive) return
                validateInputs()
            }
        }

        binding.loginEmailInput.addTextChangedListener(textWatcher)
        binding.loginPasswordInput.addTextChangedListener(textWatcher)
    }

    private fun validateInputs() {
        if (!isFragmentActive) return

        try {
            val email = binding.loginEmailInput.text.toString()
            val password = binding.loginPasswordInput.text.toString()
            binding.loginButton.isEnabled = email.isNotEmpty() && password.isNotEmpty()
        } catch (e: Exception) {
            Log.e("LoginFragment", "Error validating inputs", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        isFragmentActive = false
        _binding = null
    }
}


// Callback interface (can be in a separate file)
interface ResetPasswordCallback {
    fun onSuccess()
    fun onError(error: String)
}

// Create ForgotPasswordRequest.kt
data class ForgotPasswordRequest(
    val email: String
)

// Create ForgotPasswordResponse.kt
data class ForgotPasswordResponse(
    val message: String,
    // Temporary for testing, remove in production
    val temp_password: String? = null
)