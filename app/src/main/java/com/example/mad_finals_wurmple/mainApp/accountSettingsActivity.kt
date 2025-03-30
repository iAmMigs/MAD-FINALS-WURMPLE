package com.example.mad_finals_wurmple.mainApp

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.mad_finals_wurmple.R
import com.example.mad_finals_wurmple.auth.loginActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class accountSettingsActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // UI Elements
    private lateinit var changePassCard: CardView
    private lateinit var deleteAccCard: CardView
    private lateinit var changePassBtn: Button
    private lateinit var deleteAccBtn: Button
    private lateinit var currentPasswordInput: EditText
    private lateinit var newPasswordInput: EditText
    private lateinit var confirmPasswordInput: EditText
    private lateinit var updatePasswordButton: Button
    private lateinit var homeBtn: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.accountsettings_page)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize UI elements
        changePassCard = findViewById(R.id.changePassCard)
        deleteAccCard = findViewById(R.id.deleteAccCard)
        changePassBtn = findViewById(R.id.changePassBtn)
        deleteAccBtn = findViewById(R.id.deleteAccBtn)
        currentPasswordInput = findViewById(R.id.CurrentPassInput)
        newPasswordInput = findViewById(R.id.NewPassInput)
        confirmPasswordInput = findViewById(R.id.ConfirmPassInput)
        updatePasswordButton = findViewById(R.id.UpdatePassButton)
        homeBtn = findViewById(R.id.homeButton)

        // Setup tab switching functionality
        setupTabButtons()

        // Setup bottom navigation
        setupBottomNavigation()

        // Set up password update functionality
        setupPasswordUpdate()
    }

    private fun setupBottomNavigation() {
        // Handle home button click to navigate to dashboard
        homeBtn.setOnClickListener {
            val intent = Intent(this, dashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish() // Optional: close this activity
        }
    }

    private fun setupPasswordUpdate() {
        updatePasswordButton.setOnClickListener {
            val currentPassword = currentPasswordInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "New password and confirm password do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.isEmpty() || currentPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updatePassword(currentPassword, newPassword)
        }
    }

    private fun updatePassword(currentPassword: String, newPassword: String) {
        val user = auth.currentUser
        if (user != null && user.email != null) {
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

            user.reauthenticate(credential).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()

                            // Log out user and redirect to login screen after 2 seconds
                            auth.signOut()
                            android.os.Handler(Looper.getMainLooper()).postDelayed({
                                val intent = Intent(this, loginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                                finish() // Close account settings activity
                            }, 2000) // 2-second delay
                        } else {
                            Toast.makeText(this, "Failed to update password. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, "No user is logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabButtons() {
        // Initially show the change password card
        changePassCard.visibility = View.VISIBLE
        deleteAccCard.visibility = View.GONE

        // Set initial button styles
        setButtonActive(changePassBtn)
        setButtonInactive(deleteAccBtn)

        changePassBtn.setOnClickListener {
            // Show change password card
            changePassCard.visibility = View.VISIBLE
            deleteAccCard.visibility = View.GONE

            // Apply styles
            setButtonActive(changePassBtn)
            setButtonInactive(deleteAccBtn)

            // Optional: Add animation
            val fadeIn = AlphaAnimation(0f, 1f)
            fadeIn.duration = 300
            changePassCard.startAnimation(fadeIn)
        }

        deleteAccBtn.setOnClickListener {
            // Show delete account card
            changePassCard.visibility = View.GONE
            deleteAccCard.visibility = View.VISIBLE

            // Apply styles
            setButtonActive(deleteAccBtn)
            setButtonInactive(changePassBtn)

            // Optional: Add animation
            val fadeIn = AlphaAnimation(0f, 1f)
            fadeIn.duration = 300
            deleteAccCard.startAnimation(fadeIn)
        }
    }

    // Function to make the selected button prominent
    private fun setButtonActive(button: Button) {
        button.alpha = 1f // Full opacity
        button.scaleX = 1.0f
        button.scaleY = 1.0f
        button.elevation = 10f
    }

    // Function to make the inactive button slightly faded and smaller
    private fun setButtonInactive(button: Button) {
        button.alpha = 0.6f // Lower opacity
        button.scaleX = 0.9f // Slightly smaller
        button.scaleY = 0.9f // Slightly smaller
        button.elevation = 0f
    }
}
