package com.example.inventoryroom.data
import androidx.room.PrimaryKey
@Entity(tableName = "items")
data class Item(
    @PrimaryKey
    val id: Int,
    ...
)