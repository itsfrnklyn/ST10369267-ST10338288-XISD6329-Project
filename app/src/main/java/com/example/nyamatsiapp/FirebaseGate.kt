
package com.example.nyamatsiapp

import android.content.Context

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseGate {

    // Check whether Firebase has been initialized
    fun isConfigured(context: Context): Boolean {

        return FirebaseApp.getApps(context).isNotEmpty()
    }

    // Get Firebase Authentication
    fun auth(): FirebaseAuth {

        return FirebaseAuth.getInstance()
    }

    // Get Cloud Firestore
    fun db(): FirebaseFirestore {

        return FirebaseFirestore.getInstance()
    }
}
