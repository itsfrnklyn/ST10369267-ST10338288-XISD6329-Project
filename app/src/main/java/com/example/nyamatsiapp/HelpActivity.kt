package com.example.nyamatsiapp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class HelpActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_help)

        ScreenInsets.apply(
            this,
            findViewById(R.id.helpRoot)
        )

        val container =
            findViewById<LinearLayout>(
                R.id.helpSectionsContainer
            )

        val sections = listOf(

            "01" to Pair(
                "Register an Account",
                "Open the registration page, enter your first " +
                        "name, last name, email address and password. " +
                        "Select Register to create an account."
            ),

            "02" to Pair(
                "Log In",
                "Enter your registered email address and password. " +
                        "You can also use the Google Sign-In option."
            ),

            "03" to Pair(
                "View Services",
                "Open the Home page and select Cleaning or " +
                        "Construction to view the available services."
            ),

            "04" to Pair(
                "Make a Booking",
                "Open Bookings, confirm your details, enter the " +
                        "service address, choose a service and preferred " +
                        "date, then press Submit Booking."
            ),

            "05" to Pair(
                "Track Your Bookings",
                "Scroll to My Booking Requests. Each booking has " +
                        "a reference number, preferred date and status. " +
                        "Pending means it is awaiting review."
            ),

            "06" to Pair(
                "Booking Statuses",
                "Approved means your request was accepted. " +
                        "Rejected means it was declined. Completed means " +
                        "the request has been marked as finished."
            ),

            "07" to Pair(
                "Update Your Profile",
                "Open Settings to update your first name, " +
                        "last name or phone number. Press Save Profile."
            ),

            "08" to Pair(
                "Log Out",
                "Open Settings and press Log Out to end your session."
            ),

            "09" to Pair(
                "Contact Support",
                "Open the Contact page to find Nyamatsi Holdings " +
                        "contact details."
            ),

            "10" to Pair(
                "Data and Internet",
                "Your account uses Firebase Authentication. " +
                        "Bookings are stored in Cloud Firestore. " +
                        "An internet connection is required for online " +
                        "booking submission and synchronisation."
            )
        )

        sections.forEach { (number, section) ->
            addHelpCard(
                container,
                number,
                section.first,
                section.second
            )
        }

        findViewById<MaterialButton>(
            R.id.btnBackFromHelp
        ).setOnClickListener {
            finish()
        }
    }

    private fun addHelpCard(
        parent: LinearLayout,
        number: String,
        title: String,
        description: String
    ) {

        val card = MaterialCardView(this).apply {
            radius = dp(17).toFloat()
            cardElevation = dp(2).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeColor = Color.parseColor("#E7E9EC")
            strokeWidth = dp(1)
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(12)
        }

        parent.addView(card, params)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(18), dp(17),
                dp(18), dp(18)
            )
        }

        card.addView(content)

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        content.addView(header)

        val numberText = TextView(this).apply {
            text = number
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#008FB8"))
            setPadding(
                dp(10), dp(7),
                dp(10), dp(7)
            )
            setBackgroundColor(
                Color.parseColor("#E7F7FC")
            )
        }

        header.addView(numberText)

        val titleText = TextView(this).apply {
            text = title
            textSize = 17f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#24292F"))
            setPadding(dp(12), 0, 0, 0)
        }

        header.addView(
            titleText,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val descriptionText = TextView(this).apply {
            text = description
            textSize = 15f
            setTextColor(Color.parseColor("#59636F"))
            setLineSpacing(dp(4).toFloat(), 1f)
            setPadding(0, dp(12), 0, 0)
        }

        content.addView(descriptionText)
    }

    private fun dp(value: Int): Int {
        return (
                value * resources.displayMetrics.density
                ).toInt()
    }
}