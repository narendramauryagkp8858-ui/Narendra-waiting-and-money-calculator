package com.example.data.local

import kotlinx.coroutines.flow.Flow

class CalculationRepository(private val dao: CalculationDao) {
    val allCalculations: Flow<List<CalculationEntity>> = dao.getAllCalculations()
    val latestCalculation: Flow<CalculationEntity?> = dao.getLatestCalculation()

    suspend fun insert(calculation: CalculationEntity): Long = dao.insert(calculation)

    suspend fun getById(id: Long): CalculationEntity? = dao.getById(id)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()
}
