package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ShopProfile
import com.example.domain.BillLineItem
import com.example.domain.CalculatorEngine
import com.example.localization.LocalStrings
import com.example.receipt.ReceiptFormatter
import com.example.ui.components.NarendraTopBar
import com.example.ui.components.ResultDetailRow
import com.example.ui.components.WhatsAppActionButton
import com.example.ui.theme.translucentCardBorderColor
import com.example.ui.theme.translucentCardSurfaceColor
import com.example.ui.viewmodel.PieceToMoneyViewModel
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PieceToMoneyScreen(
    viewModel: PieceToMoneyViewModel,
    shopProfile: ShopProfile,
    billItems: List<BillLineItem>,
    onAddToBill: (itemName: String, pricePerPiece: BigDecimal, quantity: BigDecimal) -> Unit,
    onOpenCombinedBill: () -> Unit,
    onBack: () -> Unit,
    onOpenReceipt: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            viewModel.flushAutoSaveIfNeeded()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            NarendraTopBar(
                title = strings.pieceToMoneyTitle,
                onBack = onBack,
                actions = {
                    if (billItems.isNotEmpty()) {
                        Surface(
                            onClick = onOpenCombinedBill,
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("p2m_top_bill_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddShoppingCart,
                                    contentDescription = strings.viewFullBill,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Badge {
                                    Text("${billItems.size}")
                                }
                            }
                        }
                    }
                    IconButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("piece_to_money_reset_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = strings.reset
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
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (billItems.isNotEmpty()) {
                    val summaryParts = remember(billItems, strings) {
                        val totals = CalculatorEngine.calculateCombinedBillTotals(billItems)
                        buildList {
                            if (totals.weightItemCount > 0) {
                                add(CalculatorEngine.formatGramsHuman(totals.totalWeightInGrams, strings.kgUnit, strings.gramUnit))
                            }
                            if (totals.pieceItemCount > 0) {
                                add("${CalculatorEngine.formatPlainNumber(totals.totalPieces, 3)} ${strings.piecesPlural}")
                            }
                            add(CalculatorEngine.formatAmount(totals.totalAmount))
                        }.joinToString(" • ")
                    }

                    Card(
                        onClick = onOpenCombinedBill,
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${strings.billItemsCount}: ${billItems.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = summaryParts,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = strings.viewFullBill,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Input Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = translucentCardSurfaceColor()),
                    border = BorderStroke(1.dp, translucentCardBorderColor()),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.pricePerPieceInput,
                            onValueChange = viewModel::updatePrice,
                            label = { Text(strings.pricePerPieceLabel) },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                            suffix = {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = strings.perPieceUnit,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            },
                            isError = state.priceError,
                            supportingText = if (state.priceError) {
                                { Text(strings.invalidPriceError) }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("p2m_price_input")
                        )

                        OutlinedTextField(
                            value = state.quantityInput,
                            onValueChange = viewModel::updateQuantity,
                            label = { Text(strings.enterPiecesLabel) },
                            suffix = {
                                Text(
                                    text = strings.pieceUnit,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            },
                            isError = state.quantityError,
                            supportingText = if (state.quantityError) {
                                { Text(strings.invalidQuantityError) }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("p2m_quantity_input")
                        )

                        Text(
                            text = strings.quickPieces,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val quickQuantities = remember { listOf("1", "2", "5", "10", "12", "24") }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            quickQuantities.forEach { qty ->
                                AssistChip(
                                    onClick = { viewModel.applyQuickQuantity(qty) },
                                    label = { Text("$qty ${strings.pieceUnit}") }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = state.itemName,
                            onValueChange = viewModel::updateItemName,
                            label = { Text(strings.itemNameOptional) },
                            placeholder = { Text(strings.itemNameHint) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("p2m_item_name_input")
                        )

                        Button(
                            onClick = {
                                viewModel.calculateAndSave {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(strings.savedToHistory)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("p2m_calculate_save_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.calculateAndSave,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                // Live Result Card
                state.liveResult?.let { result ->
                        val qtyText = "${CalculatorEngine.formatPlainNumber(result.quantity, 3)} ${strings.pieceUnit}"
                        val rateText = "${CalculatorEngine.formatAmount(result.pricePerPiece)} / ${strings.pieceUnit}"
                        val totalText = CalculatorEngine.formatAmount(result.totalAmount)

                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.totalAmountLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (state.savedRecord != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = strings.autoSavedLabel,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = totalText,
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("p2m_result_amount")
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                )

                                ResultDetailRow(
                                    label = strings.quantityLabel,
                                    value = qtyText
                                )
                                ResultDetailRow(
                                    label = strings.ratePerPieceLabel,
                                    value = rateText
                                )
                                ResultDetailRow(
                                    label = strings.totalAmountLabel,
                                    value = totalText,
                                    highlightValue = true
                                )

                                Button(
                                    onClick = {
                                        viewModel.calculateAndSave {
                                            onAddToBill(
                                                state.itemName,
                                                result.pricePerPiece,
                                                result.quantity
                                            )
                                            viewModel.prepareForNextBatch()
                                            scope.launch {
                                                snackbarHostState.showSnackbar(strings.addedToBill)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("p2m_add_to_bill_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AddShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(strings.addToBill)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    WhatsAppActionButton(
                                        text = strings.shareWhatsApp,
                                        onClick = {
                                            val existing = state.savedRecord
                                            if (existing != null) {
                                                val text = ReceiptFormatter.buildWhatsAppTextReceipt(
                                                    existing, shopProfile, strings
                                                )
                                                ReceiptFormatter.shareDirectlyToWhatsApp(
                                                    context, text, strings
                                                )
                                            } else {
                                                viewModel.calculateAndSave { saved ->
                                                    val text = ReceiptFormatter.buildWhatsAppTextReceipt(
                                                        saved, shopProfile, strings
                                                    )
                                                    ReceiptFormatter.shareDirectlyToWhatsApp(
                                                        context, text, strings
                                                    )
                                                }
                                            }
                                        },
                                        testTag = "p2m_whatsapp_share_button",
                                        modifier = Modifier.weight(1f)
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            val existing = state.savedRecord
                                            if (existing != null) {
                                                onOpenReceipt(existing.id)
                                            } else {
                                                viewModel.calculateAndSave { saved ->
                                                    onOpenReceipt(saved.id)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("p2m_view_receipt_button"),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(strings.viewReceipt)
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }
}
