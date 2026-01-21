package com.example.pda

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.graphics.Color;
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.coroutineContext

class StockAdapter(
    private val productStockList: List<ProductStock>
) : RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val productStock = productStockList[position]
        holder.bind(productStock)

        // Set click listener for the item
        holder.itemView.setOnClickListener {
            showEditStockDialog(holder.itemView.context, productStock)
        }
    }

    override fun getItemCount(): Int {
        return productStockList.size
    }

    class StockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productNameTextView: TextView = itemView.findViewById(android.R.id.text1)
        private val stockTextView: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(productStock: ProductStock) {
            productNameTextView.text = productStock.name
            stockTextView.text = "Stock: ${productStock.stock}"

            // Highlight low stock items
            if (productStock.stock <= 5) {
                stockTextView.setTextColor(Color.RED)
            } else {
                stockTextView.setTextColor(Color.BLACK)
            }
        }
    }

    private fun showEditStockDialog(context: android.content.Context, productStock: ProductStock) {
        val input = EditText(context).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(productStock.stock.toString())
        }

        AlertDialog.Builder(context)
            .setTitle("Επεξεργασία Stock ${productStock.name}")
            .setMessage("Εισαγωγή νέου stock:")
            .setView(input)
            .setPositiveButton("Αποθήκευση") { _, _ ->
                val newStock = input.text.toString().toIntOrNull()
                if (newStock != null && newStock >= 0) {
                    updateStockInFirestore(productStock.id, newStock)
                } else {
                    Toast.makeText(context, "Invalid stock value!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Ακύρωση", null)
            .show()
    }

    private fun updateStockInFirestore(productId: String, newStock: Int) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("pda").document("cafe")
            .collection("products").document(productId)
            .update("stock", newStock)
            .addOnSuccessListener {
                //Toast.makeText(context, "Stock updated successfully!", Toast.LENGTH_SHORT).show()
               // Log.e("StockActivity", "Stock updated successfully!")
            }
            .addOnFailureListener { e ->
               // Toast.makeText(context, "Failed to update stock: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}