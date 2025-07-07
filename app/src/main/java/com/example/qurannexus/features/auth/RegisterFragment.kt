package com.example.qurannexus.features.auth

import android.graphics.Typeface
import android.os.Bundle
import android.text.Html
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.StyleSpan
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.qurannexus.R
import com.example.qurannexus.core.interfaces.AuthCallback
import com.example.qurannexus.databinding.FragmentRegisterBinding
import com.example.qurannexus.features.auth.models.RegisterRequest
import com.google.android.material.snackbar.Snackbar

class RegisterFragment : Fragment() {

    // Use ViewBinding - much cleaner and safer
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private lateinit var authService: AuthService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        authService = AuthService()
        setupListeners()
        setupTermsAndConditions()
    }

    private fun setupListeners() {
        binding.registerBackButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.termsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            binding.registerButton.isEnabled = isChecked
        }

        binding.registerButton.setOnClickListener {
            handleRegistration()
        }

        // Initially disable the button
        binding.registerButton.isEnabled = false
    }

    private fun setupTermsAndConditions() {
        val fullTextTemplate = getString(R.string.terms_and_conditions_full)
        val termsText = getString(R.string.terms_of_service)
        val policyText = getString(R.string.privacy_policy)

        // This is the final string that will be displayed
        val finalFormattedText = String.format(fullTextTemplate, termsText, policyText)

        // Create a SpannableString from the final text
        val spannableString = SpannableString(finalFormattedText)

        // Create a clickable span for "Terms of Service"
        val termsClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(context, "Navigate to Terms of Service", Toast.LENGTH_SHORT).show()
                // TODO: Replace with navigation to a WebView or browser
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false // Remove underline
                ds.color = ContextCompat.getColor(requireContext(), R.color.surah_card_dark_green_300) // Set link color
            }
        }

        // Create a clickable span for "Privacy Policy"
        val policyClickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                Toast.makeText(context, "Navigate to Privacy Policy", Toast.LENGTH_SHORT).show()
                // TODO: Replace with navigation to a WebView or browser
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = false // Remove underline
                ds.color = ContextCompat.getColor(requireContext(), R.color.surah_card_dark_green_300) // Set link color
            }
        }

        // --- THIS IS THE CORRECTED LOGIC ---
        // Find the start and end indices of the link texts within the final formatted string.
        val termsStart = finalFormattedText.indexOf(termsText)
        val termsEnd = termsStart + termsText.length

        val policyStart = finalFormattedText.indexOf(policyText)
        val policyEnd = policyStart + policyText.length

        // Safety check to avoid crashing if the text isn't found
        if (termsStart == -1 || policyStart == -1) {
            // Fallback to plain text if something went wrong
            binding.termsTextView.text = finalFormattedText
            return
        }

        // Apply the spans to the string
        spannableString.setSpan(termsClickableSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(StyleSpan(Typeface.BOLD), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        spannableString.setSpan(policyClickableSpan, policyStart, policyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spannableString.setSpan(StyleSpan(Typeface.BOLD), policyStart, policyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Set the text and make it clickable
        binding.termsTextView.text = spannableString
        binding.termsTextView.movementMethod = LinkMovementMethod.getInstance()
        // Optional: Remove the default highlight color when a link is clicked
        binding.termsTextView.highlightColor = ContextCompat.getColor(requireContext(), android.R.color.transparent)
    }

    private fun handleRegistration() {
        // Clear previous errors
        binding.registerNameLayout.error = null
        binding.registerEmailLayout.error = null
        binding.registerPasswordLayout.error = null

        val name = binding.registerNameInput.text.toString().trim()
        val email = binding.registerEmailInput.text.toString().trim()
        val password = binding.registerPasswordInput.text.toString()
        val deviceName = "Android Device"

        // --- Input Validation ---
        var isValid = true
        if (name.isEmpty()) {
            binding.registerNameLayout.error = "Name is required"
            isValid = false
        }
        if (email.isEmpty()) {
            binding.registerEmailLayout.error = "Email is required"
            isValid = false
        }
        if (password.isEmpty()) {
            binding.registerPasswordLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 8) {
            binding.registerPasswordLayout.error = "Password must be at least 8 characters"
            isValid = false
        }

        if (!isValid) return

        // --- Show Loading State ---
        setLoadingState(true)

        // --- Make API Call ---
        val request = RegisterRequest(name, email, password, password, deviceName)

        authService.register(requireContext(), request, object : AuthCallback {
            override fun onSuccess(message: String?) {
                if (!isAdded) return // Check if fragment is still attached
                setLoadingState(false)
                Toast.makeText(activity, "Registration successful! Please log in.", Toast.LENGTH_LONG).show()

                // Navigate to LoginFragment, clearing the back stack
                parentFragmentManager.popBackStack()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.authFragmentContainer, LoginFragment())
                    .commit()
            }

            override fun onError(error: String) {
                if (!isAdded) return
                setLoadingState(false)
                // Show a generic error in a Snackbar
                view?.let { Snackbar.make(it, "Registration failed: $error", Snackbar.LENGTH_LONG).show() }
            }
        })
    }

    private fun setLoadingState(isLoading: Boolean) {
        binding.registerButton.text = if (isLoading) "" else getString(R.string.register_button)
        // Add a progress bar to your button in XML if you want a visual indicator
        // binding.registerProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.registerButton.isEnabled = !isLoading
        binding.registerNameInput.isEnabled = !isLoading
        binding.registerEmailInput.isEnabled = !isLoading
        binding.registerPasswordInput.isEnabled = !isLoading
        binding.termsCheckBox.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}