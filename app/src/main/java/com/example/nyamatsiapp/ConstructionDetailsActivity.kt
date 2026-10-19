package com.example.nyamatsiapp // Make sure this matches your package name!

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ConstructionDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_construction_details)

        val btnBookNow = findViewById<MaterialButton>(R.id.btnBookNow)
        btnBookNow.setOnClickListener {
            // Go to MainActivity and tell it to open the Bookings tab
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("openTab", "bookings")
            startActivity(intent)
            finish()
        }
    }
}