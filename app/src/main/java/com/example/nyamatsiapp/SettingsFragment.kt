
package com.example.nyamatsiapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.credentials.CredentialManager
import androidx.credentials.ClearCredentialStateRequest

import com.google.android.material.button.MaterialButton
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.textfield.TextInputEditText

import android.widget.TextView

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

        // ========================================
        // 1. GET AUTHENTICATED CUSTOMER
        // ========================================

        val user = FirebaseGate.auth().currentUser

        if (user == null) {

            Toast.makeText(
                requireContext(),
                "Please log in to access settings",
                Toast.LENGTH_LONG
            ).show()

            return root
        }

        // ========================================
        // 2. CONNECT XML ELEMENTS
        // ========================================

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


        val helpButton =
            root.findViewById<MaterialButton>(
                R.id.btnHelp
            )

        val logoutButton =
            root.findViewById<MaterialButton>(
                R.id.btnLogout
            )

        // ========================================
        // 3. DISPLAY ACCOUNT INFORMATION
        // ========================================

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

        // ========================================
        // 4. LOAD PROFILE FROM FIRESTORE
        // ========================================

        val profileReference =
            FirebaseGate.db()
                .collection("users")
                .document(user.uid)

        saveButton.isEnabled = false

        profileReference.get()
            .addOnCompleteListener { task ->

                if (!isAdded || view !== root) {
                    return@addOnCompleteListener
                }

                saveButton.isEnabled = true

                if (task.isSuccessful) {

                    val document = task.result

                    if (document.exists()) {

                        firstNameInput.setText(
                            document.getString("firstName")
                                ?: firstNameInput.text?.toString().orEmpty()
                        )

                        lastNameInput.setText(
                            document.getString("lastName")
                                ?: lastNameInput.text?.toString().orEmpty()
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

        // ========================================
        // 5. SAVE PROFILE CHANGES
        // ========================================

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

                    // The UID and account email
                    // cannot be edited from Settings.
                    val profileUpdates = mapOf(
                        "userId" to user.uid,
                        "email" to user.email.orEmpty(),
                        "firstName" to firstName,
                        "lastName" to lastName,
                        "displayName" to fullName,
                        "phone" to phone
                    )

                    // Merge updates so that fields
                    // such as createdAt are retained.
                    profileReference
                        .set(
                            profileUpdates,
                            SetOptions.merge()
                        )
                        .addOnCompleteListener { saveTask ->

                            if (!isAdded || view !== root) {
                                return@addOnCompleteListener
                            }

                            if (!saveTask.isSuccessful) {

                                saveButton.isEnabled = true

                                showMessage(
                                    "Could not save your profile"
                                )

                                return@addOnCompleteListener
                            }

                            // Also update the name stored
                            // in Firebase Authentication.
                            val authProfile =
                                UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build()

                            user.updateProfile(authProfile)
                                .addOnCompleteListener { authTask ->

                                    if (!isAdded || view !== root) {
                                        return@addOnCompleteListener
                                    }

                                    saveButton.isEnabled = true

                                    if (authTask.isSuccessful) {

                                        showMessage(
                                            "Profile updated successfully"
                                        )

                                    } else {

                                        showMessage(
                                            "Profile saved in Firestore, " +
                                                    "but account display name " +
                                                    "could not be updated"
                                        )
                                    }
                                }
                        }
                }
            }
        }

        // ========================================
        // 6. SAVE APP PREFERENCES
        // ========================================

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

        // ========================================
        // 7. OPEN HELP AND SUPPORT
        // ========================================

        helpButton.setOnClickListener {

            val intent = Intent(
                requireContext(),
                HelpActivity::class.java
            )

            startActivity(intent)
        }

        // ========================================
        // 8. LOG OUT OF FIREBASE
        // ========================================

        logoutButton.setOnClickListener {

            logoutButton.isEnabled = false

            // End the Firebase Authentication session.
            FirebaseGate.auth().signOut()

            // Clear Credential Manager's session
            // to avoid retaining account selection state.
            lifecycleScope.launch {

                try {

                    CredentialManager
                        .create(requireContext())
                        .clearCredentialState(
                            ClearCredentialStateRequest()
                        )

                } catch (_: Exception) {

                    // Firebase has already been signed out.
                }

                if (isAdded) {

                    showMessage("Logged out successfully")

                    // Clear the activity back stack.
                    val intent = Intent(
                        requireContext(),
                        LoginActivity::class.java
                    )

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)

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
