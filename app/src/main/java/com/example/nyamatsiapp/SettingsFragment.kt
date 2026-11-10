package com.example.nyamatsiapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.widget.SwitchCompat
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope

import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.SetOptions

import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val root = inflater.inflate(
            R.layout.fragment_settings,
            container,
            false
        )

        // ======================================
        // 1. AUTHENTICATED USER
        // ======================================

        val user = FirebaseGate.auth().currentUser

        if (user == null) {

            showMessage("Please log in to access Settings")

            return root
        }

        // ======================================
        // 2. CONNECT UI ELEMENTS
        // ======================================

        val firstNameInput =
            root.findViewById<TextInputEditText>(
                R.id.settingsFirst
            )

        val lastNameInput =
            root.findViewById<TextInputEditText>(
                R.id.settingsLast
            )

        val phoneInput =
            root.findViewById<TextInputEditText>(
                R.id.settingsPhone
            )

        val emailText =
            root.findViewById<TextView>(
                R.id.settingsEmail
            )

        val saveButton =
            root.findViewById<MaterialButton>(
                R.id.btnSaveSettings
            )

        val notificationsSwitch =
            root.findViewById<SwitchCompat>(
                R.id.switchNotifications
            )

        val changePasswordButton =
            root.findViewById<MaterialButton>(
                R.id.btnChangePassword
            )

        val adminButton =
            root.findViewById<MaterialButton>(
                R.id.btnAdminDashboard
            )

        val helpButton =
            root.findViewById<MaterialButton>(
                R.id.btnHelp
            )

        val logoutButton =
            root.findViewById<MaterialButton>(
                R.id.btnLogout
            )

        adminButton.visibility = View.GONE

        // ======================================
        // 3. DISPLAY ACCOUNT INFORMATION
        // ======================================

        emailText.text =
            "Signed in as: ${user.email ?: "Google account"}"

        val authNameParts =
            user.displayName.orEmpty()
                .trim()
                .split(" ", limit = 2)

        firstNameInput.setText(
            authNameParts.getOrElse(0) { "" }
        )

        lastNameInput.setText(
            authNameParts.getOrElse(1) { "" }
        )

        // ======================================
        // 4. LOAD PROFILE FROM FIRESTORE
        // ======================================

        val profileReference =
            FirebaseGate.db()
                .collection("users")
                .document(user.uid)

        saveButton.isEnabled = false

        profileReference.get()
            .addOnCompleteListener profileLoad@{ task ->

                if (!isAdded || view !== root) {
                    return@profileLoad
                }

                saveButton.isEnabled = true

                if (task.isSuccessful) {

                    val document = task.result

                    if (document.exists()) {

                        firstNameInput.setText(
                            document.getString("firstName")
                                ?: firstNameInput.text
                                    ?.toString().orEmpty()
                        )

                        lastNameInput.setText(
                            document.getString("lastName")
                                ?: lastNameInput.text
                                    ?.toString().orEmpty()
                        )

                        phoneInput.setText(
                            document.getString("phone").orEmpty()
                        )
                    }

                } else {

                    showMessage(
                        "Could not load your saved profile"
                    )
                }
            }

        // ======================================
        // 5. SAVE PROFILE
        // ======================================

        saveButton.setOnClickListener {

            val firstName =
                firstNameInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val lastName =
                lastNameInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            val phone =
                phoneInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

            when {

                !Validation.name(firstName) -> {

                    firstNameInput.error =
                        "Enter a valid first name"
                }

                !Validation.name(lastName) -> {

                    lastNameInput.error =
                        "Enter a valid last name"
                }

                phone.isNotBlank() &&
                        !Validation.phone(phone) -> {

                    phoneInput.error =
                        "Enter a valid phone number"
                }

                else -> {

                    saveButton.isEnabled = false

                    val fullName =
                        "$firstName $lastName"

                    val profileUpdates = mapOf(
                        "userId" to user.uid,
                        "email" to user.email.orEmpty(),
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "displayName" to fullName,
                        "phone" to phone
                    )

                    profileReference
                        .set(
                            profileUpdates,
                            SetOptions.merge()
                        )
                        .addOnCompleteListener profileSave@{ saveTask ->

                            if (!isAdded || view !== root) {
                                return@profileSave
                            }

                            if (!saveTask.isSuccessful) {

                                saveButton.isEnabled = true

                                showMessage(
                                    "Could not save your profile"
                                )

                                return@profileSave
                            }

                            val authProfile =
                                UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build()

                            user.updateProfile(authProfile)
                                .addOnCompleteListener authUpdate@{ authTask ->

                                    if (!isAdded || view !== root) {
                                        return@authUpdate
                                    }

                                    saveButton.isEnabled = true

                                    if (authTask.isSuccessful) {

                                        showMessage(
                                            "Profile updated successfully"
                                        )

                                    } else {

                                        showMessage(
                                            "Profile saved, but the account " +
                                                    "display name was not updated"
                                        )
                                    }
                                }
                        }
                }
            }
        }

        // ======================================
        // 6. NOTIFICATION PREFERENCES
        // ======================================

        val sharedPreferences =
            requireContext().getSharedPreferences(
                "NyamatsiPrefs_${user.uid}",
                Context.MODE_PRIVATE
            )

        notificationsSwitch.isChecked =
            sharedPreferences.getBoolean(
                "updateNotices",
                true
            )

        notificationsSwitch.setOnCheckedChangeListener {
                _, isEnabled ->

            sharedPreferences.edit()
                .putBoolean(
                    "updateNotices",
                    isEnabled
                )
                .apply()
        }

        // ======================================
        // 7. CHANGE PASSWORD
        // ======================================

        changePasswordButton.setOnClickListener {

            val intent = Intent(
                requireContext(),
                ChangePasswordActivity::class.java
            )

            startActivity(intent)
        }

        // ======================================
        // 8. ADMIN DASHBOARD
        // ======================================

        FirebaseGate.db()
            .collection("admins")
            .document(user.uid)
            .get()
            .addOnSuccessListener { adminDocument ->

                if (!isAdded || view !== root) {
                    return@addOnSuccessListener
                }

                val isAdmin =
                    adminDocument.exists() &&
                            adminDocument.getBoolean("active") == true

                adminButton.visibility =
                    if (isAdmin) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
            .addOnFailureListener {

                if (isAdded && view === root) {
                    adminButton.visibility = View.GONE
                }
            }

        adminButton.setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    AdminBookingsActivity::class.java
                )
            )
        }

        // ======================================
        // 9. HELP AND SUPPORT
        // ======================================

        helpButton.setOnClickListener {

            startActivity(
                Intent(
                    requireContext(),
                    HelpActivity::class.java
                )
            )
        }

        // ======================================
        // 10. LOG OUT
        // ======================================

        logoutButton.setOnClickListener {

            logoutButton.isEnabled = false

            FirebaseGate.auth().signOut()

            lifecycleScope.launch {

                try {

                    CredentialManager
                        .create(requireContext())
                        .clearCredentialState(
                            ClearCredentialStateRequest()
                        )

                } catch (_: Exception) {

                    // Firebase is already signed out.
                }

                if (isAdded) {

                    showMessage("Logged out successfully")

                    val loginIntent = Intent(
                        requireContext(),
                        LoginActivity::class.java
                    ).apply {

                        flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }

                    startActivity(loginIntent)

                    requireActivity().finish()
                }
            }
        }

        return root
    }

    private fun showMessage(message: String) {

        if (!isAdded) return

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}