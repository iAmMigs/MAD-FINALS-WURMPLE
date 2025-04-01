package com.example.mad_finals_wurmple.mainApp.transactionClasses

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import kotlinx.coroutines.tasks.await
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
        try {
            val userId = auth.currentUser?.uid ?: run {
                Log.w(TAG, "User not logged in")
                throw Exception("User not logged in")
            }
            
            val overduesList = ArrayList<OverdueItem>()
            
            // Fetch all unpaid overdues
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
                return calculatePayablePlan(sortedOverdues, availableFunds)
            }
            
            return sortedOverdues
            
        } catch (e: Exception) {
            Log.e(TAG, "Error getting optimal payment plan: ${e.message}", e)
            Toast.makeText(context, "Error calculating payment plan: ${e.message}", Toast.LENGTH_SHORT).show()
            return emptyList()
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
                // (We could implement partial payments, but that's a more complex feature)
                break
            }
        }
        
        return paymentPlan
    }
    
    // Compare different payment sequences to find the optimal order
    // This uses a simplified approach to the traveling salesman problem
    fun findOptimalPaymentSequence(overdues: List<OverdueItem>, paymentInterval: Int = 7): List<OverdueItem> {
        // If list is empty or has only one item, no need for optimization
        if (overdues.size <= 1) return overdues
        
        // Create a copy of the list that we can modify
        val remainingOverdues = overdues.toMutableList()
        val optimalSequence = mutableListOf<OverdueItem>()
        
        val today = Date()
        var simulatedDate = Date(today.time)
        var availableFunds = 0.0 // Start with zero funds
        
        // Simulate the payment process over time
        while (remainingOverdues.isNotEmpty()) {
            // Update all overdue amounts with compound interest since last calculation
            updateOverduesWithInterest(remainingOverdues, simulatedDate)
            
            // Add funds based on payment interval
            // This is a simplification - in reality, the user might have varying income
            availableFunds += calculatePeriodicIncome(paymentInterval)
            
            // Find the overdue with highest priority (daily interest)
            val nextToPay = remainingOverdues.maxByOrNull { it.getPriorityScore() } ?: break
            
            // Check if we can pay it
            if (availableFunds >= nextToPay.amount) {
                optimalSequence.add(nextToPay)
                availableFunds -= nextToPay.amount
                remainingOverdues.remove(nextToPay)
            } else {
                // If we can't pay anything, advance time until next payment period
                simulatedDate = Date(simulatedDate.time + paymentInterval * 86400000L) // Add days in milliseconds
            }
        }
        
        return optimalSequence
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
    // This should be customized based on user's actual income pattern
    private fun calculatePeriodicIncome(days: Int): Double {
        // This is a placeholder - in a real app, you'd use the user's actual income data
        return days * 50.0 // Assuming $50 per day of disposable income
    }
    
    // Execute the payment plan
    suspend fun executePaymentPlan(optimalSequence: List<OverdueItem>): Boolean {
        try {
            val overdueManager = OverdueManager(context)
            var allPaymentsSuccessful = true
            
            for (overdue in optimalSequence) {
                try {
                    // This would need to be modified to work with the existing OverdueManager
                    // We're using a suspended function here because we want to wait for each payment
                    // to complete before moving to the next one
                    overdueManager.payOverdue(overdue.id, overdue.amount, overdue.name)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to pay overdue ${overdue.id}: ${e.message}", e)
                    allPaymentsSuccessful = false
                    // Continue with other payments even if one fails
                }
            }
            
            return allPaymentsSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Error executing payment plan: ${e.message}", e)
            Toast.makeText(context, "Failed to execute payment plan: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }
    
    // Calculate the total cost if paying in the given order
    fun calculateTotalCost(sequence: List<OverdueItem>, daysBetweenPayments: Int): Double {
        val overduesCopy = sequence.map { it.copy() }.toMutableList()
        var totalCost = 0.0
        var simulatedDate = Date()
        
        for (i in overduesCopy.indices) {
            // Apply interest for time passed
            if (i > 0) {
                simulatedDate = Date(simulatedDate.time + daysBetweenPayments * 86400000L)
            }
            
            // Update all remaining overdues with interest
            for (j in i until overduesCopy.size) {
                val overdue = overduesCopy[j]
                val daysDifference = calculateDaysDifference(overdue.lastInterestDate, simulatedDate)
                if (daysDifference > 0) {
                    val newAmount = overdue.amount * Math.pow(1 + DAILY_INTEREST_RATE, daysDifference.toDouble())
                    overduesCopy[j] = overdue.copy(
                        amount = newAmount,
                        lastInterestDate = simulatedDate
                    )
                }
            }
            
            // Add the cost of the current payment
            totalCost += overduesCopy[i].amount
        }
        
        return totalCost
    }
    
    // Advanced optimization using a full Traveling Salesman approach
    // This will check all possible permutations for small sets of overdues
    fun findOptimalSequenceExhaustive(overdues: List<OverdueItem>, daysBetweenPayments: Int): List<OverdueItem> {
        // For small sets, we can try all permutations
        // For larger sets, we'd need to use heuristics or approximation algorithms
        if (overdues.size <= 8) { // 8! = 40320 permutations, still reasonable
            var bestSequence = overdues
            var lowestCost = calculateTotalCost(overdues, daysBetweenPayments)
            
            // Generate all permutations
            val allPermutations = generatePermutations(overdues)
            
            for (permutation in allPermutations) {
                val cost = calculateTotalCost(permutation, daysBetweenPayments)
                if (cost < lowestCost) {
                    lowestCost = cost
                    bestSequence = permutation
                }
            }
            
            return bestSequence
        } else {
            // For larger sets, fall back to greedy approach
            return findOptimalPaymentSequence(overdues, daysBetweenPayments)
        }
    }
    
    // Generate all permutations of a list
    private fun <T> generatePermutations(list: List<T>): List<List<T>> {
        if (list.isEmpty()) return listOf(emptyList())
        
        val result = mutableListOf<List<T>>()
        
        for (i in list.indices) {
            val current = list[i]
            val remaining = list.subList(0, i) + list.subList(i + 1, list.size)
            
            for (permutation in generatePermutations(remaining)) {
                result.add(listOf(current) + permutation)
            }
        }
        
        return result
    }
    
    // Display the recommended payment plan to the user
    fun displayPaymentPlan(plan: List<OverdueItem>): String {
        val sb = StringBuilder()
        sb.appendLine("Optimal Payment Plan:")
        sb.appendLine("----------------------")
        
        var totalSavings = 0.0
        
        // Calculate cost of optimal plan
        val optimalCost = calculateTotalCost(plan, 7) // Assuming weekly payments
        
        // Calculate cost if paid in original order (unsorted)
        val originalCost = calculateTotalCost(plan.sortedBy { it.id }, 7) // Sort by ID as proxy for original order
        
        totalSavings = originalCost - optimalCost
        
        for (i in plan.indices) {
            val overdue = plan[i]
            sb.appendLine("${i+1}. ${overdue.name}: $${String.format("%.2f", overdue.amount)}")
            sb.appendLine("   Daily Interest: $${String.format("%.2f", overdue.getDailyInterest())}/day")
        }
        
        sb.appendLine("----------------------")
        sb.appendLine("Total to pay: $${String.format("%.2f", optimalCost)}")
        if (totalSavings > 0) {
            sb.appendLine("You'll save approximately: $${String.format("%.2f", totalSavings)}")
            sb.appendLine("compared to paying in random order")
        }
        
        return sb.toString()
    }
}