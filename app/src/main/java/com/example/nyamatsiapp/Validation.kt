
package com.example.nyamatsiapp

object Validation {

    // Check email address format
    private val emailPattern = Regex(
        "^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$"
    )

    fun email(value: String): Boolean {

        return value.length <= 254 &&
                emailPattern.matches(value.trim())
    }

    // Require at least 8 password characters
    fun password(value: String): Boolean {

        return value.length >= 8
    }

    // Check customer name
    fun name(value: String): Boolean {

        return value.trim().length in 2..80
    }

    // Check phone number format
    fun phone(value: String): Boolean {

        return Regex("^[+0-9][0-9 \\-]{8,19}$")
            .matches(value.trim())
    }

    // Check service address
    fun address(value: String): Boolean {

        return value.trim().length in 8..240
    }

    // Accept only available booking categories
    fun service(value: String): Boolean {

        return value == "Cleaning" ||
                value == "Construction"
    }

    // Check all required booking fields
    fun booking(
        name: String,
        email: String,
        phone: String,
        address: String,
        service: String,
        date: String
    ): Boolean {

        return name(name) &&
                email(email) &&
                phone(phone) &&
                address(address) &&
                service(service) &&
                Regex("^\\d{4}-\\d{2}-\\d{2}$")
                    .matches(date)
    }
}
