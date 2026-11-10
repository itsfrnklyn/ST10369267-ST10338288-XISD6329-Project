package com.example.nyamatsiapp

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source

class AdminUsersActivity : AppCompatActivity() {

    private val database by lazy {
        FirebaseGate.db()
    }

    private var usersListener: ListenerRegistration? = null

    private lateinit var usersContainer: LinearLayout
    private lateinit var searchInput: EditText
    private lateinit var countText: TextView

    private var users: List<DocumentSnapshot> = emptyList()

    private val navy = Color.rgb(0, 59, 112)
    private val cyan = Color.rgb(0, 141, 187)
    private val dark = Color.rgb(36, 41, 47)
    private val muted = Color.rgb(104, 113, 125)
    private val backgroundColor = Color.rgb(244, 245, 247)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseGate.auth().currentUser

        if (currentUser == null) {
            finish()
            return
        }

        buildScreen()

        countText.text = "Checking administrator access..."

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
                    Toast.makeText(
                        this,
                        "Administrator access denied",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                    return@addOnSuccessListener
                }

                searchInput.isEnabled = true
                loadUsers()
            }
            .addOnFailureListener {

                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(
                        this,
                        "Unable to verify administrator access",
                        Toast.LENGTH_LONG
                    ).show()

                    finish()
                }
            }
    }

    private fun buildScreen() {

        // Main root includes the phone's system insets.
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(backgroundColor)
        }

        // Add Nyamatsi branding above the content.
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
                "Manage Users",
                26f,
                dark,
                true
            )
        )

        content.addView(
            createText(
                "View registered customer profiles",
                14f,
                muted,
                false
            ).apply {
                setPadding(0, dp(6), 0, dp(22))
            }
        )

        val backButton = MaterialButton(this).apply {
            text = "Back to Dashboard"
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(dark)
            cornerRadius = dp(14)

            setOnClickListener {
                finish()
            }
        }

        content.addView(
            backButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            )
        )

        countText = createText(
            "Loading registered users...",
            15f,
            muted,
            false
        ).apply {
            setPadding(0, dp(24), 0, dp(14))
        }

        content.addView(countText)

        searchInput = EditText(this).apply {
            hint = "Search by name, email or phone"
            textSize = 15f

            setSingleLine(true)
            setTextColor(dark)
            setHintTextColor(muted)

            setPadding(
                dp(16), dp(10),
                dp(16), dp(10)
            )

            minHeight = dp(56)

            background = createBackground(
                Color.WHITE,
                14,
                Color.rgb(174, 187, 200)
            )

            isEnabled = false
        }

        content.addView(
            searchInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(56)
            ).apply {
                bottomMargin = dp(22)
            }
        )

        usersContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        content.addView(usersContainer)

        searchInput.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    showUsers()
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {}
            }
        )

        setContentView(root)

        // Prevent overlap with the notch, status bar
        // and Android gesture navigation.
        ScreenInsets.apply(this, root)
    }

    private fun loadUsers() {

        usersListener?.remove()

        usersListener = database
            .collection("users")
            .addSnapshotListener { snapshot, error ->

                if (isFinishing || isDestroyed) {
                    return@addSnapshotListener
                }

                if (error != null) {
                    countText.text = "Unable to load users"
                    usersContainer.removeAllViews()
                    return@addSnapshotListener
                }

                users = snapshot?.documents
                    .orEmpty()
                    .sortedBy {
                        getUserName(it).lowercase()
                    }

                showUsers()
            }
    }

    private fun showUsers() {

        if (!::usersContainer.isInitialized) {
            return
        }

        usersContainer.removeAllViews()

        val query = searchInput.text
            ?.toString()
            ?.trim()
            ?.lowercase()
            .orEmpty()

        val filteredUsers = users.filter { user ->

            val searchable = listOf(
                getUserName(user),
                user.getString("email").orEmpty(),
                user.getString("phone").orEmpty()
            ).joinToString(" ").lowercase()

            searchable.contains(query)
        }

        countText.text =
            "${users.size} registered profiles • " +
                    "${filteredUsers.size} shown"

        if (filteredUsers.isEmpty()) {

            usersContainer.addView(
                createText(
                    "No matching users found.",
                    15f,
                    muted,
                    false
                )
            )

            return
        }

        filteredUsers.forEach { user ->
            addUserCard(user)
        }
    }

    private fun addUserCard(user: DocumentSnapshot) {

        val name = getUserName(user)

        val email =
            user.getString("email").orEmpty()
                .ifBlank { "Not provided" }

        val phone =
            user.getString("phone").orEmpty()
                .ifBlank { "Not provided" }

        val isPaused =
            user.getBoolean("bookingAccessPaused") == true

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(20), dp(20),
                dp(20), dp(20)
            )

            background = createBackground(
                Color.WHITE,
                18,
                Color.rgb(224, 229, 235)
            )
        }

        usersContainer.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
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

        card.addView(
            createText(
                "Email: $email\n" +
                        "Phone: $phone\n" +
                        "UID: ${user.id}\n" +
                        "Booking access: " +
                        if (isPaused) "Paused" else "Active",
                14f,
                muted,
                false
            ).apply {
                setPadding(
                    0, dp(12),
                    0, 0
                )
                setLineSpacing(
                    dp(3).toFloat(),
                    1f
                )
                setTextIsSelectable(true)
            }
        )

        // View-only design:
        // No edit, pause, delete or password buttons.
    }

    private fun getUserName(user: DocumentSnapshot): String {

        val firstName =
            user.getString("firstName").orEmpty()

        val lastName =
            user.getString("lastName").orEmpty()

        val fullName =
            "$firstName $lastName".trim()

        if (fullName.isNotBlank()) {
            return fullName
        }

        return user.getString("displayName")
            ?.takeIf { it.isNotBlank() }
            ?: "Unnamed Customer"
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

    override fun onDestroy() {
        usersListener?.remove()
        usersListener = null
        super.onDestroy()
    }
}