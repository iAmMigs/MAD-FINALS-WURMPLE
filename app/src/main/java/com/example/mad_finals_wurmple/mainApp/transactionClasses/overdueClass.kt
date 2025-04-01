package com.example.mad_finals_wurmple.mainApp.transactionClasses

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.*
import com.example.mad_finals_wurmple.R
import kotlin.collections.HashMap

class OverdueHistoryManager(private val context: Context, private val view: View) {
    private val TAG = "OverdueHistoryManager"
    private val tableLayout: TableLayout = view.findViewById(R.id.tableLayout)
    private val spinnerSort: Spinner = view.findViewById(R.id.spinner_sort)
    private val txtCheapest: TextView = view.findViewById(R.id.txt_cheapest)
    private val btnCalculateCheapest: Button = view.findViewById(R.id.btn_calculate_cheapest)
    private val btnPaySelected: Button = view.findViewById(R.id.btn_pay_selected)
    private val overdueList = mutableListOf<OverdueData>()
    private val db = FirebaseFirestore.getInstance()
    private val selectedItems = mutableSetOf<String>()
    private val overduePaymentOptimizer = OverduePaymentOptimizer(context) // Initialize OverduePaymentOptimizer

    data class OverdueData(
        val transactionId: String = "",
        val name: String = "",
        val amount: Double = 0.0,
        val date: Date = Date(),
        val type: String = "",
        var isSelected: Boolean = false
    )

    init {
        try {
            setupSpinner()
            setupPaymentButton()
            fetchOverdueData()
            setupCalculateCheapestButton() // Initialize the Calculate Cheapest button functionality
        } catch (e: Exception) {
            Log.e(TAG, "Error in initialization: ${e.message}")
            Toast.makeText(context, "Error initializing overdue history: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSpinner() {
        try {
            val sortOptions = arrayOf(
                "Name (A-Z)", "Name (Z-A)", "Amount (Low-High)", "Amount (High-Low)", "Date (Recent-Oldest)", "Date (Oldest-Recent)", "Type (A-Z)", "Type (Z-A)"
            )

            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, sortOptions)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSort.adapter = adapter

            spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    try {
                        sortData(position)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sorting data: ${e.message}")
                        Toast.makeText(context, "Error sorting data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up spinner: ${e.message}")
            Toast.makeText(context, "Error setting up sorting options: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPaymentButton() {
        try {
            // Initially hide the payment button
            btnPaySelected.isVisible = false

            btnPaySelected.setOnClickListener {
                try {
                    if (selectedItems.isNotEmpty()) {
                        showPaymentDialog()
                    } else {
                        Toast.makeText(context, "No items selected for payment", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in payment button click: ${e.message}")
                    Toast.makeText(context, "Error processing payment request: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up payment button: ${e.message}")
            Toast.makeText(context, "Error setting up payment button: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCalculateCheapestButton() {
        try {
            btnCalculateCheapest.setOnClickListener {
                try {
                    // Launch coroutine to calculate the optimal payment plan
                    calculateOptimalPaymentPlan()
                } catch (e: Exception) {
                    Log.e(TAG, "Error calculating optimal payment plan: ${e.message}")
                    Toast.makeText(context, "Error calculating optimal payment plan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up calculate cheapest button: ${e.message}")
            Toast.makeText(context, "Error setting up calculate button: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchOverdueData() {
        try {
            overdueList.clear()
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.e(TAG, "User ID is null when fetching overdue data")
                Toast.makeText(context, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                return
            }

            db.collection("users").document(userId).collection("overdues")
                .get()
                .addOnSuccessListener { documents ->
                    try {
                        for (document in documents) {
                            val overdueData = OverdueData(
                                transactionId = document.id,
                                name = document.getString("transaction_name") ?: "",
                                amount = document.getDouble("transaction_amount") ?: 0.0,
                                date = document.getTimestamp("transaction_date")?.toDate() ?: Date(),
                                type = document.getString("transaction_type") ?: ""
                            )
                            overdueList.add(overdueData)
                        }
                        sortData(spinnerSort.selectedItemPosition)
                        updateCheapestPaymentVisibility()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing overdue data: ${e.message}")
                        Toast.makeText(context, "Error processing overdue data: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error getting overdue data: $exception")
                    Toast.makeText(context, "Failed to fetch overdue data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in fetchOverdueData: ${e.message}")
            Toast.makeText(context, "Error fetching overdue data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sortData(sortOption: Int) {
        try {
            when (sortOption) {
                0 -> overdueList.sortBy { it.name.lowercase() }
                1 -> overdueList.sortByDescending { it.name.lowercase() }
                2 -> overdueList.sortBy { it.amount }
                3 -> overdueList.sortByDescending { it.amount }
                4 -> overdueList.sortByDescending { it.date.time }
                5 -> overdueList.sortBy { it.date.time }
                6 -> overdueList.sortBy { it.type.lowercase() }
                7 -> overdueList.sortByDescending { it.type.lowercase() }
            }
            updateTable()
        } catch (e: Exception) {
            Log.e(TAG, "Error sorting data: ${e.message}")
            Toast.makeText(context, "Error sorting data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTable() {
        try {
            tableLayout.removeViews(1, tableLayout.childCount - 1)
            val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

            for (overdue in overdueList) {
                val tableRow = TableRow(context)
                tableRow.layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )

                // Add checkbox
                val checkbox = CheckBox(context).apply {
                    isChecked = selectedItems.contains(overdue.transactionId)
                    background = ContextCompat.getDrawable(context, R.drawable.table_border)
                    gravity = Gravity.CENTER

                    setOnCheckedChangeListener { _, isChecked ->
                        try {
                            if (isChecked) {
                                selectedItems.add(overdue.transactionId)
                            } else {
                                selectedItems.remove(overdue.transactionId)
                            }
                            // Show/hide payment button based on selection
                            btnPaySelected.isVisible = selectedItems.isNotEmpty()
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling checkbox state change: ${e.message}")
                            Toast.makeText(context, "Error selecting item: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                tableRow.addView(checkbox)

                // Add other cells
                tableRow.addView(createTableCell(overdue.name))
                tableRow.addView(createTableCell(String.format("$%.2f", overdue.amount)))
                tableRow.addView(createTableCell(dateFormat.format(overdue.date)))
                tableRow.addView(createTableCell(overdue.type))

                tableLayout.addView(tableRow)
            }

            updateCheapestPaymentVisibility()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating table: ${e.message}")
            Toast.makeText(context, "Error updating table: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createTableCell(text: String): TextView {
        try {
            return TextView(context).apply {
                this.text = text
                gravity = Gravity.CENTER
                background = ContextCompat.getDrawable(context, R.drawable.table_border)
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )
                setTextColor(ContextCompat.getColor(context, R.color.black))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating table cell: ${e.message}")
            // Create a basic TextView as fallback
            return TextView(context).apply {
                this.text = text
            }
        }
    }

    private fun showPaymentDialog() {
        try {
            val totalAmount = calculateSelectedAmount()

            val dialogView = View.inflate(context, R.layout.payment_dialog_layout, null)
            val amountTextView = dialogView.findViewById<TextView>(R.id.txt_payment_amount)

            amountTextView.text = String.format("Total amount: $%.2f", totalAmount)

            AlertDialog.Builder(context)
                .setTitle("Payment Confirmation")
                .setView(dialogView)
                .setPositiveButton("Pay") { _, _ ->
                    try {
                        processPayment(totalAmount)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing payment: ${e.message}")
                        Toast.makeText(context, "Error processing payment: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing payment dialog: ${e.message}")
            Toast.makeText(context, "Error showing payment dialog: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateSelectedAmount(): Double {
        try {
            return overdueList
                .filter { selectedItems.contains(it.transactionId) }
                .sumOf { it.amount }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating selected amount: ${e.message}")
            Toast.makeText(context, "Error calculating selected amount: ${e.message}", Toast.LENGTH_SHORT).show()
            return 0.0
        }
    }

    private fun processPayment(amount: Double) {
        try {
            val userId = getCurrentUserId()
            if (userId == null) {
                Log.e(TAG, "User ID is null when processing payment")
                Toast.makeText(context, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                return
            }

            // Get user balance
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        val currentBalance = document.getDouble("balance") ?: 0.0

                        if (currentBalance >= amount) {
                            // Update balance
                            val newBalance = currentBalance - amount
                            db.collection("users").document(userId)
                                .update("balance", newBalance)
                                .addOnSuccessListener {
                                    try {
                                        // Remove paid items
                                        removeSelectedItems(userId)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error removing selected items: ${e.message}")
                                        Toast.makeText(context, "Error removing selected items: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e(TAG, "Failed to update balance: ${exception.message}")
                                    Toast.makeText(context, "Failed to update balance: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, "Insufficient balance", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing payment document: ${e.message}")
                        Toast.makeText(context, "Error processing payment: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to check balance: ${exception.message}")
                    Toast.makeText(context, "Failed to check balance: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in processPayment: ${e.message}")
            Toast.makeText(context, "Error processing payment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeSelectedItems(userId: String) {
        try {
            // Create a batch to delete multiple documents
            val batch = db.batch()

            // Add delete operations to batch
            for (id in selectedItems) {
                val docRef = db.collection("users").document(userId).collection("overdues").document(id)
                batch.delete(docRef)
            }

            // Execute batch delete
            batch.commit()
                .addOnSuccessListener {
                    try {
                        Toast.makeText(context, "Payment successful!", Toast.LENGTH_SHORT).show()

                        // Remove paid items from local list
                        overdueList.removeAll { selectedItems.contains(it.transactionId) }

                        // Clear selected items
                        selectedItems.clear()

                        // Update UI
                        updateTable()
                        btnPaySelected.isVisible = false
                        updateCheapestPaymentVisibility()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating UI after payment: ${e.message}")
                        Toast.makeText(context, "Payment was successful but error refreshing display: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to process payment: ${exception.message}")
                    Toast.makeText(context, "Failed to process payment: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in removeSelectedItems: ${e.message}")
            Toast.makeText(context, "Error removing selected items: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCheapestPaymentVisibility() {
        try {
            val hasThreeOrMoreItems = overdueList.size >= 3
            txtCheapest.isVisible = hasThreeOrMoreItems
            btnCalculateCheapest.isVisible = hasThreeOrMoreItems
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cheapest payment visibility: ${e.message}")
            // Don't show toast for this minor UI update
        }
    }

    private fun getCurrentUserId(): String? {
        try {
            return FirebaseAuth.getInstance().currentUser?.uid
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user ID: ${e.message}")
            Toast.makeText(context, "Error getting user information: ${e.message}", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    // Coroutine method to calculate the optimal payment plan
    private fun calculateOptimalPaymentPlan() {
        try {
            // Check if context is an Activity before using lifecycleScope
            val activity = context as? Activity
            if (activity == null) {
                Log.e(TAG, "Context is not an Activity, cannot launch coroutine")
                Toast.makeText(context, "Error: Unable to calculate optimal plan", Toast.LENGTH_SHORT).show()
                return
            }

            // Use lifecycleScope to launch the coroutine
            activity.lifecycleScope.launch {
                try {
                    // Assuming OverduePaymentOptimizer is a suspend function
                    val availableFunds = 1000.0 // For example, the available funds could come from user data
                    val optimalPaymentPlan = withContext(Dispatchers.IO) {
                        try {
                            overduePaymentOptimizer.getOptimalPaymentPlan(availableFunds) // Call the suspend function here
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in optimizer computation: ${e.message}")
                            null
                        }
                    }

                    // Handle the optimal payment plan result
                    if (optimalPaymentPlan != null) {
                        Toast.makeText(context, "Optimal plan: $optimalPaymentPlan", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to calculate optimal payment plan", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in coroutine execution: ${e.message}")
                    Toast.makeText(context, "Error calculating optimal plan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching coroutine: ${e.message}")
            Toast.makeText(context, "Error launching payment optimizer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
