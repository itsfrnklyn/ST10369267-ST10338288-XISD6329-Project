
package com.example.nyamatsiapp

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.button.MaterialButton

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Main scrollable container.
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F3F3F3"))
            isFillViewport = true
        }

        // Vertical content layout.
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 40, 40, 40)
        }

        // Page heading.
        val heading = TextView(this).apply {
            text = "Help and Support"
            textSize = 26f
            setTextColor(Color.parseColor("#333333"))
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 30)
        }

        content.addView(heading)

        // Instructions for customers.
        val instructions = TextView(this).apply {

            text = """
                NYAMATSI HOLDINGS
                USER GUIDE

                1. REGISTER AN ACCOUNT

                Open the registration page.

                Enter your first name, last name,
                email address and password.

                Press Register to create an account.

                2. LOG IN

                Enter your registered email address
                and password.

                Alternatively, choose Continue
                with Google to use your Google account.

                3. VIEW SERVICES

                Open the Home page.

                Select Cleaning or Construction
                to see the available services.

                4. MAKE A BOOKING

                Open the Bookings tab.

                Confirm your name and email.

                Enter your phone number and the
                address where the service is needed.

                Select Cleaning or Construction.

                Choose your preferred date.

                Press Submit Booking.

                5. TRACK BOOKINGS

                Scroll down to My Booking Requests.

                Your submitted bookings will appear
                with a booking reference and status.

                A Pending request means the booking
                has not yet been confirmed.

                6. UPDATE YOUR PROFILE

                Open Settings.

                Edit your first name, last name
                or phone number.

                Press Save Profile.

                7. LOG OUT

                Open Settings and press Log Out.

                This ends your Firebase session.

                8. CONTACT SUPPORT

                Open the Contact page to view
                Nyamatsi Holdings contact details.

                9. DATA AND PRIVACY

                Your account is managed through
                Firebase Authentication.

                Customer profiles and bookings
                are stored in Cloud Firestore.

                You must be signed in to access
                your personal booking information.

                10. INTERNET CONNECTION

                An internet connection is required
                to submit new booking requests
                and synchronise online information.
            """.trimIndent()

            textSize = 15f
            setTextColor(Color.parseColor("#333333"))
            setLineSpacing(8f, 1f)
        }

        content.addView(instructions)

        // Back button.
        val backButton = MaterialButton(this).apply {
            text = "Back to Settings"

            setTextColor(Color.WHITE)

            setBackgroundColor(
                Color.parseColor("#333333")
            )

            setOnClickListener {
                finish()
            }
        }

        val buttonParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 32
        }

        content.addView(backButton, buttonParams)

        scrollView.addView(content)

        setContentView(scrollView)
    }
}
