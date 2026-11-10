package com.example.nyamatsiapp

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var currentPasswordInput: TextInputEditText
    private lateinit var newPasswordInput: TextInputEditText
    private lateinit var confirmPasswordInput: TextInputEditText

    private lateinit var updateButton: MaterialButton
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_change_password)

        ScreenInsets.apply(
            this,
            findViewById(R.id.passwordRoot)
        )

        currentPasswordInput =
            findViewById(R.id.etCurrentPassword)

        newPasswordInput =
            findViewById(R.id.etNewPassword)

        confirmPasswordInput =
            findViewById(R.id.etConfirmPassword)

        updateButton =
            findViewById(R.id.btnSavePassword)

        statusText =
            findViewById(R.id.tvPasswordStatus)

        val passwordInfo =
            findViewById<TextView>(R.id.tvPasswordInfo)

        val passwordForm =
            findViewById<LinearLayout>(R.id.passwordForm)

        findViewById<MaterialButton>(
            R.id.btnBackFromPassword
        ).setOnClickListener {
            finish()
        }

        // Check login session.
        val user = FirebaseGate.auth().currentUser

        if (user == null) {
            Toast.makeText(
                this,
                "Please log in first.",
                Toast.LENGTH_LONG
            ).show()

            finish()
            return
        }

        // Check whether this account has an
        // email/password sign-in method.
        val supportsPassword = user.providerData.any {
            it.providerId == EmailAuthProvider.PROVIDER_ID
        }

        if (!supportsPassword) {

            passwordForm.visibility = View.GONE

            passwordInfo.text =
                "This account does not have an email/password " +
                        "sign-in method. If you use Google Sign-In, " +
                        "change your Google password in your " +
                        "Google Account security settings."

            return
        }

        // Allow email/password users to update
        // their password securely.
        updateButton.setOnClickListener {
            changePassword()
        }
    }

    // ======================================
    // CHANGE PASSWORD
    // ======================================

    private fun changePassword() {

        val user = FirebaseGate.auth().currentUser

        if (user == null) {
            showError("Your session has ended. Please log in again.")
            return
        }

        val email = user.email

        if (email.isNullOrBlank()) {
            showError("No email address is linked to this account.")
            return
        }

        val currentPassword =
            currentPasswordInput.text?.toString().orEmpty()

        val newPassword =
            newPasswordInput.text?.toString().orEmpty()

        val confirmPassword =
            confirmPasswordInput.text?.toString().orEmpty()

        // Clear previous errors.
        currentPasswordInput.error = null
        newPasswordInput.error = null
        confirmPasswordInput.error = null

        statusText.visibility = View.GONE

        // ======================================
        // VALIDATION
        // ======================================

        when {

            currentPassword.isBlank() -> {

                currentPasswordInput.error =
                    "Enter your current password"

                return
            }

            !Validation.password(newPassword) -> {

                newPasswordInput.error =
                    "Use at least 8 characters"

                return
            }

            newPassword == currentPassword -> {

                newPasswordInput.error =
                    "Choose a different password"

                return
            }

            newPassword != confirmPassword -> {

                confirmPasswordInput.error =
                    "Passwords do not match"

                return
            }
        }

        setLoading(true)

        // ======================================
        // VERIFY CURRENT PASSWORD
        // ======================================

        val credential =
            EmailAuthProvider.getCredential(
                email,
                currentPassword
            )

        user.reauthenticate(credential)
            .addOnCompleteListener reauthListener@{ reauthTask ->

                if (isFinishing || isDestroyed) {
                    return@reauthListener
                }

                if (!reauthTask.isSuccessful) {

                    setLoading(false)

                    currentPasswordInput.error =
                        "Could not verify current password"

                    showError(
                        "Current password verification failed. " +
                                "Check your password and internet connection."
                    )

                    return@reauthListener
                }

                // ======================================
                // UPDATE PASSWORD IN FIREBASE
                // ======================================

                user.updatePassword(newPassword)
                    .addOnCompleteListener updateListener@{ updateTask ->

                        if (isFinishing || isDestroyed) {
                            return@updateListener
                        }

                        setLoading(false)

                        if (updateTask.isSuccessful) {

                            // Do not retain passwords
                            // in the input fields.
                            currentPasswordInput.text?.clear()
                            newPasswordInput.text?.clear()
                            confirmPasswordInput.text?.clear()

                            Toast.makeText(
                                this,
                                "Password updated successfully.",
                                Toast.LENGTH_LONG
                            ).show()

                            finish()

                        } else {

                            val error = updateTask.exception

                            if (
                                error is FirebaseAuthWeakPasswordException
                            ) {
                                newPasswordInput.error =
                                    "Choose a stronger password"

                                showError(
                                    "Your new password does not meet " +
                                            "the account's password requirements."
                                )

                            } else {

                                showError(
                                    "Password update failed. " +
                                            "Please try again."
                                )
                            }
                        }
                    }
            }
    }

    private fun setLoading(isLoading: Boolean) {

        updateButton.isEnabled = !isLoading

        updateButton.text =
            if (isLoading) {
                "Updating..."
            } else {
                "Update Password"
            }
    }

    private fun showError(message: String) {

        statusText.text = message
        statusText.setTextColor(
            Color.parseColor("#B42318")
        )
        statusText.visibility = View.VISIBLE
    }
}