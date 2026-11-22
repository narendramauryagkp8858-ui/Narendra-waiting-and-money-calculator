package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.CalculationEntity
import com.example.data.local.ShopProfile
import com.example.domain.BillLineItem
import com.example.domain.CalculatorEngine
import com.example.domain.WeightUnit
import com.example.localization.LocalStrings
import com.example.receipt.ReceiptFormatter
import com.example.ui.components.NarendraTopBar
import com.example.ui.components.ResultDetailRow
import com.example.ui.components.WhatsAppActionButton
import kotlinx.coroutines.launch
import java.math.BigDecimal

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CombinedBillScreen(
    billItems: List<BillLineItem>,
    shopProfile: ShopProfile,
    onAddItem: (itemName: String, pricePerKg: BigDecimal, weight: BigDecimal, unit: WeightUnit) -> Unit,
    onRemoveItem: (Long) -> Unit,
    onClearBill: () -> Unit,
    onSaveBillToHistory: (kgLabel: String, gramLabel: String, onSaved: (CalculationEntity) -> Unit) -> Unit,
    onOpenReceipt: (Long) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var itemName by remember { mutableStateOf("") }
    var priceInput by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(WeightUnit.KG) }
    var priceError by remember { mutableStateOf(false) }
    var weightError by remember { mutableStateOf(false) }

    val totals = remember(billItems) {
        CalculatorEngine.calculateCombinedBillTotals(billItems)
    }

    Scaffold(
        topBar = {
            NarendraTopBar(
                title = strings.combinedBillTitle,
                onBack = onBack,
                actions = {
                    if (billItems.isNotEmpty()) {
                        TextButton(
                            onClick = onClearBill,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("bill_clear_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = strings.clearBill,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.clearBill, color = MaterialTheme.colorScheme.error)
                        }
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Batch Entry Card (for weighing Loha 1kg + 2kg + 500g etc.)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = itemName,
                                onValueChange = { itemName = it },
                                label = { Text(strings.itemNameOptional) },
                                placeholder = { Text(strings.itemNameHint) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bill_item_name_input")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = priceInput,
                                    onValueChange = { raw ->
                                        priceInput = filterDecimal(raw)
                                        priceError = false
                                    },
                                    label = { Text(strings.productPricePerKg) },
                                    prefix = { Text("₹ ") },
                                    isError = priceError,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bill_price_input")
                                )
                                OutlinedTextField(
                                    value = weightInput,
                                    onValueChange = { raw ->
                                        weightInput = filterDecimal(raw)
                                        weightError = false
                                    },
                                    label = { Text(strings.enterWeight) },
                                    isError = weightError,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("bill_weight_input")
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = selectedUnit == WeightUnit.KG,
                                    onClick = { selectedUnit = WeightUnit.KG },
                                    label = { Text(strings.kgUnit) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedUnit == WeightUnit.GRAM,
                                    onClick = { selectedUnit = WeightUnit.GRAM },
                                    label = { Text(strings.gramUnit) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    Triple("250g", "250", WeightUnit.GRAM),
                                    Triple("500g", "500", WeightUnit.GRAM),
                                    Triple("1 KG", "1", WeightUnit.KG),
                                    Triple("2 KG", "2", WeightUnit.KG),
                                    Triple("5 KG", "5", WeightUnit.KG)
                                ).forEach { (label, valStr, unit) ->
                                    AssistChip(
                                        onClick = {
                                            weightInput = valStr
                                            selectedUnit = unit
                                            weightError = false
                                        },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val price = CalculatorEngine.parsePositiveDecimal(priceInput)
                                    val weight = CalculatorEngine.parsePositiveDecimal(weightInput)
                                    if (price == null || weight == null) {
                                        priceError = price == null
                                        weightError = weight == null
                                        return@Button
                                    }
                                    onAddItem(itemName, price, weight, selectedUnit)
                                    // Keep price & itemName so next weight (e.g. 2kg after 1kg) is 1 tap!
                                    weightInput = ""
                                    scope.launch {
                                        snackbarHostState.showSnackbar(strings.addedToBill)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("bill_add_item_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.addItemToBillBtn)
                            }
                        }
                    }
                }

                if (billItems.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PostAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = strings.emptyBillTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = strings.emptyBillSubtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(billItems, key = { _, item -> item.id }) { index, lineItem ->
                        val weightHuman = CalculatorEngine.formatGramsHuman(
                            lineItem.weightInGrams,
                            strings.kgUnit,
                            strings.gramUnit
                        )
                        val rateText = "${CalculatorEngine.formatAmount(lineItem.pricePerKg)} / KG"
                        val amountText = CalculatorEngine.formatAmount(lineItem.amount)

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "#${index + 1}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    if (lineItem.itemName.isNotBlank()) {
                                        Text(
                                            text = lineItem.itemName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "$weightHuman × $rateText",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = amountText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(
                                    onClick = { onRemoveItem(lineItem.id) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = strings.deleteEntry,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Combined Grand Total Summary & Direct WhatsApp Share
                    item {
                        val totalWeightText = CalculatorEngine.formatGramsHuman(
                            totals.totalWeightInGrams,
                            strings.kgUnit,
                            strings.gramUnit
                        )
                        val totalKgText = "${CalculatorEngine.formatPlainNumber(totals.totalWeightInKg, 3)} KG"
                        val grandTotalText = CalculatorEngine.formatAmount(totals.totalAmount)

                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = strings.grandTotalLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = grandTotalText,
                                    style = MaterialTheme.typography.displayLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("bill_grand_total_amount")
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                )
                                ResultDetailRow(
                                    label = strings.billItemsCount,
                                    value = "${billItems.size}"
                                )
                                ResultDetailRow(
                                    label = strings.totalWeightLabel,
                                    value = "$totalWeightText ($totalKgText)",
                                    highlightValue = true
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                WhatsAppActionButton(
                                    text = strings.saveAndShareWhatsApp,
                                    onClick = {
                                        onSaveBillToHistory(strings.kgUnit, strings.gramUnit) { saved ->
                                            val receiptText = ReceiptFormatter.buildWhatsAppTextReceipt(
                                                saved, shopProfile, strings
                                            )
                                            ReceiptFormatter.shareDirectlyToWhatsApp(
                                                context, receiptText, strings
                                            )
                                        }
                                    },
                                    testTag = "bill_whatsapp_share_button",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                )

                                OutlinedButton(
                                    onClick = {
                                        onSaveBillToHistory(strings.kgUnit, strings.gramUnit) { saved ->
                                            onOpenReceipt(saved.id)
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("bill_view_receipt_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
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

private fun filterDecimal(raw: String): String {
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
