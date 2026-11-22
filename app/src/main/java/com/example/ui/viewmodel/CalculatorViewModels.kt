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

    fun updateItemName(value: String) {
        _uiState.update { it.copy(itemName = value) }
    }

    fun updatePrice(value: String) {
        val filtered = filterDecimalInput(value)
        _uiState.update { state ->
            val next = state.copy(pricePerKgInput = filtered, priceError = false)
            recalculate(next)
        }
    }

    fun updateWeight(value: String) {
        val filtered = filterDecimalInput(value)
        _uiState.update { state ->
            val next = state.copy(weightInput = filtered, weightError = false)
            recalculate(next)
        }
    }

    fun selectUnit(unit: WeightUnit) {
        _uiState.update { state ->
            val next = state.copy(selectedUnit = unit)
            recalculate(next)
        }
    }

    fun applyQuickWeight(weight: String, unit: WeightUnit) {
        _uiState.update { state ->
            val next = state.copy(weightInput = weight, selectedUnit = unit, weightError = false)
            recalculate(next)
        }
    }

    fun reset() {
        _uiState.value = WeightToMoneyUiState()
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

        val result = CalculatorEngine.calculateWeightToMoney(price, weight, state.selectedUnit)
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
            timestamp = System.currentTimeMillis(),
            calculationType = CalculationType.WEIGHT_TO_MONEY.key,
            itemName = state.itemName.trim(),
            ratePerKg = rateStr,
            weightDisplay = weightDisplay,
            amountDisplay = finalAmountStr,
            finalResult = finalAmountStr,
            detailsSummary = "${CalculatorEngine.formatPlainNumber(result.inputWeight, 3)} $unitLabel × $rateStr = $finalAmountStr"
        )

        viewModelScope.launch {
            val insertedId = repository.insert(entity)
            val saved = entity.copy(id = insertedId)
            _uiState.update { it.copy(liveResult = result, savedRecord = saved) }
            onSaved(saved)
        }
    }

    private fun recalculate(state: WeightToMoneyUiState): WeightToMoneyUiState {
        val price = CalculatorEngine.parsePositiveDecimal(state.pricePerKgInput)
        val weight = CalculatorEngine.parsePositiveDecimal(state.weightInput)
        val result = if (price != null && weight != null) {
            CalculatorEngine.calculateWeightToMoney(price, weight, state.selectedUnit)
        } else {
            null
        }
        return state.copy(liveResult = result, savedRecord = null)
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

    fun updateItemName(value: String) {
        _uiState.update { it.copy(itemName = value) }
    }

    fun updatePrice(value: String) {
        val filtered = filterDecimalInput(value)
        _uiState.update { state ->
            val next = state.copy(pricePerKgInput = filtered, priceError = false)
            recalculate(next)
        }
    }

    fun updateAmount(value: String) {
        val filtered = filterDecimalInput(value)
        _uiState.update { state ->
            val next = state.copy(amountInput = filtered, amountError = false)
            recalculate(next)
        }
    }

    fun applyQuickAmount(amount: String) {
        _uiState.update { state ->
            val next = state.copy(amountInput = amount, amountError = false)
            recalculate(next)
        }
    }

    fun reset() {
        _uiState.value = MoneyToWeightUiState()
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

        val result = CalculatorEngine.calculateMoneyToWeight(price, amount)
        val rateStr = "${CalculatorEngine.formatAmount(result.pricePerKg)} / KG"
        val amountStr = CalculatorEngine.formatAmount(result.customerAmount)
        val humanWeight = CalculatorEngine.formatWeightHuman(result, "KG", "Gram")
        val totalGramsStr = "${CalculatorEngine.formatPlainNumber(result.weightInGrams, 2)} g"

        val entity = CalculationEntity(
            timestamp = System.currentTimeMillis(),
            calculationType = CalculationType.MONEY_TO_WEIGHT.key,
            itemName = state.itemName.trim(),
            ratePerKg = rateStr,
            weightDisplay = "$humanWeight ($totalGramsStr)",
            amountDisplay = amountStr,
            finalResult = humanWeight,
            detailsSummary = "$amountStr ÷ $rateStr = $humanWeight ($totalGramsStr)"
        )

        viewModelScope.launch {
            val insertedId = repository.insert(entity)
            val saved = entity.copy(id = insertedId)
            _uiState.update { it.copy(liveResult = result, savedRecord = saved) }
            onSaved(saved)
        }
    }

    private fun recalculate(state: MoneyToWeightUiState): MoneyToWeightUiState {
        val price = CalculatorEngine.parsePositiveDecimal(state.pricePerKgInput)
        val amount = CalculatorEngine.parsePositiveDecimal(state.amountInput)
        val result = if (price != null && amount != null) {
            CalculatorEngine.calculateMoneyToWeight(price, amount)
        } else {
            null
        }
        return state.copy(liveResult = result, savedRecord = null)
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
        _uiState.update { state ->
            if (state.isError) return@update state
            val currentBd = CalculatorEngine.parseDecimal(state.currentInput) ?: BigDecimal.ZERO

            if (state.accumulator != null && state.pendingOperator != null && !state.justEvaluated) {
                val res = CalculatorEngine.applyOperation(state.accumulator, currentBd, state.pendingOperator)
                if (res.isFailure) {
                    return@update state.copy(
                        isError = true,
                        currentInput = "Error",
                        expressionDisplay = "${CalculatorEngine.formatPlainNumber(state.accumulator, 8)} ${state.pendingOperator} ${state.currentInput}"
                    )
                }
                val newAcc = res.getOrThrow()
                val formattedAcc = CalculatorEngine.formatPlainNumber(newAcc, 8)
                return@update state.copy(
                    accumulator = newAcc,
                    pendingOperator = op,
                    currentInput = formattedAcc,
                    expressionDisplay = "$formattedAcc $op",
                    justEvaluated = true
                )
            }

            val acc = if (state.justEvaluated && state.accumulator != null && state.pendingOperator != null) {
                state.accumulator
            } else {
                currentBd
            }
            val formattedAcc = CalculatorEngine.formatPlainNumber(acc, 8)
            state.copy(
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
            // Save single evaluated value if non-zero
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
