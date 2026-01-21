package com.example.pda

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.pda.R
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.firestore.FirebaseFirestore

class DeliveryActivity : ComponentActivity() {

    private var clickCount05 = 0
    private var clickCount2 = 0
    private var clickCount25 = 0
    private var clickCount3 = 0
    private var clickCount5 = 0

    private var generalTotal: Double = 0.0
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var clickCountTextView05: TextView
    private lateinit var clickCountTextView2: TextView
    private lateinit var clickCountTextView25: TextView
    private lateinit var clickCountTextView3: TextView
    private lateinit var clickCountTextView5: TextView

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("click_count_05", clickCount05)
        outState.putInt("click_count_2", clickCount2)
        outState.putInt("click_count_25", clickCount25)
        outState.putInt("click_count_3", clickCount3)
        outState.putInt("click_count_5", clickCount5)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery)

        // Restore click counts from savedInstanceState
        if (savedInstanceState != null) {
            clickCount05 = savedInstanceState.getInt("click_count_05", 0)
            clickCount2 = savedInstanceState.getInt("click_count_2", 0)
            clickCount25 = savedInstanceState.getInt("click_count_25", 0)
            clickCount3 = savedInstanceState.getInt("click_count_3", 0)
            clickCount5 = savedInstanceState.getInt("click_count_5", 0)
        }

        // Initialize Firebase Analytics
        firebaseAnalytics = Firebase.analytics

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        // Load the click counts from Firestore
        loadClickCountsFromFirestore()

        // Set up Firestore real-time listener
        setupFirestoreListener()

        // Fetch the current total from Firestore
        loadCurrentGeneralTotal()

        // Initialize buttons
        val add05Button: Button = findViewById(R.id.add05)
        val add2Button: Button = findViewById(R.id.add2)
        val add25Button: Button = findViewById(R.id.add25)
        val add3Button: Button = findViewById(R.id.add3)
        val add5Button: Button = findViewById(R.id.add5)
        val clearClickCountsButton: Button = findViewById(R.id.clear_click_counts_button)

        // Set click listeners for buttons
        add05Button.setOnClickListener { updateTotal(0.5) }
        add2Button.setOnClickListener { updateTotal(2.0) }
        add25Button.setOnClickListener { updateTotal(2.5) }
        add3Button.setOnClickListener { updateTotal(3.0) }
        add5Button.setOnClickListener { updateTotal(5.0) }

        // Set click listener for the "Clear Click Counts" button
        clearClickCountsButton.setOnClickListener { clearClickCounts() }

        // Initialize the TextView for click counts
        clickCountTextView05 = findViewById(R.id.tv_count_05)
        clickCountTextView2 = findViewById(R.id.tv_count_2)
        clickCountTextView25 = findViewById(R.id.tv_count_25)
        clickCountTextView3 = findViewById(R.id.tv_count_3)
        clickCountTextView5 = findViewById(R.id.tv_count_5)

        // Update the UI with the restored click counts
        updateClickCountTextView()
    }


    // Fetch the current generalTotal from Firestore
    private fun loadCurrentGeneralTotal() {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("pda").document("cafe")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    generalTotal = document.getDouble("generalTotal") ?: 0.0
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch total: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTotal(amount: Double) {
        // Add the amount to the existing general total
        generalTotal += amount

        // Update the click count based on the amount
        when (amount) {
            0.5 -> clickCount05++
            2.0 -> clickCount2++
            2.5 -> clickCount25++
            3.0 -> clickCount3++
            5.0 -> clickCount5++
        }
        // Save the updated click counts to Firestore
        saveClickCountsToFirestore()

        // Log the button click event with the amount and click count
        val bundle = Bundle().apply {
            putDouble("amount_added", amount)
            putDouble("new_total", generalTotal)
            putInt("click_count_05", clickCount05)
            putInt("click_count_2", clickCount2)
            putInt("click_count_25", clickCount25)
            putInt("click_count_3", clickCount3)
            putInt("click_count_5", clickCount5)
        }
        firebaseAnalytics.logEvent("amount_added", bundle)

        // Save the updated total
        saveGeneralTotal(amount)

        // Pass the updated total back to MainActivity
        val resultIntent = Intent()
        resultIntent.putExtra("updatedGeneralTotal", generalTotal)
        setResult(RESULT_OK, resultIntent)

        // Update the TextView with the click counts
        updateClickCountTextView()
    }

    private fun updateClickCountTextView() {
        clickCountTextView05.text = "Delivery: $clickCount05"
        clickCountTextView2.text = "Delivery: $clickCount2"
        clickCountTextView25.text = "Delivery: $clickCount25"
        clickCountTextView3.text = "Delivery: $clickCount3"
        clickCountTextView5.text = "Delivery: $clickCount5"
    }

    private fun clearClickCounts() {
        AlertDialog.Builder(this)
            .setTitle("Καθαρισμός")
            .setMessage("Είσαι σίγουρος πως θες να καθαρίσεις τα delivery;")
            .setPositiveButton("Ναι") { _, _ ->
                // Reset the click counts
                clickCount05 = 0
                clickCount2 = 0
                clickCount25 = 0
                clickCount3 = 0
                clickCount5 = 0

                // Save the cleared click counts to Firestore
                saveClickCountsToFirestore()

                // Update the UI
                updateClickCountTextView()

                // Show a success message
                Toast.makeText(this, "Deliveries καθαρίστηκαν!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Όχι", null)
            .show()
    }


    private fun saveClickCountsToFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        val clickCounts = hashMapOf(
            "click_count_05" to clickCount05,
            "click_count_2" to clickCount2,
            "click_count_25" to clickCount25,
            "click_count_3" to clickCount3,
            "click_count_5" to clickCount5
        )
        firestore.collection("pda").document("cafe")
            .update(clickCounts as Map<String, Any>)
            .addOnSuccessListener {
               // Log.d("Firestore", "Click counts updated successfully")
            }
            .addOnFailureListener { e ->
               // Log.e("Firestore", "Failed to update click counts: ${e.message}")
            }
    }

    private fun loadClickCountsFromFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("pda").document("cafe")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    clickCount05 = document.getLong("click_count_05")?.toInt() ?: 0
                    clickCount2 = document.getLong("click_count_2")?.toInt() ?: 0
                    clickCount25 = document.getLong("click_count_25")?.toInt() ?: 0
                    clickCount3 = document.getLong("click_count_3")?.toInt() ?: 0
                    clickCount5 = document.getLong("click_count_5")?.toInt() ?: 0

                    // Update the UI with the new click counts
                    updateClickCountTextView()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to load click counts: ${e.message}")
            }
    }

    private fun setupFirestoreListener() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("pda").document("cafe")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("Firestore", "Failed to listen for updates: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    clickCount05 = snapshot.getLong("click_count_05")?.toInt() ?: 0
                    clickCount2 = snapshot.getLong("click_count_2")?.toInt() ?: 0
                    clickCount25 = snapshot.getLong("click_count_25")?.toInt() ?: 0
                    clickCount3 = snapshot.getLong("click_count_3")?.toInt() ?: 0
                    clickCount5 = snapshot.getLong("click_count_5")?.toInt() ?: 0

                    // Update the UI with the new click counts
                    updateClickCountTextView()
                }
            }
    }


    private fun saveGeneralTotal(amount: Double) {
        val firestore = FirebaseFirestore.getInstance()

        // Update the generalTotal in Firestore
        firestore.collection("pda").document("cafe")
            .update("generalTotal", generalTotal)
            .addOnSuccessListener {
                // Save the generalTotal to SharedPreferences
                sharedPreferences.edit().putFloat("generalTotal", generalTotal.toFloat()).apply()

                // Log the general total updated event
                val bundle = Bundle().apply {
                    putDouble("new_total", generalTotal)
                }
                firebaseAnalytics.logEvent("general_total_updated", bundle)

                // Show a success message with the amount added
               // Toast.makeText(this, "Προστέθηκε $amount€", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update General Total: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}