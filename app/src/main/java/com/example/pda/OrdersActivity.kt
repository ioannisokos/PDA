package com.example.pda

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.privacysandbox.tools.core.model.Type
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pda.data.ProductEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentChange
import java.util.Date
import java.util.Stack

class OrdersActivity : ComponentActivity() {
    private lateinit var firestore: FirebaseFirestore
    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var ordersAdapter: OrdersAdapter
    private val ordersList = mutableListOf<Order>()
    private val hiddenOrders = mutableListOf<Order>()
    private var firestoreListener: ListenerRegistration? = null
    private val orderListeners = mutableMapOf<String, ListenerRegistration>()
    private var showAllOrders = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        firestore = FirebaseFirestore.getInstance()

        // Set up RecyclerView
        ordersRecyclerView = findViewById(R.id.orders_recycler_view)
        ordersAdapter = OrdersAdapter(ordersList, hiddenOrders,
            { order -> toggleOrderStatus(order) }, // Existing status change handler
            { order -> updateOrderStatusInFirestore(order) }, // ✅ Firestore update function
        )

        ordersRecyclerView.layoutManager = LinearLayoutManager(this)
        ordersRecyclerView.adapter = ordersAdapter

        // Setup button click listener
        findViewById<Button>(R.id.btn_show_hidden).setOnClickListener {
            showAllOrders = !showAllOrders
            updateButtonText()
            ordersAdapter.toggleFilter(showAllOrders)
        }

        // Add these lines to reduce blinking
        ordersRecyclerView.itemAnimator = null
        ordersRecyclerView.setHasFixedSize(true)

        val dividerItemDecoration = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        ordersRecyclerView.addItemDecoration(dividerItemDecoration)
        val dividerDrawable = ContextCompat.getDrawable(this, R.drawable.custom_divider)
        dividerItemDecoration.setDrawable(dividerDrawable!!)
        ordersRecyclerView.addItemDecoration(dividerItemDecoration)

        // Load orders and set up Firestore listener
       // loadOrders() // Call this only once
        setupFirestoreListener() // Call this only once
    }

    private fun toggleOrderStatus(order: Order) {
        // Toggle locally
        order.status = if (order.status == "pending") "done" else "pending"

        // Notify adapter (this will reflect the emoji immediately)
        ordersAdapter.notifyDataSetChanged()

        // Update Firestore asynchronously
        updateOrderStatusInFirestore(order)
    }

    private fun updateButtonText() {
        findViewById<Button>(R.id.btn_show_hidden).text =
            if (showAllOrders) "Εμφάνιση εκκρεμών" else "Εμφάνιση όλων"
    }


    private fun updateOrderStatusInFirestore(order: Order) {
        if (order.id.isEmpty()) {
            Toast.makeText(this, "Invalid order ID!", Toast.LENGTH_SHORT).show()
            return
        }

        val orderRef = firestore.collection("pda").document("cafe")
            .collection("tables").document(order.tableNumber)
            .collection("orders").document(order.id)

        orderRef.update("status", order.status)
            .addOnSuccessListener {
                runOnUiThread {
                    ordersAdapter.notifyDataSetChanged() // ✅ Refresh UI
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun setupFirestoreListener() {
        // Clear existing listeners
       // firestoreListener?.remove()
       // orderListeners.values.forEach { it.remove() }
      //  orderListeners.clear()

        // Main tables listener
        firestoreListener = firestore.collection("pda/cafe/tables")
            .addSnapshotListener { tablesSnapshot, error ->
                if (error != null) {
                    Log.e("OrdersActivity", "Tables error", error)
                    return@addSnapshotListener
                }

                tablesSnapshot?.documents?.forEach { table ->
                    val tableNumber = table.id

                    // Only add new listener if one doesn't exist
                    if (!orderListeners.containsKey(tableNumber)) {
                        orderListeners[tableNumber] = table.reference.collection("orders")
                            .addSnapshotListener { ordersSnapshot, ordersError ->
                                if (ordersError != null) {
                                    Log.e("OrdersActivity", "Orders error", ordersError)
                                    return@addSnapshotListener
                                }

                                runOnUiThread {
                                    // Get current visible orders not from this table
                                    val otherOrders =
                                        ordersList.filter { it.tableNumber != tableNumber }

                                    // Get new orders from this table
                                    val newTableOrders = ordersSnapshot?.map { doc ->
                                        Order(
                                            product = doc.toObject(ProductEntity::class.java)
                                                .apply {
                                                    id = doc.id
                                                },
                                            timestamp = doc.getTimestamp("timestamp")?.toDate()
                                                ?: Date(),
                                            tableNumber = tableNumber,
                                            comment = doc.getString("comment") ?: "",
                                            status = doc.getString("status") ?: "pending",
                                            id = doc.id
                                        )
                                    } ?: emptyList()

                                    // Combine and sort
                                    ordersList.clear()
                                    ordersList.addAll(otherOrders + newTableOrders)
                                    ordersList.sortBy { it.timestamp }

                                    ordersAdapter.notifyDataSetChanged()
                                }
                            }
                    }
                }
            }
    }



    // ✅ This function completely replaces the list
    private fun updateOrdersList(newOrders: List<Order>) {
        runOnUiThread {
            // Simple comparison to prevent unnecessary updates
            if (ordersList != newOrders) {
                ordersList.clear()
                ordersList.addAll(newOrders.sortedBy { it.timestamp })
                ordersAdapter.notifyDataSetChanged()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        firestoreListener?.remove()
        orderListeners.values.forEach { it.remove() }
        orderListeners.clear()
    }

}

data class Order(
    val product: ProductEntity,
    val timestamp: Date,
    val tableNumber: String,
    val comment: String = "",
    var status: String = "pending", // Add status field with a default value of "pending"
    var id: String = "" // Add ID field

)