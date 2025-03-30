package com.example.mad_finals_wurmple.mainApp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_finals_wurmple.R

class dashboardActivity:AppCompatActivity() {

    private lateinit var menuUserName: TextView
    private lateinit var profileBtn: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dashboard_page)

        profileBtn = findViewById(R.id.profileButton)
        // Handle home button click to navigate to dashboard
        profileBtn.setOnClickListener {
            val intent = Intent(this, accountSettingsActivity::class.java)
            // Clear back stack so user can't go back to account settings with back button
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish() // Optional: close this activity
        }
    }
}