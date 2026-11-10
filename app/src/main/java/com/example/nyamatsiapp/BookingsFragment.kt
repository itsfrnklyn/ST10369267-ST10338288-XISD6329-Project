package com.example.nyamatsiapp

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.fragment.app.Fragment

import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration

import java.util.Calendar
import java.util.Locale

class BookingsFragment : Fragment() {

    private var bookingsListener: ListenerRegistration? = null

    // ============================================
    // 1. CREATE BOOKING SCREEN
    // ============================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val root = inflater.inflate(
            R.layout.fragment_bookings,
            container,
            false
        )

        val user = FirebaseGate.auth().currentUser

        if (user == null) {
            showMessage("Please log in to make a booking")
            return root
        }

        // ============================================
        // 2. CONNECT XML FIELDS
        // ============================================

        val nameInput =
            root.findViewById<TextInputEditText>(
                R.id.bookName
            )

        val emailInput =
            root.findViewById<TextInputEditText>(
                R.id.bookEmail
            )

        val phoneInput =
            root.findViewById<TextInputEditText>(
                R.id.bookPhone
            )

        val addressInput =
            root.findViewById<TextInputEditText>(
                R.id.bookAddress
            )

        val dateInput =
            root.findViewById<TextInputEditText>(
                R.id.bookDate
            )

        val serviceSpinner =
            root.findViewById<Spinner>(
                R.id.bookService
            )

        val submitButton =
            root.findViewById<Button>(
                R.id.btnSubmitBooking
            )

        val bookingMessage =
            root.findViewById<TextView>(
                R.id.bookingMessage
            )

        val historyContainer =
            root.findViewById<LinearLayout>(
                R.id.layoutBookingRequestsContainer
            )

        // ============================================
        // 3. PREFILL CUSTOMER INFORMATION
        // ============================================

        nameInput.setText(
            user.displayName.orEmpty()
        )

        emailInput.setText(
            user.email.orEmpty()
        )

        // Retrieve additional customer information.
        FirebaseGate.db()
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!isAdded || view !== root) {
                    return@addOnSuccessListener
                }

                val savedPhone =
                    document.getString("phone").orEmpty()

                val savedName =
                    document.getString("displayName").orEmpty()

                if (
                    phoneInput.text.isNullOrBlank() &&
                    savedPhone.isNotBlank()
                ) {
                    phoneInput.setText(savedPhone)
                }

                if (
                    nameInput.text.isNullOrBlank() &&
                    savedName.isNotBlank()
                ) {
                    nameInput.setText(savedName)
                }
            }

        // ============================================
        // 4. SERVICE DROPDOWN
        // ============================================

        val services = listOf(
            "Cleaning",
            "Construction"
        )

        // Custom layout ensures text remains visible.
        val serviceAdapter = ArrayAdapter(
            requireContext(),
            R.layout.service_spinner_item,
            services
        )

        serviceAdapter.setDropDownViewResource(
            R.layout.service_spinner_dropdown
        )

        serviceSpinner.adapter = serviceAdapter

        // Automatically select service if the user
        // came from Cleaning or Construction details.
        val selectedService =
            arguments?.getString("service")

        serviceSpinner.setSelection(
            if (selectedService == "Construction") 1 else 0
        )

        // ============================================
        // 5. PREFERRED DATE PICKER
        // ============================================

        // The XML now uses a separate date label.
        // There is only one hint inside the field.

        dateInput.setOnClickListener {

            val calendar = Calendar.getInstance()

            val datePicker = DatePickerDialog(
                requireContext(),

                { _, year, month, day ->

                    val selectedDate = String.format(
                        Locale.US,
                        "%04d-%02d-%02d",
                        year,
                        month + 1,
                        day
                    )

                    dateInput.setText(selectedDate)
                },

                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )

            // Prevent selecting dates before today.
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            datePicker.datePicker.minDate =
                today.timeInMillis

            datePicker.show()
        }

        // ============================================
        // 6. SUBMIT BOOKING
        // ============================================

        submitButton.setOnClickListener {

            val customerName =
                nameInput.text.toString().trim()

            val customerEmail =
                emailInput.text.toString().trim()

            val customerPhone =
                phoneInput.text.toString().trim()

            val customerAddress =
                addressInput.text.toString().trim()

            val selectedBookingService =
                serviceSpinner.selectedItem.toString()

            val preferredDate =
                dateInput.text.toString().trim()

            val booking = BookingRequest(
                name = customerName,
                email = customerEmail,
                phone = customerPhone,
                address = customerAddress,
                service = selectedBookingService,
                preferredDate = preferredDate
            )

            when {

                !Validation.name(customerName) -> {

                    nameInput.error =
                        "Enter your full name"
                }

                !Validation.email(customerEmail) -> {

                    emailInput.error =
                        "Enter a valid email address"
                }

                !Validation.phone(customerPhone) -> {

                    phoneInput.error =
                        "Enter a valid phone number"
                }

                !Validation.address(customerAddress) -> {

                    addressInput.error =
                        "Enter a valid service address"
                }

                preferredDate.isBlank() -> {

                    dateInput.error =
                        "Please select a date"

                    showMessage(
                        "Select your preferred booking date"
                    )
                }

                !booking.isValid() -> {

                    showMessage(
                        "Please check your booking information"
                    )
                }

                else -> {

                    submitButton.isEnabled = false

                    bookingMessage.text =
                        "Submitting your booking..."

                    val bookingData =
                        booking.toFirestoreMap(
                            userId = user.uid,
                            serverTimestamp =
                                FieldValue.serverTimestamp()
                        )

                    FirebaseGate.db()
                        .collection("bookings")
                        .add(bookingData)
                        .addOnCompleteListener { task ->

                            if (!isAdded || view !== root) {
                                return@addOnCompleteListener
                            }

                            submitButton.isEnabled = true

                            if (task.isSuccessful) {

                                val bookingReference =
                                    task.result.id.take(8)

                                bookingMessage.text =
                                    "Booking submitted successfully!"

                                showMessage(
                                    "Booking reference: $bookingReference"
                                )

                                // Keep customer details.
                                // Clear booking-specific fields.
                                addressInput.setText("")
                                dateInput.setText("")

                            } else {

                                bookingMessage.text =
                                    "Booking could not be submitted."

                                showMessage(
                                    task.exception
                                        ?.localizedMessage
                                        ?: "Please try again"
                                )
                            }
                        }
                }
            }
        }

        // ============================================
        // 7. LOAD BOOKING HISTORY FROM FIRESTORE
        // ============================================

        bookingsListener = FirebaseGate.db()
            .collection("bookings")
            .whereEqualTo(
                "userId",
                user.uid
            )
            .addSnapshotListener { snapshot, error ->

                if (!isAdded || view !== root) {
                    return@addSnapshotListener
                }

                if (error != null) {

                    showHistoryMessage(
                        historyContainer,
                        "Could not load your bookings."
                    )

                    return@addSnapshotListener
                }

                val bookings =
                    snapshot?.documents
                        .orEmpty()
                        .sortedByDescending { document ->

                            document
                                .getTimestamp("createdAt")
                                ?.seconds ?: 0L
                        }

                renderBookingHistory(
                    historyContainer,
                    bookings
                )
            }

        return root
    }

    // ============================================
    // 8. DISPLAY BOOKING HISTORY CARDS
    // ============================================

    private fun renderBookingHistory(
        container: LinearLayout,
        bookings: List<DocumentSnapshot>
    ) {

        container.removeAllViews()

        if (bookings.isEmpty()) {

            showHistoryMessage(
                container,
                "No booking requests yet.\nYour bookings will appear here."
            )

            return
        }

        bookings.forEach { document ->

            val bookingCard =
                createBookingCard(document)

            container.addView(bookingCard)
        }
    }

    // ============================================
    // 9. CREATE INDIVIDUAL BOOKING CARD
    // ============================================

    private fun createBookingCard(
        document: DocumentSnapshot
    ): MaterialCardView {

        val bookingService =
            document.getString("service")
                ?: "Service Request"

        val bookingStatus =
            document.getString("status")
                ?: "Pending"

        val preferredDate =
            document.getString("preferredDate")
                ?: "-"

        val bookingReference =
            document.id.take(8)

        // Card background.
        val card = MaterialCardView(
            requireContext()
        ).apply {

            radius = dp(18).toFloat()

            cardElevation = dp(2).toFloat()

            setCardBackgroundColor(
                Color.WHITE
            )

            strokeColor =
                Color.parseColor("#E6E9ED")

            strokeWidth = dp(1)
        }

        card.layoutParams =
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {

                bottomMargin = dp(14)
            }

        // Content inside card.
        val content = LinearLayout(
            requireContext()
        ).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(18),
                dp(18),
                dp(18),
                dp(18)
            )
        }

        card.addView(content)

        // =====================================
        // CARD HEADER
        // =====================================

        val header = LinearLayout(
            requireContext()
        ).apply {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL
        }

        content.addView(header)

        // Service title.
        val serviceTitle = TextView(
            requireContext()
        ).apply {

            text = bookingService

            textSize = 17f

            setTypeface(
                null,
                Typeface.BOLD
            )

            setTextColor(
                Color.parseColor("#24292F")
            )
        }

        header.addView(
            serviceTitle,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        // Booking status badge.
        val statusBadge =
            createStatusBadge(bookingStatus)

        header.addView(statusBadge)

        // Booking reference.
        val referenceText = createText(
            "Booking #$bookingReference",
            13f,
            "#76808B"
        ).apply {

            setPadding(
                0,
                dp(10),
                0,
                0
            )
        }

        content.addView(referenceText)

        // =====================================
        // DIVIDER
        // =====================================

        val divider = View(
            requireContext()
        ).apply {

            setBackgroundColor(
                Color.parseColor("#EAECEF")
            )
        }

        content.addView(
            divider,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {

                topMargin = dp(14)
                bottomMargin = dp(12)
            }
        )

        // Preferred date label.
        val dateLabel = createText(
            "Preferred Date",
            12f,
            "#76808B"
        )

        content.addView(dateLabel)

        // Preferred date value.
        val dateValue = createText(
            preferredDate,
            15f,
            "#24292F"
        ).apply {

            setTypeface(
                null,
                Typeface.BOLD
            )

            setPadding(
                0,
                dp(4),
                0,
                0
            )
        }

        content.addView(dateValue)

        // =====================================
        // STATUS DESCRIPTION
        // =====================================

        val statusDescription = when (bookingStatus) {

            "Approved" ->
                "Your booking has been approved."

            "Rejected" ->
                "Your booking request was declined."

            "Completed" ->
                "This service request has been completed."

            else ->
                "Your booking is awaiting review."
        }

        val descriptionText = createText(
            statusDescription,
            13f,
            "#68717D"
        ).apply {

            setPadding(
                0,
                dp(12),
                0,
                0
            )
        }

        content.addView(descriptionText)

        return card
    }

    // ============================================
    // 10. CREATE COLOURED STATUS BADGE
    // ============================================

    private fun createStatusBadge(
        status: String
    ): TextView {

        val badgeBackgroundColor = when (status) {

            "Approved" -> "#E4F4E8"

            "Rejected" -> "#FCE8E8"

            "Completed" -> "#E6EEFF"

            else -> "#FFF3D9"
        }

        val badgeTextColor = when (status) {

            "Approved" -> "#217A3C"

            "Rejected" -> "#B42318"

            "Completed" -> "#2858B8"

            else -> "#996300"
        }

        val badgeDrawable =
            GradientDrawable().apply {

                cornerRadius =
                    dp(30).toFloat()

                setColor(
                    Color.parseColor(
                        badgeBackgroundColor
                    )
                )
            }

        return TextView(
            requireContext()
        ).apply {

            text = status

            textSize = 12f

            setTypeface(
                null,
                Typeface.BOLD
            )

            setTextColor(
                Color.parseColor(
                    badgeTextColor
                )
            )

            setPadding(
                dp(11),
                dp(7),
                dp(11),
                dp(7)
            )

            setBackground(badgeDrawable)
        }
    }

    // ============================================
    // 11. HELPER FOR TEXT ELEMENTS
    // ============================================

    private fun createText(
        value: String,
        size: Float,
        textColor: String
    ): TextView {

        return TextView(
            requireContext()
        ).apply {

            text = value

            textSize = size

            setTextColor(
                Color.parseColor(textColor)
            )
        }
    }

    // ============================================
    // 12. EMPTY / ERROR HISTORY MESSAGE
    // ============================================

    private fun showHistoryMessage(
        container: LinearLayout,
        message: String
    ) {

        container.removeAllViews()

        val card = MaterialCardView(
            requireContext()
        ).apply {

            radius = dp(16).toFloat()

            setCardBackgroundColor(
                Color.WHITE
            )
        }

        val messageText = TextView(
            requireContext()
        ).apply {

            text = message

            textSize = 14f

            setTextColor(
                Color.parseColor("#68717D")
            )

            gravity = Gravity.CENTER

            setPadding(
                dp(18),
                dp(30),
                dp(18),
                dp(30)
            )
        }

        card.addView(messageText)

        container.addView(card)
    }

    // ============================================
    // 13. CONVERT DP TO PIXELS
    // ============================================

    private fun dp(value: Int): Int {

        return (
                value * resources.displayMetrics.density
                ).toInt()
    }

    // ============================================
    // 14. SHOW TOAST MESSAGE
    // ============================================

    private fun showMessage(
        message: String
    ) {

        if (!isAdded) return

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    // ============================================
    // 15. CLEAN UP FIRESTORE LISTENER
    // ============================================

    override fun onDestroyView() {

        bookingsListener?.remove()

        bookingsListener = null

        super.onDestroyView()
    }
}