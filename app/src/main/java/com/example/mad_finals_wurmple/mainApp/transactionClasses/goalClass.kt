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
                val goalProgress = document.getDouble("goalProgress") ?: 0.0
                val goal = document.getDouble("goal") ?: 0.0
                onProgressUpdated(goalProgress, goal)
            }
        }
    }

    // Method to ensure the goalProgress field exists in the database
    fun ensureGoalProgressFieldExists() {
        if (userId == null) return

        val userRef = db.collection("users").document(userId)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists() && !document.contains("goalProgress")) {
                // If goalProgress field doesn't exist, create it
                userRef.update("goalProgress", 0.0)
            }
        }
    }
}