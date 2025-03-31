package com.example.mad_finals_wurmple.mainApp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_finals_wurmple.R
import com.example.mad_finals_wurmple.mainApp.transactionClasses.IncomeHistoryManager
import com.example.mad_finals_wurmple.mainApp.transactionClasses.ExpenseHistoryManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class dashboardActivity : AppCompatActivity() {

    private lateinit var menuUserName: TextView
    private lateinit var profileBtn: ImageButton
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fab: ImageButton
    private lateinit var balanceTextView: TextView

    // Transaction type buttons
    private lateinit var incomeBtn: Button
    private lateinit var expensesBtn: Button
    private lateinit var goalBtn: Button
    private lateinit var overduesBtn: Button
    private lateinit var penaltiesBtn: Button

    // Content frame to swap views
    private lateinit var contentFrame: FrameLayout

    // Managers for transactions
    private var incomeHistoryManager: IncomeHistoryManager? = null
    private var expenseHistoryManager: ExpenseHistoryManager? = null

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
        balanceTextView = findViewById(R.id.balanceText)

        // Initialize transaction buttons
        incomeBtn = findViewById(R.id.incomeBtn)
        expensesBtn = findViewById(R.id.expensesBtn)
        goalBtn = findViewById(R.id.goalBtn)
        overduesBtn = findViewById(R.id.overduesBtn)
        penaltiesBtn = findViewById(R.id.penaltiesBtn)

        // Initialize content frame
        contentFrame = findViewById(R.id.contentFrame)

        // Fetch username and balance from Firestore
        fetchUsername()
        listenForBalanceUpdates()

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
        incomeBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.income_view)
            updateButtonStyles(incomeBtn)
        }

        expensesBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.expense_view)
            updateButtonStyles(expensesBtn)
        }

        goalBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.goal_view)
            updateButtonStyles(goalBtn)
        }

        overduesBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.overdue_view)
            updateButtonStyles(overduesBtn)
        }

        penaltiesBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.penalty_view)
            updateButtonStyles(penaltiesBtn)
        }
    }

    private fun loadViewIntoContentFrame(layoutResId: Int) {
        contentFrame.removeAllViews()

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(layoutResId, contentFrame, false)
        contentFrame.addView(view)

        when (layoutResId) {
            R.layout.goal_view -> initializeGoalView()
            R.layout.income_view -> initializeIncomeView()
            R.layout.expense_view -> initializeExpenseView(view) // Pass the view for initialization
            R.layout.overdue_view -> initializeOverdueView()
            R.layout.penalty_view -> initializePenaltyView()
        }
    }

    private fun updateButtonStyles(selectedButton: Button) {
        incomeBtn.setBackgroundResource(R.drawable.button_default)
        expensesBtn.setBackgroundResource(R.drawable.button_default)
        goalBtn.setBackgroundResource(R.drawable.button_default)
        overduesBtn.setBackgroundResource(R.drawable.button_default)
        penaltiesBtn.setBackgroundResource(R.drawable.button_default)

        selectedButton.setBackgroundResource(R.drawable.button_selected)
    }

    private fun initializeGoalView() {
        // Setup goal-specific elements
    }

    private fun initializeIncomeView() {
        val incomeView = contentFrame.getChildAt(0)
        incomeHistoryManager = IncomeHistoryManager(this, incomeView)
    }

    private fun initializeExpenseView(view: android.view.View) {
        expenseHistoryManager = ExpenseHistoryManager(this, view)
    }

    private fun initializeOverdueView() {
        val calculateBtn = contentFrame.findViewById<Button>(R.id.btn_calculate_cheapest)
        calculateBtn?.setOnClickListener {
            Toast.makeText(this, "Calculating cheapest payment plan...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializePenaltyView() {
        // Setup penalty-specific elements
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
                    menuUserName.text = username ?: "Guest"
                } else {
                    Log.e("Firestore", "User document does not exist")
                    menuUserName.text = "Guest"
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error retrieving username: ${e.message}", e)
                Toast.makeText(this, "Failed to get username. Please check your internet connection.", Toast.LENGTH_LONG).show()
                menuUserName.text = "Guest"
            }
    }

    private fun listenForBalanceUpdates() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        userRef.addSnapshotListener { documentSnapshot, e ->
            if (e != null) {
                Toast.makeText(this, "Failed to load balance: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                val balance = documentSnapshot.getDouble("balance") ?: 0.0
                balanceTextView.text = "$balance"
            }
        }
    }
}