package com.example.qurannexus.core.utils

import android.app.Activity
import com.example.qurannexus.R
import android.content.Context
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.String
import javax.inject.Inject
import kotlin.Exception
import kotlin.Int
import kotlin.arrayOf


class UtilityService @Inject constructor() {
    fun convertToArabicNumber(number : Int): kotlin.String {
        val arabicNumbers = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
        val arabicNumber = StringBuilder()
        val numStr = String.valueOf(number)

        for (digit in numStr.toCharArray()) {
            arabicNumber.append(arabicNumbers[Character.getNumericValue(digit)])
        }

        return arabicNumber.toString()
    }

    // Add new method for handling bottom navigation insets
    fun setupBottomNavPadding(fragment: Fragment?, contentView: View?) {
        if (fragment == null || contentView == null) return

        try {
            val context: Context = fragment.getContext() ?: return

            ViewCompat.setOnApplyWindowInsetsListener(contentView) { v: View?, windowInsets: WindowInsetsCompat ->
                val navBarHeight =
                    windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
                val meowNavHeight =
                    fragment.resources.getDimensionPixelSize(com.example.qurannexus.R.dimen.meow_bottom_nav_height)

                // Set bottom padding to accommodate both the navigation bar and bottom nav
                val totalBottomPadding = meowNavHeight + navBarHeight
                contentView.setPadding(
                    contentView.paddingLeft,
                    contentView.paddingTop,
                    contentView.paddingRight,
                    totalBottomPadding
                )
                windowInsets
            }
        } catch (e: Exception) {
            Log.e("UtilityService", "Error setting up bottom nav padding", e)
        }
    }

    /**
     * Displays a beautiful, modern dialog informing the user they need to log in.
     * On Android 12+ (API 31+), it blurs the background.
     * On older versions, it uses the default system dimming.
     *
     * @param context The context from which the dialog is being shown (must be an Activity context).
     * @param onLoginClicked A lambda function to be executed when the user clicks the "Log In" button.
     */
    fun showLoginRequiredDialog(context: Context, onLoginClicked: () -> Unit) {
        // We need an Activity context to get the window's decor view.
        val activity = context as? Activity
        if (activity == null) {
            Log.e("UtilityService", "Context provided is not an Activity, cannot show dialog with blur.")
            // Fallback or just return if needed
            return
        }

        val rootView = activity.window.decorView.rootView

        // Apply blur effect ONLY on Android 12 (API 31) and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            applyBlurEffect(rootView)
        }

        // Inflate the custom layout using the context's layout inflater.
        val dialogView = LayoutInflater.from(context)
            .inflate(R.layout.dialog_login_required, null)

        // Build the dialog
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .create()

        // CRITICAL: Remove the blur effect when the dialog is dismissed for any reason.
        dialog.setOnDismissListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                clearBlurEffect(rootView)
            }
        }

        // --- Setup Listeners ---
        val loginButton = dialogView.findViewById<Button>(R.id.button_login)
        val cancelButton = dialogView.findViewById<TextView>(R.id.text_cancel)

        loginButton.setOnClickListener {
            // Dismissing the dialog will trigger the OnDismissListener to clear the blur.
            dialog.dismiss()
            onLoginClicked()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        // This is crucial to make the custom rounded background visible.
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCanceledOnTouchOutside(true) // User can dismiss by tapping outside

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyBlurEffect(rootView: View) {
        // radiusX and radiusY are the blur radii in pixels.
        // The tile mode determines how edges are handled.
        rootView.setRenderEffect(
            RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.MIRROR)
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun clearBlurEffect(rootView: View) {
        rootView.setRenderEffect(null)
    }

}