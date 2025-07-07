package com.example.qurannexus.features.auth

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.example.qurannexus.R
import com.example.qurannexus.core.activities.MainActivity
import com.example.qurannexus.core.interfaces.AuthCallback
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.databinding.FragmentLoginBinding
import com.example.qurannexus.features.auth.models.LoginRequest
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout

class LoginFragment : Fragment() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private lateinit var authService: AuthService
    private val deviceName = "Android Device"

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
        authService = AuthService()
        setupListeners()
        setupInputValidation()
    }

    private fun setupListeners() {
        binding.loginBackButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.loginButton.setOnClickListener {
            handleLogin()
        }

        binding.forgotPasswordTextView.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.signUpPromptTextView.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.authFragmentContainer, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }
    }
    private fun showForgotPasswordDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_forgot_password, null)
        val emailLayout = dialogView.findViewById<TextInputLayout>(R.id.forgotPasswordEmailLayout)
        val emailInput = emailLayout.editText

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Forgot Password")
            .setMessage("Enter your email address to receive password reset instructions.")
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Send", null) // Set to null to override and prevent auto-dismiss
            .show()

        // Override the positive button's click listener
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val email = emailInput?.text.toString().trim()

            if (email.isEmpty()) {
                emailLayout.error = "Email cannot be empty"
                return@setOnClickListener
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLayout.error = "Invalid email format"
                return@setOnClickListener
            }

            // Looks valid, proceed with API call
            emailLayout.error = null
            dialog.dismiss() // Dismiss the dialog manually
            setLoadingState(true) // Show loading on the main screen

            authService.forgotPassword(requireContext(), email, object : ResetPasswordCallback {
                override fun onSuccess() {
                    if (!isAdded) return
                    setLoadingState(false)
                    Snackbar.make(binding.root, "Password reset instructions sent to your email.", Snackbar.LENGTH_LONG).show()
                }

                override fun onError(error: String) {
                    if (!isAdded) return
                    setLoadingState(false)
                    Snackbar.make(binding.root, "Error: $error", Snackbar.LENGTH_LONG).show()
                }
            })
        }
    }
    private fun handleLogin() {
        // Clear previous errors
        binding.loginEmailLayout.error = null
        binding.loginPasswordLayout.error = null

        val email = binding.loginEmailInput.text.toString().trim()
        val password = binding.loginPasswordInput.text.toString()

        var isValid = true
        if (email.isEmpty()) {
            binding.loginEmailLayout.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.loginEmailLayout.error = "Invalid email format"
            isValid = false
        }

        if (password.isEmpty()) {
            binding.loginPasswordLayout.error = "Password is required"
            isValid = false
        }

        if (!isValid) return

        setLoadingState(true)

        val request = LoginRequest(email, password, deviceName)

        authService.login(requireContext(), request, object : AuthCallback {
            override fun onSuccess(token: String) {
                if (!isAdded) return
                ApiService.setAuthToken(token)

                // The profile fetch can happen in the background, just start MainActivity
                startMainActivity()
                // No need to call setLoadingState(false) as we are leaving the screen
            }

            override fun onError(error: String) {
                if (!isAdded) return
                setLoadingState(false)
                Snackbar.make(binding.root, "Login failed. Please check your credentials.", Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private fun startMainActivity() {
        if (!isAdded) return
        val intent = Intent(activity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.loginProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loginButton.text = if (isLoading) "" else "Login"
        binding.loginButton.isEnabled = !isLoading
        binding.loginEmailInput.isEnabled = !isLoading
        binding.loginPasswordInput.isEnabled = !isLoading
        binding.forgotPasswordTextView.isEnabled = !isLoading
        binding.signUpPromptTextView.isEnabled = !isLoading
    }

    private fun setupInputValidation() {
        binding.loginEmailInput.addTextChangedListener {
            binding.loginEmailLayout.error = null // Clear error on type
        }
        binding.loginPasswordInput.addTextChangedListener {
            binding.loginPasswordLayout.error = null // Clear error on type
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
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