package com.example.nyamatsiapp

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source

class AdminBookingsActivity : AppCompatActivity() {

    private lateinit var bookingsContainer: LinearLayout
    private lateinit var summaryText: TextView

    private var bookingListener: ListenerRegistration? = null

    private val database by lazy {
        FirebaseGate.db()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check whether a Firebase user is signed in.
        val currentUser = FirebaseGate.auth().currentUser

        if (currentUser == null) {
            finish()
            return
        }

        buildScreen()

        summaryText.text = "Checking administrator access..."

        // Read from the server to avoid accepting a
        // potentially outdated cached administrator role.
        database.collection("admins")
            .document(currentUser.uid)
            .get(Source.SERVER)
            .addOnSuccessListener { document ->

                if (isFinishing || isDestroyed) {
                    return@addOnSuccessListener
                }

                if (
                    !document.exists() ||
                    document.getBoolean("active") != true
                ) {
                    Toast.makeText(
                        this,
                        "Administrator access denied",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@addOnSuccessListener
                }

                summaryText.text = "Loading bookings..."
                loadBookings()
            }
            .addOnFailureListener {

                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(
                        this,
                        "Could not verify administrator access",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }
            }
    }

    private fun buildScreen() {

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(243, 243, 243))
            isFillViewport = true
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(18), dp(20),
                dp(18), dp(24)
            )
        }

        scrollView.addView(mainLayout)

        val heading = TextView(this).apply {
            text = "Admin Booking Management"
            textSize = 24f
            setTextColor(Color.rgb(45, 45, 45))
            setTypeface(null, 1)
        }

        mainLayout.addView(heading)

        val description = TextView(this).apply {
            text = "Review and manage customer service requests"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(8), 0, dp(12))
        }

        mainLayout.addView(description)

        val backButton = Button(this).apply {
            text = "Back to Application"
            setOnClickListener {
                finish()
            }
        }

        mainLayout.addView(backButton)

        summaryText = TextView(this).apply {
            text = "Loading..."
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, dp(20), 0, dp(14))
        }

        mainLayout.addView(summaryText)

        bookingsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        mainLayout.addView(bookingsContainer)

        setContentView(scrollView)
    }

    private fun loadBookings() {

        bookingListener?.remove()

        bookingListener = database
            .collection("bookings")
            .addSnapshotListener { snapshot, error ->

                if (isFinishing || isDestroyed) {
                    return@addSnapshotListener
                }

                if (error != null) {
                    summaryText.text =
                        "Unable to load bookings: ${error.localizedMessage}"
                    bookingsContainer.removeAllViews()
                    return@addSnapshotListener
                }

                val bookings = snapshot
                    ?.documents
                    .orEmpty()
                    .sortedByDescending {
                        it.getTimestamp("createdAt")
                            ?.seconds ?: 0L
                    }

                showBookings(bookings)
            }
    }

    private fun showBookings(
        bookings: List<DocumentSnapshot>
    ) {

        bookingsContainer.removeAllViews()

        val pending = bookings.count {
            it.getString("status") == "Pending"
        }

        val approved = bookings.count {
            it.getString("status") == "Approved"
        }

        val completed = bookings.count {
            it.getString("status") == "Completed"
        }

        val rejected = bookings.count {
            it.getString("status") == "Rejected"
        }

        summaryText.text =
            "Total: ${bookings.size}  |  Pending: $pending\n" +
                    "Approved: $approved  |  Completed: $completed\n" +
                    "Rejected: $rejected"

        if (bookings.isEmpty()) {

            val emptyText = TextView(this).apply {
                text = "No customer bookings available."
                textSize = 16f
                setTextColor(Color.DKGRAY)
            }

            bookingsContainer.addView(emptyText)
            return
        }

        bookings.forEach { document ->
            addBookingCard(document)
        }
    }

    private fun addBookingCard(
        document: DocumentSnapshot
    ) {

        val status =
            document.getString("status") ?: "Pending"

        val customerName =
            document.getString("name") ?: "Unknown"

        val email =
            document.getString("email") ?: "-"

        val phone =
            document.getString("phone") ?: "-"

        val address =
            document.getString("address") ?: "-"

        val service =
            document.getString("service") ?: "-"

        val preferredDate =
            document.getString("preferredDate") ?: "-"

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(16), dp(16),
                dp(16), dp(16)
            )

            setBackgroundColor(Color.WHITE)

            elevation = dp(3).toFloat()
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(14)
        }

        bookingsContainer.addView(card, params)

        val info = TextView(this).apply {

            text =
                "Booking: ${document.id.take(8)}\n" +
                        "Customer: $customerName\n" +
                        "Email: $email\n" +
                        "Phone: $phone\n" +
                        "Address: $address\n" +
                        "Service: $service\n" +
                        "Preferred Date: $preferredDate\n" +
                        "Status: $status"

            textSize = 15f
            setTextColor(Color.rgb(40, 40, 40))
            setLineSpacing(dp(3).toFloat(), 1f)
        }

        card.addView(info)

        when (status) {

            "Pending" -> {

                addActionButton(
                    card,
                    "Approve Booking"
                ) {
                    confirmStatusChange(
                        document.id,
                        "Approved"
                    )
                }

                addActionButton(
                    card,
                    "Reject Booking"
                ) {
                    confirmStatusChange(
                        document.id,
                        "Rejected"
                    )
                }
            }

            "Approved" -> {

                addActionButton(
                    card,
                    "Mark as Completed"
                ) {
                    confirmStatusChange(
                        document.id,
                        "Completed"
                    )
                }
            }

            else -> {

                val finalText = TextView(this).apply {
                    text = "No further actions available."
                    textSize = 13f
                    setTextColor(Color.GRAY)
                    setPadding(0, dp(10), 0, 0)
                }

                card.addView(finalText)
            }
        }
    }

    private fun addActionButton(
        parent: LinearLayout,
        title: String,
        onAction: () -> Unit
    ) {

        val button = Button(this).apply {
            text = title
            setOnClickListener {
                onAction()
            }
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(8)
        }

        parent.addView(button, params)
    }

    private fun confirmStatusChange(
        bookingId: String,
        newStatus: String
    ) {

        AlertDialog.Builder(this)
            .setTitle("Confirm Booking Update")
            .setMessage(
                "Change booking ${bookingId.take(8)} " +
                        "to $newStatus?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->
                updateBookingStatus(
                    bookingId,
                    newStatus
                )
            }
            .show()
    }

    private fun updateBookingStatus(
        bookingId: String,
        newStatus: String
    ) {

        database.collection("bookings")
            .document(bookingId)
            .update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to
                            FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {

                Toast.makeText(
                    this,
                    "Booking updated to $newStatus",
                    Toast.LENGTH_LONG
                ).show()
            }
            .addOnFailureListener { error ->

                Toast.makeText(
                    this,
                    "Update failed: ${error.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun dp(value: Int): Int {
        return (
                value * resources.displayMetrics.density
                ).toInt()
    }

    override fun onDestroy() {
        bookingListener?.remove()
        bookingListener = null
        super.onDestroy()
    }
}