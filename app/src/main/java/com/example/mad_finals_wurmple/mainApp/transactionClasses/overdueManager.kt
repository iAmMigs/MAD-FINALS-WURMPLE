package com.example.mad_finals_wurmple.mainApp.transactionClasses

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OverdueManager(private val context: Context) {
    private val TAG = "OverdueManager"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val DAILY_INTEREST_RATE = 0.005 // 0.5% daily interest

    // Call this method whenever you want to update overdues (e.g., on app start or when viewing overdues)
    fun calculateAndApplyOverdueInterest() {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "User not logged in")
                return
            }

            val overduesRef = db.collection("users").document(userId).collection("overdues")

            // Get all unpaid overdues
            overduesRef.whereEqualTo("is_paid", false)
                .get()
                .addOnSuccessListener { documents ->
                    try {
                        val today = Date()

                        for (document in documents) {
                            try {
                                val lastInterestDate = document.getDate("last_interest_date")
                                val overdueAmount = document.getDouble("transaction_amount")
                                val overdueName = document.getString("transaction_name") ?: "Overdue"

                                if (lastInterestDate == null || overdueAmount == null) {
                                    Log.w(TAG, "Incomplete overdue data for document ${document.id}")
                                    continue
                                }

                                // Calculate days since last interest was applied
                                val daysDifference = calculateDaysDifference(lastInterestDate, today)

                                if (daysDifference > 0) {
                                    // Calculate compound interest
                                    val interestAmount = calculateCompoundInterest(overdueAmount, daysDifference)

                                    if (interestAmount > 0) {
                                        // Update the overdue document with new last interest date
                                        overduesRef.document(document.id).update(
                                            "last_interest_date", today,
                                            "transaction_amount", overdueAmount + interestAmount
                                        ).addOnFailureListener { e ->
                                            Log.e(TAG, "Failed to update overdue document: ${e.message}", e)
                                        }

                                        // Add interest transaction to expenses
                                        addInterestToExpenses(
                                            userId,
                                            interestAmount,
                                            "Interest for $overdueName (${daysDifference} days)"
                                        )
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error processing overdue document ${document.id}: ${e.message}", e)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing overdues: ${e.message}", e)
                        Toast.makeText(context, "Error updating overdues: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to fetch overdues: ${e.message}", e)
                    Toast.makeText(context, "Failed to update overdues: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error applying overdue interest: ${e.message}", e)
            Toast.makeText(context, "Error updating overdues: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Calculate the number of days between two dates
    private fun calculateDaysDifference(startDate: Date, endDate: Date): Int {
        try {
            val differenceInMillis = endDate.time - startDate.time
            return TimeUnit.MILLISECONDS.toDays(differenceInMillis).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating days difference: ${e.message}", e)
            return 0 // Return 0 days on error to prevent interest calculation
        }
    }

    // Calculate compound interest for the given number of days
    private fun calculateCompoundInterest(principal: Double, days: Int): Double {
        try {
            // Using compound interest formula: P(1 + r)^t - P
            val compoundFactor = Math.pow(1 + DAILY_INTEREST_RATE, days.toDouble())
            val finalAmount = principal * compoundFactor
            return finalAmount - principal
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating compound interest: ${e.message}", e)
            return 0.0 // Return 0 interest on error
        }
    }

    // Add interest transaction to expenses collection
    private fun addInterestToExpenses(userId: String, amount: Double, name: String) {
        try {
            val interestData = hashMapOf(
                "transaction_date" to Date(),
                "transaction_name" to name,
                "transaction_amount" to amount
            )

            // Add to expenses collection
            db.collection("users").document(userId)
                .collection("expense")
                .add(interestData)
                .addOnSuccessListener {
                    // Also update the user's balance
                    updateUserBalance(userId, -amount)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to add interest expense: ${e.message}", e)
                    Toast.makeText(context, "Failed to add interest expense: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding interest to expenses: ${e.message}", e)
            Toast.makeText(context, "Error recording interest charge: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Update user's balance to reflect the interest expense
    private fun updateUserBalance(userId: String, amountChange: Double) {
        try {
            val userRef = db.collection("users").document(userId)

            db.runTransaction { transaction ->
                try {
                    val snapshot = transaction.get(userRef)
                    val currentBalance = snapshot.getDouble("balance") ?: 0.0

                    // Update the balance
                    transaction.update(userRef, "balance", currentBalance + amountChange)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in balance update transaction: ${e.message}", e)
                    throw e // Re-throw to trigger the failure listener
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to update balance: ${e.message}", e)
                Toast.makeText(context, "Failed to update balance: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user balance: ${e.message}", e)
            Toast.makeText(context, "Error updating balance: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Method to pay off an overdue (fixed transaction handling)
    fun payOverdue(overdueId: String, paymentAmount: Double, paymentName: String) {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                return
            }

            val overdueRef = db.collection("users").document(userId)
                .collection("overdues").document(overdueId)

            // First get the current overdue amount
            overdueRef.get().addOnSuccessListener { overdueDoc ->
                val currentOverdueAmount = overdueDoc.getDouble("transaction_amount") ?: 0.0

                if (paymentAmount >= currentOverdueAmount) {
                    // Mark overdue as paid if payment covers full amount
                    overdueRef.update("is_paid", true)

                    // Calculate change
                    val change = paymentAmount - currentOverdueAmount
                    val amountPaid = currentOverdueAmount

                    // Add the payment to income collection
                    addTransactionToCollection(
                        userId,
                        amountPaid,
                        "Payment for overdue: $paymentName",
                        "income",
                        Date()
                    )

                    // Update user balance
                    updateUserBalance(userId, amountPaid)

                    Toast.makeText(
                        context,
                        "Payment successful. ${if (change > 0) "Change: $${String.format("%.2f", change)}" else ""}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    // Reduce overdue amount by payment
                    overdueRef.update("transaction_amount", currentOverdueAmount - paymentAmount)

                    // Add the payment to income collection
                    addTransactionToCollection(
                        userId,
                        paymentAmount,
                        "Partial payment for overdue: $paymentName",
                        "income",
                        Date()
                    )

                    // Update user balance
                    updateUserBalance(userId, paymentAmount)

                    Toast.makeText(context, "Partial payment successful", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch overdue: ${e.message}", e)
                Toast.makeText(context, "Payment failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing overdue payment: ${e.message}", e)
            Toast.makeText(context, "Error processing payment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Added suspending version for use with the OverduePaymentOptimizer
    suspend fun payOverdueSuspending(overdueId: String, paymentAmount: Double, paymentName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext false
                }

                val overdueRef = db.collection("users").document(userId)
                    .collection("overdues").document(overdueId)

                // Get the current overdue amount
                val overdueDoc = overdueRef.get().await()
                val currentOverdueAmount = overdueDoc.getDouble("transaction_amount") ?: 0.0

                if (paymentAmount >= currentOverdueAmount) {
                    // Mark overdue as paid if payment covers full amount
                    overdueRef.update("is_paid", true).await()

                    // Calculate change
                    val change = paymentAmount - currentOverdueAmount
                    val amountPaid = currentOverdueAmount

                    // Add the payment to income collection
                    addTransactionToCollection(
                        userId,
                        amountPaid,
                        "Payment for overdue: $paymentName",
                        "income",
                        Date()
                    )

                    // Update user balance
                    updateUserBalance(userId, amountPaid)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Payment successful. ${if (change > 0) "Change: $${String.format("%.2f", change)}" else ""}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // Reduce overdue amount by payment
                    overdueRef.update("transaction_amount", currentOverdueAmount - paymentAmount).await()

                    // Add the payment to income collection
                    addTransactionToCollection(
                        userId,
                        paymentAmount,
                        "Partial payment for overdue: $paymentName",
                        "income",
                        Date()
                    )

                    // Update user balance
                    updateUserBalance(userId, paymentAmount)

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Partial payment successful", Toast.LENGTH_SHORT).show()
                    }
                }

                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error processing overdue payment: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error processing payment: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }
        }
    }

    // Helper method to add transactions to collections
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
                    Toast.makeText(context, "Failed to record transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding transaction to collection: ${e.message}", e)
            Toast.makeText(context, "Error recording transaction: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}