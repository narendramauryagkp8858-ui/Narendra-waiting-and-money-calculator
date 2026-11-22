package com.example.receipt

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import com.example.data.local.CalculationEntity
import com.example.data.local.ShopProfile
import com.example.localization.AppStrings
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ReceiptPdfHelper {

    fun generatePdfBytes(
        calculation: CalculationEntity,
        shopProfile: ShopProfile,
        strings: AppStrings
    ): ByteArray {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        drawReceiptPage(canvas, pageWidth.toFloat(), pageHeight.toFloat(), calculation, shopProfile, strings)
        document.finishPage(page)

        val baos = ByteArrayOutputStream()
        document.writeTo(baos)
        document.close()
        return baos.toByteArray()
    }

    private fun drawReceiptPage(
        canvas: Canvas,
        pageWidth: Float,
        pageHeight: Float,
        calculation: CalculationEntity,
        shopProfile: ShopProfile,
        strings: AppStrings
    ) {
        canvas.drawColor(Color.WHITE)

        val margin = 52f
        val contentWidth = pageWidth - (margin * 2)
        val cardRect = RectF(margin - 12f, 36f, pageWidth - margin + 12f, pageHeight - 48f)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(cardRect, 16f, 16f, borderPaint)

        val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B6E4F")
            style = Paint.Style.FILL
        }
        val headerRect = RectF(cardRect.left, cardRect.top, cardRect.right, cardRect.top + 14f)
        canvas.drawRoundRect(headerRect, 8f, 8f, headerBgPaint)

        var y = cardRect.top + 48f

        // Optional Shop Logo
        if (shopProfile.logoPath.isNotBlank()) {
            val logoFile = File(shopProfile.logoPath)
            if (logoFile.exists()) {
                val bmp = BitmapFactory.decodeFile(logoFile.absolutePath)
                if (bmp != null) {
                    val targetSize = 72
                    val scaled = Bitmap.createScaledBitmap(bmp, targetSize, targetSize, true)
                    val left = (pageWidth - targetSize) / 2f
                    canvas.drawBitmap(scaled, left, y, null)
                    y += targetSize + 18f
                    if (scaled != bmp) scaled.recycle()
                    bmp.recycle()
                }
            }
        }

        val shopTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(shopProfile.effectiveShopName, pageWidth / 2f, y, shopTitlePaint)
        y += 24f

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 13f
            textAlign = Paint.Align.CENTER
        }
        if (shopProfile.ownerName.isNotBlank()) {
            canvas.drawText("${strings.ownerPrefix}: ${shopProfile.ownerName}", pageWidth / 2f, y, subPaint)
            y += 18f
        }
        if (shopProfile.mobileNumber.isNotBlank()) {
            canvas.drawText("${strings.mobilePrefix}: ${shopProfile.mobileNumber}", pageWidth / 2f, y, subPaint)
            y += 18f
        }
        if (shopProfile.address.isNotBlank()) {
            drawCenteredWrappedText(canvas, shopProfile.address, pageWidth / 2f, y, contentWidth - 20f, subPaint)
            y += 22f
        }

        y += 8f
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#94A3B8")
            strokeWidth = 1.2f
            pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
        }
        canvas.drawLine(margin + 8f, y, pageWidth - margin - 8f, y, dashPaint)
        y += 28f

        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B6E4F")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(strings.shopReceiptHeader, pageWidth / 2f, y, sectionPaint)
        y += 28f

        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#475569")
            textSize = 14f
            textAlign = Paint.Align.LEFT
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }

        fun drawRow(label: String, value: String) {
            if (value.isBlank()) return
            canvas.drawText(label, margin + 12f, y, labelPaint)
            canvas.drawText(value, pageWidth - margin - 12f, y, valuePaint)
            y += 28f
        }

        drawRow(strings.dateTimeLabel, ReceiptFormatter.formatTimestamp(calculation.timestamp))
        drawRow(strings.calculationTypeLabel, ReceiptFormatter.calculationTypeLabel(calculation.calculationType, strings))
        if (calculation.itemName.isNotBlank()) {
            drawRow("Item", calculation.itemName)
        }
        drawRow(strings.ratePerKgLabel, calculation.ratePerKg)
        drawRow(strings.weightLabel, calculation.weightDisplay)
        drawRow(strings.amountLabel, calculation.amountDisplay)

        if (calculation.detailsSummary.isNotBlank()) {
            y += 4f
            canvas.drawText(strings.calculationDetailsLabel, margin + 12f, y, labelPaint)
            y += 20f
            val detailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#334155")
                textSize = 13f
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText(calculation.detailsSummary, margin + 12f, y, detailPaint)
            y += 24f
        }

        y += 8f
        canvas.drawLine(margin + 8f, y, pageWidth - margin - 8f, y, dashPaint)
        y += 24f

        // Highlighted Total Result Box
        val totalBox = RectF(margin + 8f, y, pageWidth - margin - 8f, y + 66f)
        val totalBoxBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#ECFDF5")
            style = Paint.Style.FILL
        }
        val totalBoxBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0B6E4F")
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        canvas.drawRoundRect(totalBox, 12f, 12f, totalBoxBg)
        canvas.drawRoundRect(totalBox, 12f, 12f, totalBoxBorder)

        val totalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#065F46")
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val totalValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#064E3B")
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(strings.finalResultLabel, totalBox.left + 18f, totalBox.centerY() + 6f, totalLabelPaint)
        canvas.drawText(calculation.finalResult, totalBox.right - 18f, totalBox.centerY() + 8f, totalValuePaint)

        // Footer branding
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#64748B")
            textSize = 11.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(strings.receiptFooterBrand, pageWidth / 2f, cardRect.bottom - 26f, footerPaint)
    }

    private fun drawCenteredWrappedText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint
    ) {
        if (paint.measureText(text) <= maxWidth) {
            canvas.drawText(text, centerX, startY, paint)
            return
        }
        val words = text.split(" ")
        val line1 = StringBuilder()
        val line2 = StringBuilder()
        var onFirst = true
        for (word in words) {
            if (onFirst && paint.measureText("$line1 $word".trim()) <= maxWidth) {
                if (line1.isNotEmpty()) line1.append(" ")
                line1.append(word)
            } else {
                onFirst = false
                if (line2.isNotEmpty()) line2.append(" ")
                line2.append(word)
            }
        }
        canvas.drawText(line1.toString(), centerX, startY, paint)
        if (line2.isNotEmpty()) {
            canvas.drawText(line2.toString(), centerX, startY + 16f, paint)
        }
    }

    fun createShareablePdfUri(
        context: Context,
        calculation: CalculationEntity,
        shopProfile: ShopProfile,
        strings: AppStrings
    ): Uri {
        val receiptsDir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(receiptsDir, "receipt_${calculation.id}_${System.currentTimeMillis()}.pdf")
        val bytes = generatePdfBytes(calculation, shopProfile, strings)
        file.writeBytes(bytes)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun sharePdf(context: Context, pdfUri: Uri, chooserTitle: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun writePdfToDestinationUri(
        context: Context,
        destinationUri: Uri,
        calculation: CalculationEntity,
        shopProfile: ShopProfile,
        strings: AppStrings
    ): Boolean {
        return try {
            val bytes = generatePdfBytes(calculation, shopProfile, strings)
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(bytes)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun printReceipt(
        context: Context,
        calculation: CalculationEntity,
        shopProfile: ShopProfile,
        strings: AppStrings
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "${shopProfile.effectiveShopName} - ${strings.receiptTitle}"
        val pdfBytes = generatePdfBytes(calculation, shopProfile, strings)

        val adapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }
                val info = PrintDocumentInfo.Builder("receipt_${calculation.id}.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onWriteCancelled()
                    return
                }
                try {
                    destination?.fileDescriptor?.let { fd ->
                        FileOutputStream(fd).use { out ->
                            out.write(pdfBytes)
                        }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }

        printManager.print(jobName, adapter, PrintAttributes.Builder().build())
    }
}
