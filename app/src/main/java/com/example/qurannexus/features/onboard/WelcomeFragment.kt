package com.example.qurannexus.features.onboard

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.example.qurannexus.R
import com.example.qurannexus.core.activities.MainActivity
import com.example.qurannexus.core.network.ApiService
import com.example.qurannexus.features.auth.AuthService
import com.example.qurannexus.features.auth.LoginFragment
import com.example.qurannexus.features.auth.RegisterFragment

class WelcomeFragment : Fragment() {
    private lateinit var loginButton: Button
    private lateinit var registerButton: Button
    private lateinit var skipText: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)
        loginButton = view.findViewById(R.id.welcomePageLoginButton)
        registerButton = view.findViewById(R.id.welcomePageRegisterButton)
        skipText = view.findViewById(R.id.skipTextView)

        setupClickListeners()
        return view
    }

    private fun setupClickListeners() {
        skipText.setOnClickListener {
            startMainActivity()
        }

        loginButton.setOnClickListener {
            navigateToFragment(LoginFragment())
        }

        registerButton.setOnClickListener {
            navigateToFragment(RegisterFragment())
        }
    }

    private fun startMainActivity() {
        val intent = Intent(context, MainActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }

    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.authFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
}