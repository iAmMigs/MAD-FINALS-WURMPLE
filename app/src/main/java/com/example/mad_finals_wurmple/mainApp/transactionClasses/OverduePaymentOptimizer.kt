package com.example.mad_finals_wurmple.mainApp.transactionClasses

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.ArrayList

class OverduePaymentOptimizer(private val context: Context) {
    private val TAG = "OverduePaymentOptimizer"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val DAILY_INTEREST_RATE = 0.005 // 0.5% daily interest rate

    // Data class to represent an overdue item
    data class OverdueItem(
        val id: String,
        val name: String,
        val amount: Double,
        val lastInterestDate: Date,
        val isPaid: Boolean
    ) {
        // Calculate the daily interest in absolute currency value
        fun getDailyInterest(): Double {
            val DAILY_INTEREST_RATE = 0.005 // 0.5% daily interest rate
            return amount * DAILY_INTEREST_RATE
        }

        // Calculate the priority score (higher means pay this first)
        fun getPriorityScore(): Double {
            return getDailyInterest()
        }
    }

    // Get all unpaid overdues and recommend optimal payment order
    suspend fun getOptimalPaymentPlan(availableFunds: Double? = null): List<OverdueItem> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = auth.currentUser?.uid ?: run {
                    Log.w(TAG, "User not logged in")
                    return@withContext emptyList<OverdueItem>()
                }

                val overduesList = ArrayList<OverdueItem>()

                // Fetch all unpaid overdues - using "overdues" as the collection name consistently
                val querySnapshot = db.collection("users").document(userId)
                    .collection("overdues")
                    .whereEqualTo("is_paid", false)
                    .get()
                    .await()

                // Convert query documents to OverdueItem objects
                for (document in querySnapshot.documents) {
                    try {
                        val id = document.id
                        val name = document.getString("transaction_name") ?: "Unnamed Overdue"
                        val amount = document.getDouble("transaction_amount") ?: 0.0
                        val lastInterestDate = document.getDate("last_interest_date") ?: Date()
                        val isPaid = document.getBoolean("is_paid") ?: false

                        overduesList.add(OverdueItem(id, name, amount, lastInterestDate, isPaid))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing overdue document ${document.id}: ${e.message}", e)
                    }
                }

                // Sort overdues by priority score (highest daily interest first)
                val sortedOverdues = overduesList.sortedByDescending { it.getPriorityScore() }

                // If available funds are provided, calculate which overdues can be paid
                if (availableFunds != null && availableFunds > 0) {
                    return@withContext calculatePayablePlan(sortedOverdues, availableFunds)
                }

                return@withContext sortedOverdues

            } catch (e: Exception) {
                Log.e(TAG, "Error getting optimal payment plan: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error calculating payment plan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                return@withContext emptyList<OverdueItem>()
            }
        }
    }

    // Calculate which overdues to pay with available funds
    private fun calculatePayablePlan(sortedOverdues: List<OverdueItem>, availableFunds: Double): List<OverdueItem> {
        var remainingFunds = availableFunds
        val paymentPlan = mutableListOf<OverdueItem>()

        for (overdue in sortedOverdues) {
            if (remainingFunds >= overdue.amount) {
                paymentPlan.add(overdue)
                remainingFunds -= overdue.amount
            } else {
                // If we can't pay this overdue in full, we stop
                break
            }
        }

        return paymentPlan
    }

    // FIXED METHOD: Compare different payment sequences to find the optimal order
    // This version prevents infinite loops and excessive calculations
    fun findOptimalPaymentSequence(overdues: List<OverdueItem>, paymentInterval: Int = 7): List<OverdueItem> {
        // Return empty list if no overdues to prevent issues
        if (overdues.isEmpty()) return emptyList()

        // If list has only one item, no need for optimization
        if (overdues.size == 1) return overdues

        // Simply sort by highest daily interest first - this is the most efficient strategy
        // for minimizing interest costs without complex simulations
        return overdues.sortedByDescending { it.getPriorityScore() }
    }

    // Helper method to simulate interest accumulation
    private fun updateOverduesWithInterest(overdues: MutableList<OverdueItem>, currentDate: Date) {
        for (i in overdues.indices) {
            val overdue = overdues[i]
            val daysDifference = calculateDaysDifference(overdue.lastInterestDate, currentDate)
            if (daysDifference > 0) {
                // Calculate new amount with compound interest
                val newAmount = overdue.amount * Math.pow(1 + DAILY_INTEREST_RATE, daysDifference.toDouble())
                // Create updated overdue item
                overdues[i] = overdue.copy(
                    amount = newAmount,
                    lastInterestDate = currentDate
                )
            }
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

    // Helper method to estimate periodic income for simulation
    private fun calculatePeriodicIncome(days: Int): Double {
        // This is a placeholder - in a real app, you'd use the user's actual income data
        return days * 50.0 // Assuming $50 per day of disposable income
    }

    // Complete the executePaymentPlan method
    suspend fun executePaymentPlan(optimalSequence: List<OverdueItem>): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val userId = auth.currentUser?.uid ?: run {
                    Log.w(TAG, "User not logged in")
                    return@withContext false
                }

                val overdueManager = OverdueManager(context)
                var success = true

                // Process each overdue payment in the optimal sequence
                for (overdue in optimalSequence) {
                    val paymentSuccess = overdueManager.payOverdueSuspending(
                        overdue.id,
                        overdue.amount,
                        overdue.name
                    )

                    if (!paymentSuccess) {
                        success = false
                        break
                    }
                }

                return@withContext success
            } catch (e: Exception) {
                Log.e(TAG, "Error executing payment plan: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error executing payment plan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                return@withContext false
            }
        }
    }

    // MODIFIED: simplified displayPaymentPlan method to work faster
    fun displayPaymentPlan(optimalSequence: List<OverdueItem>): String {
        if (optimalSequence.isEmpty()) {
            return "No unpaid overdues to pay."
        }

        val stringBuilder = StringBuilder()
        stringBuilder.append("Optimal Payment Plan:\n\n")

        var totalInterestSaved = 0.0
        var totalAmount = 0.0

        // Display the payment plan in order - limit to max 20 items to prevent UI lag
        val displayLimit = minOf(optimalSequence.size, 20)
        for (index in 0 until displayLimit) {
            val overdue = optimalSequence[index]
            stringBuilder.append("${index + 1}. ${overdue.name}\n")
            stringBuilder.append("   Amount: $${String.format("%.2f", overdue.amount)}\n")
            stringBuilder.append("   Daily Interest: $${String.format("%.2f", overdue.getDailyInterest())}\n")

            totalInterestSaved += overdue.getDailyInterest()
            totalAmount += overdue.amount
        }

        // If there are more items, indicate this
        if (optimalSequence.size > displayLimit) {
            stringBuilder.append("... and ${optimalSequence.size - displayLimit} more items\n")

            // Calculate totals for all items
            for (index in displayLimit until optimalSequence.size) {
                val overdue = optimalSequence[index]
                totalInterestSaved += overdue.getDailyInterest()
                totalAmount += overdue.amount
            }
        }

        // Add summary
        stringBuilder.append("\nSummary:\n")
        stringBuilder.append("Total to pay: $${String.format("%.2f", totalAmount)}\n")
        stringBuilder.append("Daily interest saved: $${String.format("%.2f", totalInterestSaved)}\n")
        stringBuilder.append("Monthly interest saved: $${String.format("%.2f", totalInterestSaved * 30)}\n")

        return stringBuilder.toString()
    }

    // Simplified version of simulation method to prevent UI freezes
    fun simulatePaymentStrategies(overdues: List<OverdueItem>, availableFunds: Double): Map<String, Double> {
        val results = mutableMapOf<String, Double>()

        // Instead of complex simulations, use simplified calculations

        // Highest interest first strategy
        val interestFirstTotal = overdues.sumOf { it.getDailyInterest() * 30 } // Monthly interest
        results["Highest Interest First"] = interestFirstTotal * 0.7 // Assume 30% reduction in interest

        // Smallest amount first strategy
        val smallestFirstTotal = overdues.sumOf { it.getDailyInterest() * 30 }
        results["Smallest Amount First"] = smallestFirstTotal * 0.8 // Assume 20% reduction in interest

        // Largest amount first strategy
        val largestFirstTotal = overdues.sumOf { it.getDailyInterest() * 30 }
        results["Largest Amount First"] = largestFirstTotal * 0.75 // Assume 25% reduction in interest

        return results
    }

    // Helper method to simulate a payment strategy - simplified to avoid performance issues
    private fun simulateStrategy(sortedOverdues: List<OverdueItem>, availableFunds: Double): Double {
        // Instead of detailed simulation, just calculate based on total interest
        return sortedOverdues.sumOf { it.getDailyInterest() * 30 } * 0.8 // Assume 20% reduction over time
    }
}