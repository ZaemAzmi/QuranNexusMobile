package com.example.qurannexus.features.statistics

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.qurannexus.R
import com.example.qurannexus.core.utils.UtilityService
import com.example.qurannexus.features.auth.AuthActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomepageStatisticsLockedFragment : Fragment(){

    @Inject
    lateinit var utilityService: UtilityService

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.layout_home_statistics_locked, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val unlockButton = view.findViewById<View>(R.id.button_unlock_stats)
        unlockButton.setOnClickListener {
            // Use the utility service to show the login dialog
            utilityService.showLoginRequiredDialog(requireActivity()) {
                // This is the action to perform on login click
                val intent = Intent(requireActivity(), AuthActivity::class.java).apply {
                    putExtra(AuthActivity.EXTRA_ACTION, AuthActivity.ACTION_NAVIGATE_TO_LOGIN)
                }
                startActivity(intent)
            }
        }
    }
}