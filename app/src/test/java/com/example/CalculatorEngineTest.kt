package com.example

import com.example.data.local.CalculationEntity
import com.example.data.local.ShopProfile
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
}
