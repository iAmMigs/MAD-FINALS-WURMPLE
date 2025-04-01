package com.example.mad_finals_wurmple.mainApp.transactionClasses

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.mad_finals_wurmple.R

class OptimalPaymentDialogFragment : DialogFragment() {

    private lateinit var txtOptimalPlan: TextView
    private lateinit var btnClose: Button

    private var optimalPlanText: String = ""

    companion object {
        private const val ARG_OPTIMAL_PLAN = "optimal_plan"

        fun newInstance(optimalPlan: String): OptimalPaymentDialogFragment {
            val fragment = OptimalPaymentDialogFragment()
            val args = Bundle()
            args.putString(ARG_OPTIMAL_PLAN, optimalPlan)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        optimalPlanText = arguments?.getString(ARG_OPTIMAL_PLAN, "") ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.optimal_payment_dialog_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        txtOptimalPlan = view.findViewById(R.id.txt_optimal_plan)
        btnClose = view.findViewById(R.id.btn_close)

        // Set text
        txtOptimalPlan.text = optimalPlanText

        // Set button click listener
        btnClose.setOnClickListener {
            dismiss()
        }
    }
}