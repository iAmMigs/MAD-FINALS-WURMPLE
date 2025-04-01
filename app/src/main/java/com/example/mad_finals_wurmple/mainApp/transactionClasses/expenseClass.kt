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

class ExpenseHistoryManager(private val context: Context, private val view: View) {
    private val tableLayout: TableLayout = view.findViewById(R.id.tableLayout)
    private val spinnerSort: Spinner = view.findViewById(R.id.spinner_sort)
    private val expenseList = mutableListOf<ExpenseData>()
    private val db = FirebaseFirestore.getInstance()

    data class ExpenseData(
        val transactionId: String = "",
        val name: String = "",
        val amount: Double = 0.0,
        val date: Date = Date()
    )

    init {
        setupSpinner()
        fetchExpenseData()
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

    private fun fetchExpenseData() {
        expenseList.clear()
        val userId = getCurrentUserId() ?: return

        db.collection("users").document(userId).collection("expense")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val expenseData = ExpenseData(
                        transactionId = document.id,
                        name = document.getString("transaction_name") ?: "",
                        amount = document.getDouble("transaction_amount") ?: 0.0,
                        date = document.getTimestamp("transaction_date")?.toDate() ?: Date()
                    )
                    expenseList.add(expenseData)
                }

                println("Fetched Expense List: ${expenseList.size} items") // Debugging
                sortData(spinnerSort.selectedItemPosition)
            }
            .addOnFailureListener { exception ->
                println("Error getting expense data: $exception")
            }
    }

    private fun sortData(sortOption: Int) {
        when (sortOption) {
            0 -> bubbleSortBy { it.name.lowercase() } // Name (A-Z)
            1 -> bubbleSortByDescending { it.name.lowercase() } // Name (Z-A)
            2 -> bubbleSortBy { it.amount } // Amount (Low-High)
            3 -> bubbleSortByDescending { it.amount } // Amount (High-Low)
            4 -> bubbleSortByDescending { it.date.time } // Date (Recent-Oldest)
            5 -> bubbleSortBy { it.date.time } // Date (Oldest-Recent)
        }

        println("Sorting option: $sortOption") // Debugging
        updateTable()
    }

    private fun <T : Comparable<T>> bubbleSortBy(selector: (ExpenseData) -> T) {
        for (i in 0 until expenseList.size - 1) {
            for (j in 0 until expenseList.size - i - 1) {
                if (selector(expenseList[j]) > selector(expenseList[j + 1])) {
                    val temp = expenseList[j]
                    expenseList[j] = expenseList[j + 1]
                    expenseList[j + 1] = temp
                }
            }
        }
    }

    private fun <T : Comparable<T>> bubbleSortByDescending(selector: (ExpenseData) -> T) {
        for (i in 0 until expenseList.size - 1) {
            for (j in 0 until expenseList.size - i - 1) {
                if (selector(expenseList[j]) < selector(expenseList[j + 1])) {
                    val temp = expenseList[j]
                    expenseList[j] = expenseList[j + 1]
                    expenseList[j + 1] = temp
                }
            }
        }
    }

    private fun updateTable() {
        tableLayout.removeViews(1, tableLayout.childCount - 1)
        val dateFormat = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())

        for (expense in expenseList) {
            val tableRow = TableRow(context)
            tableRow.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )

            tableRow.addView(createTableCell(expense.name))
            tableRow.addView(createTableCell(String.format("$%.2f", expense.amount)))
            tableRow.addView(createTableCell(dateFormat.format(expense.date)))

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
}