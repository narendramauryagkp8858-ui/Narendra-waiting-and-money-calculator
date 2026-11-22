package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history")
data class CalculationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val calculationType: String,
    val itemName: String = "",
    val ratePerKg: String = "",
    val weightDisplay: String = "",
    val amountDisplay: String = "",
    val finalResult: String,
    val detailsSummary: String = ""
)
