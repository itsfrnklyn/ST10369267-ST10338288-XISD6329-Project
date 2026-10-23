
package com.example.nyamatsiapp

import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.button.MaterialButton

class CleaningDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_cleaning_details
        )

        val btnBookNow =
            findViewById<MaterialButton>(
                R.id.btnBookNow
            )

        btnBookNow.setOnClickListener {

            val intent = Intent(
                this,
                MainActivity::class.java
            )

            // Open the bookings tab.
            intent.putExtra(
                "openTab",
                "bookings"
            )

            // Automatically select Cleaning.
            intent.putExtra(
                "service",
                "Cleaning"
            )

            startActivity(intent)
            finish()
        }
    }
}
