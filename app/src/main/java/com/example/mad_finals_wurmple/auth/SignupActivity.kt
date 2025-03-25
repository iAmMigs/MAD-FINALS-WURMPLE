package com.example.mad_finals_wurmple.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_finals_wurmple.R
import com.google.firebase.auth.FirebaseAuth

class signupActivity : AppCompatActivity() {

    private lateinit var signupUsername: EditText
    private lateinit var signupEmail: EditText
    private lateinit var signupPassword: EditText
    private lateinit var confirmPassword: EditText
    private lateinit var signupButton: Button
    private lateinit var redirectLoginBtn: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup_page)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

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
                            Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                            val intent = Intent()
                            intent.setClassName("com.example.mad_finals_wurmple", "com.example.mad_finals_wurmple.dashboard.dashboardActivity")
                            startActivity(intent)
                            finish()  // Finish signup activity
                        } else {
                            Toast.makeText(this, "Signup failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } catch (e: IllegalArgumentException) {
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "An error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        redirectLoginBtn.setOnClickListener {
            startActivity(Intent(this, loginActivity::class.java))
        }
    }
}
