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
import com.example.domain.BillItemMode
import com.example.domain.BillLineItem
import com.example.domain.CalculationType
import com.example.domain.CalculatorEngine
import com.example.domain.WeightUnit
import com.example.localization.stringsFor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

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

    val historyCount: StateFlow<Int> = calculationRepository.calculationCount
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val latestCalculation: StateFlow<CalculationEntity?> = calculationRepository.latestCalculation
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _billItems = MutableStateFlow<List<BillLineItem>>(emptyList())
    val billItems: StateFlow<List<BillLineItem>> = _billItems.asStateFlow()
    private var nextBillItemId = 1L
    private var activeBillHistoryId: Long? = null

    fun addWeightItemToBill(
        itemName: String,
        pricePerKg: BigDecimal,
        weight: BigDecimal,
        unit: WeightUnit
    ) {
        val calc = CalculatorEngine.calculateWeightToMoney(pricePerKg, weight, unit)
        val newItem = BillLineItem(
            id = nextBillItemId++,
            mode = BillItemMode.WEIGHT,
            itemName = itemName.trim(),
            pricePerKg = calc.pricePerKg,
            weightInGrams = calc.weightInGrams,
            amount = calc.totalAmount
        )
        _billItems.update { it + newItem }
        autoSaveActiveBill()
    }

    fun addExactItemToBill(
        itemName: String,
        pricePerKg: BigDecimal,
        weightInGrams: BigDecimal,
        amount: BigDecimal
    ) {
        val newItem = BillLineItem(
            id = nextBillItemId++,
            mode = BillItemMode.WEIGHT,
            itemName = itemName.trim(),
            pricePerKg = pricePerKg,
            weightInGrams = weightInGrams,
            amount = amount
        )
        _billItems.update { it + newItem }
        autoSaveActiveBill()
    }

    fun addPieceItemToBill(
        itemName: String,
        pricePerPiece: BigDecimal,
        quantity: BigDecimal
    ) {
        val calc = CalculatorEngine.calculatePieceToMoney(pricePerPiece, quantity)
        val newItem = BillLineItem(
            id = nextBillItemId++,
            mode = BillItemMode.PIECE,
            itemName = itemName.trim(),
            pricePerKg = calc.pricePerPiece,
            pieceQuantity = calc.quantity,
            amount = calc.totalAmount
        )
        _billItems.update { it + newItem }
        autoSaveActiveBill()
    }

    fun removeBillItem(id: Long) {
        _billItems.update { list -> list.filterNot { it.id == id } }
        if (_billItems.value.isNotEmpty()) {
            autoSaveActiveBill()
        } else {
            activeBillHistoryId = null
        }
    }

    fun clearBillItems() {
        _billItems.value = emptyList()
        activeBillHistoryId = null
    }

    private fun autoSaveActiveBill() {
        val strings = stringsFor(userPreferences.value.appLanguage)
        val entity = buildCombinedBillEntity(
            items = _billItems.value,
            kgLabel = strings.kgUnit,
            gramLabel = strings.gramUnit,
            pieceLabel = strings.pieceUnit
        ) ?: return

        viewModelScope.launch {
            val id = calculationRepository.insert(entity)
            activeBillHistoryId = id
        }
    }

    private fun buildCombinedBillEntity(
        items: List<BillLineItem>,
        kgLabel: String,
        gramLabel: String,
        pieceLabel: String
    ): CalculationEntity? {
        if (items.isEmpty()) return null

        val totals = CalculatorEngine.calculateCombinedBillTotals(items)
        val totalWeightHuman = CalculatorEngine.formatGramsHuman(totals.totalWeightInGrams, kgLabel, gramLabel)
        val totalWeightKgStr = "${CalculatorEngine.formatPlainNumber(totals.totalWeightInKg, 3)} KG"
        val totalPiecesStr = CalculatorEngine.formatPlainNumber(totals.totalPieces, 3)
        val totalAmountStr = CalculatorEngine.formatAmount(totals.totalAmount)

        val weightOrQtyDisplay = buildList {
            if (totals.weightItemCount > 0) add("$totalWeightHuman ($totalWeightKgStr)")
            if (totals.pieceItemCount > 0) add("$totalPiecesStr $pieceLabel")
        }.joinToString(" • ")

        val distinctRatesAndModes = items.map { it.mode to CalculatorEngine.formatAmount(it.pricePerKg) }.distinct()
        val rateDisplay = if (distinctRatesAndModes.size == 1) {
            val (mode, rate) = distinctRatesAndModes.first()
            if (mode == BillItemMode.PIECE) "$rate / $pieceLabel" else "$rate / KG"
        } else {
            "Multiple"
        }

        val lines = items.mapIndexed { idx, item ->
            val amtStr = CalculatorEngine.formatAmount(item.amount)
            val labelPrefix = if (item.itemName.isNotBlank()) "${item.itemName}: " else ""
            when (item.mode) {
                BillItemMode.WEIGHT -> {
                    val wtStr = CalculatorEngine.formatGramsHuman(item.weightInGrams, kgLabel, gramLabel)
                    val rateStr = "${CalculatorEngine.formatAmount(item.pricePerKg)}/KG"
                    "${idx + 1}. $labelPrefix$wtStr × $rateStr = $amtStr"
                }
                BillItemMode.PIECE -> {
                    val qtyStr = CalculatorEngine.formatPlainNumber(item.pieceQuantity, 3)
                    val rateStr = "${CalculatorEngine.formatAmount(item.pricePerKg)}/$pieceLabel"
                    "${idx + 1}. $labelPrefix$qtyStr $pieceLabel × $rateStr = $amtStr"
                }
            }
        }.joinToString("\n")

        val itemNames = items.mapNotNull { it.itemName.takeIf { name -> name.isNotBlank() } }.distinct()
        val titleSummary = if (itemNames.isNotEmpty()) {
            "${items.size} Items (${itemNames.joinToString(", ")})"
        } else {
            "${items.size} Items"
        }

        return CalculationEntity(
            id = activeBillHistoryId ?: 0L,
            timestamp = System.currentTimeMillis(),
            calculationType = CalculationType.COMBINED_BILL.key,
            itemName = titleSummary,
            ratePerKg = rateDisplay,
            weightDisplay = weightOrQtyDisplay,
            amountDisplay = totalAmountStr,
            finalResult = totalAmountStr,
            detailsSummary = lines
        )
    }

    fun saveCombinedBillToHistory(
        kgLabel: String = "KG",
        gramLabel: String = "Gram",
        pieceLabel: String = "Piece",
        onSaved: (CalculationEntity) -> Unit
    ) {
        val entity = buildCombinedBillEntity(
            items = _billItems.value,
            kgLabel = kgLabel,
            gramLabel = gramLabel,
            pieceLabel = pieceLabel
        ) ?: return

        viewModelScope.launch {
            val id = calculationRepository.insert(entity)
            activeBillHistoryId = id
            onSaved(entity.copy(id = id))
        }
    }

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

    fun restoreHistoryItem(entity: CalculationEntity) {
        viewModelScope.launch {
            calculationRepository.insert(entity)
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
