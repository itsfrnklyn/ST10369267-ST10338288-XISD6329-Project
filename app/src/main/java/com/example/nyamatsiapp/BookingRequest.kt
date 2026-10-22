
package com.example.nyamatsiapp

data class BookingRequest(

    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val service: String,
    val preferredDate: String

) {

    // Check the booking information
    fun isValid(): Boolean {

        return Validation.booking(
            name,
            email,
            phone,
            address,
            service,
            preferredDate
        )
    }

    // Prepare booking data for Firestore
    fun toFirestoreMap(
        userId: String,
        serverTimestamp: Any
    ): Map<String, Any> {

        require(
            userId.isNotBlank() && isValid()
        ) {
            "Invalid booking or unauthenticated user"
        }

        return mapOf(

            "userId" to userId,

            "name" to name.trim(),

            "email" to email.trim(),

            "phone" to phone.trim(),

            "address" to address.trim(),

            "service" to service,

            "preferredDate" to preferredDate,

            "status" to "Pending",

            "createdAt" to serverTimestamp
        )
    }
}
