package com.example.mad_finals_wurmple.mainApp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import com.example.mad_finals_wurmple.R
import com.example.mad_finals_wurmple.mainApp.transactionClasses.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TransactionDialogFragment : DialogFragment() {
    private val TAG = "TransactionDialogFrag"

    private lateinit var addAmountBtn: Button
    private lateinit var decreaseAmountBtn: Button
    private lateinit var confirmAddBtn: Button
    private lateinit var confirmDecreaseBtn: Button
    private lateinit var addAmountCard: CardView
    private lateinit var decreaseAmountCard: CardView
    private lateinit var closeButton: ImageButton
    private lateinit var addAmountInput: EditText
    private lateinit var addTransNameInput: EditText
    private lateinit var decreaseAmountInput: EditText
    private lateinit var decreaseTransNameInput: EditText

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var goalManager: goalClass  // Add the goal manager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.popup_createtransactions, container, false)

        try {
            // Initialize Firebase
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            goalManager = goalClass(requireContext())  // Initialize goal manager

            // Initialize UI components
            initializeViews(view)

            // Setup event listeners
            setupEventListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreateView: ${e.message}", e)
            Toast.makeText(context, "Error initializing transaction dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    private fun initializeViews(view: View) {
        try {
            addAmountBtn = view.findViewById(R.id.AddAmount)
            decreaseAmountBtn = view.findViewById(R.id.DecreaseAmount)
            addAmountCard = view.findViewById(R.id.changePassCard)
            decreaseAmountCard = view.findViewById(R.id.deleteAccCard)
            closeButton = view.findViewById(R.id.closeButton)

            addAmountInput = view.findViewById(R.id.AddAmountInput)
            addTransNameInput = view.findViewById(R.id.transNameInput)
            decreaseAmountInput = view.findViewById(R.id.DecreaseAmountInput)
            decreaseTransNameInput = view.findViewById(R.id.transactionNameDecrease)

            confirmAddBtn = view.findViewById(R.id.addButton)
            confirmDecreaseBtn = view.findViewById(R.id.decreaseButton)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing views: ${e.message}", e)
            Toast.makeText(context, "Error setting up transaction UI: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupEventListeners() {
        try {
            addAmountBtn.setOnClickListener {
                addAmountCard.visibility = View.VISIBLE
                decreaseAmountCard.visibility = View.GONE
            }

            decreaseAmountBtn.setOnClickListener {
                addAmountCard.visibility = View.GONE
                decreaseAmountCard.visibility = View.VISIBLE
            }

            closeButton.setOnClickListener {
                dismiss()
            }

            confirmAddBtn.setOnClickListener {
                try {
                    if (validateTransaction(addAmountInput, addTransNameInput)) {
                        saveTransaction(addAmountInput.text.toString().toDouble(), addTransNameInput.text.toString(), "income")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding income: ${e.message}", e)
                    Toast.makeText(context, "Error adding income: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            confirmDecreaseBtn.setOnClickListener {
                try {
                    if (validateTransaction(decreaseAmountInput, decreaseTransNameInput)) {
                        saveTransaction(decreaseAmountInput.text.toString().toDouble(), decreaseTransNameInput.text.toString(), "expense")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error adding expense: ${e.message}", e)
                    Toast.makeText(context, "Error adding expense: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up event listeners: ${e.message}", e)
            Toast.makeText(context, "Error setting up transaction actions: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateTransaction(amountInput: EditText, nameInput: EditText): Boolean {
        try {
            val amount = amountInput.text.toString().trim()
            val name = nameInput.text.toString().trim()

            if (amount.isEmpty()) {
                amountInput.error = "Please enter amount"
                return false
            }

            // Check if amount is a valid number
            try {
                amount.toDouble()
            } catch (e: NumberFormatException) {
                amountInput.error = "Please enter a valid number"
                return false
            }

            if (name.isEmpty()) {
                nameInput.error = "Please enter transaction name"
                return false
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error validating transaction: ${e.message}", e)
            Toast.makeText(context, "Error validating input: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun saveTransaction(amount: Double, name: String, type: String) {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                return
            }

            val userRef = db.collection("users").document(userId)

            db.runTransaction { transaction ->
                try {
                    val snapshot = transaction.get(userRef)
                    val currentBalance = snapshot.getDouble("balance") ?: 0.0
                    val currentGoalProgress = snapshot.getDouble("goalProgress") ?: 0.0
                    val goal = snapshot.getDouble("goal") ?: 0.0

                    // Prevent expenses from making the balance negative
                    if (type == "expense" && amount > currentBalance) {
                        throw IllegalArgumentException("Insufficient balance to complete this transaction.")
                    }

                    // Calculate new balance
                    val newBalance = if (type == "income") {
                        currentBalance + amount
                    } else {
                        currentBalance - amount
                    }

                    // Calculate new goal progress
                    var newGoalProgress = if (type == "income") {
                        currentGoalProgress + amount
                    } else {
                        currentGoalProgress - amount
                    }

                    // Ensure goal progress stays within valid range (0 to goal amount)
                    if (newGoalProgress < 0) newGoalProgress = 0.0
                    if (goal > 0 && newGoalProgress > goal) newGoalProgress = goal

                    // Update balance and goal progress
                    transaction.update(userRef, mapOf(
                        "balance" to newBalance,
                        "goalProgress" to newGoalProgress
                    ))

                    return@runTransaction true // Success flag
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Transaction blocked: ${e.message}")
                    return@runTransaction false // Fail flag
                }
            }.addOnSuccessListener { success ->
                if (success) {
                    // Add transaction record
                    addTransactionToCollection(userId, amount, name, type, Date())
                    Toast.makeText(requireContext(), "$type transaction added: $$amount", Toast.LENGTH_SHORT).show()
                    dismiss()
                } else {
                    Toast.makeText(requireContext(), "Transaction failed: Insufficient balance!", Toast.LENGTH_LONG).show()
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Transaction failed: ${e.message}", e)
                Toast.makeText(requireContext(), "Failed to add transaction: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction: ${e.message}", e)
            Toast.makeText(context, "Error saving transaction: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addTransactionToCollection(userId: String, amount: Double, name: String, type: String, date: Date) {
        try {
            val transactionData = hashMapOf(
                "transaction_date" to date,
                "transaction_name" to name,
                "transaction_amount" to amount
            )

            db.collection("users").document(userId)
                .collection(type)
                .add(transactionData)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to record transaction: ${e.message}", e)
                    Toast.makeText(requireContext(), "Failed to record transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding transaction to collection: ${e.message}", e)
            Toast.makeText(context, "Error recording transaction details: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createOverdueEntry(userId: String, name: String, amount: Double) {
        try {
            val currentDate = Date()
            val overdueData = hashMapOf(
                "transaction_date" to currentDate,
                "transaction_name" to name,
                "transaction_amount" to amount,
                "transaction_type" to "expense",  // Marking the type of transaction that caused the overdue
                "last_interest_date" to currentDate,  // Track when interest was last applied
                "is_paid" to false  // Track if the overdue has been paid
            )

            db.collection("users").document(userId)
                .collection("overdues")
                .add(overdueData)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to create overdue: ${e.message}", e)
                    Toast.makeText(requireContext(), "Failed to create overdue: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating overdue entry: ${e.message}", e)
            Toast.makeText(context, "Error creating overdue record: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}