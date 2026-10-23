
package com.example.nyamatsiapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope

import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.textfield.TextInputEditText

import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue

import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var btnLogin: Button
    private lateinit var btnGoogleLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        val etEmail =
            findViewById<TextInputEditText>(R.id.etEmail)

        val etPassword =
            findViewById<TextInputEditText>(R.id.etPassword)

        btnLogin =
            findViewById(R.id.btnLogin)

        btnGoogleLogin =
            findViewById(R.id.btnGoogleLogin)

        val tvGoToRegister =
            findViewById<TextView>(R.id.tvGoToRegister)

        // Make sure Firebase was initialized.
        if (!FirebaseGate.isConfigured(this)) {

            btnLogin.isEnabled = false
            btnGoogleLogin.isEnabled = false

            showMessage("Firebase configuration missing")
            return
        }

        // Reuse an existing authenticated session.
        if (FirebaseGate.auth().currentUser != null) {
            openMainActivity()
            return
        }

        // EMAIL AND PASSWORD LOGIN
        btnLogin.setOnClickListener {

            val email =
                etEmail.text.toString().trim()

            val password =
                etPassword.text.toString()

            when {

                !Validation.email(email) -> {
                    etEmail.error = "Enter a valid email"
                }

                password.isEmpty() -> {
                    etPassword.error = "Enter your password"
                }

                else -> {

                    btnLogin.isEnabled = false

                    FirebaseGate.auth()
                        .signInWithEmailAndPassword(
                            email,
                            password
                        )
                        .addOnCompleteListener(this) { task ->

                            btnLogin.isEnabled = true

                            if (task.isSuccessful) {

                                showMessage("Login successful")
                                openMainActivity()

                            } else {

                                showMessage(
                                    "Unable to sign in. " +
                                            "Check your credentials."
                                )
                            }
                        }
                }
            }
        }

        // GOOGLE SINGLE SIGN-ON
        btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }

        // OPEN REGISTRATION PAGE
        tvGoToRegister.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
        }
    }

    private fun signInWithGoogle() {

        // Retrieve the generated Google Web OAuth client ID.
        val clientIdResource = resources.getIdentifier(
            "default_web_client_id",
            "string",
            packageName
        )

        if (clientIdResource == 0) {

            showMessage(
                "Google Sign-In configuration is missing"
            )
            return
        }

        val webClientId = getString(clientIdResource)

        btnGoogleLogin.isEnabled = false

        lifecycleScope.launch {

            try {

                // Configure the Google account chooser.
                val googleIdOption =
                    GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(webClientId)
                        .build()

                val credentialRequest =
                    GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                // Display account selection on the phone.
                val credentialResult =
                    CredentialManager
                        .create(this@LoginActivity)
                        .getCredential(
                            this@LoginActivity,
                            credentialRequest
                        )

                val credential =
                    credentialResult.credential

                if (
                    credential !is CustomCredential ||
                    credential.type !=
                    GoogleIdTokenCredential
                        .TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {

                    btnGoogleLogin.isEnabled = true

                    showMessage(
                        "Unsupported Google credential"
                    )
                    return@launch
                }

                // Retrieve Google's ID token.
                val googleToken =
                    GoogleIdTokenCredential
                        .createFrom(credential.data)
                        .idToken

                // Exchange the Google token for Firebase credentials.
                val firebaseCredential =
                    GoogleAuthProvider.getCredential(
                        googleToken,
                        null
                    )

                FirebaseGate.auth()
                    .signInWithCredential(firebaseCredential)
                    .addOnCompleteListener(
                        this@LoginActivity
                    ) { authTask ->

                        if (!authTask.isSuccessful) {

                            btnGoogleLogin.isEnabled = true

                            showMessage(
                                "Google authentication failed"
                            )
                            return@addOnCompleteListener
                        }

                        val firebaseUser =
                            FirebaseGate.auth().currentUser

                        if (firebaseUser == null) {

                            btnGoogleLogin.isEnabled = true

                            showMessage(
                                "Unable to retrieve Google account"
                            )
                            return@addOnCompleteListener
                        }

                        // Create a profile for new Google users.
                        // Existing profiles must not be overwritten.
                        val profileReference =
                            FirebaseGate.db()
                                .collection("users")
                                .document(firebaseUser.uid)

                        profileReference.get()
                            .addOnCompleteListener { profileTask ->

                                if (!profileTask.isSuccessful) {

                                    btnGoogleLogin.isEnabled = true

                                    FirebaseGate.auth().signOut()

                                    showMessage(
                                        "Unable to access your profile"
                                    )
                                    return@addOnCompleteListener
                                }

                                if (profileTask.result.exists()) {

                                    btnGoogleLogin.isEnabled = true

                                    showMessage(
                                        "Google login successful"
                                    )
                                    openMainActivity()

                                } else {

                                    val displayName =
                                        firebaseUser.displayName.orEmpty()

                                    val nameParts =
                                        displayName.split(
                                            " ",
                                            limit = 2
                                        )

                                    val profile = hashMapOf(
                                        "userId" to firebaseUser.uid,
                                        "firstName" to
                                                nameParts.getOrElse(0) { "" },
                                        "lastName" to
                                                nameParts.getOrElse(1) { "" },
                                        "displayName" to displayName,
                                        "email" to
                                                firebaseUser.email.orEmpty(),
                                        "phone" to "",
                                        "createdAt" to
                                                FieldValue.serverTimestamp()
                                    )

                                    profileReference.set(profile)
                                        .addOnCompleteListener { saveTask ->

                                            btnGoogleLogin.isEnabled = true

                                            if (saveTask.isSuccessful) {

                                                showMessage(
                                                    "Google login successful"
                                                )

                                                openMainActivity()

                                            } else {

                                                FirebaseGate.auth()
                                                    .signOut()

                                                showMessage(
                                                    "Google account connected, " +
                                                            "but profile saving failed"
                                                )
                                            }
                                        }
                                }
                            }
                    }

            } catch (error: Exception) {

                btnGoogleLogin.isEnabled = true

                showMessage(
                    "Google Sign-In was cancelled " +
                            "or could not be completed"
                )
            }
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
