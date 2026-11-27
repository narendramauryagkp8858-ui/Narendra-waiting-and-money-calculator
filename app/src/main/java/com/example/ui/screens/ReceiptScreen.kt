package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.CalculationEntity
import com.example.data.local.ShopProfile
import com.example.localization.LocalStrings
import com.example.receipt.ReceiptFormatter
import com.example.receipt.ReceiptPdfHelper
import com.example.ui.components.NarendraTopBar
import com.example.ui.components.ResultDetailRow
import com.example.ui.components.ShopLogoAvatar
import com.example.ui.components.WhatsAppActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ReceiptScreen(
    calculationId: Long,
    shopProfile: ShopProfile,
    loadCalculation: suspend (Long) -> CalculationEntity?,
    onEditShopProfile: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var calculation by remember { mutableStateOf<CalculationEntity?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(calculationId) {
        isLoading = true
        calculation = loadCalculation(calculationId)
        isLoading = false
    }

    val createPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val currentCalc = calculation
        if (uri != null && currentCalc != null) {
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    ReceiptPdfHelper.writePdfToDestinationUri(
                        context = context,
                        destinationUri = uri,
                        calculation = currentCalc,
                        shopProfile = shopProfile,
                        strings = strings
                    )
                }
                snackbarHostState.showSnackbar(
                    if (success) strings.pdfSavedSuccess else strings.pdfError
                )
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            NarendraTopBar(
                title = strings.receiptTitle,
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = onEditShopProfile,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("receipt_edit_shop_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = strings.editShopProfile
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val calc = calculation
                if (calc == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(strings.noCalculationForShareTitle)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 600.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Printable Paper Receipt Card
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("receipt_preview_card")
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Top Brand Strip
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .background(MaterialTheme.colorScheme.primary)
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (shopProfile.logoPath.isNotBlank()) {
                                        ShopLogoAvatar(
                                            logoPath = shopProfile.logoPath,
                                            shopName = shopProfile.effectiveShopName,
                                            size = 64.dp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }

                                    Text(
                                        text = shopProfile.effectiveShopName,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("receipt_shop_name")
                                    )

                                    if (shopProfile.ownerName.isNotBlank()) {
                                        Text(
                                            text = "${strings.ownerPrefix}: ${shopProfile.ownerName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    if (shopProfile.mobileNumber.isNotBlank()) {
                                        Text(
                                            text = "${strings.mobilePrefix}: ${shopProfile.mobileNumber}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    if (shopProfile.address.isNotBlank()) {
                                        Text(
                                            text = shopProfile.address,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    DashedReceiptDivider(color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = strings.shopReceiptHeader,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    ResultDetailRow(
                                        label = strings.dateTimeLabel,
                                        value = ReceiptFormatter.formatTimestamp(calc.timestamp)
                                    )
                                    ResultDetailRow(
                                        label = strings.calculationTypeLabel,
                                        value = ReceiptFormatter.calculationTypeLabel(calc.calculationType, strings)
                                    )
                                    if (calc.itemName.isNotBlank()) {
                                        ResultDetailRow(label = "Item", value = calc.itemName)
                                    }
                                    if (calc.ratePerKg.isNotBlank()) {
                                        ResultDetailRow(
                                            label = ReceiptFormatter.rateFieldLabel(calc.calculationType, strings),
                                            value = calc.ratePerKg
                                        )
                                    }
                                    if (calc.weightDisplay.isNotBlank()) {
                                        ResultDetailRow(
                                            label = ReceiptFormatter.quantityOrWeightFieldLabel(calc.calculationType, strings),
                                            value = calc.weightDisplay
                                        )
                                    }
                                    if (calc.amountDisplay.isNotBlank()) {
                                        ResultDetailRow(label = strings.amountLabel, value = calc.amountDisplay)
                                    }
                                    if (calc.detailsSummary.isNotBlank()) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp)
                                        ) {
                                            Text(
                                                text = strings.calculationDetailsLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = calc.detailsSummary,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    DashedReceiptDivider(color = MaterialTheme.colorScheme.outline)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Highlighted Total Amount Box
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = strings.finalResultLabel,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Text(
                                                text = calc.finalResult,
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = strings.receiptFooterBrand,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Action Buttons: Direct WhatsApp, Share via Other Apps, Share PDF, Download PDF, Print Receipt
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            WhatsAppActionButton(
                                text = strings.shareWhatsApp,
                                onClick = {
                                    val text = ReceiptFormatter.buildWhatsAppTextReceipt(
                                        calc, shopProfile, strings
                                    )
                                    ReceiptFormatter.shareDirectlyToWhatsApp(context, text, strings)
                                },
                                testTag = "receipt_share_whatsapp_button",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            )

                            OutlinedButton(
                                onClick = {
                                    val text = ReceiptFormatter.buildWhatsAppTextReceipt(
                                        calc, shopProfile, strings
                                    )
                                    ReceiptFormatter.shareText(context, text, strings.shareChooserTitle)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("receipt_share_text_button")
                            ) {
                                Text(strings.shareToOtherApps)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            try {
                                                val uri = withContext(Dispatchers.IO) {
                                                    ReceiptPdfHelper.createShareablePdfUri(
                                                        context, calc, shopProfile, strings
                                                    )
                                                }
                                                ReceiptPdfHelper.sharePdf(
                                                    context, uri, strings.shareAsPdf
                                                )
                                            } catch (_: Exception) {
                                                snackbarHostState.showSnackbar(strings.pdfError)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("receipt_share_pdf_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PictureAsPdf,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.shareAsPdf)
                                }

                                OutlinedButton(
                                    onClick = {
                                        createPdfLauncher.launch("Receipt_${calc.id}.pdf")
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp)
                                        .testTag("receipt_download_pdf_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(strings.downloadPdf)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    ReceiptPdfHelper.printReceipt(context, calc, shopProfile, strings)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("receipt_print_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(strings.printReceipt)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashedReceiptDivider(color: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        drawLine(
            color = color,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
        )
    }
}
