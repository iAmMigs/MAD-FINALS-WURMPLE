package com.example.mad_finals_wurmple.mainApp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.fragment.app.DialogFragment
import com.example.mad_finals_wurmple.R
import com.example.mad_finals_wurmple.mainApp.transactionClasses.goalClass
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TransactionDialogFragment : DialogFragment() {

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

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        goalManager = goalClass(requireContext())  // Initialize goal manager

        // Initialize UI components
        initializeViews(view)

        // Setup event listeners
        setupEventListeners()

        return view
    }

    private fun initializeViews(view: View) {
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
    }

    private fun setupEventListeners() {
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
            if (validateTransaction(addAmountInput, addTransNameInput)) {
                saveTransaction(addAmountInput.text.toString().toDouble(), addTransNameInput.text.toString(), "income")
            }
        }

        confirmDecreaseBtn.setOnClickListener {
            if (validateTransaction(decreaseAmountInput, decreaseTransNameInput)) {
                saveTransaction(decreaseAmountInput.text.toString().toDouble(), decreaseTransNameInput.text.toString(), "expense")
            }
        }
    }

    private fun validateTransaction(amountInput: EditText, nameInput: EditText): Boolean {
        val amount = amountInput.text.toString().trim()
        val name = nameInput.text.toString().trim()

        if (amount.isEmpty()) {
            amountInput.error = "Please enter amount"
            return false
        }
        if (name.isEmpty()) {
            nameInput.error = "Please enter transaction name"
            return false
        }
        return true
    }

    private fun saveTransaction(amount: Double, name: String, type: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentBalance = snapshot.getDouble("balance") ?: 0.0
            // Get current goal progress
            val currentGoalProgress = snapshot.getDouble("goalProgress") ?: 0.0
            val goal = snapshot.getDouble("goal") ?: 0.0

            val newBalance = if (type == "income") {
                currentBalance + amount
            } else {
                if (currentBalance < amount) {
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Insufficient balance for this expense!", Toast.LENGTH_SHORT).show()
                    }
                    return@runTransaction
                }
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

            // Update both balance and goal progress in Firestore in the same transaction
            transaction.update(userRef,
                mapOf(
                    "balance" to newBalance,
                    "goalProgress" to newGoalProgress
                )
            )

            // Save transaction details
            val transactionData = hashMapOf(
                "transaction_date" to Date(),
                "transaction_name" to name,
                "transaction_amount" to amount
            )
            db.collection("users").document(userId)
                .collection(type).add(transactionData)
        }.addOnSuccessListener {
            Toast.makeText(requireContext(), "$type transaction added: $$amount", Toast.LENGTH_SHORT).show()
            dismiss()
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Failed to add transaction: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}