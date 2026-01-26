package com.example.pda

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModelProvider
import com.example.pda.data.ProductEntity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.analytics
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import android.text.InputType
import android.view.WindowManager
import androidx.core.content.ContextCompat
import com.google.firebase.installations.FirebaseInstallations

class SecondActivity : ComponentActivity() {

    private lateinit var expandableListView: ExpandableListView
    private lateinit var selectedItemsList: ListView
    private lateinit var totalPriceTextView: TextView
    private lateinit var clearTableButton: Button
    private lateinit var transferOrderButton: Button
    private lateinit var stickyHeader: TextView
    private lateinit var viewModel: GeneralTotalViewModel


    private lateinit var firestore: FirebaseFirestore
    private val allowedDeviceId = "****************"
    private val allowedDeviceId2 = "***************"
    private val allowedDeviceId3 = "***************"

    

    private val categoryIds = mutableMapOf<String, String>() // Map of category names to IDs
    private val commentsMap = mutableMapOf<String, String>() // String as key Product IDs

    private var tableNumber: String = ""
    private val selectedProducts = mutableListOf<ProductEntity>()
    private lateinit var selectedProductsAdapter: ArrayAdapter<String>

    private val categoryList = listOf("Καφέδες", "Σοκολάτες", "Κρύα τσαι","Ζεστό τσαι", "Αναψυκτικά", "Χυμοί","Σνακ", "Μπύρες", "Κρασιά", "Ποτά","Delivery")
    private val productMap = mapOf(
        "Καφέδες" to listOf("Espresso μονό", "Espresso διπλό", "Freddo espresso", "Freddo cappuccino", "Cappuccino μονό", "Cappuccino διπλό", "Latte ζεστό" , "Latte κρύο" , "Macchiato μονό", "Macchiato διπλό", "Americano", "Americano διπλός" , "Ελληνικός μονός", "Ελληνικός διπλός", "Φραπέ", "Νες" , "Γαλλικός","Mocaccino"),
        "Σοκολάτες" to listOf("Σοκολάτα", "Σοκολάτα με σαντιγί", "Σοκολάτα λευκή", "Σοκολάτα καραμέλα", "Σοκολάτα φουντούκι", "Σοκολάτα φράουλα", "Σοκολάτα λευκή μαστίχα", "Σοκολάτα cranberry", "Σοκολάτα καυτερή","Κακάο","Νερό"),
        "Κρύα τσαι" to listOf("Τσάι ροδάκινο", "Τσάι λεμόνι", "Τσάι χωρίς ζάχαρη"),
        "Ζεστό τσαι" to listOf("Τσάι του βουνού","Πράσινο Τσάι","Μαύρο τσάι","Χαμομήλι","Caramel toffee","Jardin blue","De chinois","Bali","4 φρούτα"),
        "Αναψυκτικά" to listOf("Πορτοκαλάδα με ανθρακικό", "Πορτοκαλάδα χωρίς ανθρακικό", "Λεμονάδα", "Ροζ λεμονάδα", "Βυσσινάδα", "Coca-cola", "Coca-cola zero", "Ξινό νερό Φλώρινας"),
        "Χυμοί" to listOf("Φυσικός χυμός πορτοκάλι", "Χυμός πορτοκάλι", "Χυμός ροδάκινο", "Χυμός μπανάνα", "Χυμός βύσσινο", "Χυμός λεμόνι", "Χυμός ανάμεικτος", "Giagia mas λεμονάδα", "Giagia mas λεμόνι φράουλα", "Giagia mas πράσινο μήλο-ρόδι", "Giagia mas ροδακινάδα", "Giagia mas πανδαισία"),
        "Σνακ" to listOf("Τοστ ζαμ/τυρί","Τοστ γαλ/τυρί","Τοστ τυρί"),
        "Μπύρες" to listOf("Εζα lager", "Εζα άνευ" , "Άλφα", "Κάιζερ", "Fisher", "Mythos ice", "Βαρέλι Άλφα"),
        "Κρασιά" to listOf("Λευκό ξηρό", "Λευκό ημίγλυκο", "Κόκκινο ξηρό", "Ροζέ ημίγλυκο", "Moschato dusty", "Sangria"),
        "Ποτά" to listOf("Τσίπουρο Τυρνάβου", "Τσίπουρο Ηδονικό", "Ούζο Τυρνάβου", "Ποτό απλό", "Ποτό special", "Ποτό premium", "Cocktail"),
        "Delivery" to listOf("Delivery 0.5€","Delivery 2€","Delivery 2.5€","Delivery 3€","Delivery 5€")
    )

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        firestore = FirebaseFirestore.getInstance()


        // Fetch category IDs
        fetchCategoryIds()

        //transfer
        transferOrderButton = findViewById(R.id.transfer_order_button)
        transferOrderButton.setOnClickListener { showTransferDialog() }

        // Initialize views
        expandableListView = findViewById(R.id.expandable_category_list)
        selectedItemsList = findViewById(R.id.selected_items_list)
        totalPriceTextView = findViewById(R.id.total_price)
        clearTableButton = findViewById(R.id.clear_table_button)
        stickyHeader = findViewById(R.id.sticky_header)

        // Get table number from intent
        // Log the Intent and its extras
       // Log.d("SecondActivityDebug", "Intent: $intent")
       // Log.d("SecondActivityDebug", "Intent extras: ${intent.extras}")

        //keyboard views
        // This prevents the keyboard from pushing any views up
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        // Get table number from intent
        val tableNumberInt = intent.getIntExtra("TABLE_NUMBER", -1) // Retrieve as Int
        if (tableNumberInt == -1) {
            Toast.makeText(this, "Invalid table number!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Convert tableNumber to String and prepend "table_"
        tableNumber = "$tableNumberInt" // Now tableNumber is "table_1", "table_2", etc.
       // Log.d("SecondActivityDebug", "Received tableNumber: $tableNumber")


        findViewById<TextView>(R.id.table_number).text = "Τραπέζι $tableNumber"


        //Log.d("FirestoreDebug", "Λήφθηκε tableNumber: $tableNumber")

        // Set up adapters
        selectedProductsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        selectedItemsList.adapter = selectedProductsAdapter

        // Set up ExpandableListView
        setupExpandableListView()

        // Load table data
        loadProductsFromFirestore()
        loadSelectedProducts()


        // Get shared ViewModel instance
        viewModel = ViewModelProvider(this).get(GeneralTotalViewModel::class.java)

        // Set up button click listeners
        clearTableButton.setOnClickListener { clearTable() }

        // Set up item click listener for selected items list
        selectedItemsList.setOnItemClickListener { _, _, position, _ ->
            val group = groupedProductList[position]
            val selectedProduct = group.first() // ή διάλεξε ένα συγκεκριμένο από την ομάδα
            showRemoveOrAddDialog(selectedProduct)
        }



        // Set up scroll listener for sticky header
        expandableListView.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {}

            override fun onScroll(
                view: AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                updateStickyHeader(firstVisibleItem)
            }
        })


    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("TABLE_NUMBER", tableNumber)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        tableNumber = savedInstanceState.getString("TABLE_NUMBER", "")
        if (tableNumber.isEmpty()) {
            Toast.makeText(this, "Invalid table number!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showRemoveOrAddDialog(product: ProductEntity) {
        AlertDialog.Builder(this)
            .setTitle("Επιλογή")
            .setMessage("Τι θες να κανεις με ${product.name}?")
            .setPositiveButton("Πληρώθηκε") { _, _ ->
                addToGeneralTotal(product)
                removeProductFromTable(product)
                // Log the product paid event
                val bundle = Bundle().apply {
                    putString("product_name", product.name) // Name of the product
                    putDouble("product_price", product.price) // Price of the product
                    putString("table_number", tableNumber) // Table number where the product was paid
                }
                Firebase.analytics.logEvent("product_paid", bundle)

            }
            .setNegativeButton("Αφαίρεση") { _, _ ->
                removeProductFromTable(product)
            }
            .setNeutralButton("Ακύρωση", null)
            .show()
    }

    private fun addToGeneralTotal(product: ProductEntity) {
        viewModel.updateTotal(product.price) // Update generalTotal

        Toast.makeText(this, "Προστέθηκε ${product.price}€", Toast.LENGTH_SHORT).show()
    }

    // Extension to convert dp to px
    fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    private fun setupExpandableListView() {
        // Store original heights in pixels
        val originalExpHeight = 335.dpToPx(this)
        val originalListHeight = 176.dpToPx(this)
        val originalListMargin = 24.dpToPx(this)

        expandableListView.setOnGroupExpandListener { groupPosition ->
            // Calculate available space in the container
            val container = findViewById<LinearLayout>(R.id.lists_container)
            val availableHeight = container.height - originalListMargin

            // ExpandableListView takes 2/3 of space
            expandableListView.layoutParams.height = (availableHeight * 3 / 3).toInt()

            // ListView takes 1/3 of space
            selectedItemsList.layoutParams.height = (availableHeight * 0 / 3).toInt()
            (selectedItemsList.layoutParams as ViewGroup.MarginLayoutParams).topMargin = 8.dpToPx(this)

            // Request layout update
            expandableListView.requestLayout()
            selectedItemsList.requestLayout()
        }

        expandableListView.setOnGroupCollapseListener { groupPosition ->
            // Return to original sizes
            expandableListView.layoutParams.height = originalExpHeight
            selectedItemsList.layoutParams.height = originalListHeight
            (selectedItemsList.layoutParams as ViewGroup.MarginLayoutParams).topMargin = originalListMargin

            // Request layout update
            expandableListView.requestLayout()
            selectedItemsList.requestLayout()
        }


        val adapter = object : BaseExpandableListAdapter() {
            override fun getGroupCount() = categoryList.size
            override fun getChildrenCount(groupPosition: Int) = productMap[categoryList[groupPosition]]?.size ?: 0
            override fun getGroup(groupPosition: Int) = categoryList[groupPosition]
            override fun getChild(groupPosition: Int, childPosition: Int) = productMap[categoryList[groupPosition]]?.get(childPosition) ?: ""
            override fun getGroupId(groupPosition: Int) = groupPosition.toLong()
            override fun getChildId(groupPosition: Int, childPosition: Int) = childPosition.toLong()
            override fun hasStableIds() = false

            override fun getGroupView(
                groupPosition: Int,
                isExpanded: Boolean,
                convertView: View?,
                parent: ViewGroup?
            ): View {
                val groupName = getGroup(groupPosition) as String
                val textView = TextView(this@SecondActivity).apply {
                    text = groupName
                    textSize = 20f
                    setPadding(100, 30, 0, 30)
                }
                return textView
            }

            override fun getChildView(
                groupPosition: Int,
                childPosition: Int,
                isLastChild: Boolean,
                convertView: View?,
                parent: ViewGroup?
            ): View {
                val productName = getChild(groupPosition, childPosition)
                val category = getGroup(groupPosition) as String

                val layout = LinearLayout(this@SecondActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(50, 20, 20, 20)
                }

                var sugarOption: String? = null
                var temperatureOption: String? = null

                // Declare the commentInput before using it
                val commentInput = EditText(this@SecondActivity).apply {
                    hint = "Σχόλιο"
                }

                // Add the "Add Product" button first
                val addButton = Button(this@SecondActivity).apply {
                    text = "$productName"
                    setBackgroundColor(Color.parseColor("#008080")) // Set button color
                    setTextColor(Color.WHITE) // Set text color
                    val backgroundDrawable = ContextCompat.getDrawable(this@SecondActivity, R.drawable.button_background_selector)
                    background = backgroundDrawable
                    setOnClickListener {
                        val comment = commentInput.text.toString()
                        // Include sugarOption for "Καφέδες" and temperatureOption for "Σοκολάτες"
                        val finalProductName = when {
                            category == "Καφέδες" && sugarOption != null -> "$productName - $sugarOption"
                            category == "Σοκολάτες" && temperatureOption != null -> "$productName - $temperatureOption"
                            else -> productName
                        }
                        addProductToTable(finalProductName, sugarOption ?: "Μέτριος", comment)
                    }
                }
                layout.addView(addButton)

                // Declare CheckBox variables outside the listener
                lateinit var checkBoxSugar: CheckBox
                lateinit var checkBoxNoSugar: CheckBox
                lateinit var checkBoxSugarSugar: CheckBox
                lateinit var checkBoxHot: CheckBox
                lateinit var checkBoxCold: CheckBox

                // Add the CheckBoxes for the "Καφέδες" category
                if (category == "Καφέδες") {
                    checkBoxSugar = CheckBox(this@SecondActivity).apply {
                        text = "Σκέτος"
                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                sugarOption = "Σκέτος"
                                // Uncheck other sugar options
                                checkBoxNoSugar.isChecked = false
                                checkBoxSugarSugar.isChecked = false
                            }
                        }
                    }

                    checkBoxNoSugar = CheckBox(this@SecondActivity).apply {
                        text = "Μέτριος"
                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                sugarOption = "Μέτριος"
                                // Uncheck other sugar options
                                checkBoxSugar.isChecked = false
                                checkBoxSugarSugar.isChecked = false
                            }
                        }
                    }

                    checkBoxSugarSugar = CheckBox(this@SecondActivity).apply {
                        text = "Γλυκός"
                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                sugarOption = "Γλυκός"
                                // Uncheck other sugar options
                                checkBoxSugar.isChecked = false
                                checkBoxNoSugar.isChecked = false
                            }
                        }
                    }

                    layout.addView(checkBoxSugar)
                    layout.addView(checkBoxNoSugar)
                    layout.addView(checkBoxSugarSugar)
                }

                // Add the CheckBoxes for the "Σοκολάτες" category
                if (category == "Σοκολάτες") {
                    checkBoxHot = CheckBox(this@SecondActivity).apply {
                        text = "Ζεστή"
                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                temperatureOption = "Ζεστή"
                                // Uncheck the other temperature option
                                checkBoxCold.isChecked = false
                            }
                        }
                    }

                    checkBoxCold = CheckBox(this@SecondActivity).apply {
                        text = "Κρύα"
                        setOnCheckedChangeListener { _, isChecked ->
                            if (isChecked) {
                                temperatureOption = "Κρύα"
                                // Uncheck the other temperature option
                                checkBoxHot.isChecked = false
                            }
                        }
                    }

                    layout.addView(checkBoxHot)
                    layout.addView(checkBoxCold)
                }

                // Add the comment input last
                layout.addView(commentInput)

                return layout
            }

            override fun isChildSelectable(groupPosition: Int, childPosition: Int) = true
        }

        expandableListView.setAdapter(adapter)
    }

    private fun updateStickyHeader(firstVisibleItem: Int) {
        // Get the position of the first visible item
        val packedPosition = expandableListView.getExpandableListPosition(firstVisibleItem)
        val groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition)

        if (groupPosition >= 0) {
            // Get the category name for the current group
            val category = categoryList[groupPosition]
            stickyHeader.text = category
            stickyHeader.visibility = View.VISIBLE
        } else {
            // Hide the sticky header if no group is visible
            stickyHeader.visibility = View.GONE
        }
        stickyHeader.setOnClickListener {
            val packedPosition = expandableListView.getExpandableListPosition(firstVisibleItem)
            val groupPosition = ExpandableListView.getPackedPositionGroup(packedPosition)

            if (expandableListView.isGroupExpanded(groupPosition)) {
                expandableListView.collapseGroup(groupPosition)
            } else {
                expandableListView.expandGroup(groupPosition)
            }
        }
    }



    private fun addProductToTable(productName: String, sugarOption: String, comment: String) {
        val baseProductName = productName.split(" - ")[0]
        val price = getProductPrice(baseProductName)

        val product = ProductEntity(
            name = productName,
            price = price
        )

        val orderData = hashMapOf(
            "name" to product.name,
            "price" to product.price,
            "comment" to comment,
            "timestamp" to FieldValue.serverTimestamp(),
            "status" to "pending" // Add status field with default value "pending"
        )

        firestore.collection("pda").document("cafe")
            .collection("tables").document(tableNumber)
            .collection("orders").add(orderData)
            .addOnSuccessListener { documentReference ->
                product.id = documentReference.id
                product.comment = comment
                selectedProducts.add(product)

                // Log the product added event
                val bundle = Bundle().apply {
                    putString("product_name", product.name)
                    putDouble("product_price", product.price)
                    putString("table_number", tableNumber)
                }
                Firebase.analytics.logEvent("product_added", bundle)

                documentReference.update("productId", product.id)
                    .addOnSuccessListener {
                        // Log.d("FirestoreDebug", "Product ID saved: ${product.id}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FirestoreDebug", "Failed to save product ID: ${e.message}")
                    }

                commentsMap[product.id] = comment

                updateSelectedListView()
                updateTotalPrice()
                // Decrease stock for the product
                //decreaseStock(product.id) // Call this function
                decreaseStock(baseProductName)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to add product: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun loadSelectedProducts() {
        firestore.collection("pda").document("cafe")
            .collection("tables").document(tableNumber)
            .collection("orders").get()
            .addOnSuccessListener { snapshot ->
                selectedProducts.clear()

                for (doc in snapshot.documents) {
                    val product = doc.toObject(ProductEntity::class.java) ?: continue

                    // Restore product ID properly
                    val productId = doc.getString("productId") ?: doc.id
                    product.id = productId

                    // Restore comment and status
                    val comment = doc.getString("comment") ?: ""
                    val status = doc.getString("status") ?: "pending" // Default to "pending"
                    product.comment = comment
                    product.status = status // Add status to the product
                    commentsMap[product.id] = comment

                    selectedProducts.add(product)

                    // Log.d("FirestoreDebug", "Loaded product: ${product.name}, ID: ${product.id}, Comment: $comment, Status: $status")
                }

                updateSelectedListView()
                updateTotalPrice()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreDebug", "Failed to load products: ${e.message}")
            }
    }




    private val groupedProductList = mutableListOf<List<ProductEntity>>()

    private fun updateSelectedListView() {
        // Keep the order that where added
        val grouped = linkedMapOf<Triple<String, String, String>, MutableList<ProductEntity>>()

        for (product in selectedProducts) {
            val key = Triple(product.name, product.comment, product.status)
            if (!grouped.containsKey(key)) {
                grouped[key] = mutableListOf()
            }
            grouped[key]!!.add(product)
        }

        groupedProductList.clear() 
        val productDisplayList = grouped.map { (key, group) ->
            groupedProductList.add(group)  

            val quantity = group.size
            val product = group.first()
            val comment = key.second
            val quantityText = if (quantity > 1) "x$quantity " else ""
            val totalPrice = quantity * product.price
            val mainLine = "$quantityText${product.name} - €${"%.2f".format(totalPrice)}"

            if (comment.isBlank()) mainLine else "$mainLine\n📝 Σχόλιο: $comment"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, productDisplayList)
        selectedItemsList.adapter = adapter
    }





    private fun getProductPrice(productName: String): Double {
        return when (productName) {
            //"product name ->price 
            //example
            "Espresso" -> 2.5
            else -> 0.0
        }
    }


    private fun loadProductsFromFirestore() {
        firestore.collection("pda").document("cafe").collection("categories")
            .get()
            .addOnSuccessListener { categoriesSnapshot ->
                val categories = mutableListOf<String>()
                val categoryProducts = mutableMapOf<String, List<ProductEntity>>()

                for (categoryDoc in categoriesSnapshot) {
                    val categoryName = categoryDoc.id
                    firestore.collection("pda").document("cafe")
                        .collection("categories").document(categoryName)
                        .collection("products").get()
                        .addOnSuccessListener { productsSnapshot ->
                            val products = productsSnapshot.toObjects(ProductEntity::class.java)
                            categories.add(categoryName)
                            categoryProducts[categoryName] = products
                            setupExpandableListView()
                        }
                }
            }
    }
    private fun fetchCategoryIds() {
        firestore.collection("pda").document("cafe")
            .collection("categories")
            .get()
            .addOnSuccessListener { categoriesSnapshot ->
                for (category in categoriesSnapshot) {
                    val categoryName = category.getString("name")
                    if (categoryName != null) {
                        categoryIds[categoryName] = category.id
                    }
                }
              //  Log.d("Firestore", "Fetched categories: $categoryIds")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to fetch categories: ${e.message}")
            }
    }


    private fun updateTotalPrice() {
        val total = selectedProducts.sumOf { it.price }
        totalPriceTextView.text = "Σύνολο: $total €"

        firestore.collection("pda").document("cafe")
            .collection("tables").document(tableNumber)
            .update("totalPrice", total)
    }


    private fun removeProductFromTable(productToRemove: ProductEntity) {
        if (productToRemove.id.isEmpty()) {
            Toast.makeText(this, "Invalid product ID!", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("pda").document("cafe")
            .collection("tables").document(tableNumber)
            .collection("orders").document(productToRemove.id)
            .delete()
            .addOnSuccessListener {
                selectedProducts.remove(productToRemove)
                commentsMap.remove(productToRemove.id)

                // Analytics event
                val bundle = Bundle().apply {
                    putString("product_name", productToRemove.name)
                    putDouble("product_price", productToRemove.price)
                    putString("table_number", tableNumber)
                }
                Firebase.analytics.logEvent("product_removed", bundle)

                updateSelectedListView()
                updateTotalPrice()

                //  Stock update
                increaseStock(productToRemove.name)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to remove product: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



    private fun decreaseStock(productName: String) {
        val productsRef = firestore.collection("pda").document("cafe")
            .collection("products")

        // Find the product in the "products" collection
        productsRef.whereEqualTo("name", productName).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0] // Get the first matching product
                    val productRef = document.reference
                    val currentStock = document.getLong("stock") ?: 0

                    if (currentStock > 0) {
                        productRef.update("stock", currentStock - 1)
                            .addOnSuccessListener {
                              //  Log.d("StockUpdate", "Stock decreased for $productName: ${currentStock - 1}")
                            }
                            .addOnFailureListener { e ->
                               // Log.e("StockUpdate", "Failed to decrease stock: ${e.message}")
                            }
                    }
                } else {
                   // Log.e("StockUpdate", "Product not found: $productName")
                }
            }
            .addOnFailureListener { e ->
               // Log.e("StockUpdate", "Failed to fetch product: ${e.message}")
            }
    }



    private fun increaseStock(productName: String) {
        val productsRef = firestore.collection("pda").document("cafe")
            .collection("products")

        // Find the product in the "products" collection
        productsRef.whereEqualTo("name", productName).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0] // Get the first matching product
                    val productRef = document.reference
                    val currentStock = document.getLong("stock") ?: 0

                    productRef.update("stock", currentStock + 1)
                        .addOnSuccessListener {
                           // Log.d("StockUpdate", "Stock increased for $productName: ${currentStock + 1}")
                        }
                        .addOnFailureListener { e ->
                          //  Log.e("StockUpdate", "Failed to increase stock: ${e.message}")
                        }
                } else {
                  //  Log.e("StockUpdate", "Product not found: $productName")
                }
            }
            .addOnFailureListener { e ->
              //  Log.e("StockUpdate", "Failed to fetch product: ${e.message}")
            }
    }



    private fun clearTable() {
        // Everyone can clear the table with the actual total
        val tableTotal = selectedProducts.sumOf { it.price }

        // Check if this device is allowed to edit the total
        checkDeviceAuthorization { isAuthorized ->
            if (isAuthorized) {
                // Authorized device - show edit dialog
                showEditTotalDialog(tableTotal)
            } else {
                // Unauthorized device - clear with original total
                clearTableWithTotal(tableTotal)
            }
        }
    }

    private fun checkDeviceAuthorization(callback: (Boolean) -> Unit) {
        FirebaseInstallations.getInstance().id
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseDeviceId = task.result
                    Log.d("DeviceID", "Firebase Device ID: $firebaseDeviceId")
                    // Check against allowed device IDs(here are 3)
                    callback(firebaseDeviceId == allowedDeviceId || firebaseDeviceId == allowedDeviceId2 || firebaseDeviceId==allowedDeviceId3)
                } else {
                    Log.e("DeviceID", "Failed to get Firebase Device ID", task.exception)
                    callback(false)
                }
            }
    }

    private fun showEditTotalDialog(tableTotal: Double) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Επεξεργασία τραπεζιού")

        val input = EditText(this).apply {
            setText(tableTotal.toString())
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        builder.setView(input)

        builder.setPositiveButton("Επιβεβαίωση") { _, _ ->
            val newTotal = input.text.toString().toDoubleOrNull() ?: tableTotal
            clearTableWithTotal(newTotal)
        }

        builder.setNegativeButton("Ακύρωση") { dialog, _ ->
            dialog.dismiss()
            // If canceled, still allow clearing with original total
            clearTableWithTotal(tableTotal)
        }

        builder.show()
    }

    private fun clearTableWithTotal(finalTotal: Double) {
        val resultIntent = Intent().apply {
            putExtra("addedAmount", finalTotal)
        }
        setResult(RESULT_OK, resultIntent)

        firestore.collection("pda").document("cafe")
            .collection("tables").document(tableNumber)
            .collection("orders").get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                batch.commit()
                    .addOnSuccessListener {
                        selectedProducts.clear()
                        updateSelectedListView()
                        updateTotalPrice()

                        Firebase.analytics.logEvent("table_cleared", Bundle().apply {
                            putDouble("table_total", finalTotal)
                            putString("table_number", tableNumber)
                        })

                        finish()
                        Toast.makeText(this, "Το τραπέζι καθαρίστηκε", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Σφάλμα: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Σφάλμα: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Add these new functions to your class
    private fun showTransferDialog() {
        if (selectedProducts.isEmpty()) {
            Toast.makeText(this, "Δεν υπάρχουν παραγγελίες για μεταφορά", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_transfer_order, null)
        val tableSpinner = dialogView.findViewById<Spinner>(R.id.table_spinner)

        // Load available tables (1-24 excluding current table)
        val tables = (1..24).map { it.toString() }.filter { it != tableNumber }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tables)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tableSpinner.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Μεταφορά Παραγγελίας")
            .setView(dialogView)
            .setPositiveButton("Μεταφορά") { _, _ ->
                val selectedTable = tableSpinner.selectedItem.toString()
                if (selectedTable.isNotEmpty()) {
                    transferOrdersToTable(selectedTable)
                }
            }
            .setNegativeButton("Ακύρωση", null)
            .show()
    }

    private fun transferOrdersToTable(newTableNumber: String) {
        val progressDialog = AlertDialog.Builder(this).apply {
            setMessage("Μεταφορά παραγγελιών...")
            setView(ProgressBar(context).apply { isIndeterminate = true })
            setCancelable(false)
        }.create().apply { show() }

        firestore.collection("pda").document("cafe")
            .collection("tables").document(tableNumber)
            .collection("orders").get()
            .addOnSuccessListener { snapshot ->
                val batch = firestore.batch()

                // Transfer each order while maintaining the same document ID
                snapshot.documents.forEach { doc ->
                    val newOrderRef = firestore.collection("pda").document("cafe")
                        .collection("tables").document(newTableNumber)
                        .collection("orders").document(doc.id) // Keep same document ID

                    batch.set(newOrderRef, doc.data!!)
                    batch.delete(doc.reference)
                }

                // Update totals
                val transferredTotal = selectedProducts.sumOf { it.price }
                batch.update(
                    firestore.collection("pda").document("cafe")
                        .collection("tables").document(tableNumber),
                    "totalPrice", 0.0
                )
                batch.update(
                    firestore.collection("pda").document("cafe")
                        .collection("tables").document(newTableNumber),
                    "totalPrice", FieldValue.increment(transferredTotal)
                )

                batch.commit()
                    .addOnSuccessListener {
                        progressDialog.dismiss()
                        // Clear local data AFTER successful transfer
                        selectedProducts.clear()
                        commentsMap.clear()
                        updateSelectedListView()
                        updateTotalPrice()

                        // Refresh data to ensure UI matches Firestore
                        loadSelectedProducts()

                        Toast.makeText(this@SecondActivity,
                            "Μεταφέρθηκαν στο τραπέζι $newTableNumber",
                            Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        progressDialog.dismiss()
                        Toast.makeText(this@SecondActivity,
                            "Σφάλμα: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Toast.makeText(this, "Σφάλμα φόρτωσης: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



}


