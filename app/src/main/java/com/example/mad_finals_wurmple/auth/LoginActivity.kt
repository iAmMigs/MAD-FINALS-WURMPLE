package com.example.mad_finals_wurmple.auth

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_finals_wurmple.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var loginEmail: EditText
    private lateinit var loginPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var redirectSignupBtn: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var forgotPass: Button
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        forgotPass = findViewById(R.id.forgotPassButton)
        loginEmail = findViewById(R.id.EmailInput)
        loginPassword = findViewById(R.id.PassInput)
        loginButton = findViewById(R.id.loginButton)
        redirectSignupBtn = findViewById(R.id.signupRedirectButton)

        // Set the Enter key behavior for email input
        loginEmail.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                loginPassword.requestFocus()
                return@setOnEditorActionListener true // Consume the event
            }
            false
        }

        // Set the Enter key behavior for password input
        loginPassword.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                loginButton.performClick()
                return@setOnEditorActionListener true // Consume the event
            }
            false
        }

        loginButton.setOnClickListener {
            val email = loginEmail.text.toString().trim()
            val password = loginPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                // Check if email exists in Firestore before attempting login
                db.collection("users").whereEqualTo("email", email).get()
                    .addOnSuccessListener { documents ->
                        if (documents.isEmpty) {
                            Toast.makeText(this, "No account found with this email.", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // Proceed with Firebase Authentication
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                                    val intent = Intent()
                                    intent.setClassName("com.example.mad_finals_wurmple", "com.example.mad_finals_wurmple.mainApp.dashboardActivity")
                                    startActivity(intent)
                                    finish()
                                } else {
                                    val errorMessage = when (task.exception) {
                                        is FirebaseAuthInvalidUserException -> "No account found with this email."
                                        is FirebaseAuthInvalidCredentialsException -> "Incorrect password. Please try again."
                                        else -> "Login failed. Please check your credentials."
                                    }
                                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error checking email: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        forgotPass.setOnClickListener {
            startActivity(Intent(this, ForgotPassActivity::class.java))
        }

        redirectSignupBtn.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}
