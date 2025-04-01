package com.example.mad_finals_wurmple.mainApp.transactionClasses

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class OverdueManager(private val context: Context) {
    private val TAG = "OverdueManager"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val DAILY_INTEREST_RATE = 0.005 // 0.5% daily interest

    // Method to pay an overdue item
    fun payOverdue(overdueId: String, amount: Double, transactionName: String) {
        val userId = auth.currentUser?.uid ?: return

        db.runTransaction { transaction ->
            // Get user document
            val userRef = db.collection("users").document(userId)
            val userDoc = transaction.get(userRef)

            // Get current balance
            val currentBalance = userDoc.getDouble("balance") ?: 0.0

            // Check if user has enough balance
            if (currentBalance < amount) {
                throw Exception("Insufficient balance to pay overdue")
            }

            // Update user balance
            transaction.update(userRef, "balance", currentBalance - amount)

            // Mark overdue as paid
            val overdueRef = userRef.collection("overdues").document(overdueId)
            transaction.update(overdueRef, "is_paid", true)
        }.addOnSuccessListener {
            Log.d(TAG, "Successfully paid overdue: $overdueId")
            Toast.makeText(context, "Successfully paid: $transactionName", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error paying overdue: ${e.message}", e)
            Toast.makeText(context, "Failed to pay overdue: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Suspending version of payOverdue for use in coroutines
    suspend fun payOverdueSuspending(overdueId: String, amount: Double, transactionName: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false

            // Use a task-based approach since we can't use runTransaction with suspend
            val userRef = db.collection("users").document(userId)
            val userDoc = userRef.get().await()
            // Get current balance
            val currentBalance = userDoc.getDouble("balance") ?: 0.0

            // Check if user has enough balance
            if (currentBalance < amount) {
                throw Exception("Insufficient balance to pay overdue")
            }

            // Update user balance
            userRef.update("balance", currentBalance - amount)

            // Mark overdue as paid
            val overdueRef = userRef.collection("overdues").document(overdueId)
            overdueRef.update("is_paid", true)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error in payOverdueSuspending: ${e.message}", e)
            false
        }
    }

    // Calculate and apply interest to all unpaid overdues
    fun calculateAndApplyOverdueInterest() {
        val userId = auth.currentUser?.uid ?: return

        // Get all unpaid overdues
        db.collection("users").document(userId)
            .collection("overdues")
            .whereEqualTo("is_paid", false)
            .get()
            .addOnSuccessListener { documents ->
                val today = Date()

                for (document in documents) {
                    try {
                        val overdueId = document.id
                        val amount = document.getDouble("transaction_amount") ?: 0.0
                        val lastInterestDate = document.getDate("last_interest_date") ?: today

                        // Calculate days since last interest calculation
                        val daysSinceLastInterest = calculateDaysDifference(lastInterestDate, today)

                        // If at least one day has passed, calculate and apply interest
                        if (daysSinceLastInterest > 0) {
                            // Calculate new amount with compound interest
                            val newAmount = amount * Math.pow(1 + DAILY_INTEREST_RATE, daysSinceLastInterest.toDouble())

                            // Update the overdue with new amount and interest date
                            db.collection("users").document(userId)
                                .collection("overdues")
                                .document(overdueId)
                                .update(
                                    mapOf(
                                        "transaction_amount" to newAmount,
                                        "last_interest_date" to today
                                    )
                                )
                                .addOnSuccessListener {
                                    Log.d(TAG, "Applied interest to overdue $overdueId: Old = $amount, New = $newAmount")
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error updating overdue interest: ${e.message}", e)
                                }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing overdue document ${document.id}: ${e.message}", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting overdues for interest calculation: ${e.message}", e)
            }
    }

    // Calculate number of days between two dates
    private fun calculateDaysDifference(startDate: Date, endDate: Date): Int {
        try {
            val differenceInMillis = endDate.time - startDate.time
            return (differenceInMillis / (1000 * 60 * 60 * 24)).toInt()
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating days difference: ${e.message}", e)
            return 0
        }
    }
}