
package com.example.nyamatsiapp

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast

import androidx.fragment.app.Fragment

import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration

import java.util.Calendar
import java.util.Locale

class BookingsFragment : Fragment() {

    // Keep a reference so we can stop listening
    // when the customer leaves this screen.
    private var bookingsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Load the existing booking page.
        val root = inflater.inflate(
            R.layout.fragment_bookings,
            container,
            false
        )

        // Retrieve the current Firebase user.
        val user = FirebaseGate.auth().currentUser

        if (user == null) {
            Toast.makeText(
                requireContext(),
                "Please log in to make a booking",
                Toast.LENGTH_LONG
            ).show()

            return root
        }

        // Connect the XML fields to Kotlin.
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

        val bookingHistory =
            root.findViewById<TextView>(
                R.id.bookingHistory
            )

        // -----------------------------------
        // SECTION 1: Prefill customer details
        // -----------------------------------

        nameInput.setText(user.displayName.orEmpty())
        emailInput.setText(user.email.orEmpty())

        // Load the phone number from Firestore,
        // if the customer has saved one.
        FirebaseGate.db()
            .collection("users")
            .document(user.uid)
            .get()
            .addOnSuccessListener { document ->

                if (!isAdded || view !== root) {
                    return@addOnSuccessListener
                }

                val phone =
                    document.getString("phone").orEmpty()

                if (phoneInput.text.isNullOrBlank()) {
                    phoneInput.setText(phone)
                }

                val displayName =
                    document.getString("displayName").orEmpty()

                if (
                    nameInput.text.isNullOrBlank() &&
                    displayName.isNotBlank()
                ) {
                    nameInput.setText(displayName)
                }
            }

        // -----------------------------------
        // SECTION 2: Service selection
        // -----------------------------------

        val services = listOf(
            "Cleaning",
            "Construction"
        )

        val serviceAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            services
        )

        serviceAdapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        serviceSpinner.adapter = serviceAdapter

        // Preselect service if customer came
        // from a Cleaning or Construction page.
        val selectedService =
            arguments?.getString("service")

        if (selectedService == "Construction") {
            serviceSpinner.setSelection(1)
        } else {
            serviceSpinner.setSelection(0)
        }

        // -----------------------------------
        // SECTION 3: Preferred date picker
        // -----------------------------------

        dateInput.setOnClickListener {

            val calendar = Calendar.getInstance()

            val datePicker = DatePickerDialog(
                requireContext(),

                { _, year, month, day ->

                    // Firestore uses yyyy-MM-dd format.
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

            // Do not allow past dates.
            val startOfToday = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            datePicker.datePicker.minDate =
                startOfToday.timeInMillis

            datePicker.show()
        }

        // -----------------------------------
        // SECTION 4: Submit a booking
        // -----------------------------------

        submitButton.setOnClickListener {

            val customerName =
                nameInput.text.toString().trim()

            val customerEmail =
                emailInput.text.toString().trim()

            val customerPhone =
                phoneInput.text.toString().trim()

            val customerAddress =
                addressInput.text.toString().trim()

            val service =
                serviceSpinner.selectedItem.toString()

            val preferredDate =
                dateInput.text.toString().trim()

            // Create a BookingRequest object
            // using the Phase 3 data class.
            val booking = BookingRequest(
                name = customerName,
                email = customerEmail,
                phone = customerPhone,
                address = customerAddress,
                service = service,
                preferredDate = preferredDate
            )

            // Validate each field.
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
                        "Enter the service address"
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

                    // Disable repeated submissions.
                    submitButton.isEnabled = false

                    bookingMessage.text =
                        "Submitting your booking..."

                    // Convert booking to Firestore data.
                    val bookingData = booking.toFirestoreMap(
                        userId = user.uid,
                        serverTimestamp =
                            FieldValue.serverTimestamp()
                    )

                    // Create a new booking document.
                    FirebaseGate.db()
                        .collection("bookings")
                        .add(bookingData)
                        .addOnCompleteListener { task ->

                            if (!isAdded || view !== root) {
                                return@addOnCompleteListener
                            }

                            submitButton.isEnabled = true

                            if (task.isSuccessful) {

                                val bookingId =
                                    task.result.id

                                bookingMessage.text =
                                    "Booking submitted successfully!"

                                showMessage(
                                    "Booking reference: " +
                                            bookingId.take(8)
                                )

                                // Clear only booking-specific inputs.
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

        // -----------------------------------
        // SECTION 5: Booking history
        // -----------------------------------

        bookingsListener = FirebaseGate.db()
            .collection("bookings")
            .whereEqualTo("userId", user.uid)
            .addSnapshotListener { snapshot, error ->

                if (!isAdded || view !== root) {
                    return@addSnapshotListener
                }

                if (error != null) {

                    bookingHistory.text =
                        "Could not load your bookings."

                    return@addSnapshotListener
                }

                val bookings = snapshot
                    ?.documents
                    .orEmpty()
                    .sortedByDescending { document ->
                        document.getTimestamp("createdAt")
                            ?.seconds ?: 0L
                    }

                if (bookings.isEmpty()) {

                    bookingHistory.text =
                        "You haven't submitted any bookings yet."

                } else {

                    val historyText =
                        bookings.joinToString("\n\n") { document ->

                            val serviceName =
                                document.getString("service")
                                    ?: "Unknown"

                            val preferredDate =
                                document.getString("preferredDate")
                                    ?: "-"

                            val status =
                                document.getString("status")
                                    ?: "Pending"

                            val reference =
                                document.id.take(8)

                            "Booking: $reference\n" +
                                    "Service: $serviceName\n" +
                                    "Date: $preferredDate\n" +
                                    "Status: $status"
                        }

                    bookingHistory.text = historyText
                }
            }

        return root
    }

    // Stop listening when leaving the page.
    override fun onDestroyView() {

        bookingsListener?.remove()
        bookingsListener = null

        super.onDestroyView()
    }

    private fun showMessage(message: String) {

        if (!isAdded) return

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
