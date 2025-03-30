package com.example.mad_finals_wurmple.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_finals_wurmple.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class signupActivity : AppCompatActivity() {

    private lateinit var signupUsername: EditText
    private lateinit var signupEmail: EditText
    private lateinit var signupPassword: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var signupButton: Button
    private lateinit var redirectLoginBtn: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_page)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        signupUsername = findViewById(R.id.UserInput)
        signupEmail = findViewById(R.id.signupEmail)
        signupPassword = findViewById(R.id.PassInput)
        confirmPassword = findViewById(R.id.ConfirmPassInput)
        signupButton = findViewById(R.id.signupButton)
        redirectLoginBtn = findViewById(R.id.loginRedirectButton)

        signupButton.setOnClickListener {
            val username = signupUsername.text.toString().trim()
            val email = signupEmail.text.toString().trim()
            val password = signupPassword.text.toString().trim()
            val confirmPass = confirmPassword.text.toString().trim()

            // Check for empty fields
            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate email format
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if passwords match
            if (password != confirmPass) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate username (only letters, numbers, and underscores, no spaces)
            val usernamePattern = Regex("^[a-zA-Z0-9_]+$")
            if (!usernamePattern.matches(username)) {
                Toast.makeText(this, "Username must be one word with only letters, numbers, or underscores", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check password length
            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create user in Firebase Auth
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid

                        if (userId != null) {
                            // Save user details in Firestore
                            val user = hashMapOf(
                                "username" to username,
                                "email" to email
                            )

                            db.collection("users").document(userId)
                                .set(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Successfully created account!", Toast.LENGTH_SHORT).show()

                                    // Delay before going back to login screen
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        val intent = Intent(this, loginActivity::class.java)
                                        startActivity(intent)
                                        finish() // Close signup activity
                                    }, 2000) // 2-second delay
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to save user info: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        redirectLoginBtn.setOnClickListener {
            startActivity(Intent(this, loginActivity::class.java))
        }
    }
}
