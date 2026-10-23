
package com.example.nyamatsiapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep our original registration design
        setContentView(R.layout.activity_register)

        // Connect Kotlin to the existing XML fields
        val etFirstName =
            findViewById<TextInputEditText>(R.id.etFirstName)

        val etLastName =
            findViewById<TextInputEditText>(R.id.etLastName)

        val etEmail =
            findViewById<TextInputEditText>(R.id.etRegEmail)

        val etPassword =
            findViewById<TextInputEditText>(R.id.etRegPassword)

        val btnRegister =
            findViewById<Button>(R.id.btnRegister)

        val tvGoToLogin =
            findViewById<TextView>(R.id.tvGoToLogin)

        // Check Firebase initialization
        if (!FirebaseGate.isConfigured(this)) {
            btnRegister.isEnabled = false
            showMessage("Firebase is not configured")
            return
        }

        // REGISTER BUTTON
        btnRegister.setOnClickListener {

            val firstName =
                etFirstName.text.toString().trim()

            val lastName =
                etLastName.text.toString().trim()

            val email =
                etEmail.text.toString().trim()

            // Do not trim the actual password
            val password =
                etPassword.text.toString()

            // Validate the entered information
            when {

                !Validation.name(firstName) -> {
                    etFirstName.error =
                        "Enter a valid first name"
                }

                !Validation.name(lastName) -> {
                    etLastName.error =
                        "Enter a valid last name"
                }

                !Validation.email(email) -> {
                    etEmail.error =
                        "Enter a valid email address"
                }

                !Validation.password(password) -> {
                    etPassword.error =
                        "Password must have at least 8 characters"
                }

                else -> {

                    // Prevent duplicate submissions
                    btnRegister.isEnabled = false

                    showMessage("Creating your account...")

                    // Create a real Firebase Auth account
                    FirebaseGate.auth()
                        .createUserWithEmailAndPassword(
                            email,
                            password
                        )
                        .addOnCompleteListener(this) { task ->

                            if (!task.isSuccessful) {

                                btnRegister.isEnabled = true

                                showMessage(
                                    task.exception?.localizedMessage
                                        ?: "Registration failed"
                                )

                                return@addOnCompleteListener
                            }

                            // Get newly registered user
                            val user =
                                FirebaseGate.auth().currentUser

                            if (user == null) {

                                btnRegister.isEnabled = true

                                showMessage(
                                    "Unable to retrieve account"
                                )

                                return@addOnCompleteListener
                            }

                            val fullName =
                                "$firstName $lastName"

                            // Save name in Firebase Authentication
                            val profileUpdates =
                                UserProfileChangeRequest.Builder()
                                    .setDisplayName(fullName)
                                    .build()

                            user.updateProfile(profileUpdates)

                            // Save customer details in Firestore
                            val customer = hashMapOf(
                                "userId" to user.uid,
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "displayName" to fullName,
                                "email" to email,
                                "phone" to "",
                                "createdAt" to
                                        FieldValue.serverTimestamp()
                            )

                            FirebaseGate.db()
                                .collection("users")
                                .document(user.uid)
                                .set(customer)
                                .addOnCompleteListener { saveTask ->

                                    btnRegister.isEnabled = true

                                    if (saveTask.isSuccessful) {

                                        showMessage(
                                            "Registration successful!"
                                        )

                                        openMainActivity()

                                    } else {

                                        // Account exists in Firebase Auth,
                                        // but the profile wasn't saved.
                                        FirebaseGate.auth().signOut()

                                        showMessage(
                                            "Account created, but profile " +
                                                    "could not be saved. " +
                                                    "Please contact support."
                                        )
                                    }
                                }
                        }
                }
            }
        }

        // RETURN TO LOGIN
        tvGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun openMainActivity() {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    private fun showMessage(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
