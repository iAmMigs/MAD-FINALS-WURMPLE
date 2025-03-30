package com.example.mad_finals_wurmple.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_finals_wurmple.R
import com.example.mad_finals_wurmple.mainApp.dashboardActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupActivity : AppCompatActivity() {

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

            // Validate username format (only letters, numbers, and underscores, no spaces)
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

            try {
                // Check if username already exists in Firestore
                db.collection("users").whereEqualTo("username", username).get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            Toast.makeText(this, "Username is already taken", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        // If username is unique, proceed with Firebase Authentication
                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val userId = auth.currentUser?.uid

                                    if (userId != null) {
                                        // Create user data
                                        val user = hashMapOf(
                                            "username" to username,
                                            "email" to email
                                        )

                                        // Save user data to Firestore
                                        db.collection("users").document(userId)
                                            .set(user)
                                            .addOnSuccessListener {
                                                // Create empty subcollections by writing and immediately deleting a placeholder document
                                                val emptyData = hashMapOf("placeholder" to true)

                                                listOf("income", "expenses", "goal").forEach { collection ->
                                                    db.collection("users").document(userId)
                                                        .collection(collection).document("temp")
                                                        .set(emptyData)
                                                        .addOnSuccessListener {
                                                            db.collection("users").document(userId)
                                                                .collection(collection).document("temp")
                                                                .delete()
                                                        }
                                                        .addOnFailureListener { e ->
                                                            Toast.makeText(this, "Failed to initialize $collection collection: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                }

                                                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                                                val intent = Intent(this, dashboardActivity::class.java)
                                                startActivity(intent)
                                                finish()
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
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Error checking username: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        redirectLoginBtn.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
