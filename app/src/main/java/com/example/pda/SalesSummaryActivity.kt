package com.example.pda

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate


class SalesSummaryActivity : ComponentActivity() {

    private lateinit var pieChart: PieChart
    private lateinit var firestore: FirebaseFirestore
    private val productTotals = mutableMapOf<String, Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sales_summary)

        pieChart = findViewById(R.id.pieChart)
        firestore = FirebaseFirestore.getInstance()

        pieChart.setUsePercentValues(true)

        loadSalesData()
    }

    private fun loadSalesData() {
        firestore.collectionGroup("orders")
            .whereEqualTo("status", "done")
            .get()
            .addOnSuccessListener { snapshot ->
                productTotals.clear()

                for (doc in snapshot) {
                    val name = doc.getString("name") ?: continue
                    val price = doc.getDouble("price") ?: 0.0

                    productTotals[name] = productTotals.getOrDefault(name, 0.0) + price
                }

                showPieChart()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Αποτυχία φόρτωσης πωλήσεων", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showPieChart() {
        val entries = productTotals.entries.sortedByDescending { it.value }.map { (name, total) ->
            PieEntry(total.toFloat(), name)
        }

        val dataSet = PieDataSet(entries, "Πωλήσεις ανά προϊόν").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 14f
        }

        val data = PieData(dataSet)

        pieChart.data = data
        pieChart.setUsePercentValues(true)
        pieChart.description.isEnabled = false
        pieChart.centerText = "Πωλήσεις Προϊόντων"
        pieChart.animateY(1000)
        pieChart.invalidate()
    }
}
