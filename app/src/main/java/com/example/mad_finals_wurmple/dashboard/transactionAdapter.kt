package com.example.mad_finals_wurmple.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mad_finals_wurmple.R
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(private val transactions: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val amountTextView: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        val typeTextView: TextView = itemView.findViewById(R.id.tvTransactionType)
        val dateTextView: TextView = itemView.findViewById(R.id.tvTransactionDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        // Format amount with + for income, - for expense
        val amountText = when(transaction.type) {
            "Income" -> "+$${String.format("%.2f", transaction.amount)}"
            "Expense" -> "-$${String.format("%.2f", transaction.amount)}"
            else -> "$${String.format("%.2f", transaction.amount)}"
        }

        holder.amountTextView.text = amountText
        holder.typeTextView.text = transaction.type

        // Format date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        holder.dateTextView.text = dateFormat.format(transaction.timestamp)
    }

    override fun getItemCount() = transactions.size
}