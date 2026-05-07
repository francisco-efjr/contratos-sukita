package com.sukita.contratos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sukita.contratos.model.Apartment
import kotlinx.coroutines.flow.Flow

@Dao
interface ApartmentDao {

    @Query("SELECT * FROM apartments ORDER BY name ASC")
    fun getAll(): Flow<List<Apartment>>

    @Query("SELECT * FROM apartments WHERE id = :id")
    suspend fun getById(id: Int): Apartment?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(apartments: List<Apartment>)

    @Query("SELECT COUNT(*) FROM apartments")
    suspend fun count(): Int
}
