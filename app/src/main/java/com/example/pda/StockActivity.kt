package com.example.pda

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class StockActivity : ComponentActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var stockRecyclerView: RecyclerView
    private lateinit var stockAdapter: StockAdapter
    private val productStockList = mutableListOf<ProductStock>()
    private var stockListener: ListenerRegistration? = null // 🔥 Store listener reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stock)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()

        // Initialize RecyclerView
        stockRecyclerView = findViewById(R.id.stock_recycler_view)
        stockRecyclerView.layoutManager = LinearLayoutManager(this)
        stockAdapter = StockAdapter(productStockList)
        stockRecyclerView.adapter = stockAdapter

        // Load stock data with real-time updates
        loadStockChanges()
    }

    private fun loadStockChanges() {
        val specificProducts = listOf(
            "Πορτοκαλάδα με ανθρακικό", "Πορτοκαλάδα χωρίς ανθρακικό", "Λεμονάδα", "Ροζ λεμονάδα", "Βυσσινάδα",
            "Coca-cola", "Coca-cola zero", "Ξινό νερό Φλώρινας", "Τσάι ροδάκινο", "Τσάι λεμόνι", "Τσάι χωρίς ζάχαρη",
            "Εζα lager", "Άλφα", "Κάιζερ", "Fisher", "Mythos ice", "Λευκό ξηρό", "Λευκό ημίγλυκο", "Κόκκινο ξηρό",
            "Ροζέ ημίγλυκο", "Τσίπουρο Τυρνάβου", "Τσίπουρο Ηδονικό", "Ούζο Τυρνάβου"
        )

        stockListener = firestore.collection("pda").document("cafe")
            .collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("StockActivity", "Firestore listen failed: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    productStockList.clear()

                    for (document in snapshot.documents) {
                        val productName = document.getString("name") ?: continue
                        val stock = document.getLong("stock")?.toInt() ?: 0

                        if (specificProducts.contains(productName)) {
                            val productStock = ProductStock(
                                id = document.id,
                                name = productName,
                                stock = stock
                            )
                            productStockList.add(productStock)
                        }
                    }

                    stockAdapter.notifyDataSetChanged() // Notify the adapter of real-time changes
                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        stockListener?.remove() // 🚀 Remove Firestore listener to prevent memory leaks
    }
}


data class ProductStock(
    val id: String, // Firestore document ID
    val name: String, // Product name
    var stock: Int=0 // Current stock count
)