package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_overrides")
data class CategoryOverride(
    @PrimaryKey val packageName: String,
    val category: String,
    val updatedAt: Long = System.currentTimeMillis()
)
