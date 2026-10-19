package com.example.nyamatsiapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        val openTab = intent.getStringExtra("openTab")

        if (openTab == "bookings") {
            bottomNav.selectedItemId = R.id.nav_bookings
            loadFragment(BookingsFragment())
        } else if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { loadFragment(HomeFragment()); true }
                R.id.nav_bookings -> { loadFragment(BookingsFragment()); true }
                R.id.nav_contact -> { loadFragment(ContactFragment()); true }
                R.id.nav_settings -> { loadFragment(SettingsFragment()); true } // MUST BE HERE
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}