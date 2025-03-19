package com.example.mad_finals_wurmple

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_page)

        // Initialize Firebase Auth and Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

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

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPass) {
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                // Check if password is at least 6 characters long
                if (password.length < 6) {
                    throw IllegalArgumentException("Password must be at least 6 characters long")
                }

                // Create user in Firebase Auth
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            if (userId != null) {
                                // Create a user object to save in Firestore
                                val user = hashMapOf(
                                    "username" to username,
                                    "email" to email,
                                    "balance" to 1000.0,
                                    "income" to 0.0,
                                    "loss" to 0.0
                                )

                                // Save user data to Firestore
                                firestore.collection("users").document(userId)
                                    .set(user)
                                    .addOnSuccessListener {
                                        // User document created successfully
                                        Log.d("Firebase", "User data saved successfully to Firestore")

                                        // Create the currentPortfolio subcollection and add an initial empty investment record
                                        val currentPortfolioRef = firestore.collection("users").document(userId).collection("currentPortfolio")
                                        val initialCurrentInvestment = hashMapOf(
                                            "investmentDate" to System.currentTimeMillis(),
                                            "investmentAmount" to 0.0,
                                            "investmentDetails" to "Initial Active Investment"
                                        )
                                        currentPortfolioRef.add(initialCurrentInvestment)
                                            .addOnSuccessListener {
                                                Log.d("Firebase", "Initial current portfolio added.")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("Firebase", "Failed to add to current portfolio: ${e.message}")
                                            }

                                        // Create the investmentHistory subcollection, which will hold completed investments (empty initially)
                                        val investmentHistoryRef = firestore.collection("users").document(userId).collection("investmentHistory")

                                        // Notify user and navigate to menuActivity
                                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                                        startActivity(Intent(this, menuActivity::class.java))
                                        finish()  // Finish signup activity
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("Firebase", "Failed to save user data: ${e.message}")
                                        Toast.makeText(this, "Failed to save user data!", Toast.LENGTH_SHORT).show()
                                    }

                            } else {
                                Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            } catch (e: IllegalArgumentException) {
                // Catch password length error
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                // Catch other unexpected errors
                Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        redirectLoginBtn.setOnClickListener {
            startActivity(Intent(this, loginActivity::class.java))
        }
    }
}
