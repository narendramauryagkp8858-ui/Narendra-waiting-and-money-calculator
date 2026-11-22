package com.example.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

enum class CalculationType(val key: String) {
    WEIGHT_TO_MONEY("WEIGHT_TO_MONEY"),
    MONEY_TO_WEIGHT("MONEY_TO_WEIGHT"),
    COMBINED_BILL("COMBINED_BILL"),
    NORMAL("NORMAL");

    companion object {
        fun fromKey(key: String): CalculationType =
            entries.find { it.key == key } ?: WEIGHT_TO_MONEY
    }
}

enum class WeightUnit {
    KG,
    GRAM
}

data class BillLineItem(
    val id: Long,
    val itemName: String,
    val pricePerKg: BigDecimal,
    val weightInGrams: BigDecimal,
    val amount: BigDecimal
)

data class CombinedBillTotals(
    val totalWeightInGrams: BigDecimal,
    val wholeKg: Long,
    val remainingGrams: BigDecimal,
    val totalWeightInKg: BigDecimal,
    val totalAmount: BigDecimal
)

data class WeightMoneyResult(
    val pricePerKg: BigDecimal,
    val inputWeight: BigDecimal,
    val inputUnit: WeightUnit,
    val weightInGrams: BigDecimal,
    val weightInKg: BigDecimal,
    val totalAmount: BigDecimal
)

data class MoneyWeightResult(
    val pricePerKg: BigDecimal,
    val customerAmount: BigDecimal,
    val weightInGrams: BigDecimal,
    val weightInKg: BigDecimal,
    val wholeKg: Long,
    val remainingGrams: BigDecimal
)

object CalculatorEngine {
    private val THOUSAND = BigDecimal("1000")
    private val HUNDRED = BigDecimal("100")
    private val CALC_SCALE = 10
    private val MC = MathContext(16, RoundingMode.HALF_UP)

    fun parsePositiveDecimal(raw: String): BigDecimal? {
        val cleaned = raw.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        return try {
            val bd = BigDecimal(cleaned)
            if (bd > BigDecimal.ZERO) bd else null
        } catch (_: NumberFormatException) {
            null
        }
    }

    fun parseDecimal(raw: String): BigDecimal? {
        val cleaned = raw.trim().replace(",", "")
        if (cleaned.isEmpty()) return null
        return try {
            BigDecimal(cleaned)
        } catch (_: NumberFormatException) {
            null
        }
    }

    /**
     * Amount = (Weight in Gram / 1000) * Price per KG
     */
    fun calculateWeightToMoney(
        pricePerKg: BigDecimal,
        weight: BigDecimal,
        unit: WeightUnit
    ): WeightMoneyResult {
        require(pricePerKg > BigDecimal.ZERO) { "Price per KG must be greater than zero" }
        require(weight > BigDecimal.ZERO) { "Weight must be greater than zero" }

        val weightInGrams = when (unit) {
            WeightUnit.KG -> weight.multiply(THOUSAND)
            WeightUnit.GRAM -> weight
        }
        val weightInKg = weightInGrams.divide(THOUSAND, CALC_SCALE, RoundingMode.HALF_UP)
        val totalAmount = weightInKg.multiply(pricePerKg).setScale(2, RoundingMode.HALF_UP)

        return WeightMoneyResult(
            pricePerKg = pricePerKg,
            inputWeight = weight,
            inputUnit = unit,
            weightInGrams = weightInGrams.stripTrailingZerosSafe(),
            weightInKg = weightInKg.stripTrailingZerosSafe(),
            totalAmount = totalAmount.stripTrailingZerosSafe()
        )
    }

    /**
     * Weight in Gram = (Amount / Price per KG) * 1000
     */
    fun calculateMoneyToWeight(
        pricePerKg: BigDecimal,
        amount: BigDecimal
    ): MoneyWeightResult {
        require(pricePerKg > BigDecimal.ZERO) { "Price per KG must be greater than zero" }
        require(amount > BigDecimal.ZERO) { "Amount must be greater than zero" }

        val weightInKg = amount.divide(pricePerKg, CALC_SCALE, RoundingMode.HALF_UP)
        val weightInGrams = weightInKg.multiply(THOUSAND).setScale(2, RoundingMode.HALF_UP)

        val wholeKg = weightInGrams.divideToIntegralValue(THOUSAND).toLong()
        val remainingGrams = weightInGrams.remainder(THOUSAND).setScale(2, RoundingMode.HALF_UP)

        return MoneyWeightResult(
            pricePerKg = pricePerKg,
            customerAmount = amount,
            weightInGrams = weightInGrams.stripTrailingZerosSafe(),
            weightInKg = weightInKg.setScale(3, RoundingMode.HALF_UP).stripTrailingZerosSafe(),
            wholeKg = wholeKg,
            remainingGrams = remainingGrams.stripTrailingZerosSafe()
        )
    }

    /**
     * Evaluates a binary arithmetic operation with BigDecimal accuracy.
     */
    fun applyOperation(
        left: BigDecimal,
        right: BigDecimal,
        operator: String
    ): Result<BigDecimal> {
        return try {
            val res = when (operator) {
                "+" -> left.add(right, MC)
                "-" -> left.subtract(right, MC)
                "×", "*" -> left.multiply(right, MC)
                "÷", "/" -> {
                    if (right.compareTo(BigDecimal.ZERO) == 0) {
                        return Result.failure(ArithmeticException("Cannot divide by zero"))
                    }
                    left.divide(right, 10, RoundingMode.HALF_UP)
                }
                else -> right
            }
            Result.success(res.stripTrailingZerosSafe())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun applyPercent(base: BigDecimal?, value: BigDecimal, operator: String?): BigDecimal {
        return if (base != null && (operator == "+" || operator == "-")) {
            base.multiply(value, MC).divide(HUNDRED, 10, RoundingMode.HALF_UP).stripTrailingZerosSafe()
        } else {
            value.divide(HUNDRED, 10, RoundingMode.HALF_UP).stripTrailingZerosSafe()
        }
    }

    fun formatAmount(value: BigDecimal): String {
        val scaled = value.setScale(2, RoundingMode.HALF_UP)
        val hasFraction = scaled.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0
        val pattern = if (hasFraction) "#,##0.00" else "#,##0"
        val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale.forLanguageTag("en-IN")))
        return "₹${df.format(scaled)}"
    }

    fun formatPlainNumber(value: BigDecimal, maxDecimals: Int = 3): String {
        val stripped = value.setScale(maxDecimals, RoundingMode.HALF_UP).stripTrailingZerosSafe()
        return stripped.toPlainString()
    }

    fun formatWeightHuman(result: MoneyWeightResult, kgLabel: String, gramLabel: String): String {
        return formatGramsHuman(result.weightInGrams, kgLabel, gramLabel)
    }

    fun formatGramsHuman(totalGrams: BigDecimal, kgLabel: String, gramLabel: String): String {
        val wholeKg = totalGrams.divideToIntegralValue(THOUSAND).toLong()
        val remainingGrams = totalGrams.remainder(THOUSAND).setScale(2, RoundingMode.HALF_UP).stripTrailingZerosSafe()
        return when {
            wholeKg > 0 && remainingGrams.compareTo(BigDecimal.ZERO) > 0 ->
                "$wholeKg $kgLabel ${formatPlainNumber(remainingGrams, 2)} $gramLabel"
            wholeKg > 0 ->
                "$wholeKg $kgLabel"
            else ->
                "${formatPlainNumber(totalGrams, 2)} $gramLabel"
        }
    }

    fun calculateCombinedBillTotals(items: List<BillLineItem>): CombinedBillTotals {
        val totalGrams = items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.weightInGrams) }
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZerosSafe()
        val wholeKg = totalGrams.divideToIntegralValue(THOUSAND).toLong()
        val remainingGrams = totalGrams.remainder(THOUSAND).setScale(2, RoundingMode.HALF_UP).stripTrailingZerosSafe()
        val totalKg = totalGrams.divide(THOUSAND, 3, RoundingMode.HALF_UP).stripTrailingZerosSafe()
        val totalAmount = items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZerosSafe()

        return CombinedBillTotals(
            totalWeightInGrams = totalGrams,
            wholeKg = wholeKg,
            remainingGrams = remainingGrams,
            totalWeightInKg = totalKg,
            totalAmount = totalAmount
        )
    }

    private fun BigDecimal.stripTrailingZerosSafe(): BigDecimal {
        return if (this.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal.ZERO
        } else {
            this.stripTrailingZeros()
        }
    }
}
