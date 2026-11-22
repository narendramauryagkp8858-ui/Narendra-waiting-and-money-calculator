package com.example.receipt

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.local.CalculationEntity
import com.example.data.local.ShopProfile
import com.example.domain.CalculationType
import com.example.localization.AppStrings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptFormatter {
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun calculationTypeLabel(typeKey: String, strings: AppStrings): String {
        return when (CalculationType.fromKey(typeKey)) {
            CalculationType.WEIGHT_TO_MONEY -> strings.weightToMoneyTitle
            CalculationType.MONEY_TO_WEIGHT -> strings.moneyToWeightTitle
            CalculationType.COMBINED_BILL -> strings.combinedBillTitle
            CalculationType.NORMAL -> strings.normalCalculatorTitle
        }
    }

    fun buildWhatsAppTextReceipt(
        calculation: CalculationEntity,
        shopProfile: ShopProfile,
        strings: AppStrings
    ): String {
        val sb = StringBuilder()
        sb.appendLine("*${shopProfile.effectiveShopName}*")
        if (shopProfile.ownerName.isNotBlank()) {
            sb.appendLine("${strings.ownerPrefix}: ${shopProfile.ownerName}")
        }
        if (shopProfile.mobileNumber.isNotBlank()) {
            sb.appendLine("${strings.mobilePrefix}: ${shopProfile.mobileNumber}")
        }
        if (shopProfile.address.isNotBlank()) {
            sb.appendLine(shopProfile.address)
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("*${strings.shopReceiptHeader}*")
        sb.appendLine("${strings.dateTimeLabel}: ${formatTimestamp(calculation.timestamp)}")
        sb.appendLine("${strings.calculationTypeLabel}: ${calculationTypeLabel(calculation.calculationType, strings)}")
        if (calculation.itemName.isNotBlank()) {
            sb.appendLine("Item: ${calculation.itemName}")
        }
        if (calculation.ratePerKg.isNotBlank()) {
            sb.appendLine("${strings.ratePerKgLabel}: ${calculation.ratePerKg}")
        }
        if (calculation.weightDisplay.isNotBlank()) {
            sb.appendLine("${strings.weightLabel}: ${calculation.weightDisplay}")
        }
        if (calculation.amountDisplay.isNotBlank()) {
            sb.appendLine("${strings.amountLabel}: ${calculation.amountDisplay}")
        }
        if (calculation.detailsSummary.isNotBlank()) {
            sb.appendLine("${strings.calculationDetailsLabel}:")
            sb.appendLine(calculation.detailsSummary)
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("*${strings.finalResultLabel}: ${calculation.finalResult}*")
        sb.appendLine("--------------------------------")
        sb.append("_${strings.receiptFooterBrand}_")
        return sb.toString()
    }

    /**
     * Shares receipt text directly to WhatsApp (or WhatsApp Business), avoiding generic share sheets.
     * Falls back to Android's native share chooser with a notice only if WhatsApp is not installed.
     */
    fun shareDirectlyToWhatsApp(
        context: Context,
        text: String,
        strings: AppStrings
    ) {
        val pm = context.packageManager
        val targetPackage = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { pkg ->
            val probe = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage(pkg)
            }
            probe.resolveActivity(pm) != null
        }

        if (targetPackage != null) {
            try {
                val waIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage(targetPackage)
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(waIntent)
                return
            } catch (_: ActivityNotFoundException) {
                // Fall through to chooser fallback
            }
        }

        Toast.makeText(context, strings.whatsappNotInstalled, Toast.LENGTH_SHORT).show()
        shareText(context, text, strings.shareChooserTitle)
    }

    fun shareText(context: Context, text: String, chooserTitle: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
