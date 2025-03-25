import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.mad_finals_wurmple.R
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var usernameTxt: TextView
    private lateinit var balanceTxt: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressCircle: CircularProgressIndicator
    private lateinit var etAmount: EditText
    private lateinit var spinnerTransactionType: Spinner
    private lateinit var btnConfirm: Button
    private lateinit var btnDelete: Button
    private lateinit var viewTransactionBtn: Button
    private lateinit var goalEditText: EditText
    private lateinit var btnAddGoal: Button
    private lateinit var tvAmount: TextView

    private var currentBalance: Double = 0.0
    private var monthlyGoal: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dashboard_page)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize UI components
        initializeComponents()

        // Setup transaction type spinner
        setupTransactionTypeSpinner()

        // Fetch user data from Firestore
        fetchUserData()

        // Setup button click listeners
        setupButtonListeners()
    }

    private fun initializeComponents() {
        usernameTxt = findViewById(R.id.menuUsernameTxt)
        balanceTxt = findViewById(R.id.balanceTxt)
        progressBar = findViewById(R.id.progressBar)
        progressCircle = findViewById(R.id.progressCircle)
        etAmount = findViewById(R.id.etAmount)
        spinnerTransactionType = findViewById(R.id.spinnerTransactionType)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnDelete = findViewById(R.id.btnDelete)
        viewTransactionBtn = findViewById(R.id.viewtransactionBtn)
        goalEditText = findViewById(R.id.goalEditText)
        btnAddGoal = findViewById(R.id.btnAddGoal)
        tvAmount = findViewById(R.id.tvAmount)
    }

    private fun setupTransactionTypeSpinner() {
        val transactionTypes = arrayOf("Income", "Expense")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, transactionTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTransactionType.adapter = adapter
    }

    private fun fetchUserData() {
        val userId = "currentUserID" // Replace with actual user authentication method
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    usernameTxt.text = document.getString("username") ?: "User"
                    currentBalance = document.getDouble("balance") ?: 0.0
                    monthlyGoal = document.getDouble("monthlyGoal") ?: 0.0

                    updateBalanceAndGoalUI()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupButtonListeners() {
        btnConfirm.setOnClickListener { addTransaction() }
        btnDelete.setOnClickListener { clearTransaction() }
        viewTransactionBtn.setOnClickListener {
            val intent = Intent(this, transactionActivity::class.java)
            startActivity(intent)
        }
        btnAddGoal.setOnClickListener { updateMonthlyGoal() }
    }

    private fun addTransaction() {
        try {
            val amount = etAmount.text.toString().toDoubleOrNull()
            val transactionType = spinnerTransactionType.selectedItem.toString()

            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                return
            }

            val userId = "currentUserID" // Replace with actual user authentication
            val transaction = hashMapOf(
                "amount" to amount,
                "type" to transactionType,
                "timestamp" to FieldValue.serverTimestamp()
            )

            // Add transaction to Firestore
            db.collection("users").document(userId).collection("transactions")
                .add(transaction)
                .addOnSuccessListener {
                    updateBalance(amount, transactionType)
                    etAmount.text.clear()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error adding transaction: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing transaction: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBalance(amount: Double, transactionType: String) {
        val userId = "currentUserID" // Replace with actual user authentication
        val balanceUpdate = if (transactionType == "Income") amount else -amount

        db.collection("users").document(userId)
            .update("balance", FieldValue.increment(balanceUpdate))
            .addOnSuccessListener {
                currentBalance += balanceUpdate
                updateBalanceAndGoalUI()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating balance: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateBalanceAndGoalUI() {
        balanceTxt.text = String.format("$%.2f", currentBalance)
        tvAmount.text = String.format("$%.2f", currentBalance)

        // Update progress bars
        val progress = if (monthlyGoal > 0) {
            ((currentBalance / monthlyGoal) * 100).toInt().coerceAtMost(100)
        } else {
            0
        }
        progressBar.progress = progress
        progressCircle.progress = progress
    }

    private fun clearTransaction() {
        etAmount.text.clear()
        spinnerTransactionType.setSelection(0)
    }

    private fun updateMonthlyGoal() {
        try {
            val goalAmount = goalEditText.text.toString().toDoubleOrNull()

            if (goalAmount == null || goalAmount <= 0) {
                Toast.makeText(this, "Please enter a valid goal amount", Toast.LENGTH_SHORT).show()
                return
            }

            val userId = "currentUserID" // Replace with actual user authentication
            db.collection("users").document(userId)
                .update("monthlyGoal", goalAmount)
                .addOnSuccessListener {
                    monthlyGoal = goalAmount
                    updateBalanceAndGoalUI()
                    goalEditText.text.clear()
                    Toast.makeText(this, "Monthly goal updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error updating goal: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, "Error processing goal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
