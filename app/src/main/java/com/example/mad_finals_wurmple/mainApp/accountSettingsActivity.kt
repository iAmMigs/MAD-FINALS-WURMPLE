package com.example.mad_finals_wurmple.mainApp
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.mad_finals_wurmple.R
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
            // Clear back stack so user can't go back to account settings with back button
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish() // Optional: close this activity
        }

        // Add other bottom navigation buttons here as needed
    }

    private fun setupPasswordUpdate() {
        // Update password logic
        updatePasswordButton.setOnClickListener {
            val currentPassword = currentPasswordInput.text.toString().trim()
            val newPassword = newPasswordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "New password and confirm password do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.isEmpty()) {
                Toast.makeText(this, "Please enter a new password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentPassword.isEmpty()) {
                Toast.makeText(this, "Please enter your current password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                if (newPassword.length < 6) {
                    throw IllegalArgumentException("Password must be at least 6 characters long")
                }

                updatePassword(currentPassword, newPassword)

            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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

        // Make change password button appear active
        changePassBtn.elevation = 10f
        deleteAccBtn.elevation = 0f

        changePassBtn.setOnClickListener {
            // Show change password card
            changePassCard.visibility = View.VISIBLE
            deleteAccCard.visibility = View.GONE

            // Update button elevations
            changePassBtn.elevation = 10f
            deleteAccBtn.elevation = 0f

            // Optional: Add animation
            val fadeIn = AlphaAnimation(0f, 1f)
            fadeIn.duration = 300
            changePassCard.startAnimation(fadeIn)
        }

        deleteAccBtn.setOnClickListener {
            // Show delete account card
            changePassCard.visibility = View.GONE
            deleteAccCard.visibility = View.VISIBLE

            // Update button elevations
            changePassBtn.elevation = 0f
            deleteAccBtn.elevation = 10f

            // Optional: Add animation
            val fadeIn = AlphaAnimation(0f, 1f)
            fadeIn.duration = 300
            deleteAccCard.startAnimation(fadeIn)
        }
    }
}