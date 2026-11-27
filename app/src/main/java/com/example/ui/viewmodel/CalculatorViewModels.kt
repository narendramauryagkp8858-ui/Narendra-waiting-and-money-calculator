package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.CalculationEntity
import com.example.data.local.CalculationRepository
import com.example.domain.CalculationType
import com.example.domain.CalculatorEngine
import com.example.domain.MoneyWeightResult
import com.example.domain.PieceMoneyResult
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
        val result = state.liveResult ?: return
        val signature = buildSignature(state)
        if (signature != lastSavedSignature) {
            autoSaveJob?.cancel()
            lastSavedSignature = signature
            repository.insertNonCancellable(buildEntity(state, result))
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

    private fun buildEntity(
        state: WeightToMoneyUiState,
        result: WeightMoneyResult
    ): CalculationEntity {
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

        return CalculationEntity(
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

        val entity = buildEntity(state, result)
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
        val result = state.liveResult ?: return
        val signature = buildSignature(state)
        if (signature != lastSavedSignature) {
            autoSaveJob?.cancel()
            lastSavedSignature = signature
            repository.insertNonCancellable(buildEntity(state, result))
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

    private fun buildEntity(
        state: MoneyToWeightUiState,
        result: MoneyWeightResult
    ): CalculationEntity {
        val rateStr = "${CalculatorEngine.formatAmount(result.pricePerKg)} / KG"
        val amountStr = CalculatorEngine.formatAmount(result.customerAmount)
        val humanWeight = CalculatorEngine.formatWeightHuman(result, "KG", "Gram")
        val totalGramsStr = "${CalculatorEngine.formatPlainNumber(result.weightInGrams, 2)} g"

        return CalculationEntity(
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

        val entity = buildEntity(state, result)
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

data class PieceToMoneyUiState(
    val itemName: String = "",
    val pricePerPieceInput: String = "",
    val quantityInput: String = "",
    val liveResult: PieceMoneyResult? = null,
    val priceError: Boolean = false,
    val quantityError: Boolean = false,
    val savedRecord: CalculationEntity? = null
)

class PieceToMoneyViewModel(
    private val repository: CalculationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PieceToMoneyUiState())
    val uiState: StateFlow<PieceToMoneyUiState> = _uiState.asStateFlow()

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
            val next = state.copy(pricePerPieceInput = filtered, priceError = false)
            recalculate(next)
        }
        scheduleAutoSave()
    }

    fun updateQuantity(value: String) {
        val filtered = filterDecimalInput(value)
        _uiState.update { state ->
            val next = state.copy(quantityInput = filtered, quantityError = false)
            recalculate(next)
        }
        scheduleAutoSave()
    }

    fun applyQuickQuantity(qty: String) {
        _uiState.update { state ->
            val next = state.copy(quantityInput = qty, quantityError = false)
            recalculate(next)
        }
        scheduleAutoSave()
    }

    fun reset() {
        autoSaveJob?.cancel()
        activeSessionHistoryId = null
        lastSavedSignature = null
        _uiState.value = PieceToMoneyUiState()
    }

    fun prepareForNextBatch() {
        autoSaveJob?.cancel()
        activeSessionHistoryId = null
        lastSavedSignature = null
        _uiState.update { state ->
            state.copy(
                itemName = "",
                pricePerPieceInput = "",
                quantityInput = "",
                liveResult = null,
                priceError = false,
                quantityError = false,
                savedRecord = null
            )
        }
    }

    fun flushAutoSaveIfNeeded() {
        val state = _uiState.value
        val result = state.liveResult ?: return
        val signature = buildSignature(state)
        if (signature != lastSavedSignature) {
            autoSaveJob?.cancel()
            lastSavedSignature = signature
            repository.insertNonCancellable(buildEntity(state, result))
        }
    }

    fun calculateAndSave(onSaved: (CalculationEntity) -> Unit = {}) {
        val state = _uiState.value
        val price = CalculatorEngine.parsePositiveDecimal(state.pricePerPieceInput)
        val qty = CalculatorEngine.parsePositiveDecimal(state.quantityInput)

        if (price == null || qty == null) {
            _uiState.update {
                it.copy(
                    priceError = price == null,
                    quantityError = qty == null
                )
            }
            return
        }

        autoSaveJob?.cancel()
        val result = CalculatorEngine.calculatePieceToMoney(price, qty)
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

    private fun buildEntity(
        state: PieceToMoneyUiState,
        result: PieceMoneyResult
    ): CalculationEntity {
        val qtyStr = CalculatorEngine.formatPlainNumber(result.quantity, 3)
        val rateStr = "${CalculatorEngine.formatAmount(result.pricePerPiece)} / Piece"
        val finalAmountStr = CalculatorEngine.formatAmount(result.totalAmount)

        return CalculationEntity(
            id = activeSessionHistoryId ?: 0L,
            timestamp = System.currentTimeMillis(),
            calculationType = CalculationType.PIECE_TO_MONEY.key,
            itemName = state.itemName.trim(),
            ratePerKg = rateStr,
            weightDisplay = "$qtyStr Piece",
            amountDisplay = finalAmountStr,
            finalResult = finalAmountStr,
            detailsSummary = "$qtyStr Piece × $rateStr = $finalAmountStr"
        )
    }

    private suspend fun persistCurrentResult(
        state: PieceToMoneyUiState,
        result: PieceMoneyResult
    ): CalculationEntity {
        val signature = buildSignature(state)
        val existing = state.savedRecord
        if (existing != null && signature == lastSavedSignature) {
            return existing
        }

        val entity = buildEntity(state, result)
        val insertedId = repository.insert(entity)
        activeSessionHistoryId = insertedId
        lastSavedSignature = signature
        val saved = entity.copy(id = insertedId)
        _uiState.update { it.copy(liveResult = result, savedRecord = saved) }
        return saved
    }

    private fun buildSignature(state: PieceToMoneyUiState): String =
        "${state.pricePerPieceInput}|${state.quantityInput}|${state.itemName.trim()}"

    private fun recalculate(state: PieceToMoneyUiState): PieceToMoneyUiState {
        val price = CalculatorEngine.parsePositiveDecimal(state.pricePerPieceInput)
        val qty = CalculatorEngine.parsePositiveDecimal(state.quantityInput)
        val result = if (price != null && qty != null) {
            CalculatorEngine.calculatePieceToMoney(price, qty)
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
    val expression: String = "",
    val evaluatedHeader: String = "",
    val finalResultDisplay: String = "",
    val livePreview: String = "",
    val isError: Boolean = false,
    val justEvaluated: Boolean = false,
    val lastSavedRecord: CalculationEntity? = null
) {
    val primaryDisplay: String
        get() = when {
            isError -> finalResultDisplay
            justEvaluated && finalResultDisplay.isNotEmpty() -> finalResultDisplay
            expression.isNotEmpty() -> expression
            else -> "0"
        }
}

class NormalCalculatorViewModel(
    private val repository: CalculationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NormalCalculatorUiState())
    val uiState: StateFlow<NormalCalculatorUiState> = _uiState.asStateFlow()

    private val operators = setOf('+', '-', '×', '÷')
    private var autoSaveJob: Job? = null
    private var activeSessionHistoryId: Long? = null
    private var lastSavedSignature: String? = null

    fun onDigit(digit: String) {
        _uiState.update { state ->
            val baseExpr = when {
                state.isError -> ""
                state.justEvaluated -> ""
                else -> state.expression
            }

            // If previous token ended with %, insert × before new digit
            val exprWithMult = if (baseExpr.endsWith("%")) "$baseExpr × " else baseExpr

            // Avoid multiple leading zeros in the active number token
            val activeToken = currentNumberToken(exprWithMult)
            val nextExpr = when {
                activeToken == "0" && digit == "0" -> exprWithMult
                activeToken == "0" && digit != "0" -> exprWithMult.dropLast(1) + digit
                else -> exprWithMult + digit
            }
            buildStateFromExpression(nextExpr, state.lastSavedRecord)
        }
        scheduleNormalAutoSave()
    }

    fun onDoubleZero() {
        _uiState.update { state ->
            val baseExpr = when {
                state.isError -> ""
                state.justEvaluated -> ""
                else -> state.expression
            }
            val exprWithMult = if (baseExpr.endsWith("%")) "$baseExpr × " else baseExpr
            val activeToken = currentNumberToken(exprWithMult)
            val nextExpr = when {
                activeToken.isEmpty() || activeToken == "0" -> {
                    if (activeToken == "0") exprWithMult else "${exprWithMult}0"
                }
                else -> "${exprWithMult}00"
            }
            buildStateFromExpression(nextExpr, state.lastSavedRecord)
        }
        scheduleNormalAutoSave()
    }

    fun onDecimal() {
        _uiState.update { state ->
            val baseExpr = when {
                state.isError -> ""
                state.justEvaluated -> ""
                else -> state.expression
            }
            val exprWithMult = if (baseExpr.endsWith("%")) "$baseExpr × " else baseExpr
            val activeToken = currentNumberToken(exprWithMult)
            if (activeToken.contains(".")) return@update state

            val nextExpr = if (activeToken.isEmpty() || activeToken == "-") {
                "${exprWithMult}0."
            } else {
                "$exprWithMult."
            }
            buildStateFromExpression(nextExpr, state.lastSavedRecord)
        }
        scheduleNormalAutoSave()
    }

    fun onClearAll() {
        flushAutoSaveIfNeeded()
        autoSaveJob?.cancel()
        activeSessionHistoryId = null
        lastSavedSignature = null
        _uiState.value = NormalCalculatorUiState()
    }

    fun onBackspace() {
        _uiState.update { state ->
            if (state.isError) {
                return@update NormalCalculatorUiState()
            }
            if (state.justEvaluated) {
                // Allow editing the evaluated result back rather than losing it
                val edited = state.finalResultDisplay.dropLast(1)
                return@update if (edited.isEmpty() || edited == "-") {
                    NormalCalculatorUiState(lastSavedRecord = state.lastSavedRecord)
                } else {
                    buildStateFromExpression(edited, state.lastSavedRecord)
                }
            }
            val trimmed = state.expression.trimEnd()
            if (trimmed.isEmpty()) {
                return@update NormalCalculatorUiState(lastSavedRecord = state.lastSavedRecord)
            }
            val nextExpr = if (trimmed.last() in operators) {
                trimmed.dropLast(1).trimEnd()
            } else {
                trimmed.dropLast(1)
            }
            buildStateFromExpression(nextExpr, state.lastSavedRecord)
        }
        scheduleNormalAutoSave()
    }

    fun onOperator(op: String) {
        val opChar = op.firstOrNull() ?: return
        _uiState.update { state ->
            if (state.isError) return@update state

            val baseExpr = if (state.justEvaluated && state.finalResultDisplay.isNotEmpty()) {
                state.finalResultDisplay
            } else {
                state.expression
            }

            val trimmed = baseExpr.trimEnd()
            if (trimmed.isEmpty()) {
                return@update if (opChar == '-') {
                    buildStateFromExpression("-", state.lastSavedRecord)
                } else {
                    state
                }
            }

            val nextExpr = when {
                trimmed == "-" -> if (opChar == '-') "-" else ""
                trimmed.last() in operators -> {
                    // Replace the trailing operator cleanly
                    "${trimmed.dropLast(1).trimEnd()} $opChar "
                }
                trimmed.endsWith(".") -> {
                    "${trimmed.dropLast(1)} $opChar "
                }
                else -> "$trimmed $opChar "
            }
            buildStateFromExpression(nextExpr, state.lastSavedRecord)
        }
        scheduleNormalAutoSave()
    }

    fun onPercent() {
        _uiState.update { state ->
            if (state.isError) return@update state
            val baseExpr = if (state.justEvaluated && state.finalResultDisplay.isNotEmpty()) {
                state.finalResultDisplay
            } else {
                state.expression
            }
            val trimmed = baseExpr.trimEnd()
            if (trimmed.isEmpty()) return@update state
            val lastCh = trimmed.last()
            if (!lastCh.isDigit() && lastCh != '.') return@update state
            val cleanBase = if (lastCh == '.') trimmed.dropLast(1) else trimmed
            if (cleanBase.isEmpty()) return@update state
            buildStateFromExpression("$cleanBase%", state.lastSavedRecord)
        }
        scheduleNormalAutoSave()
    }

    fun onEquals(onSaved: ((CalculationEntity) -> Unit)? = null) {
        val state = _uiState.value
        if (state.isError) return

        // If already evaluated, return existing record for Share/Receipt without creating duplicates
        if (state.justEvaluated && state.lastSavedRecord != null) {
            onSaved?.invoke(state.lastSavedRecord)
            return
        }

        val raw = if (state.justEvaluated && state.finalResultDisplay.isNotEmpty()) {
            state.finalResultDisplay
        } else {
            state.expression
        }.trim()

        if (raw.isEmpty()) return

        // Trim any trailing dangling operator when user taps = or Share
        val evalExpr = raw.trimEnd { it == ' ' || it in operators }
        if (evalExpr.isEmpty()) return

        val res = CalculatorEngine.evaluateExpression(evalExpr, allowTrailingOperator = false)
        if (res.isFailure) {
            autoSaveJob?.cancel()
            _uiState.update {
                it.copy(
                    evaluatedHeader = evalExpr,
                    finalResultDisplay = "Error",
                    livePreview = "",
                    isError = true,
                    justEvaluated = true
                )
            }
            return
        }

        val finalValue = res.getOrNull() ?: return
        val finalStr = CalculatorEngine.formatPlainNumber(finalValue, 8)

        autoSaveJob?.cancel()
        _uiState.update {
            it.copy(
                expression = finalStr,
                evaluatedHeader = "$evalExpr =",
                finalResultDisplay = finalStr,
                livePreview = "",
                isError = false,
                justEvaluated = true
            )
        }

        saveNormalHistory(evalExpr, finalStr, finalizeSession = true, onSaved = onSaved)
    }

    fun flushAutoSaveIfNeeded() {
        val state = _uiState.value
        if (state.isError || state.justEvaluated) return
        val preview = state.livePreview
        if (preview.isEmpty()) return

        val effectiveExpr = state.expression.trim().trimEnd { it == ' ' || it in operators }
        if (effectiveExpr.isEmpty()) return

        val signature = "$effectiveExpr=$preview"
        if (signature != lastSavedSignature) {
            autoSaveJob?.cancel()
            lastSavedSignature = signature
            repository.insertNonCancellable(
                CalculationEntity(
                    id = activeSessionHistoryId ?: 0L,
                    timestamp = System.currentTimeMillis(),
                    calculationType = CalculationType.NORMAL.key,
                    finalResult = preview,
                    detailsSummary = "$effectiveExpr = $preview"
                )
            )
        }
    }

    private fun scheduleNormalAutoSave() {
        autoSaveJob?.cancel()
        val state = _uiState.value
        if (state.isError || state.justEvaluated || state.livePreview.isEmpty()) return
        val effectiveExpr = state.expression.trim().trimEnd { it == ' ' || it in operators }
        if (effectiveExpr.isEmpty()) return

        val preview = state.livePreview
        val signature = "$effectiveExpr=$preview"
        if (signature == lastSavedSignature) return

        autoSaveJob = viewModelScope.launch {
            delay(650L)
            saveNormalHistory(effectiveExpr, preview, finalizeSession = false, onSaved = null)
        }
    }

    private fun buildStateFromExpression(
        expr: String,
        lastSaved: CalculationEntity?
    ): NormalCalculatorUiState {
        val cleanPreview = computeLivePreview(expr)
        return NormalCalculatorUiState(
            expression = expr,
            evaluatedHeader = "",
            finalResultDisplay = "",
            livePreview = cleanPreview,
            isError = false,
            justEvaluated = false,
            lastSavedRecord = lastSaved
        )
    }

    private fun computeLivePreview(expr: String): String {
        val trimmed = expr.trim()
        if (trimmed.isEmpty()) return ""
        val hasOperatorOrPercent = trimmed.drop(1).any { it in operators || it == '%' }
        if (!hasOperatorOrPercent) return ""

        // Must have at least one complete operation after stripping any trailing operator
        val effective = trimmed.trimEnd { it == ' ' || it in operators }
        val effectiveHasOp = effective.drop(1).any { it in operators || it == '%' }
        if (!effectiveHasOp) return ""

        val res = CalculatorEngine.evaluateExpression(effective, allowTrailingOperator = false)
        val value = res.getOrNull() ?: return ""
        return CalculatorEngine.formatPlainNumber(value, 8)
    }

    private fun currentNumberToken(expr: String): String {
        val trimmed = expr.trimEnd()
        if (trimmed.isEmpty() || expr.endsWith(" ")) return ""
        val lastOpIndex = trimmed.indexOfLast { it in operators }
        return if (lastOpIndex == -1) trimmed else trimmed.substring(lastOpIndex + 1).trimStart()
    }

    private fun saveNormalHistory(
        expression: String,
        resultStr: String,
        finalizeSession: Boolean,
        onSaved: ((CalculationEntity) -> Unit)?
    ) {
        val signature = "$expression=$resultStr"
        val currentSaved = _uiState.value.lastSavedRecord
        if (signature == lastSavedSignature && currentSaved != null && activeSessionHistoryId != null) {
            if (finalizeSession) {
                activeSessionHistoryId = null
                lastSavedSignature = null
            }
            onSaved?.invoke(currentSaved)
            return
        }

        val entity = CalculationEntity(
            id = activeSessionHistoryId ?: 0L,
            timestamp = System.currentTimeMillis(),
            calculationType = CalculationType.NORMAL.key,
            finalResult = resultStr,
            detailsSummary = "$expression = $resultStr"
        )
        viewModelScope.launch {
            val id = repository.insert(entity)
            val saved = entity.copy(id = id)
            if (finalizeSession) {
                activeSessionHistoryId = null
                lastSavedSignature = null
            } else {
                activeSessionHistoryId = id
                lastSavedSignature = signature
            }
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
            modelClass.isAssignableFrom(PieceToMoneyViewModel::class.java) ->
                PieceToMoneyViewModel(repository) as T
            modelClass.isAssignableFrom(NormalCalculatorViewModel::class.java) ->
                NormalCalculatorViewModel(repository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
