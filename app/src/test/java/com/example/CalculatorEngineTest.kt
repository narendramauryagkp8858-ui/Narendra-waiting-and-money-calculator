package com.example

import com.example.data.local.CalculationEntity
import com.example.data.local.ShopProfile
import com.example.domain.BillItemMode
import com.example.domain.BillLineItem
import com.example.domain.CalculationType
import com.example.domain.CalculatorEngine
import com.example.domain.WeightUnit
import com.example.localization.EnglishStrings
import com.example.receipt.ReceiptFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalculatorEngineTest {

    @Test
    fun `weight to money calculates exact amount for grams`() {
        val result = CalculatorEngine.calculateWeightToMoney(
            pricePerKg = BigDecimal("100"),
            weight = BigDecimal("250"),
            unit = WeightUnit.GRAM
        )
        assertEquals(0, BigDecimal("25").compareTo(result.totalAmount))
        assertEquals("₹25", CalculatorEngine.formatAmount(result.totalAmount))
    }

    @Test
    fun `weight to money calculates exact decimal amount without float drift`() {
        val result = CalculatorEngine.calculateWeightToMoney(
            pricePerKg = BigDecimal("125.50"),
            weight = BigDecimal("1.5"),
            unit = WeightUnit.KG
        )
        assertEquals(0, BigDecimal("188.25").compareTo(result.totalAmount))
        assertEquals("₹188.25", CalculatorEngine.formatAmount(result.totalAmount))
    }

    @Test
    fun `money to weight calculates exact grams and kg breakdown`() {
        val fiftyRs = CalculatorEngine.calculateMoneyToWeight(
            pricePerKg = BigDecimal("100"),
            amount = BigDecimal("50")
        )
        assertEquals(0, BigDecimal("500").compareTo(fiftyRs.weightInGrams))
        assertEquals("500 Gram", CalculatorEngine.formatWeightHuman(fiftyRs, "KG", "Gram"))

        val mixedKg = CalculatorEngine.calculateMoneyToWeight(
            pricePerKg = BigDecimal("80"),
            amount = BigDecimal("100")
        )
        assertEquals(1L, mixedKg.wholeKg)
        assertEquals(0, BigDecimal("250").compareTo(mixedKg.remainingGrams))
        assertEquals("1 KG 250 Gram", CalculatorEngine.formatWeightHuman(mixedKg, "KG", "Gram"))
    }

    @Test
    fun `normal calculator uses exact BigDecimal arithmetic and prevents div by zero`() {
        val sum = CalculatorEngine.applyOperation(BigDecimal("0.1"), BigDecimal("0.2"), "+").getOrThrow()
        assertEquals("0.3", CalculatorEngine.formatPlainNumber(sum, 8))

        val divZero = CalculatorEngine.applyOperation(BigDecimal("10"), BigDecimal.ZERO, "÷")
        assertTrue(divZero.isFailure)
    }

    @Test
    fun `receipt formatter prioritizes shopkeepers shop name with Narendra branding`() {
        val shop = ShopProfile(
            shopName = "BK KIRANA STORE",
            ownerName = "Brijesh Kumar",
            mobileNumber = "9876543210",
            address = "Main Market"
        )
        val entity = CalculationEntity(
            id = 1L,
            timestamp = 1700000000000L,
            calculationType = CalculationType.WEIGHT_TO_MONEY.key,
            ratePerKg = "₹100 / KG",
            weightDisplay = "250 Gram (0.25 KG)",
            amountDisplay = "₹25",
            finalResult = "₹25",
            detailsSummary = "250 Gram × ₹100 / KG = ₹25"
        )
        val text = ReceiptFormatter.buildWhatsAppTextReceipt(entity, shop, EnglishStrings)
        assertTrue(text.contains("BK KIRANA STORE"))
        assertTrue(text.contains("Narendra Weight & Money Calculator"))
        assertTrue(text.contains("₹25"))
    }

    @Test
    fun `combined bill totals sum multiple weights and amounts accurately`() {
        val items = listOf(
            com.example.domain.BillLineItem(
                id = 1L,
                itemName = "Loha",
                pricePerKg = BigDecimal("80"),
                weightInGrams = BigDecimal("1000"),
                amount = BigDecimal("80")
            ),
            com.example.domain.BillLineItem(
                id = 2L,
                itemName = "Loha",
                pricePerKg = BigDecimal("80"),
                weightInGrams = BigDecimal("2000"),
                amount = BigDecimal("160")
            ),
            com.example.domain.BillLineItem(
                id = 3L,
                itemName = "Loha",
                pricePerKg = BigDecimal("80"),
                weightInGrams = BigDecimal("500"),
                amount = BigDecimal("40")
            )
        )
        val totals = CalculatorEngine.calculateCombinedBillTotals(items)
        assertEquals(0, BigDecimal("3500").compareTo(totals.totalWeightInGrams))
        assertEquals(3L, totals.wholeKg)
        assertEquals(0, BigDecimal("500").compareTo(totals.remainingGrams))
        assertEquals(0, BigDecimal("280").compareTo(totals.totalAmount))
        assertEquals("3 KG 500 Gram", CalculatorEngine.formatGramsHuman(totals.totalWeightInGrams, "KG", "Gram"))
    }

    @Test
    fun `evaluateExpression handles multi-step expressions, precedence, percent, and live trailing operators`() {
        val multi = CalculatorEngine.evaluateExpression("100 + 50 - 20").getOrThrow()!!
        assertEquals("130", CalculatorEngine.formatPlainNumber(multi, 8))

        val precedence = CalculatorEngine.evaluateExpression("100 + 50 × 2").getOrThrow()!!
        assertEquals("200", CalculatorEngine.formatPlainNumber(precedence, 8))

        val pct = CalculatorEngine.evaluateExpression("200 + 10%").getOrThrow()!!
        assertEquals("220", CalculatorEngine.formatPlainNumber(pct, 8))

        val exactDecimal = CalculatorEngine.evaluateExpression("0.1 + 0.2").getOrThrow()!!
        assertEquals("0.3", CalculatorEngine.formatPlainNumber(exactDecimal, 8))

        val trailingOpPreview = CalculatorEngine.evaluateExpression("100 + 50 +", allowTrailingOperator = true).getOrThrow()!!
        assertEquals("150", CalculatorEngine.formatPlainNumber(trailingOpPreview, 8))

        val divZero = CalculatorEngine.evaluateExpression("50 ÷ 0")
        assertTrue(divZero.isFailure)
    }

    @Test
    fun `piece to money calculates exact amount for quantity and rate`() {
        val result = CalculatorEngine.calculatePieceToMoney(
            pricePerPiece = BigDecimal("45.50"),
            quantity = BigDecimal("6")
        )
        assertEquals(0, BigDecimal("273").compareTo(result.totalAmount))
        assertEquals("₹273", CalculatorEngine.formatAmount(result.totalAmount))
    }

    @Test
    fun `combined bill totals sum both weight and piece items accurately`() {
        val items = listOf(
            BillLineItem(
                id = 1L,
                mode = BillItemMode.WEIGHT,
                itemName = "Loha",
                pricePerKg = BigDecimal("80"),
                weightInGrams = BigDecimal("1500"),
                amount = BigDecimal("120")
            ),
            BillLineItem(
                id = 2L,
                mode = BillItemMode.PIECE,
                itemName = "Nut Bolt",
                pricePerKg = BigDecimal("15"),
                pieceQuantity = BigDecimal("4"),
                amount = BigDecimal("60")
            ),
            BillLineItem(
                id = 3L,
                mode = BillItemMode.PIECE,
                itemName = "Washer",
                pricePerKg = BigDecimal("5"),
                pieceQuantity = BigDecimal("6"),
                amount = BigDecimal("30")
            )
        )
        val totals = CalculatorEngine.calculateCombinedBillTotals(items)
        assertEquals(1, totals.weightItemCount)
        assertEquals(2, totals.pieceItemCount)
        assertEquals(0, BigDecimal("1500").compareTo(totals.totalWeightInGrams))
        assertEquals(0, BigDecimal("10").compareTo(totals.totalPieces))
        assertEquals(0, BigDecimal("210").compareTo(totals.totalAmount))
    }
}
