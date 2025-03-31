package com.example.mad_finals_wurmple.mainApp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_finals_wurmple.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.fragment.app.DialogFragment


class dashboardActivity : AppCompatActivity() {

    private lateinit var menuUserName: TextView
    private lateinit var profileBtn: ImageButton
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fab: ImageButton

    // Transaction type buttons
    private lateinit var incomeBtn: Button
    private lateinit var expensesBtn: Button
    private lateinit var goalBtn: Button
    private lateinit var overduesBtn: Button
    private lateinit var penaltiesBtn: Button

    // Content frame to swap views
    private lateinit var contentFrame: FrameLayout

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
        fab = findViewById(R.id.fab)

        // Initialize transaction buttons
        incomeBtn = findViewById(R.id.incomeBtn)
        expensesBtn = findViewById(R.id.expensesBtn)
        goalBtn = findViewById(R.id.goalBtn)
        overduesBtn = findViewById(R.id.overduesBtn)
        penaltiesBtn = findViewById(R.id.penaltiesBtn)

        // Initialize content frame
        contentFrame = findViewById(R.id.contentFrame)

        // Fetch username from Firestore
        fetchUsername()

        // Handle profile button click
        profileBtn.setOnClickListener {
            val intent = Intent(this, accountSettingsActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        // Show popup on FAB click
        fab.setOnClickListener {
            val transactionDialog = TransactionDialogFragment()
            transactionDialog.show(supportFragmentManager, "TransactionDialog")
        }

        // Setup transaction type button listeners
        setupTransactionButtons()

        // Default view is Goal (already included in layout)
    }

    private fun setupTransactionButtons() {
        // Income button
        incomeBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.income_view)
            updateButtonStyles(incomeBtn)
        }

        // Expenses button
        expensesBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.expense_view)
            updateButtonStyles(expensesBtn)
        }

        // Goal button (default)
        goalBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.goal_view)
            updateButtonStyles(goalBtn)
        }

        // Overdues button
        overduesBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.overdue_view)
            updateButtonStyles(overduesBtn)
        }

        // Penalties button
        penaltiesBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.penalty_view)
            updateButtonStyles(penaltiesBtn)
        }
    }

    private fun loadViewIntoContentFrame(layoutResId: Int) {
        // Clear existing views
        contentFrame.removeAllViews()

        // Inflate the new layout
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(layoutResId, contentFrame, false)

        // Add the inflated layout to the frame
        contentFrame.addView(view)

        // Initialize the specific layout if needed
        when (layoutResId) {
            R.layout.goal_view -> initializeGoalView()
            R.layout.income_view -> initializeIncomeView()
            R.layout.expense_view -> initializeExpenseView()
            R.layout.overdue_view -> initializeOverdueView()
            R.layout.penalty_view -> initializePenaltyView()
        }
    }

    // This method highlights the selected button and resets others
    private fun updateButtonStyles(selectedButton: Button) {
        // Reset all buttons to default style
        incomeBtn.setBackgroundResource(R.drawable.button_default)
        expensesBtn.setBackgroundResource(R.drawable.button_default)
        goalBtn.setBackgroundResource(R.drawable.button_default)
        overduesBtn.setBackgroundResource(R.drawable.button_default)
        penaltiesBtn.setBackgroundResource(R.drawable.button_default)

        // Set selected button style
        selectedButton.setBackgroundResource(R.drawable.button_selected)
    }

    // Initialize views for each transaction type
    private fun initializeGoalView() {
        // Initialize goal specific elements
        // Example: Setup progress view, set current goal amounts, etc.
    }

    private fun initializeIncomeView() {
        // Initialize income specific elements
        // Example: Setup spinner, load income data to table, etc.
    }

    private fun initializeExpenseView() {
        // Initialize expense specific elements
        // Example: Setup spinner, load expense data to table, etc.
    }

    private fun initializeOverdueView() {
        // Initialize overdue specific elements
        // Example: Setup spinner, load overdue data to table, setup calculate button, etc.
        val calculateBtn = contentFrame.findViewById<Button>(R.id.btn_calculate_cheapest)
        calculateBtn?.setOnClickListener {
            // Handle calculate cheapest way logic
            Toast.makeText(this, "Calculating cheapest payment plan...", Toast.LENGTH_SHORT).show()
            // Implement calculation logic here
        }
    }

    private fun initializePenaltyView() {
        // Initialize penalty specific elements
        // Example: Setup spinner, load penalty data to table, etc.
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