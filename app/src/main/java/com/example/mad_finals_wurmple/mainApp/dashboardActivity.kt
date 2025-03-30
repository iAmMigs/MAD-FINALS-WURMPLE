package com.example.mad_finals_wurmple.mainApp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_finals_wurmple.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class dashboardActivity : AppCompatActivity() {

    private lateinit var menuUserName: TextView
    private lateinit var profileBtn: ImageButton
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dashboard_page)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UI elements
        menuUserName = findViewById(R.id.usernameText)
        profileBtn = findViewById(R.id.profileButton)

        // Fetch username from Firestore
        fetchUsername()

        // Handle profile button click
        profileBtn.setOnClickListener {
            val intent = Intent(this, accountSettingsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun fetchUsername() {
        val user = auth.currentUser
        if (user == null) {
            menuUserName.text = "Guest"
            return
        }

        val userId = user.uid
        val userRef = db.collection("users").document(userId)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username")
                    if (!username.isNullOrEmpty()) {
                        menuUserName.text = username
                    } else {
                        Log.e("Firestore", "Username field is empty")
                        menuUserName.text = "Guest"
                    }
                } else {
                    Log.e("Firestore", "User document does not exist")
                    Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show()
                    menuUserName.text = "Guest"
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error retrieving username: ${e.message}", e)
                Toast.makeText(this, "Failed to get username. Please check your internet connection.", Toast.LENGTH_LONG).show()
                menuUserName.text = "Guest"
            }
    }
}
