package com.example.mad_finals_wurmple.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_finals_wurmple.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

class loginActivity : AppCompatActivity() {

    private lateinit var loginEmail: EditText
    private lateinit var loginPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var redirectSignupBtn: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var forgotPass: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_page)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize UI elements
        forgotPass = findViewById(R.id.forgotPassButton)
        loginEmail = findViewById(R.id.EmailInput)
        loginPassword = findViewById(R.id.PassInput)
        loginButton = findViewById(R.id.loginButton)
        redirectSignupBtn = findViewById(R.id.signupRedirectButton)

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

        forgotPass.setOnClickListener {
            startActivity(Intent(this, forgotPassActivity::class.java))
        }

        redirectSignupBtn.setOnClickListener {
            startActivity(Intent(this, signupActivity::class.java))
        }
    }
}