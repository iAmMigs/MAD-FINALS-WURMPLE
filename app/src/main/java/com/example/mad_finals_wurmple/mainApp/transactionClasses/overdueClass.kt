package com.example.mad_finals_wurmple.mainApp.transactionClasses

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.example.mad_finals_wurmple.R

class OverdueHistoryManager(private val context: Context, private val view: View) {
    private val tableLayout: TableLayout = view.findViewById(R.id.tableLayout)
    private val spinnerSort: Spinner = view.findViewById(R.id.spinner_sort)
    private val overdueList = mutableListOf<OverdueData>()
    private val db = FirebaseFirestore.getInstance()

    data class OverdueData(
        val transactionId: String = "",
        val name: String = "",
        val amount: Double = 0.0,
        val date: Date = Date(),
        val lastInterestDate: Date = Date(),
        val isPaid: Boolean = false
    )

    init {
        setupSpinner()
        fetchOverdueData()
    }

    private fun setupSpinner() {
        val sortOptions = arrayOf(
            "Name (A-Z)", "Name (Z-A)", "Amount (Low-High)", "Amount (High-Low)", "Date (Recent-Oldest)", "Date (Oldest-Recent)"
        )

        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, sortOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSort.adapter = adapter

        spinnerSort.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                sortData(position)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
    }

    private fun fetchOverdueData() {
        overdueList.clear()
        val userId = getCurrentUserId() ?: return

        // Using "overdues" collection name consistently
        db.collection("users").document(userId).collection("overdues")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val overdueData = OverdueData(
                        transactionId = document.id,
                        name = document.getString("transaction_name") ?: "",
                        amount = document.getDouble("transaction_amount") ?: 0.0,
                        date = document.getTimestamp("transaction_date")?.toDate() ?: Date(),
                        lastInterestDate = document.getTimestamp("last_interest_date")?.toDate() ?: Date(),
                        isPaid = document.getBoolean("is_paid") ?: false
                    )
                    overdueList.add(overdueData)
                }

                println("Fetched Overdue List: ${overdueList.size} items") // Debugging
                sortData(spinnerSort.selectedItemPosition)
            }
            .addOnFailureListener { exception ->
                println("Error getting overdue data: $exception")
            }
    }

    private fun sortData(sortOption: Int) {
        when (sortOption) {
            0 -> SortData.insertionSortBy(overdueList) { it.name } // Name (A-Z)
            1 -> SortData.insertionSortByDescending(overdueList) { it.name } // Name (Z-A)
            2 -> SortData.insertionSortBy(overdueList) { it.amount } // Amount (Low-High)
            3 -> SortData.insertionSortByDescending(overdueList) { it.amount } // Amount (High-Low)
            4 -> SortData.insertionSortByDescending(overdueList) { it.date } // Date (Recent-Oldest)
            5 -> SortData.insertionSortBy(overdueList) { it.date } // Date (Oldest-Recent)
        }

        println("Sorting option: $sortOption") // Debugging
        updateTable()
    }

    private fun updateTable() {
        tableLayout.removeViews(1, tableLayout.childCount - 1)
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

        for (overdue in overdueList) {
            val tableRow = TableRow(context)
            tableRow.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            tableRow.addView(createTableCell(overdue.name))
            tableRow.addView(createTableCell(String.format("$%.2f", overdue.amount)))
            tableRow.addView(createTableCell(dateFormat.format(overdue.date)))
            tableRow.addView(createTableCell(if (overdue.isPaid) "Paid" else "Unpaid"))

            tableLayout.addView(tableRow)
        }
    }

    private fun createTableCell(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setPadding(8, 8, 8, 8)
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(context, R.drawable.table_border)
            layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            setTextColor(ContextCompat.getColor(context, R.color.black))
        }
    }

    private fun getCurrentUserId(): String? {
        return FirebaseAuth.getInstance().currentUser?.uid
    }

    // Method to calculate optimal payment plan
    fun calculateOptimalPlan(): String {
        val optimizer = OverduePaymentOptimizer(context)

        // Use the optimizer to get unpaid overdues and convert to the format needed
        val overdueItems = overdueList.filter { !it.isPaid }.map { overdue ->
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
}