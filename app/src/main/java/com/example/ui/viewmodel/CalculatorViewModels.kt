package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CalculationEntity
import com.example.data.local.CalculationRepository
import com.example.domain.CalculationType
import com.example.domain.CalculatorEngine
import com.example.domain.MoneyWeightResult
import com.example.domain.WeightMoneyResult
import com.example.domain.WeightUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

data class WeightToMoneyUiState(
    val itemName: String = "",
    val pricePerKgInput: String = "",
    val weightInput: String = "",
    val selectedUnit: WeightUnit = WeightUnit.GRAM,
    val liveResult: WeightMoneyResult? = null,
    val priceError: Boolean = false,
    val weightError: Boolean = false,
    val savedRecord: CalculationEntity? = null
)

class WeightToMoneyViewModel(
    private val repository: CalculationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeightToMoneyUiState())
    val uiState: StateFlow<WeightToMoneyUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var activeSessionHistoryId: Long? = null
    private var lastSavedSignature: String? = null

    fun updateItemName(value: String) {
        _uiState.update { it.copy(itemName = value) }
        scheduleAutoSave()
    }

    fun updatePrice(value: String) {
        val filtered = filterDecimalInput(value)
        _uiState.update { state ->
            val next = state.copy(pricePerKgInput = filtered, priceError = false)
            recalculate(next)
        }
        scheduleAutoSave()
    }

    fun updateWeight(value: String) {
        val filtered = filterDecimalInput(value)
        _uiState.update { state ->
            val next = state.copy(weightInput = filtered, weightError = false)
            recalculate(next)
        }
        scheduleAutoSave()
    }

    fun selectUnit(unit: WeightUnit) {
        _uiState.update { state ->
            val next = state.copy(selectedUnit = unit)
            recalculate(next)
        }
        scheduleAutoSave()
    }

    fun applyQuickWeight(weight: String, unit: WeightUnit) {
        _uiState.update { state ->
            val next = state.copy(weightInput = weight, selectedUnit = unit, weightError = false)
            recalculate(next)
        }
        scheduleAutoSave()
    }

    fun reset() {
        autoSaveJob?.cancel()
        activeSessionHistoryId = null
        lastSavedSignature = null
        _uiState.value = WeightToMoneyUiState()
    }

    /**
     * Clears weight input after adding an item to the combined bill while keeping pricePerKg ready
     * for the next weight batch (e.g., weighing Loha 1kg, then 2kg, then 500g at same rate).
     */
    fun prepareForNextBatch() {
        autoSaveJob?.cancel()
        activeSessionHistoryId = null
        lastSavedSignature = null
        _uiState.update { state ->
            state.copy(
                weightInput = "",
                liveResult = null,
                weightError = false,
                savedRecord = null
            )
        }
    }

    fun flushAutoSaveIfNeeded() {
        val state = _uiState.value
        if (state.liveResult != null && buildSignature(state) != lastSavedSignature) {
            autoSaveJob?.cancel()
            viewModelScope.launch {
                persistCurrentResult(state, state.liveResult)
            }
        }
    }

    fun calculateAndSave(onSaved: (CalculationEntity) -> Unit = {}) {
        val state = _uiState.value
        val price = CalculatorEngine.parsePositiveDecimal(state.pricePerKgInput)
        val weight = CalculatorEngine.parsePositiveDecimal(state.weightInput)

        if (price == null || weight == null) {
            _uiState.update {
                it.copy(
                    priceError = price == null,
                    weightError = weight == null
                )
            }
            return
        }

        autoSaveJob?.cancel()
        val result = CalculatorEngine.calculateWeightToMoney(price, weight, state.selectedUnit)
        viewModelScope.launch {
            val saved = persistCurrentResult(state, result)
            onSaved(saved)
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        val state = _uiState.value
        val result = state.liveResult ?: return
        val signature = buildSignature(state)
        if (signature == lastSavedSignature) return

        autoSaveJob = viewModelScope.launch {
            delay(650L)
            persistCurrentResult(_uiState.value, result)
        }
    }

    private suspend fun persistCurrentResult(
        state: WeightToMoneyUiState,
        result: WeightMoneyResult
    ): CalculationEntity {
        val signature = buildSignature(state)
        val existing = state.savedRecord
        if (existing != null && signature == lastSavedSignature) {
            return existing
        }

        val unitLabel = if (result.inputUnit == WeightUnit.KG) "KG" else "Gram"
        val gramsStr = CalculatorEngine.formatPlainNumber(result.weightInGrams, 2)
        val kgStr = CalculatorEngine.formatPlainNumber(result.weightInKg, 3)
        val rateStr = "${CalculatorEngine.formatAmount(result.pricePerKg)} / KG"
        val weightDisplay = if (result.inputUnit == WeightUnit.KG) {
            "${CalculatorEngine.formatPlainNumber(result.inputWeight, 3)} KG ($gramsStr g)"
        } else {
            "$gramsStr Gram ($kgStr KG)"
        }
        val finalAmountStr = CalculatorEngine.formatAmount(result.totalAmount)

        val entity = CalculationEntity(
            id = activeSessionHistoryId ?: 0L,
            timestamp = System.currentTimeMillis(),
            calculationType = CalculationType.WEIGHT_TO_MONEY.key,
            itemName = state.itemName.trim(),
            ratePerKg = rateStr,
            weightDisplay = weightDisplay,
            amountDisplay = finalAmountStr,
            finalResult = finalAmountStr,
            detailsSummary = "${CalculatorEngine.formatPlainNumber(result.inputWeight, 3)} $unitLabel × $rateStr = $finalAmountStr"
        )

        val insertedId = repository.insert(entity)
        activeSessionHistoryId = insertedId
        lastSavedSignature = signature
        val saved = entity.copy(id = insertedId)
        _uiState.update { it.copy(liveResult = result, savedRecord = saved) }
        return saved
    }

    private fun buildSignature(state: WeightToMoneyUiState): String =
        "${state.pricePerKgInput}|${state.weightInput}|${state.selectedUnit.name}|${state.itemName.trim()}"

    private fun recalculate(state: WeightToMoneyUiState): WeightToMoneyUiState {
        val price = CalculatorEngine.parsePositiveDecimal(state.pricePerKgInput)
        val weight = CalculatorEngine.parsePositiveDecimal(state.weightInput)
        val result = if (price != null && weight != null) {
            CalculatorEngine.calculateWeightToMoney(price, weight, state.selectedUnit)
        } else {
            null
        }
        return state.copy(
            liveResult = result,
            savedRecord = if (result == null) null else state.savedRecord
        )
    }
}

data class MoneyToWeightUiState(
    val itemName: String = "",
    val pricePerKgInput: String = "",
    val amountInput: String = "",
    val liveResult: MoneyWeightResult? = null,
    val priceError: Boolean = false,
    val amountError: Boolean = false,
    val savedRecord: CalculationEntity? = null
)

class MoneyToWeightViewModel(
    private val repository: CalculationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoneyToWeightUiState())
    val uiState: StateFlow<MoneyToWeightUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null
    private var activeSessionHistoryId: Long? = null
    private var lastSavedSignature: String? = null

    fun updateItemName(value: String) {
        _uiState.update { it.copy(itemName = value) }
        scheduleAutoSave()
    }

    fun updatePrice(value: String) {
        val filtered = filterDecimalInput(value)
        _uiState.update { state ->
            val next = state.copy(pricePerKgInput = filtered, priceError = false)
            recalculate(next)
        }
        scheduleAutoSave()
    }

    fun updateAmount(value: String) {
        val filtered = filterDecimalInput(value)
        _uiState.update { state ->
            val next = state.copy(amountInput = filtered, amountError = false)
            recalculate(next)
        }
        scheduleAutoSave()
    }

    fun applyQuickAmount(amount: String) {
        _uiState.update { state ->
            val next = state.copy(amountInput = amount, amountError = false)
            recalculate(next)
        }
        scheduleAutoSave()
    }

    fun reset() {
        autoSaveJob?.cancel()
        activeSessionHistoryId = null
        lastSavedSignature = null
        _uiState.value = MoneyToWeightUiState()
    }

    fun prepareForNextBatch() {
        autoSaveJob?.cancel()
        activeSessionHistoryId = null
        lastSavedSignature = null
        _uiState.update { state ->
            state.copy(
                amountInput = "",
                liveResult = null,
                amountError = false,
                savedRecord = null
            )
        }
    }

    fun flushAutoSaveIfNeeded() {
        val state = _uiState.value
        if (state.liveResult != null && buildSignature(state) != lastSavedSignature) {
            autoSaveJob?.cancel()
            viewModelScope.launch {
                persistCurrentResult(state, state.liveResult)
            }
        }
    }

    fun calculateAndSave(onSaved: (CalculationEntity) -> Unit = {}) {
        val state = _uiState.value
        val price = CalculatorEngine.parsePositiveDecimal(state.pricePerKgInput)
        val amount = CalculatorEngine.parsePositiveDecimal(state.amountInput)

        if (price == null || amount == null) {
            _uiState.update {
                it.copy(
                    priceError = price == null,
                    amountError = amount == null
                )
            }
            return
        }

        autoSaveJob?.cancel()
        val result = CalculatorEngine.calculateMoneyToWeight(price, amount)
        viewModelScope.launch {
            val saved = persistCurrentResult(state, result)
            onSaved(saved)
        }
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        val state = _uiState.value
        val result = state.liveResult ?: return
        val signature = buildSignature(state)
        if (signature == lastSavedSignature) return

        autoSaveJob = viewModelScope.launch {
            delay(650L)
            persistCurrentResult(_uiState.value, result)
        }
    }

    private suspend fun persistCurrentResult(
        state: MoneyToWeightUiState,
        result: MoneyWeightResult
    ): CalculationEntity {
        val signature = buildSignature(state)
        val existing = state.savedRecord
        if (existing != null && signature == lastSavedSignature) {
            return existing
        }

        val rateStr = "${CalculatorEngine.formatAmount(result.pricePerKg)} / KG"
        val amountStr = CalculatorEngine.formatAmount(result.customerAmount)
        val humanWeight = CalculatorEngine.formatWeightHuman(result, "KG", "Gram")
        val totalGramsStr = "${CalculatorEngine.formatPlainNumber(result.weightInGrams, 2)} g"

        val entity = CalculationEntity(
            id = activeSessionHistoryId ?: 0L,
            timestamp = System.currentTimeMillis(),
            calculationType = CalculationType.MONEY_TO_WEIGHT.key,
            itemName = state.itemName.trim(),
            ratePerKg = rateStr,
            weightDisplay = "$humanWeight ($totalGramsStr)",
            amountDisplay = amountStr,
            finalResult = humanWeight,
            detailsSummary = "$amountStr ÷ $rateStr = $humanWeight ($totalGramsStr)"
        )

        val insertedId = repository.insert(entity)
        activeSessionHistoryId = insertedId
        lastSavedSignature = signature
        val saved = entity.copy(id = insertedId)
        _uiState.update { it.copy(liveResult = result, savedRecord = saved) }
        return saved
    }

    private fun buildSignature(state: MoneyToWeightUiState): String =
        "${state.pricePerKgInput}|${state.amountInput}|${state.itemName.trim()}"

    private fun recalculate(state: MoneyToWeightUiState): MoneyToWeightUiState {
        val price = CalculatorEngine.parsePositiveDecimal(state.pricePerKgInput)
        val amount = CalculatorEngine.parsePositiveDecimal(state.amountInput)
        val result = if (price != null && amount != null) {
            CalculatorEngine.calculateMoneyToWeight(price, amount)
        } else {
            null
        }
        return state.copy(
            liveResult = result,
            savedRecord = if (result == null) null else state.savedRecord
        )
    }
}

data class NormalCalculatorUiState(
    val expressionDisplay: String = "",
    val currentInput: String = "0",
    val pendingOperator: String? = null,
    val accumulator: BigDecimal? = null,
    val isError: Boolean = false,
    val justEvaluated: Boolean = false,
    val lastSavedRecord: CalculationEntity? = null
)

class NormalCalculatorViewModel(
    private val repository: CalculationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NormalCalculatorUiState())
    val uiState: StateFlow<NormalCalculatorUiState> = _uiState.asStateFlow()

    fun onDigit(digit: String) {
        _uiState.update { state ->
            if (state.isError) {
                return@update NormalCalculatorUiState(currentInput = digit)
            }
            if (state.justEvaluated && state.pendingOperator == null) {
                return@update NormalCalculatorUiState(currentInput = digit)
            }
            val nextInput = when {
                state.justEvaluated -> digit
                state.currentInput == "0" -> digit
                state.currentInput.length >= 15 -> state.currentInput
                else -> state.currentInput + digit
            }
            state.copy(
                currentInput = nextInput,
                justEvaluated = false
            )
        }
    }

    fun onDecimal() {
        _uiState.update { state ->
            if (state.isError) {
                return@update NormalCalculatorUiState(currentInput = "0.")
            }
            if (state.justEvaluated && state.pendingOperator == null) {
                return@update NormalCalculatorUiState(currentInput = "0.")
            }
            if (state.justEvaluated) {
                return@update state.copy(currentInput = "0.", justEvaluated = false)
            }
            if (state.currentInput.contains(".")) return@update state
            state.copy(currentInput = state.currentInput + ".")
        }
    }

    fun onClearAll() {
        _uiState.value = NormalCalculatorUiState()
    }

    fun onBackspace() {
        _uiState.update { state ->
            if (state.isError || state.justEvaluated) {
                return@update NormalCalculatorUiState()
            }
            val next = if (state.currentInput.length <= 1 || state.currentInput == "-") {
                "0"
            } else {
                state.currentInput.dropLast(1)
            }
            state.copy(currentInput = next)
        }
    }

    fun onOperator(op: String) {
        val currentState = _uiState.value
        if (currentState.isError) return

        val currentBd = CalculatorEngine.parseDecimal(currentState.currentInput) ?: BigDecimal.ZERO

        if (currentState.accumulator != null && currentState.pendingOperator != null && !currentState.justEvaluated) {
            val expr = "${CalculatorEngine.formatPlainNumber(currentState.accumulator, 8)} ${currentState.pendingOperator} ${CalculatorEngine.formatPlainNumber(currentBd, 8)}"
            val res = CalculatorEngine.applyOperation(currentState.accumulator, currentBd, currentState.pendingOperator)
            if (res.isFailure) {
                _uiState.update {
                    it.copy(
                        isError = true,
                        currentInput = "Error",
                        expressionDisplay = expr
                    )
                }
                return
            }
            val newAcc = res.getOrThrow()
            val formattedAcc = CalculatorEngine.formatPlainNumber(newAcc, 8)
            _uiState.update {
                it.copy(
                    accumulator = newAcc,
                    pendingOperator = op,
                    currentInput = formattedAcc,
                    expressionDisplay = "$formattedAcc $op",
                    justEvaluated = true
                )
            }
            saveNormalHistory(expr, formattedAcc, null)
            return
        }

        val acc = if (currentState.justEvaluated && currentState.accumulator != null && currentState.pendingOperator != null) {
            currentState.accumulator
        } else {
            currentBd
        }
        val formattedAcc = CalculatorEngine.formatPlainNumber(acc, 8)
        _uiState.update {
            it.copy(
                accumulator = acc,
                pendingOperator = op,
                expressionDisplay = "$formattedAcc $op",
                justEvaluated = true
            )
        }
    }

    fun onPercent() {
        _uiState.update { state ->
            if (state.isError) return@update state
            val currentBd = CalculatorEngine.parseDecimal(state.currentInput) ?: return@update state
            val percentVal = CalculatorEngine.applyPercent(state.accumulator, currentBd, state.pendingOperator)
            state.copy(
                currentInput = CalculatorEngine.formatPlainNumber(percentVal, 8),
                justEvaluated = false
            )
        }
    }

    fun onEquals(onSaved: ((CalculationEntity) -> Unit)? = null) {
        val state = _uiState.value
        if (state.isError) return

        val right = CalculatorEngine.parseDecimal(state.currentInput) ?: BigDecimal.ZERO
        val left = state.accumulator
        val op = state.pendingOperator

        if (left == null || op == null) {
            val plain = CalculatorEngine.formatPlainNumber(right, 8)
            if (right.compareTo(BigDecimal.ZERO) != 0) {
                saveNormalHistory(plain, plain, onSaved)
            }
            return
        }

        val expr = "${CalculatorEngine.formatPlainNumber(left, 8)} $op ${CalculatorEngine.formatPlainNumber(right, 8)}"
        val result = CalculatorEngine.applyOperation(left, right, op)
        if (result.isFailure) {
            _uiState.update {
                it.copy(
                    isError = true,
                    currentInput = "Error",
                    expressionDisplay = expr,
                    accumulator = null,
                    pendingOperator = null,
                    justEvaluated = true
                )
            }
            return
        }

        val finalValue = result.getOrThrow()
        val finalStr = CalculatorEngine.formatPlainNumber(finalValue, 8)

        _uiState.update {
            it.copy(
                expressionDisplay = "$expr =",
                currentInput = finalStr,
                accumulator = null,
                pendingOperator = null,
                justEvaluated = true,
                isError = false
            )
        }

        saveNormalHistory(expr, finalStr, onSaved)
    }

    private fun saveNormalHistory(
        expression: String,
        resultStr: String,
        onSaved: ((CalculationEntity) -> Unit)?
    ) {
        val entity = CalculationEntity(
            timestamp = System.currentTimeMillis(),
            calculationType = CalculationType.NORMAL.key,
            finalResult = resultStr,
            detailsSummary = "$expression = $resultStr"
        )
        viewModelScope.launch {
            val id = repository.insert(entity)
            val saved = entity.copy(id = id)
            _uiState.update { it.copy(lastSavedRecord = saved) }
            onSaved?.invoke(saved)
        }
    }
}

private fun filterDecimalInput(raw: String): String {
    val sb = StringBuilder()
    var hasDot = false
    for (ch in raw) {
        if (ch.isDigit()) {
            sb.append(ch)
        } else if (ch == '.' && !hasDot) {
            if (sb.isEmpty()) sb.append('0')
            sb.append('.')
            hasDot = true
        }
    }
    return sb.toString()
}

class CalculatorViewModelFactory(
    private val repository: CalculationRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(WeightToMoneyViewModel::class.java) ->
                WeightToMoneyViewModel(repository) as T
            modelClass.isAssignableFrom(MoneyToWeightViewModel::class.java) ->
                MoneyToWeightViewModel(repository) as T
            modelClass.isAssignableFrom(NormalCalculatorViewModel::class.java) ->
                NormalCalculatorViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
