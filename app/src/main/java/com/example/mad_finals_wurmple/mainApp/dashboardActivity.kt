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

        // Ensure goalProgress field exists in the database
        ensureGoalProgressFieldExists()
    }

    private fun ensureGoalProgressFieldExists() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists() && !document.contains("goalProgress")) {
                // Initialize goalProgress field if it doesn't exist
                userRef.update("goalProgress", 0.0)
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
            R.layout.expense_view -> initializeExpenseView(view) // Pass the view for initialization
            R.layout.overdue_view -> initializeOverdueView()
        }
    }

    private fun initializeGoalView(view: android.view.View) {
        // Get references to UI elements
        val halfCircleProgress = view.findViewById<HalfCircleProgressView>(R.id.half_circle_progress)
        val currentAmountText = view.findViewById<TextView>(R.id.tv_current_amount)
        val goalAmountText = view.findViewById<TextView>(R.id.tv_goal_amount)
        val goalInputField = view.findViewById<EditText>(R.id.ConfirmInput)
        val setNewGoalButton = view.findViewById<Button>(R.id.btn_new_goal)

        // Ensure goalProgress field exists
        goalManager.ensureGoalProgressFieldExists()

        // Update progress visualization with current data
        goalManager.getGoalProgress { progress, goal ->
            runOnUiThread {
                // Update text views
                currentAmountText.text = "$$progress"
                goalAmountText.text = "$$goal"

                // Calculate progress percentage for the half circle view
                val progressPercentage = if (goal > 0) (progress / goal) else 0.0
                halfCircleProgress.setProgress(progressPercentage.toFloat().coerceAtMost(1f))
            }
        }

        // Set up the button to update the goal
        setNewGoalButton.setOnClickListener {
            goalManager.showConfirmationDialog(goalInputField) { newGoal ->
                // This lambda is called when the goal is successfully updated
                runOnUiThread {
                    // Update the goal amount display
                    goalAmountText.text = "$$newGoal"
                    currentAmountText.text = "$0" // Reset progress display to 0
                    halfCircleProgress.setProgress(0f) // Reset progress bar
                }
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

    private fun initializeOverdueView() {
        val overdueView = contentFrame.getChildAt(0)
        overdueHistoryManager = OverdueHistoryManager(this, overdueView)

        // Call the overdue interest calculator
        overdueManager.calculateAndApplyOverdueInterest()

        val calculateBtn = overdueView.findViewById<Button>(R.id.btn_calculate_cheapest)
        calculateBtn?.setOnClickListener {
            Toast.makeText(this, "Calculating cheapest payment plan...", Toast.LENGTH_SHORT).show()
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
                balanceTextView.text = "$balance"

                // Update goal progress whenever there's a database change
                if (goalBtn.background.constantState == resources.getDrawable(R.drawable.button_selected).constantState) {
                    // Only update if goal view is currently active
                    val view = contentFrame.getChildAt(0)
                    val halfCircleProgress = view.findViewById<HalfCircleProgressView>(R.id.half_circle_progress)
                    val currentAmountText = view.findViewById<TextView>(R.id.tv_current_amount)

                    // Get the goal progress value from Firestore
                    goalManager.getGoalProgress { progress, goal ->
                        runOnUiThread {
                            currentAmountText.text = "$$progress"
                            val progressPercentage = if (goal > 0) (progress / goal) else 0.0
                            halfCircleProgress.setProgress(progressPercentage.toFloat().coerceAtMost(1f))
                        }
                    }
                }
            }
        }
    }
}