package com.example.mad_finals_wurmple.dashboard

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mad_finals_wurmple.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class TransactionActivity : AppCompatActivity() {

    private lateinit var recyclerViewTransactions: RecyclerView
    private lateinit var transactionAdapter: TransactionAdapter
    private lateinit var db: FirebaseFirestore

    // Date range buttons
    private lateinit var btnToday: Button
    private lateinit var btn7Days: Button
    private lateinit var btn30Days: Button
    private lateinit var btnYear: Button
    private lateinit var backBtn: Button

    private val transactionsList = mutableListOf<Transaction>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transactions_page)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        initializeComponents()

        // Setup RecyclerView
        setupRecyclerView()

        // Setup button listeners
        setupButtonListeners()

        // Fetch transactions with default (today) filter
        fetchTransactions(DateRange.TODAY)
    }

    private fun initializeComponents() {
        recyclerViewTransactions = findViewById(R.id.recyclerViewTransactions)
        btnToday = findViewById(R.id.btnToday)
        btn7Days = findViewById(R.id.btn7Days)
        btn30Days = findViewById(R.id.btn30Days)
        btnYear = findViewById(R.id.btnYear)
        backBtn = findViewById(R.id.backBtn)
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(transactionsList)
        recyclerViewTransactions.layoutManager = LinearLayoutManager(this)
        recyclerViewTransactions.adapter = transactionAdapter
        recyclerViewTransactions.visibility = View.VISIBLE
    }

    private fun setupButtonListeners() {
        btnToday.setOnClickListener { fetchTransactions(DateRange.TODAY) }
        btn7Days.setOnClickListener { fetchTransactions(DateRange.SEVEN_DAYS) }
        btn30Days.setOnClickListener { fetchTransactions(DateRange.THIRTY_DAYS) }
        btnYear.setOnClickListener { fetchTransactions(DateRange.YEAR) }
        backBtn.setOnClickListener { finish() }
    }

    private fun fetchTransactions(dateRange: DateRange) {
        val userId = "currentUserID" // Replace with actual user authentication

        // Calculate the start date based on the selected range
        val calendar = Calendar.getInstance()
        val endDate = calendar.time

        when (dateRange) {
            DateRange.TODAY -> calendar.add(Calendar.DAY_OF_YEAR, -1)
            DateRange.SEVEN_DAYS -> calendar.add(Calendar.DAY_OF_YEAR, -7)
            DateRange.THIRTY_DAYS -> calendar.add(Calendar.MONTH, -1)
            DateRange.YEAR -> calendar.add(Calendar.YEAR, -1)
        }
        val startDate = calendar.time

        // Fetch transactions within the specified date range
        db.collection("users").document(userId).collection("transactions")
            .whereGreaterThan("timestamp", startDate)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                transactionsList.clear()
                for (document in querySnapshot.documents) {
                    val transaction = document.toObject(Transaction::class.java)
                    transaction?.let { transactionsList.add(it) }
                }

                // Update the RecyclerView
                transactionAdapter.notifyDataSetChanged()

                // Show a message if no transactions found
                if (transactionsList.isEmpty()) {
                    Toast.makeText(this, "No transactions found for selected period", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching transactions: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Enum to define date range
    enum class DateRange {
        TODAY,
        SEVEN_DAYS,
        THIRTY_DAYS,
        YEAR
    }
}
