package com.sukita.contratos.data

import com.sukita.contratos.model.Apartment
import kotlinx.coroutines.flow.Flow

class ApartmentRepository(private val dao: ApartmentDao) {

    fun getAllApartments(): Flow<List<Apartment>> = dao.getAll()

    suspend fun getById(id: Int): Apartment? = dao.getById(id)
}
