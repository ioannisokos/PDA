package com.example.pda

import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class OrdersAdapter(
    private val ordersList: List<Order>,
    private val hiddenOrders: MutableList<Order>,
    private val onStatusChangeListener: (Order) -> Unit, // ✅ Listener for status change
    private val updateStatusCallback: (Order) -> Unit // ✅ Firestore update function
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    private var showAllOrders = true // Track filter state

    //filter state
    fun toggleFilter(showAll: Boolean) {
        showAllOrders = showAll
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        // Inflate the default layout for each order item
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val visibleOrders = if (showAllOrders) {
            ordersList
        } else {
            ordersList.filter { it.status != "done" }
        }

        //val visibleOrders = ordersList.filter { it !in hiddenOrders }
        val order = visibleOrders[position]

        // Replace "pending" and "done" with emojis
        val statusEmoji = when (order.status) {
            "pending" -> "🔴"
            "done" -> "🟢"
            else -> "🔴"
        }

        // Display order details
        val orderText = SpannableStringBuilder()

        // Bold Table number
        val tableText = "Τραπέζι ${order.tableNumber}: "
        val spannableTableText = SpannableString(tableText)
        spannableTableText.setSpan(
            android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
            0, // start index of tableNumber
            tableText.length, // end index
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        orderText.append(spannableTableText)

        // Add the rest normally
        orderText.append("${order.product.name} - €${"%.2f".format(order.product.price)}\n")
        orderText.append("📝 Σχόλιο: ${order.comment}\n")
        orderText.append("🕒 ${order.timestamp}\n")
        orderText.append("📌 Status: $statusEmoji")

        // Finally set it to the TextView
        holder.orderTextView.text = orderText


        // Handle long-press to toggle status and update Firestore
        holder.itemView.setOnLongClickListener {
            onStatusChangeListener(order) // Let activity handle it
            true
        }

    }

    override fun getItemCount(): Int {
        // Return the number of visible orders (excluding hidden ones)
        //return ordersList.filter { it !in hiddenOrders }.size
        return if (showAllOrders) {
            ordersList.size
        } else {
            ordersList.count { it.status != "done" }
        }
    }

    // ViewHolder for each order item
    class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderTextView: TextView = itemView.findViewById(android.R.id.text1)
    }
}