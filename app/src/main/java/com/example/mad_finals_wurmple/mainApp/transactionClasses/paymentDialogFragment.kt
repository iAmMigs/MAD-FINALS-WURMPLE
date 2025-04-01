package com.example.mad_finals_wurmple.mainApp.transactionClasses

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.example.mad_finals_wurmple.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.parcelize.Parcelize
import java.util.*

class PaymentDialogFragment : DialogFragment() {

    private lateinit var txtPaymentAmount: TextView
    private lateinit var btnConfirmPayment: Button
    private lateinit var btnCancel: Button

    private var totalAmount: Double = 0.0
    private var selectedOverdues: ArrayList<SerializableOverdueData> = arrayListOf()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var overdueManager: OverdueManager

    // Use a serializable class instead of Parcelable to avoid plugin dependencies
    data class SerializableOverdueData(
        val transactionId: String,
        val name: String,
        val amount: Double,
        val date: Long, // Store date as long (milliseconds)
        val lastInterestDate: Long, // Store date as long (milliseconds)
        val isPaid: Boolean,
        val isSelected: Boolean
    ) : java.io.Serializable {
        // Convert from OverdueHistoryManager.OverdueData
        companion object {
            private const val serialVersionUID = 1L

            fun from(data: OverdueHistoryManager.OverdueData): SerializableOverdueData {
                return SerializableOverdueData(
                    transactionId = data.transactionId,
                    name = data.name,
                    amount = data.amount,
                    date = data.date.time,
                    lastInterestDate = data.lastInterestDate.time,
                    isPaid = data.isPaid,
                    isSelected = data.isSelected
                )
            }
        }

        // Convert back to OverdueHistoryManager.OverdueData
        fun toOverdueData(): OverdueHistoryManager.OverdueData {
            return OverdueHistoryManager.OverdueData(
                transactionId = transactionId,
                name = name,
                amount = amount,
                date = Date(date),
                lastInterestDate = Date(lastInterestDate),
                isPaid = isPaid,
                isSelected = isSelected
            )
        }
    }

    companion object {
        private const val ARG_TOTAL_AMOUNT = "total_amount"
        private const val ARG_SELECTED_OVERDUES = "selected_overdues"

        fun newInstance(totalAmount: Double, selectedOverdues: List<OverdueHistoryManager.OverdueData>): PaymentDialogFragment {
            val fragment = PaymentDialogFragment()
            val args = Bundle()
            args.putDouble(ARG_TOTAL_AMOUNT, totalAmount)

            // Convert to SerializableOverdueData to avoid serialization issues
            val serializableOverdues = selectedOverdues.map { SerializableOverdueData.from(it) }
            args.putSerializable(ARG_SELECTED_OVERDUES, ArrayList(serializableOverdues))

            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Get arguments
        totalAmount = arguments?.getDouble(ARG_TOTAL_AMOUNT, 0.0) ?: 0.0

        // Get the serializable ArrayList
        @Suppress("UNCHECKED_CAST")
        selectedOverdues = arguments?.getSerializable(ARG_SELECTED_OVERDUES) as? ArrayList<SerializableOverdueData>
            ?: arrayListOf()

        // Initialize OverdueManager
        overdueManager = OverdueManager(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.payment_dialog_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        txtPaymentAmount = view.findViewById(R.id.txt_payment_amount)
        btnConfirmPayment = view.findViewById(R.id.btn_confirm_payment)
        btnCancel = view.findViewById(R.id.btn_cancel)

        // Set total amount text
        txtPaymentAmount.text = "Total amount: $${String.format("%.2f", totalAmount)}"

        // Set button click listeners
        btnConfirmPayment.setOnClickListener {
            processPayment()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun processPayment() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        // Check if user has enough balance
        val userRef = db.collection("users").document(userId)
        userRef.get().addOnSuccessListener { document ->
            val currentBalance = document.getDouble("balance") ?: 0.0

            if (currentBalance < totalAmount) {
                Toast.makeText(context, "Insufficient balance to pay selected overdues", Toast.LENGTH_SHORT).show()
                dismiss()
                return@addOnSuccessListener
            }

            // Process payment for each selected overdue
            var successfulPayments = 0

            try {
                // First update the balance
                userRef.update("balance", currentBalance - totalAmount)
                    .addOnSuccessListener {
                        // Then process each overdue
                        for (serializableOverdue in selectedOverdues) {
                            val overdueRef = db.collection("users").document(userId)
                                .collection("overdues").document(serializableOverdue.transactionId)

                            overdueRef.update("is_paid", true)
                                .addOnSuccessListener {
                                    successfulPayments++

                                    // When all payments are processed, show success message
                                    if (successfulPayments == selectedOverdues.size) {
                                        Toast.makeText(
                                            context,
                                            "Successfully paid $successfulPayments ${if (successfulPayments == 1) "overdue" else "overdues"}",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // Send result back to activity to trigger refresh
                                        setFragmentResult("payment_completed", bundleOf("success" to true))

                                        dismiss()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Error marking overdue as paid: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Error updating balance: ${e.message}", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
            } catch (e: Exception) {
                Toast.makeText(context, "Error during payment: ${e.message}", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error checking balance: ${e.message}", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }
}