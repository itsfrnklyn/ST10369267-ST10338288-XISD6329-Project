
package com.example.nyamatsiapp

import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure the user is authenticated.
        if (
            !FirebaseGate.isConfigured(this) ||
            FirebaseGate.auth().currentUser == null
        ) {

            val intent = Intent(
                this,
                LoginActivity::class.java
            )

            intent.flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK

            startActivity(intent)
            finish()
            return
        }

        // Load our existing design.
        setContentView(R.layout.activity_main)

        val bottomNav =
            findViewById<BottomNavigationView>(
                R.id.bottomNav
            )

        // Preserve original navigation.
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

        if (savedInstanceState == null) {

            val openTab =
                intent.getStringExtra("openTab")


            if (openTab == "bookings") {

                val selectedService =
                    intent.getStringExtra("service")

                // Create the booking fragment.
                val bookingFragment = BookingsFragment()

                // Pass the selected service to it.
                val arguments = Bundle()

                arguments.putString(
                    "service",
                    selectedService
                )

                bookingFragment.arguments = arguments

                // Mark Bookings as the selected tab.
                bottomNav.menu.findItem(
                    R.id.nav_bookings
                ).isChecked = true

                // Open the booking form.
                loadFragment(bookingFragment)

            } else {

                loadFragment(HomeFragment())
            }

        }
    }

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
