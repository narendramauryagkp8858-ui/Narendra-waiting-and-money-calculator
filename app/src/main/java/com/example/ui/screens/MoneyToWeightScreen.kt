package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ShopProfile
import com.example.domain.CalculatorEngine
import com.example.localization.LocalStrings
import com.example.receipt.ReceiptFormatter
import com.example.ui.components.NarendraTopBar
import com.example.ui.components.ResultDetailRow
import com.example.ui.components.WhatsAppActionButton
import com.example.ui.viewmodel.MoneyToWeightViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoneyToWeightScreen(
    viewModel: MoneyToWeightViewModel,
    shopProfile: ShopProfile,
    onBack: () -> Unit,
    onOpenReceipt: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            NarendraTopBar(
                title = strings.moneyToWeightTitle,
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = { viewModel.reset() },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("money_to_weight_reset_button")
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
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = state.pricePerKgInput,
                            onValueChange = viewModel::updatePrice,
                            label = { Text(strings.productPricePerKg) },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                            suffix = {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = strings.priceUnitPerKg,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
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
                                .testTag("m2w_price_input")
                        )

                        OutlinedTextField(
                            value = state.amountInput,
                            onValueChange = viewModel::updateAmount,
                            label = { Text(strings.enterAmountRs) },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold) },
                            isError = state.amountError,
                            supportingText = if (state.amountError) {
                                { Text(strings.invalidAmountError) }
                            } else null,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("m2w_amount_input")
                        )

                        Text(
                            text = strings.quickAmounts,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("10", "20", "50", "100", "200", "500").forEach { amt ->
                                AssistChip(
                                    onClick = { viewModel.applyQuickAmount(amt) },
                                    label = { Text("₹$amt") }
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
                                .testTag("m2w_item_name_input")
                        )

                        Button(
                            onClick = {
                                viewModel.calculateAndSave {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(strings.savedToHistory)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("m2w_calculate_save_button"),
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

                AnimatedVisibility(visible = state.liveResult != null) {
                    state.liveResult?.let { result ->
                        val humanWeight = CalculatorEngine.formatWeightHuman(
                            result = result,
                            kgLabel = strings.kgUnit,
                            gramLabel = strings.gramUnit
                        )
                        val gramsText = "${CalculatorEngine.formatPlainNumber(result.weightInGrams, 2)} g"
                        val kgText = "${CalculatorEngine.formatPlainNumber(result.weightInKg, 3)} KG"
                        val rateText = "${CalculatorEngine.formatAmount(result.pricePerKg)} / KG"
                        val customerAmountText = CalculatorEngine.formatAmount(result.customerAmount)

                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                            ),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
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
                                        text = strings.weightReceivedLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    if (state.savedRecord != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = strings.savedToHistory,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = humanWeight,
                                    style = MaterialTheme.typography.displayMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.testTag("m2w_result_weight")
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                )

                                ResultDetailRow(
                                    label = strings.ratePerKgLabel,
                                    value = rateText
                                )
                                ResultDetailRow(
                                    label = strings.customerAmountLabel,
                                    value = customerAmountText
                                )
                                ResultDetailRow(
                                    label = strings.totalWeightLabel,
                                    value = "$gramsText ($kgText)",
                                    highlightValue = true
                                )

                                Spacer(modifier = Modifier.height(4.dp))

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
                                                ReceiptFormatter.shareText(
                                                    context, text, strings.shareChooserTitle
                                                )
                                            } else {
                                                viewModel.calculateAndSave { saved ->
                                                    val text = ReceiptFormatter.buildWhatsAppTextReceipt(
                                                        saved, shopProfile, strings
                                                    )
                                                    ReceiptFormatter.shareText(
                                                        context, text, strings.shareChooserTitle
                                                    )
                                                }
                                            }
                                        },
                                        testTag = "m2w_whatsapp_share_button",
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
                                            .testTag("m2w_view_receipt_button"),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ReceiptLong,
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
}
