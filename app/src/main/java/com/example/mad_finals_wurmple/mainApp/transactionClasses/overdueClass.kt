package com.example.mad_finals_wurmple.mainApp.transactionClasses

import android.content.Context
import android.graphics.Typeface
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.example.mad_finals_wurmple.R

class OverdueHistoryManager(
    private val context: Context,
    private val view: View,
    private val fragmentManager: FragmentManager
) {
    private val tableLayout: TableLayout = view.findViewById(R.id.tableLayout)
    private val spinnerSort: Spinner = view.findViewById(R.id.spinner_sort)
    private val btnPaySelected: Button = view.findViewById(R.id.btn_pay_selected)
    private val txtCheapest: TextView = view.findViewById(R.id.txt_cheapest)
    private val btnCalculateCheapest: Button = view.findViewById(R.id.btn_calculate_cheapest)

    private val overdueList = mutableListOf<OverdueData>()
    private val selectedOverdues = mutableListOf<OverdueData>()
    private val db = FirebaseFirestore.getInstance()
    private val displayMetrics: DisplayMetrics = context.resources.displayMetrics

    data class OverdueData(
        val transactionId: String = "",
        val name: String = "",
        val amount: Double = 0.0,
        val date: Date = Date(),
        val lastInterestDate: Date = Date(),
        val isPaid: Boolean = false,
        var isSelected: Boolean = false
    )

    init {
        setupSpinner()
        setupButtons()
        fetchOverdueData()
    }

    private fun setupSpinner() {
        val sortOptions = arrayOf(
            "Name (A-Z)", "Name (Z-A)",
            "Amount (Low-High)", "Amount (High-Low)",
            "Date (Recent-Oldest)", "Date (Oldest-Recent)"
        )

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSort.adapter = adapter

        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sortData(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupButtons() {
        // Set up the Pay Selected button
        btnPaySelected.setOnClickListener {
            if (selectedOverdues.isEmpty()) {
                Toast.makeText(context, "Please select at least one overdue to pay", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Calculate total amount to pay
            val totalAmount = selectedOverdues.sumOf { it.amount }

            // Show payment dialog
            val paymentDialog = PaymentDialogFragment.newInstance(totalAmount, selectedOverdues)
            paymentDialog.show(fragmentManager, "PaymentDialog")
        }

        // Set up Calculate Cheapest button
        btnCalculateCheapest.setOnClickListener {
            val optimalPlan = calculateOptimalPlan()
            // Show optimal payment plan in a dialog
            val optimalDialog = OptimalPaymentDialogFragment.newInstance(optimalPlan)
            optimalDialog.show(fragmentManager, "OptimalPaymentDialog")
        }
    }
    private fun fetchOverdueData() {
        // Clear existing lists
        overdueList.clear()
        selectedOverdues.clear()

        // Get current user ID
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch overdues from Firestore
        db.collection("users")
            .document(user.uid)
            .collection("overdues")
            .whereEqualTo("is_paid", false)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    try {
                        val overdueData = OverdueData(
                            transactionId = document.id,
                            name = document.getString("transaction_name") ?: "Unknown",
                            amount = document.getDouble("transaction_amount") ?: 0.0,
                            date = document.getTimestamp("transaction_date")?.toDate() ?: Date(),
                            lastInterestDate = document.getTimestamp("last_interest_date")?.toDate() ?: Date(),
                            isPaid = false,
                            isSelected = false
                        )
                        overdueList.add(overdueData)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            "Error parsing overdue: ${e.localizedMessage}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                // Update UI on main thread
                updateUiAfterFetch()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Error fetching overdue data: ${exception.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateUiAfterFetch() {
        // Sort data with current spinner selection
        val currentSortPosition = spinnerSort.selectedItemPosition
        sortData(currentSortPosition)

        // Show/hide cheapest calculation options
        val hasUnpaidOverdues = overdueList.isNotEmpty()
        txtCheapest.visibility = if (hasUnpaidOverdues) View.VISIBLE else View.GONE
        btnCalculateCheapest.visibility = if (hasUnpaidOverdues) View.VISIBLE else View.GONE
    }

    private fun sortData(sortOption: Int) {
        when (sortOption) {
            0 -> overdueList.sortBy { it.name } // Name (A-Z)
            1 -> overdueList.sortByDescending { it.name } // Name (Z-A)
            2 -> overdueList.sortBy { it.amount } // Amount (Low-High)
            3 -> overdueList.sortByDescending { it.amount } // Amount (High-Low)
            4 -> overdueList.sortByDescending { it.date } // Date (Recent-Oldest)
            5 -> overdueList.sortBy { it.date } // Date (Oldest-Recent)
        }

        updateTable()
    }

    private fun createTableCell(text: String, weight: Float = 2f): TextView {
        return TextView(context).apply {
            layoutParams = TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, weight).apply {
                // Ensure fixed width distribution based on weight
                width = 0
            }
            gravity = Gravity.CENTER
            this.text = text
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            background = ContextCompat.getDrawable(context, R.drawable.table_border)
            setTextColor(ContextCompat.getColor(context, R.color.black))
            textSize = 16f

            // Ensure text wrapping and full visibility
            isSingleLine = false
            maxLines = Int.MAX_VALUE
            ellipsize = null
        }
    }

    private fun createCheckboxCell(overdue: OverdueData? = null): View {
        val checkboxContainer = LinearLayout(context).apply {
            layoutParams = TableRow.LayoutParams(
                dpToPx(48),
                TableRow.LayoutParams.MATCH_PARENT
            )
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(context, R.drawable.table_border)
        }

        val checkbox = CheckBox(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER
        }

        overdue?.let {
            checkbox.isChecked = it.isSelected
            checkbox.setOnCheckedChangeListener { _, isChecked ->
                it.isSelected = isChecked
                if (isChecked) {
                    selectedOverdues.add(it)
                } else {
                    selectedOverdues.remove(it)
                }
                btnPaySelected.visibility = if (selectedOverdues.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }

        checkboxContainer.addView(checkbox)
        return checkboxContainer
    }

    private fun updateTable() {
        tableLayout.removeViews(1, tableLayout.childCount - 1)
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        selectedOverdues.clear()
        btnPaySelected.visibility = View.GONE

        for (overdue in overdueList) {
            val tableRow = TableRow(context).apply {
                layoutParams = TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT
                )

                // If you had any additional background setting, remove it here
            }

            tableRow.addView(createCheckboxCell(overdue))
            tableRow.addView(createTableCell(overdue.name))
            tableRow.addView(createTableCell(String.format("$%.2f", overdue.amount)))
            tableRow.addView(createTableCell(dateFormat.format(overdue.date)))
            tableRow.addView(createTableCell(if (overdue.isPaid) "Paid" else "Unpaid"))

            tableLayout.addView(tableRow)
        }
    }

    // Utility method to convert dp to pixels
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    // Method to calculate optimal payment plan
    fun calculateOptimalPlan(): String {
        val optimizer = OverduePaymentOptimizer(context)

        // Use the optimizer to get unpaid overdues and convert to the format needed
        val overdueItems = overdueList.map { overdue ->
            OverduePaymentOptimizer.OverdueItem(
                id = overdue.transactionId,
                name = overdue.name,
                amount = overdue.amount,
                lastInterestDate = overdue.lastInterestDate,
                isPaid = overdue.isPaid
            )
        }

        // Find optimal payment sequence (assuming weekly payments)
        val optimalSequence = optimizer.findOptimalPaymentSequence(overdueItems, 7)

        // Generate and return the display text
        return optimizer.displayPaymentPlan(optimalSequence)
    }

    // Method to refresh the data after a payment is made
    fun refreshData() {
        fetchOverdueData()
    }
}