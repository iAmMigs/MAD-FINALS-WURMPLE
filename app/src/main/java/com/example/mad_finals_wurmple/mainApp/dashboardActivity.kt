package com.example.mad_finals_wurmple.mainApp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.mad_finals_wurmple.R
import com.example.mad_finals_wurmple.mainApp.transactionClasses.*
import com.example.mad_finals_wurmple.mainApp.ui.HalfCircleProgressView
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

    // Content frame to swap views
    private lateinit var contentFrame: FrameLayout

    // Managers for transactions
    private var incomeHistoryManager: IncomeHistoryManager? = null
    private var expenseHistoryManager: ExpenseHistoryManager? = null
    private var overdueHistoryManager: OverdueHistoryManager? = null
    private lateinit var goalManager: goalClass
    private lateinit var overdueManager: OverdueManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dashboard_page)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize goal manager
        goalManager = goalClass(this)
        overdueManager = OverdueManager(this)

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

        // Setup transaction type buttons listeners
        setupTransactionButtons()

        // Load the goal view by default
        loadViewIntoContentFrame(R.layout.goal_view)

        // Ensure goal field exists in the database
        goalManager.ensureGoalFieldExists()

        // Set up the payment dialog result handling
        supportFragmentManager.setFragmentResultListener("payment_completed", this) { _, _ ->
            // Refresh the overdue view when a payment is completed
            refreshOverdueView()
            // Also update the goal view if it's visible, since payments affect goal progress
            if (goalBtn.background.constantState == resources.getDrawable(R.drawable.button_selected).constantState) {
                updateGoalView()
            }
        }
    }

    private fun setupTransactionButtons() {
        incomeBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.income_view)
        }

        expensesBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.expense_view)
        }

        goalBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.goal_view)
        }

        overduesBtn.setOnClickListener {
            loadViewIntoContentFrame(R.layout.overdue_view)
        }
    }

    private fun loadViewIntoContentFrame(layoutResId: Int) {
        contentFrame.removeAllViews()

        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(layoutResId, contentFrame, false)
        contentFrame.addView(view)

        when (layoutResId) {
            R.layout.goal_view -> initializeGoalView(view)
            R.layout.income_view -> initializeIncomeView()
            R.layout.expense_view -> initializeExpenseView(view)
            R.layout.overdue_view -> initializeOverdueView(view)
        }
    }

    private fun initializeGoalView(view: android.view.View) {
        // Get references to UI elements
        val halfCircleProgress = view.findViewById<HalfCircleProgressView>(R.id.half_circle_progress)
        val currentAmountText = view.findViewById<TextView>(R.id.tv_current_amount)
        val goalAmountText = view.findViewById<TextView>(R.id.tv_goal_amount)
        val goalInputField = view.findViewById<EditText>(R.id.ConfirmInput)
        val setNewGoalButton = view.findViewById<Button>(R.id.btn_new_goal)

        // Update progress visualization with current data - now using account balance
        goalManager.getGoalProgress { progress, goal ->
            runOnUiThread {
                // Update text views
                currentAmountText.text = String.format("$%.2f", progress)
                goalAmountText.text = String.format("$%.2f", goal)

                // Calculate progress percentage for the half circle view
                val progressPercentage = if (goal > 0) (progress / goal) else 0.0
                halfCircleProgress.setProgress(progressPercentage.toFloat().coerceAtMost(1f))
            }
        }

        // Set up the button to update the goal
        setNewGoalButton.setOnClickListener {
            goalManager.showConfirmationDialog(goalInputField) { newGoal ->
                // This lambda is called when the goal is successfully updated
                // Update the goal amount display immediately after setting new goal
                updateGoalView()
            }
        }
    }

    // New method to update the goal view
    private fun updateGoalView() {
        val view = contentFrame.getChildAt(0) ?: return
        // Only proceed if we're currently on the goal view
        if (goalBtn.background.constantState != resources.getDrawable(R.drawable.button_selected).constantState) {
            return
        }

        val halfCircleProgress = view.findViewById<HalfCircleProgressView>(R.id.half_circle_progress) ?: return
        val currentAmountText = view.findViewById<TextView>(R.id.tv_current_amount) ?: return
        val goalAmountText = view.findViewById<TextView>(R.id.tv_goal_amount) ?: return

        // Get fresh goal progress data
        goalManager.getGoalProgress { progress, goal ->
            runOnUiThread {
                // Update text views
                currentAmountText.text = String.format("$%.2f", progress)
                goalAmountText.text = String.format("$%.2f", goal)

                // Calculate progress percentage for the half circle view
                val progressPercentage = if (goal > 0) (progress / goal) else 0.0
                halfCircleProgress.setProgress(progressPercentage.toFloat().coerceAtMost(1f))
            }
        }
    }

    private fun initializeIncomeView() {
        val incomeView = contentFrame.getChildAt(0)
        incomeHistoryManager = IncomeHistoryManager(this, incomeView)
    }

    private fun initializeExpenseView(view: android.view.View) {
        expenseHistoryManager = ExpenseHistoryManager(this, view)
    }

    private fun initializeOverdueView(view: android.view.View) {
        // Create a new instance of OverdueHistoryManager with supportFragmentManager
        overdueHistoryManager = OverdueHistoryManager(this, view, supportFragmentManager)

        // Call the overdue interest calculator to update interest on all overdues
        overdueManager.calculateAndApplyOverdueInterest()
    }

    // Method to refresh the overdue view after a payment is made
    private fun refreshOverdueView() {
        // Get the current view in the content frame
        val currentView = contentFrame.getChildAt(0) ?: return

        // Check if we're currently showing the overdue view
        if (overduesBtn.background.constantState == resources.getDrawable(R.drawable.button_selected).constantState) {
            // Create a new instance of OverdueHistoryManager to refresh the data
            overdueHistoryManager = OverdueHistoryManager(this, currentView, supportFragmentManager)
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
                balanceTextView.text = String.format("$%.2f", balance)

                // Update goal progress whenever there's a database change
                if (goalBtn.background.constantState == resources.getDrawable(R.drawable.button_selected).constantState) {
                    // Only update if goal view is currently active
                    updateGoalView()
                }

                // Check if a payment has occurred and refresh overdue view if needed
                if (overduesBtn.background.constantState == resources.getDrawable(R.drawable.button_selected).constantState) {
                    // Only refresh if overdue view is currently active
                    refreshOverdueView()
                }
            }
        }
    }
}