package com.example.nyamatsiapp

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // =====================================
        // CHECK FIREBASE AUTHENTICATION
        // =====================================

        if (
            !FirebaseGate.isConfigured(this) ||
            FirebaseGate.auth().currentUser == null
        ) {

            val loginIntent = Intent(
                this,
                LoginActivity::class.java
            ).apply {

                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(loginIntent)
            finish()
            return
        }

        // =====================================
        // LOAD MAIN APPLICATION
        // =====================================

        setContentView(R.layout.activity_main)

        // Handle the status bar, notch and
        // Android navigation area.
        ScreenInsets.apply(
            this,
            findViewById(R.id.mainRoot)
        )

        // =====================================
        // FLOATING NAVIGATION SETUP
        // =====================================

        val bottomNav =
            findViewById<BottomNavigationView>(
                R.id.bottomNav
            )

        // Remove any default background tint.
        bottomNav.backgroundTintList = null

        bottomNav.background =
            ColorDrawable(Color.TRANSPARENT)

        // Preserve the original navy/cyan PNG icons.
        bottomNav.itemIconTintList = null

        // Remove unwanted elevation.
        bottomNav.elevation = 0f

        // Always display all navigation labels.
        bottomNav.labelVisibilityMode =
            com.google.android.material.navigation
                .NavigationBarView.LABEL_VISIBILITY_LABELED

        // =====================================
        // HANDLE NAVIGATION
        // =====================================

        bottomNav.setOnItemSelectedListener { item ->

            when (item.itemId) {

                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }

                R.id.nav_bookings -> {
                    loadFragment(BookingsFragment())
                    true
                }

                R.id.nav_contact -> {
                    loadFragment(ContactFragment())
                    true
                }

                R.id.nav_settings -> {
                    loadFragment(SettingsFragment())
                    true
                }

                else -> false
            }
        }

        // =====================================
        // OPEN INITIAL PAGE
        // =====================================

        if (savedInstanceState == null) {

            val openTab =
                intent.getStringExtra("openTab")

            if (openTab == "bookings") {

                val bookingFragment =
                    BookingsFragment().apply {

                        arguments = Bundle().apply {

                            putString(
                                "service",
                                intent.getStringExtra("service")
                            )
                        }
                    }

                bottomNav.menu.findItem(
                    R.id.nav_bookings
                ).isChecked = true

                loadFragment(bookingFragment)

            } else {

                bottomNav.menu.findItem(
                    R.id.nav_home
                ).isChecked = true

                loadFragment(HomeFragment())
            }
        }
    }

    // =====================================
    // REPLACE MAIN SCREEN CONTENT
    // =====================================

    private fun loadFragment(fragment: Fragment) {

        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.fragmentContainer,
                fragment
            )
            .commit()
    }
}