package com.example.nyamatsiapp

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source

class AdminBookingsActivity : AppCompatActivity() {

    private val database by lazy {
        FirebaseGate.db()
    }

    private var bookingListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null

    private lateinit var bookingsContainer: LinearLayout
    private lateinit var statsContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var manageUsersButton: MaterialButton

    private var bookings: List<DocumentSnapshot> = emptyList()
    private var totalUsers = 0
    private var usersLoaded = false
    private var bookingsLoaded = false

    private val navy = Color.rgb(0, 59, 112)
    private val cyan = Color.rgb(0, 141, 187)
    private val dark = Color.rgb(36, 41, 47)
    private val muted = Color.rgb(104, 113, 125)
    private val pageBackground = Color.rgb(244, 245, 247)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseGate.auth().currentUser

        if (currentUser == null) {
            finish()
            return
        }

        buildScreen()

        statusText.text = "Checking administrator access..."

        database.collection("admins")
            .document(currentUser.uid)
            .get(Source.SERVER)
            .addOnSuccessListener { admin ->

                if (isFinishing || isDestroyed) {
                    return@addOnSuccessListener
                }

                if (
                    !admin.exists() ||
                    admin.getBoolean("active") != true
                ) {
                    toast("Administrator access denied")
                    finish()
                    return@addOnSuccessListener
                }

                manageUsersButton.isEnabled = true
                statusText.text = "Loading dashboard..."

                listenForUsers()
                listenForBookings()
            }
            .addOnFailureListener {

                if (!isFinishing && !isDestroyed) {
                    toast("Could not verify administrator access")
                    finish()
                }
            }
    }

    private fun buildScreen() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(pageBackground)
        }

        // =====================================
        // NYAMATSI HEADER
        // =====================================

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(Color.WHITE)

            setPadding(
                dp(16), dp(10),
                dp(16), dp(10)
            )

            elevation = dp(3).toFloat()
        }

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.full_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = "Nyamatsi Holdings Logo"
        }

        header.addView(
            logo,
            LinearLayout.LayoutParams(
                dp(150),
                dp(64)
            )
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // =====================================
        // SCROLLABLE DASHBOARD
        // =====================================

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
        }

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(18), dp(24),
                dp(18), dp(40)
            )
        }

        scroll.addView(content)

        content.addView(
            createText(
                "Admin Dashboard",
                26f,
                dark,
                true
            )
        )

        content.addView(
            createText(
                "Nyamatsi Holdings • Management overview",
                14f,
                muted,
                false
            ).apply {
                setPadding(0, dp(6), 0, dp(20))
            }
        )

        val backButton = createButton(
            "Back to Application",
            dark
        )

        content.addView(backButton)

        backButton.setOnClickListener {
            finish()
        }

        statusText = createText(
            "Loading...",
            14f,
            muted,
            false
        ).apply {
            setPadding(0, dp(22), 0, dp(16))
        }

        content.addView(statusText)

        // =====================================
        // STATISTIC CARDS
        // =====================================

        statsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(statsContainer)

        // =====================================
        // MANAGE USERS
        // =====================================

        manageUsersButton = createButton(
            "View Registered Users",
            navy
        ).apply {
            isEnabled = false

            setOnClickListener {
                startActivity(
                    Intent(
                        this@AdminBookingsActivity,
                        AdminUsersActivity::class.java
                    )
                )
            }
        }

        content.addView(
            manageUsersButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply {
                topMargin = dp(16)
                bottomMargin = dp(28)
            }
        )

        // =====================================
        // BOOKING MANAGEMENT
        // =====================================

        content.addView(
            createText(
                "Booking Management",
                22f,
                dark,
                true
            )
        )

        content.addView(
            createText(
                "Review and update customer service requests.",
                14f,
                muted,
                false
            ).apply {
                setPadding(0, dp(8), 0, dp(18))
            }
        )

        bookingsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(bookingsContainer)

        setContentView(root)

        // Fix notch, status-bar and navigation-bar overlap.
        ScreenInsets.apply(this, root)

        renderStatistics()
    }

    private fun listenForUsers() {

        userListener?.remove()

        userListener = database
            .collection("users")
            .addSnapshotListener { snapshot, error ->

                if (isFinishing || isDestroyed) {
                    return@addSnapshotListener
                }

                if (error != null) {
                    usersLoaded = false

                    statusText.text =
                        "Unable to load user statistics"

                    renderStatistics()
                    return@addSnapshotListener
                }

                totalUsers = snapshot?.size() ?: 0
                usersLoaded = true

                renderStatistics()
                updateStatus()
            }
    }

    private fun listenForBookings() {

        bookingListener?.remove()

        bookingListener = database
            .collection("bookings")
            .addSnapshotListener { snapshot, error ->

                if (isFinishing || isDestroyed) {
                    return@addSnapshotListener
                }

                if (error != null) {
                    bookingsLoaded = false

                    statusText.text =
                        "Unable to load booking information"

                    bookingsContainer.removeAllViews()
                    renderStatistics()
                    return@addSnapshotListener
                }

                bookings = snapshot?.documents
                    .orEmpty()
                    .sortedByDescending {
                        it.getTimestamp("createdAt")
                            ?.seconds ?: 0L
                    }

                bookingsLoaded = true

                renderStatistics()
                renderBookings()
                updateStatus()
            }
    }

    private fun updateStatus() {

        if (usersLoaded && bookingsLoaded) {
            statusText.text = "Dashboard updated"
        }
    }

    private fun renderStatistics() {

        statsContainer.removeAllViews()

        val values = listOf(
            "Registered Users" to
                    if (usersLoaded) "$totalUsers" else "—",

            "Total Bookings" to
                    if (bookingsLoaded) "${bookings.size}" else "—",

            "Pending" to countBookings("Pending"),
            "Approved" to countBookings("Approved"),
            "Completed" to countBookings("Completed"),
            "Rejected" to countBookings("Rejected")
        )

        for (rowIndex in 0 until 3) {

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            statsContainer.addView(row)

            for (columnIndex in 0 until 2) {

                val statistic = values[
                    rowIndex * 2 + columnIndex
                ]

                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL

                    setPadding(
                        dp(16), dp(18),
                        dp(16), dp(18)
                    )

                    background = createBackground(
                        Color.WHITE,
                        18,
                        Color.rgb(224, 229, 235)
                    )
                }

                row.addView(
                    card,
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    ).apply {
                        bottomMargin = dp(12)

                        if (columnIndex == 0) {
                            rightMargin = dp(6)
                        } else {
                            leftMargin = dp(6)
                        }
                    }
                )

                card.addView(
                    createText(
                        statistic.first,
                        13f,
                        muted,
                        false
                    )
                )

                card.addView(
                    createText(
                        statistic.second,
                        27f,
                        navy,
                        true
                    ).apply {
                        setPadding(0, dp(12), 0, 0)
                    }
                )
            }
        }
    }

    private fun countBookings(status: String): String {

        if (!bookingsLoaded) {
            return "—"
        }

        return bookings.count {
            it.getString("status")
                ?.equals(status, ignoreCase = true) == true
        }.toString()
    }

    private fun renderBookings() {

        bookingsContainer.removeAllViews()

        if (bookings.isEmpty()) {

            bookingsContainer.addView(
                createText(
                    "No customer bookings available.",
                    15f,
                    muted,
                    false
                )
            )

            return
        }

        bookings.forEach { booking ->
            addBookingCard(booking)
        }
    }

    private fun addBookingCard(
        booking: DocumentSnapshot
    ) {

        val status =
            booking.getString("status") ?: "Pending"

        val name =
            booking.getString("name") ?: "Unknown Customer"

        val email =
            booking.getString("email") ?: "-"

        val phone =
            booking.getString("phone") ?: "-"

        val address =
            booking.getString("address") ?: "-"

        val service =
            booking.getString("service") ?: "-"

        val preferredDate =
            booking.getString("preferredDate") ?: "-"

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(18), dp(20),
                dp(18), dp(20)
            )

            background = createBackground(
                Color.WHITE,
                18,
                Color.rgb(224, 229, 235)
            )
        }

        bookingsContainer.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(18)
            }
        )

        card.addView(
            createText(
                name,
                19f,
                dark,
                true
            )
        )

        val statusColor = when (status.lowercase()) {
            "approved" -> Color.rgb(22, 105, 182)
            "completed" -> Color.rgb(28, 120, 66)
            "rejected" -> Color.rgb(180, 40, 40)
            else -> Color.rgb(161, 105, 13)
        }

        card.addView(
            createText(
                "Status: $status",
                14f,
                statusColor,
                true
            ).apply {
                setPadding(0, dp(8), 0, dp(14))
            }
        )

        card.addView(
            createText(
                "Reference: ${booking.id.take(8)}\n" +
                        "Email: $email\n" +
                        "Phone: $phone\n" +
                        "Service: $service\n" +
                        "Date: $preferredDate\n" +
                        "Address: $address",
                14f,
                dark,
                false
            ).apply {
                setLineSpacing(
                    dp(3).toFloat(),
                    1f
                )
                setPadding(0, 0, 0, dp(12))
            }
        )

        when (status.lowercase()) {

            "pending" -> {

                addBookingButton(
                    card,
                    "Approve Booking",
                    cyan
                ) {
                    confirmBookingUpdate(
                        booking.id,
                        "Pending",
                        "Approved"
                    )
                }

                addBookingButton(
                    card,
                    "Reject Booking",
                    Color.rgb(165, 49, 49)
                ) {
                    confirmBookingUpdate(
                        booking.id,
                        "Pending",
                        "Rejected"
                    )
                }
            }

            "approved" -> {

                addBookingButton(
                    card,
                    "Mark as Completed",
                    Color.rgb(28, 120, 66)
                ) {
                    confirmBookingUpdate(
                        booking.id,
                        "Approved",
                        "Completed"
                    )
                }
            }

            else -> {

                card.addView(
                    createText(
                        "No further actions available.",
                        13f,
                        muted,
                        false
                    ).apply {
                        setPadding(0, dp(10), 0, 0)
                    }
                )
            }
        }
    }

    // Add proper vertical spacing between action buttons.
    private fun addBookingButton(
        parent: LinearLayout,
        label: String,
        color: Int,
        action: () -> Unit
    ) {

        val button = createButton(label, color)

        parent.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply {
                topMargin = dp(12)
            }
        )

        button.setOnClickListener {
            action()
        }
    }

    private fun confirmBookingUpdate(
        bookingId: String,
        expectedStatus: String,
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

                updateBooking(
                    bookingId,
                    expectedStatus,
                    newStatus
                )
            }
            .show()
    }

    private fun updateBooking(
        bookingId: String,
        expectedStatus: String,
        newStatus: String
    ) {

        val reference = database
            .collection("bookings")
            .document(bookingId)

        database.runTransaction { transaction ->

            val document = transaction.get(reference)

            if (!document.exists()) {
                throw IllegalStateException(
                    "Booking no longer exists"
                )
            }

            val currentStatus =
                document.getString("status") ?: "Pending"

            if (!currentStatus.equals(
                    expectedStatus,
                    ignoreCase = true
                )
            ) {
                throw IllegalStateException(
                    "Booking status has already changed"
                )
            }

            transaction.update(
                reference,
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to
                            FieldValue.serverTimestamp()
                )
            )

            true

        }.addOnSuccessListener {

            toast("Booking updated to $newStatus")

        }.addOnFailureListener { error ->

            toast(
                error.localizedMessage
                    ?: "Unable to update booking"
            )
        }
    }

    private fun createButton(
        label: String,
        color: Int
    ): MaterialButton {

        return MaterialButton(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(color)
            cornerRadius = dp(14)
            textSize = 14f
        }
    }

    private fun createText(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean
    ): TextView {

        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)

            if (bold) {
                setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun createBackground(
        color: Int,
        radius: Int,
        strokeColor: Int? = null
    ): GradientDrawable {

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = dp(radius).toFloat()

            if (strokeColor != null) {
                setStroke(dp(1), strokeColor)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (
                value * resources.displayMetrics.density
                ).toInt()
    }

    private fun toast(value: String) {

        Toast.makeText(
            this,
            value,
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onDestroy() {

        bookingListener?.remove()
        userListener?.remove()

        bookingListener = null
        userListener = null

        super.onDestroy()
    }
}