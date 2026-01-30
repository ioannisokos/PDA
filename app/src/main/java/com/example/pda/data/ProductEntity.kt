package com.example.pda.data
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class ProductEntity(
    var id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val categoryId: String = "",
    var comment: String = "",
    var timestamp: Date? = null, 
    var status: String = "pending", // status field
    var quantity: Int = 1
)
