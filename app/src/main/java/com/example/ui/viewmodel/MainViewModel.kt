package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AppLanguage
import com.example.data.local.CalculationEntity
import com.example.data.local.CalculationRepository
import com.example.data.local.ShopProfile
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferences
import com.example.data.local.UserPreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val calculationRepository = CalculationRepository(db.calculationDao())
    private val preferencesRepository = UserPreferencesRepository(application)

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences()
        )

    val calculationHistory: StateFlow<List<CalculationEntity>> = calculationRepository.allCalculations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val latestCalculation: StateFlow<CalculationEntity?> = calculationRepository.latestCalculation
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun saveShopProfile(profile: ShopProfile) {
        viewModelScope.launch {
            preferencesRepository.saveShopProfile(profile)
        }
    }

    fun updateShopLogo(uri: Uri) {
        viewModelScope.launch {
            preferencesRepository.copyLogoToInternalStorage(uri)
        }
    }

    fun removeShopLogo() {
        viewModelScope.launch {
            preferencesRepository.removeLogo()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.saveThemeMode(mode)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesRepository.saveLanguage(language)
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            calculationRepository.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            calculationRepository.clearAll()
        }
    }

    suspend fun getCalculationById(id: Long): CalculationEntity? {
        return calculationRepository.getById(id)
    }
}
