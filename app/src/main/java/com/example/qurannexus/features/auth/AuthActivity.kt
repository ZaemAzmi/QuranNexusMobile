package com.example.qurannexus.features.auth

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.LifecycleOwner
import com.example.qurannexus.R
import com.example.qurannexus.features.onboard.WelcomeFragment

class AuthActivity : AppCompatActivity(), LifecycleOwner{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auth)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED // Consume insets
        }
        if(savedInstanceState == null){
            supportFragmentManager.beginTransaction()
                .replace(R.id.authFragmentContainer, WelcomeFragment())
                .commit()
        }
    }
}