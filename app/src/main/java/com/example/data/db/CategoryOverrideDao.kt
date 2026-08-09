package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CategoryOverride
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryOverrideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setOverride(override: CategoryOverride)

    @Query("SELECT * FROM category_overrides")
    fun getAllOverridesFlow(): Flow<List<CategoryOverride>>

    @Query("SELECT * FROM category_overrides")
    suspend fun getAllOverrides(): List<CategoryOverride>

    @Query("DELETE FROM category_overrides WHERE packageName = :packageName")
    suspend fun removeOverride(packageName: String)
}
