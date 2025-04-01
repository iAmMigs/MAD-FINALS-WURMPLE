package com.example.mad_finals_wurmple.mainApp.transactionClasses

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.mad_finals_wurmple.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class goalClass(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    fun showConfirmationDialog(inputField: EditText, onGoalUpdated: (Double) -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.goal_confirmation_dialog, null)
        val alertDialog = AlertDialog.Builder(context).setView(dialogView).create()

        val btnConfirm = dialogView.findViewById<Button>(R.id.btn_confirm_goal)
        val btnCancel = dialogView.findViewById<Button>(R.id.btn_cancel_goal)

        btnConfirm.setOnClickListener {
            alertDialog.dismiss()
            try {
                val newGoal = inputField.text.toString().toDouble()
                updateGoal(newGoal, onGoalUpdated)
            } catch (e: NumberFormatException) {
                Toast.makeText(context, "Please enter a valid number", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun updateGoal(newGoal: Double, onGoalUpdated: (Double) -> Unit) {
        if (userId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = db.collection("users").document(userId)

        // When setting a new goal, reset goalProgress to 0
        userRef.update(
            mapOf(
                "goal" to newGoal,
                "goalProgress" to 0.0
            )
        )
            .addOnSuccessListener {
                Toast.makeText(context, "Goal updated successfully", Toast.LENGTH_SHORT).show()
                onGoalUpdated(newGoal)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to update goal", Toast.LENGTH_SHORT).show()
            }
    }

    fun getGoalProgress(onProgressUpdated: (Double, Double) -> Unit) {
        if (userId == null) return

        val userRef = db.collection("users").document(userId)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                // Instead of using goalProgress field, calculate it based on balance
                val goal = document.getDouble("goal") ?: 0.0
                val balance = document.getDouble("balance") ?: 0.0

                // Check if there are any unpaid overdues
                checkUnpaidOverdues { hasUnpaidOverdues ->
                    if (hasUnpaidOverdues) {
                        // If there are unpaid overdues, progress is 0
                        onProgressUpdated(0.0, goal)
                    } else {
                        // If all overdues are paid, use balance as progress
                        onProgressUpdated(balance, goal)
                    }
                }
            }
        }
    }

    // Helper method to check if there are any unpaid overdues
    private fun checkUnpaidOverdues(callback: (Boolean) -> Unit) {
        if (userId == null) {
            callback(false)
            return
        }

        db.collection("users").document(userId)
            .collection("overdues")
            .whereEqualTo("is_paid", false)
            .limit(1) // We only need to know if at least one exists
            .get()
            .addOnSuccessListener { querySnapshot ->
                // If the query returns any documents, there are unpaid overdues
                callback(!querySnapshot.isEmpty)
            }
            .addOnFailureListener {
                // On failure, assume no unpaid overdues for safety
                callback(false)
            }
    }

    // Method to ensure the goal field exists in the database
    fun ensureGoalFieldExists() {
        if (userId == null) return

        val userRef = db.collection("users").document(userId)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val updates = mutableMapOf<String, Any>()

                // Check if goal field exists, if not create it
                if (!document.contains("goal")) {
                    updates["goal"] = 0.0
                }

                // We no longer need the goalProgress field since we use balance
                // But we'll keep it in the database for backward compatibility
                if (!document.contains("goalProgress")) {
                    updates["goalProgress"] = 0.0
                }

                // Only update if we have changes to make
                if (updates.isNotEmpty()) {
                    userRef.update(updates)
                }
            }
        }
    }

    // Method to update goal progress in the UI without changing the database
    // This is useful for immediate UI feedback while the database is being updated
    fun updateGoalProgressUI(onProgressUpdated: (Double, Double) -> Unit) {
        if (userId == null) return

        val userRef = db.collection("users").document(userId)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val goal = document.getDouble("goal") ?: 0.0
                val balance = document.getDouble("balance") ?: 0.0

                // Check if there are any unpaid overdues
                checkUnpaidOverdues { hasUnpaidOverdues ->
                    if (hasUnpaidOverdues) {
                        // If there are unpaid overdues, progress is 0
                        onProgressUpdated(0.0, goal)
                    } else {
                        // If all overdues are paid, use balance as progress
                        onProgressUpdated(balance, goal)
                    }
                }
            }
        }
    }
}