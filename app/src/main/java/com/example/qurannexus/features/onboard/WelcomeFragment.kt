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
import com.example.qurannexus.features.auth.LoginFragment
import com.example.qurannexus.features.auth.RegisterFragment

class WelcomeFragment : Fragment() {

    private lateinit var loginButton : Button
    private lateinit var registerButon : Button
    private lateinit var skipText : TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)
        loginButton = view.findViewById(R.id.welcomePageLoginButton)
        registerButon = view.findViewById(R.id.welcomePageRegisterButton)
        skipText = view.findViewById(R.id.skipTextView)

        skipText.setOnClickListener{
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }
        loginButton.setOnClickListener{
            navigateToFragment(LoginFragment())
        }
        registerButon.setOnClickListener{
            navigateToFragment(RegisterFragment())
        }
        return view
    }
    private fun navigateToFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.authFragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment WelcomeFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            WelcomeFragment().apply {
                arguments = Bundle().apply {
//                    putString(ARG_PARAM1, param1)
//                    putString(ARG_PARAM2, param2)
                }
            }
    }
}