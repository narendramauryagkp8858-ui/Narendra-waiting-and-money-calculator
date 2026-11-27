package com.example.data.local

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CalculationRepository(private val dao: CalculationDao) {
    private val persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val allCalculations: Flow<List<CalculationEntity>> = dao.getAllCalculations()
    val latestCalculation: Flow<CalculationEntity?> = dao.getLatestCalculation()

    suspend fun insert(calculation: CalculationEntity): Long = dao.insert(calculation)

    fun insertNonCancellable(calculation: CalculationEntity) {
        persistenceScope.launch {
            dao.insert(calculation)
        }
    }

    suspend fun getById(id: Long): CalculationEntity? = dao.getById(id)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clearAll()
}
