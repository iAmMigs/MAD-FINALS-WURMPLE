package com.example.mad_finals_wurmple.dashboard

import android.util.Log
import com.google.firebase.firestore.FieldValue

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.NumberFormat
import java.util.Locale
import com.example.mad_finals_wurmple.R

class MainActivity : ComponentActivity() {
    private lateinit var db: FirebaseFirestore

    // UI Components
    private lateinit var usernameText: TextView
    private lateinit var balanceText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressCircle: CircularProgressIndicator
    private lateinit var tvAmount: TextView

    // Current user ID (replace with actual authentication method)
    private val currentUserId = "user123"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_page)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        initializeUIComponents()

        // Set up button click listeners
        setupButtonListeners()

        // Fetch initial user data
        fetchUserData()
    }

    private fun initializeUIComponents() {
        try {
            usernameText = findViewById(R.id.menuUsernameTxt)
            balanceText = findViewById(R.id.balanceTxt)
            progressBar = findViewById(R.id.progressBar)
            progressCircle = findViewById(R.id.progressCircle)
            tvAmount = findViewById(R.id.tvAmount)
        } catch (e: Exception) {
            showError("UI Initialization Error", e)
        }
    }

    private fun setupButtonListeners() {
        try {
            findViewById<Button>(R.id.btnToday).setOnClickListener { updateData(1) }
            findViewById<Button>(R.id.btn7Days).setOnClickListener { updateData(7) }
            findViewById<Button>(R.id.btn30Days).setOnClickListener { updateData(30) }
            findViewById<Button>(R.id.btnYear).setOnClickListener { updateData(365) }
            findViewById<Button>(R.id.btnAddIncome).setOnClickListener { addIncome() }
            findViewById<Button>(R.id.btnAddExpense).setOnClickListener { addExpense() }
        } catch (e: Exception) {
            showError("Button Setup Error", e)
        }
    }

    private fun fetchUserData() {
        try {
            db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        // Safely extract and display user data
                        val username = document.getString("username") ?: "Unknown User"
                        val balance = document.getDouble("balance") ?: 0.0
                        val monthlyProgress = document.getLong("monthlyProgress")?.toInt() ?: 0

                        // Format balance with currency
                        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)

                        // Update UI
                        usernameText.text = username
                        balanceText.text = currencyFormatter.format(balance)
                        tvAmount.text = currencyFormatter.format(balance)

                        // Set progress for both progress bar and circle
                        progressBar.progress = monthlyProgress
                        progressCircle.progress = monthlyProgress
                    } else {
                        showError("User Data Error", "No user data found")
                    }
                }
                .addOnFailureListener { e ->
                    showError("Firestore Fetch Error", e)
                }
        } catch (e: Exception) {
            showError("Data Fetch Error", e)
        }
    }

    private fun updateData(days: Int) {
        try {
            // Example implementation of data update based on time range
            val updateData = hashMapOf(
                "lastViewedRange" to days
            )

            db.collection("users").document(currentUserId)
                .set(updateData, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Data updated for $days days", Toast.LENGTH_SHORT).show()
                    // Optionally fetch updated data
                    fetchUserData()
                }
                .addOnFailureListener { e ->
                    showError("Update Data Error", e)
                }
        } catch (e: Exception) {
            showError("Data Update Error", e)
        }
    }

    private fun addIncome() {
        try {
            // Placeholder for income addition logic
            val incomeAmount = 100.0 // This could come from a dialog or input

            db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener { document ->
                    val currentBalance = document.getDouble("balance") ?: 0.0
                    val newBalance = currentBalance + incomeAmount

                    val updateData = hashMapOf(
                        "balance" to newBalance,
                        "totalIncome" to FieldValue.increment(incomeAmount)
                    )

                    db.collection("users").document(currentUserId)
                        .update(updateData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Income added successfully", Toast.LENGTH_SHORT).show()
                            fetchUserData()
                        }
                        .addOnFailureListener { e ->
                            showError("Income Addition Error", e)
                        }
                }
                .addOnFailureListener { e ->
                    showError("Balance Fetch Error", e)
                }
        } catch (e: Exception) {
            showError("Add Income Error", e)
        }
    }

    private fun addExpense() {
        try {
            // Placeholder for expense addition logic
            val expenseAmount = 50.0 // This could come from a dialog or input

            db.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener { document ->
                    val currentBalance = document.getDouble("balance") ?: 0.0
                    val newBalance = currentBalance - expenseAmount

                    val updateData = hashMapOf(
                        "balance" to newBalance,
                        "totalExpenses" to FieldValue.increment(expenseAmount)
                    )

                    db.collection("users").document(currentUserId)
                        .update(updateData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Expense added successfully", Toast.LENGTH_SHORT).show()
                            fetchUserData()
                        }
                        .addOnFailureListener { e ->
                            showError("Expense Addition Error", e)
                        }
                }
                .addOnFailureListener { e ->
                    showError("Balance Fetch Error", e)
                }
        } catch (e: Exception) {
            showError("Add Expense Error", e)
        }
    }

    // Generic error handling method
    private fun showError(context: String, e: Exception) {
        Log.e("MainActivity", "$context: ${e.message}", e)
        Toast.makeText(this, "Error in $context: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }

    private fun showError(context: String, message: String) {
        Log.e("MainActivity", "$context: $message")
        Toast.makeText(this, "Error in $context: $message", Toast.LENGTH_LONG).show()
    }
}