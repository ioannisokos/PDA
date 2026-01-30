package com.example.pda

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.os.Bundle
import android.graphics.Color
import android.os.Build
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.pda.data.ProductEntity
import com.example.pda.data.TableEntity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {

    data class CategoryEntity(
        val id: String = "", // Firestore auto-generates IDs
        val name: String
    )

    private var generalTotal: Double = 0.0
    private lateinit var generalTotalEditText: EditText
    private lateinit var clearGeneralTotalButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private var isPasswordVerified = false
    private lateinit var totalPriceTextView: TextView
    private var firestoreListener: ListenerRegistration? = null // Listener for real-time updates
    private lateinit var viewModel: GeneralTotalViewModel
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth

    private val tableOrderListeners = mutableMapOf<String, ListenerRegistration>()


    private val deliveryActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val updatedTotal = result.data?.getDoubleExtra("updatedGeneralTotal", 0.0) ?: 0.0
            generalTotal = updatedTotal
            // Update the UI or perform any other actions with the updated total
           // Toast.makeText(this, "Updated General Total: $generalTotal", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase

        FirebaseApp.initializeApp(this)

        // Initialize Firebase Analytics
        firebaseAnalytics = Firebase.analytics
        auth = FirebaseAuth.getInstance()


        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(GeneralTotalViewModel::class.java)

        // Observe changes in generalTotal and update UI
        viewModel.generalTotal.observe(this) { total ->
            generalTotalEditText.setText("%.2f€".format(total))
        }


        generalTotalEditText = findViewById(R.id.et_general_total)
        clearGeneralTotalButton = findViewById(R.id.btn_clear_general_total)

        sharedPreferences = getSharedPreferences("AppPreferences", MODE_PRIVATE)

        // Load saved general total
        generalTotal = sharedPreferences.getFloat("generalTotal", 0f).toDouble()
        hideGeneralTotal() // Hide it initially

        // Require password to edit general total
        generalTotalEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                requestPasswordIfNeeded {
                    showGeneralTotal()
                }
            }
        }

        generalTotalEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                requestPasswordIfNeeded {
                    saveGeneralTotalFromEditText()
                    hideGeneralTotal()
                }
                true
            } else {
                false
            }
        }

        //  Require password to view general total
        generalTotalEditText.setOnClickListener {
            requestPasswordIfNeeded {
                showGeneralTotal()
            }
        }

        // Require password to clear general total
        clearGeneralTotalButton.setOnClickListener {
            requestPasswordIfNeeded {
                // Only show the confirmation dialog after password is verified
                confirmClearGeneralTotal()
            }
        }

        // Initialize views
        totalPriceTextView = findViewById(R.id.total_price_text_view)

        // Setup the more tables button
        val moreTablesButton: ImageButton = findViewById(R.id.btn_more_tables)
        moreTablesButton.setOnClickListener { view ->
            showTablesPopupMenu(view)
        }

        // Set up table buttons
        val tableButtons = (1..24).map {
            findViewById<Button>(resources.getIdentifier("btn_table_$it", "id", packageName))
        }

        tableButtons.forEachIndexed { index, button ->
            val tableNumber = index + 1 // tableNumber is Int
            button.setOnClickListener {
                // Log the table selection event
                val bundle = Bundle().apply {
                    putString("table_number", tableNumber.toString())
                }
                firebaseAnalytics.logEvent("table_selected", bundle)

                Log.d("MainActivityDebug", "Sending tableNumber: $tableNumber")
                val intent = Intent(this, SecondActivity::class.java)
                intent.putExtra("TABLE_NUMBER", tableNumber) // Pass as Int
                startActivityForResult(intent, tableNumber)
            }
        }

        // Set up Orders button listener
        findViewById<Button>(R.id.btn_manage_orders).setOnClickListener {
            val intent = Intent(this, OrdersActivity::class.java)
            startActivity(intent)
        }

        // Set up Delivery button listener
        findViewById<Button>(R.id.btn_manage_deliveries).setOnClickListener {
            val intent = Intent(this,DeliveryActivity::class.java)
           // startActivity(intent)
            deliveryActivityLauncher.launch(intent)
        }

        // Set up Stock button listener
        findViewById<Button>(R.id.btn_manage_stock).setOnClickListener {
            val intent = Intent(this, StockActivity::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btn_manage_dash).setOnClickListener {
            val intent = Intent(this, SalesSummaryActivity::class.java)
            startActivity(intent)
        }


        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("pda").document("cafe")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    val updatedTotal = snapshot.getDouble("generalTotal") ?: 0.0
                    generalTotal = updatedTotal
                    generalTotalEditText.setText("%.2f€".format(generalTotal))
                }
            }


        // Add sample data to Firestore (call this only once)
        //addSampleData()
        setupFirestoreListener()
       // initializeGeneralTotalInFirestore()
        //  Always fetch the latest total when opening the app
        fetchGeneralTotal()
        //tables Color
        setupTableStatusListener()

    }

    private fun addSampleData() {
        // Add categories, products, and tables to Firestore (to add remove the addSample... from comments,then add them back) 
       // addSampleProducts()
       // addSampleTables()
      //  addSampleCategories()
    }

/*
    private fun addSampleCategories() {
        val firestore = FirebaseFirestore.getInstance()
        val categories = listOf(
            "Καφέδες","Delivery"
        )

        for (category in categories) {
            val categoryEntity = CategoryEntity(name = category)
            firestore.collection("pda").document("cafe")
                .collection("categories")
                .add(categoryEntity)

        }
    }



    private fun addSampleProducts() {
        Log.d("FirestoreDebug", "Η addSampleProducts() κλήθηκε!")
        var firestore = FirebaseFirestore.getInstance()

        // Map of category names to Firestore document IDs
        val categoryIds = mutableMapOf<String, String>()

        // Fetch categories from Firestore
        firestore.collection("pda").document("cafe")
            .collection("categories")
            .get()
            .addOnSuccessListener { categoriesSnapshot ->
                // Populate the categoryIds map
                for (category in categoriesSnapshot) {
                    val categoryName = category.getString("name")
                    if (categoryName != null) {
                        categoryIds[categoryName] = category.id
                    }
                }

                // Ensure the required categories exist
                val requiredCategories = listOf("Καφέδες", ,"Delivery")
                for (category in requiredCategories) {
                    if (categoryIds[category] == null) {
                        Log.e("Firestore", "Category not found: $category")
                        return@addOnSuccessListener
                    }
                }

                // First, fetch the category IDs from Firestore
                /*  firestore.collection("categories")
            .get()
            .addOnSuccessListener { categoriesSnapshot ->
                for (category in categoriesSnapshot) {
                    categoryIds[category.getString("name")!!] = category.id
                }
*/
                // Now add the products
                val products = listOf(
                    ProductEntity(
                        name = "Espresso μονό",
                        price = 2.5,
                        categoryId = categoryIds["Καφέδες"]!!,
                    ),
                     ProductEntity(
                        name = "Delivery 5€",
                        price = 5.0,
                        categoryId = categoryIds["Delivery"]!!
                    )
                )
                for (product in products) {
                    firestore.collection("pda").document("cafe")
                        .collection("products")
                        .add(product) // Use add() to auto-generate document ID

            }
            }
        /*   .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to fetch categories: ${e.message}")
            }*/

    }

    private fun addSampleTables() {

        var firestore = FirebaseFirestore.getInstance()
        val tables = listOf(
            TableEntity(id = "1", totalPrice = 0.0),
            TableEntity(id = "30", totalPrice = 0.0),

        )

        for (table in tables) {
            firestore.collection("pda").document("cafe")
                .collection("tables").document(table.id)
                .set(table)
        }
    }


    private fun initializeGeneralTotalInFirestore() {
        val firestore = FirebaseFirestore.getInstance()
        val cafeRef = firestore.collection("pda").document("cafe")

        cafeRef.get().addOnSuccessListener { document ->
            if (!document.exists() || document.getDouble("generalTotal") == null) {
                // Initialize generalTotal to 0.0 if it doesn't exist
                cafeRef.set(mapOf("generalTotal" to 0.0), SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("FirestoreDebug", "Initialized generalTotal in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreDebug", "Failed to initialize generalTotal: ${e.message}")
                    }
            }
        }
    }

*/

            override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            val addedAmount = data.getDoubleExtra("addedAmount", 0.0)
            generalTotal += addedAmount
            saveGeneralTotal()
            hideGeneralTotal()
          //  Toast.makeText(this, "General Total Updated!", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupFirestoreListener() {
        // Listen for changes in the "tables" collection
        val firestore = FirebaseFirestore.getInstance()
        firestoreListener = firestore.collection("pda").document("cafe")
            .collection("tables")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to listen for updates: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                // Calculate the total sum of all table totals
                var totalSum = 0.0
                if (snapshot != null) {
                    for (table in snapshot) {
                        val tableTotal = table.getDouble("totalPrice") ?: 0.0
                        totalSum += tableTotal
                    }
                }

                // Update the TextView with the new total sum
                totalPriceTextView.text = "%.2f€".format(totalSum)
            }
    }


    private fun setupTableStatusListener() {
        val firestore = FirebaseFirestore.getInstance()
        val tableNumbers = (1..24).map { it.toString() }

        for (tableNum in tableNumbers) {
            if (tableOrderListeners.containsKey(tableNum)) continue

            val buttonId = resources.getIdentifier("btn_table_$tableNum", "id", packageName)
            val button = findViewById<Button>(buttonId)

            // Ignore the tables without button
            if (button == null) {
                Log.w("TableStatus", "⚠️ Button not found for table: $tableNum")
                continue
            }

            val listener = firestore.collection("pda").document("cafe")
                .collection("tables").document(tableNum)
                .collection("orders")
                .addSnapshotListener { ordersSnapshot, error ->
                    if (error != null) {
                        Log.e("OrdersListener", " Error for table $tableNum: ${error.message}")
                        return@addSnapshotListener
                    }

                    runOnUiThread {
                        val color = if (ordersSnapshot == null || ordersSnapshot.isEmpty)
                            Color.parseColor("#483C4F") // empty
                        else
                            Color.parseColor("#006b80") // occupied

                        button.backgroundTintList = ColorStateList.valueOf(color)
                    }
                }

            tableOrderListeners[tableNum] = listener
        }
    }



    private fun showGeneralTotal() {
        generalTotalEditText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        generalTotalEditText.setText("%.2f€".format(generalTotal))
    }

    private fun hideGeneralTotal() {
        generalTotalEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        generalTotalEditText.setText("*****")
    }


    private fun showTablesPopupMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.tables_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.table_30 -> openTable(30)
                R.id.btn_logout -> {
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    private fun openTable(tableNumber: Int): Boolean {
        val intent = Intent(this, SecondActivity::class.java).apply {
            putExtra("TABLE_NUMBER", tableNumber)
        }
        startActivity(intent)
        return true
    }


    private fun saveGeneralTotal() {
        val firestore = FirebaseFirestore.getInstance()

        // Update the generalTotal in Firestore
        firestore.collection("pda").document("cafe")
            .update("generalTotal", generalTotal)
            .addOnSuccessListener {
                // Save to SharedPreferences
                sharedPreferences.edit().putFloat("generalTotal", generalTotal.toFloat()).apply()

                // Fetch latest value from Firestore to sync devices
                fetchGeneralTotal()

                // Log the general total updated event
                val bundle = Bundle().apply {
                    putDouble("new_total", generalTotal)
                }
                firebaseAnalytics.logEvent("general_total_updated", bundle)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update General Total: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun saveGeneralTotalFromEditText() {
        try {
            // Read the value from the EditText
            val newTotal = generalTotalEditText.text.toString().toDouble()

            // Update the generalTotal variable
            generalTotal = newTotal

            // Save the updated generalTotal to Firestore and SharedPreferences
            saveGeneralTotal()

            // Log the general total update event
            val bundle = Bundle().apply {
                putDouble("new_total", newTotal)
            }
            firebaseAnalytics.logEvent("general_total_updated", bundle)

            // Hide the generalTotal (if needed)
            hideGeneralTotal()

            // Show a success message
          //  Toast.makeText(this, "General Total Updated!", Toast.LENGTH_SHORT).show()
        } catch (e: NumberFormatException) {
            // Handle invalid input
         //   Toast.makeText(this, "Invalid input! Please enter a valid number.", Toast.LENGTH_SHORT).show()
            hideGeneralTotal()
        }
    }

    // Function to fetch latest generalTotal from Firestore
    private fun fetchGeneralTotal() {
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("pda").document("cafe")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val latestTotal = document.getDouble("generalTotal") ?: 0.0
                    generalTotal = latestTotal

                    // Update UI with latest value
                    generalTotalEditText.setText("%.2f€".format(generalTotal))
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch General Total: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun confirmClearGeneralTotal() {
        AlertDialog.Builder(this)
            .setTitle("Καθαρισμός")
            .setMessage("Είσαι σίγουρος πως θες να καθαρίσεις το γενικό σύνολο;")
            .setPositiveButton("Ναι") { _, _ ->
                viewModel.clearTotal()
                // Log the general total cleared event
                val bundle = Bundle().apply {
                    putString("user", auth.currentUser?.uid ?: "unknown")
                    putString("device", Build.MODEL)
                    putString("time", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                        Date()
                    ))
                }
                firebaseAnalytics.logEvent("general_total_cleared", bundle)

                hideGeneralTotal()
                Toast.makeText(this, "Το Γενικό Σύνολο Καθαρίστηκε!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Όχι", null)
            .show()
    }

    private fun verifyPassword(onSuccess: () -> Unit) {
        if (isPasswordVerified) {
            onSuccess()
            return
        }

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER // Allow only numeric input
        }

        AlertDialog.Builder(this)
            .setTitle("Εισαγωγή κωδικού")
            .setMessage("Εισάγεται κωδικό για συνέχεια")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                if (input.text.toString() == "0000") {
                    isPasswordVerified = true // Set the flag to true
                    onSuccess() // Execute the success action
                } else {
                    Toast.makeText(this, "Incorrect password!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Ακύρωση", null)
            .setCancelable(false) // Prevent dismissing the dialog by tapping outside
            .show()
    }

    private fun requestPasswordIfNeeded(onSuccess: () -> Unit) {
        if (isPasswordVerified) {
            onSuccess() // If already verified, execute the action
        } else {
            verifyPassword {
                onSuccess() // If verified now, execute the action
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the Firestore listener when the activity is destroyed
        firestoreListener?.remove()
        resetPasswordVerification() // Reset the flag when the activity is destroyed
        for (listener in tableOrderListeners.values) {
            listener.remove()
        }
        tableOrderListeners.clear()
    }

    private fun resetPasswordVerification() {
        isPasswordVerified = false // Reset the flag
    }
}
