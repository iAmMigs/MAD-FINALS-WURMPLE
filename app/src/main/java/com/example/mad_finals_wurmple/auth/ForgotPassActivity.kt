package com.example.mad_finals_wurmple.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_finals_wurmple.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class ForgotPassActivity : AppCompatActivity() {

    private lateinit var resetButton: Button
    private lateinit var emailEditText: EditText
    private lateinit var auth: FirebaseAuth
    private lateinit var redirectBackBtn: Button
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.forgotpass_page)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        resetButton = findViewById(R.id.resetButton)
        emailEditText = findViewById(R.id.Email)
        redirectBackBtn = findViewById(R.id.redirectBackButton)

        redirectBackBtn.setOnClickListener {
            Log.d("RedirectLogin", "Button clicked")
            startActivity(Intent(this, LoginActivity::class.java))
        }

        emailEditText.setOnEditorActionListener(TextView.OnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                resetButton.performClick()
                return@OnEditorActionListener true
            }
            false
        })

        // Set onClickListener for RESET button
        resetButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if email exists before sending reset email
            checkEmailExists(email)
        }
    }

    private fun checkEmailExists(email: String) {
        // Show loading state
        resetButton.isEnabled = false
        resetButton.text = "Checking..."

        // Method 1: Use Firestore to check if the email exists
        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Email exists in Firestore, send password reset email
                    sendPasswordResetEmail(email)
                } else {
                    // Method 2: Use Firebase Auth's fetchSignInMethodsForEmail as a fallback
                    auth.fetchSignInMethodsForEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val signInMethods = task.result?.signInMethods
                                if (!signInMethods.isNullOrEmpty()) {
                                    // Email exists in Firebase Auth, send password reset email
                                    sendPasswordResetEmail(email)
                                } else {
                                    // Email does not exist
                                    Toast.makeText(this, "No account found with this email", Toast.LENGTH_SHORT).show()
                                    resetButton.isEnabled = true
                                    resetButton.text = "RESET PASSWORD"
                                }
                            } else {
                                // Error checking email
                                Toast.makeText(this, "Error checking email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                resetButton.isEnabled = true
                                resetButton.text = "RESET PASSWORD"
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                // Error checking Firestore
                Log.e("ForgotPassword", "Error checking email in Firestore", e)

                // Try with fetchSignInMethodsForEmail as fallback
                auth.fetchSignInMethodsForEmail(email)
                    .addOnCompleteListener { task ->
                        resetButton.isEnabled = true
                        resetButton.text = "RESET PASSWORD"

                        if (task.isSuccessful) {
                            val signInMethods = task.result?.signInMethods
                            if (!signInMethods.isNullOrEmpty()) {
                                sendPasswordResetEmail(email)
                            } else {
                                Toast.makeText(this, "No account found with this email", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Error checking email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                resetButton.isEnabled = true
                resetButton.text = "RESET PASSWORD"

                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent. Check your inbox.", Toast.LENGTH_LONG).show()
                } else {
                    val errorMessage = when (task.exception) {
                        is FirebaseAuthInvalidUserException -> "No account found with this email."
                        else -> "Error sending password reset email: ${task.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }
}