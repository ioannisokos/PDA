package com.example.pda.data

data class Order(
    val id: String = "",
    val tableId: String = "",
    val items: List<ProductItem> = listOf(),
    val status: String = "pending"
)