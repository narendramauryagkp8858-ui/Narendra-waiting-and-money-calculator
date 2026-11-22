package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ShopProfile
import com.example.localization.LocalStrings
import com.example.receipt.ReceiptFormatter
import com.example.ui.components.NarendraTopBar
import com.example.ui.components.WhatsAppActionButton
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.NormalCalculatorViewModel

@Composable
fun NormalCalculatorScreen(
    viewModel: NormalCalculatorViewModel,
    shopProfile: ShopProfile,
    onBack: () -> Unit,
    onOpenReceipt: (Long) -> Unit
) {
    val strings = LocalStrings.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val displayScrollState = rememberScrollState()

    LaunchedEffect(state.primaryDisplay) {
        displayScrollState.animateScrollTo(displayScrollState.maxValue)
    }

    Scaffold(
        topBar = {
            NarendraTopBar(
                title = strings.normalCalculatorTitle,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Calculator Display Panel
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(displayScrollState),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.End
                    ) {
                        // Past evaluated expression header after pressing =
                        AnimatedVisibility(visible = state.evaluatedHeader.isNotEmpty()) {
                            Text(
                                text = state.evaluatedHeader,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .testTag("calc_expression_display")
                            )
                        }

                        // Full running expression OR final result after pressing =
                        val mainText = if (state.isError) strings.cannotDivideByZero else state.primaryDisplay
                        val mainFontSize = when {
                            state.isError -> 22.sp
                            mainText.length > 22 -> 24.sp
                            mainText.length > 13 -> 30.sp
                            else -> 40.sp
                        }
                        val mainLineHeight = when {
                            state.isError -> 28.sp
                            mainText.length > 22 -> 32.sp
                            mainText.length > 13 -> 38.sp
                            else -> 48.sp
                        }

                        Text(
                            text = mainText,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = mainFontSize,
                                lineHeight = mainLineHeight
                            ),
                            color = when {
                                state.isError -> MaterialTheme.colorScheme.error
                                state.justEvaluated -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("calc_main_display")
                        )

                        // Subtle Live Preview Result WITHOUT pressing '='
                        AnimatedVisibility(visible = state.livePreview.isNotEmpty() && !state.justEvaluated && !state.isError) {
                            Text(
                                text = "= ${state.livePreview}",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontFamily = JetBrainsMonoFamily,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .testTag("calc_live_preview")
                            )
                        }
                    }
                }

                // Direct WhatsApp Share & Receipt Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WhatsAppActionButton(
                        text = strings.shareWhatsApp,
                        onClick = {
                            val existing = state.lastSavedRecord
                            if (existing != null && state.justEvaluated) {
                                val text = ReceiptFormatter.buildWhatsAppTextReceipt(
                                    existing, shopProfile, strings
                                )
                                ReceiptFormatter.shareDirectlyToWhatsApp(context, text, strings)
                            } else {
                                viewModel.onEquals { saved ->
                                    val text = ReceiptFormatter.buildWhatsAppTextReceipt(
                                        saved, shopProfile, strings
                                    )
                                    ReceiptFormatter.shareDirectlyToWhatsApp(context, text, strings)
                                }
                            }
                        },
                        testTag = "calc_whatsapp_share_button",
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedButton(
                        onClick = {
                            val existing = state.lastSavedRecord
                            if (existing != null && state.justEvaluated) {
                                onOpenReceipt(existing.id)
                            } else {
                                viewModel.onEquals { saved ->
                                    onOpenReceipt(saved.id)
                                }
                            }
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("calc_receipt_button"),
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

                // Keypad Grid
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CalcKeyButton(
                            label = "AC",
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            onClick = viewModel::onClearAll,
                            testTag = "calc_key_ac",
                            modifier = Modifier.weight(1f)
                        )
                        ElevatedButton(
                            onClick = viewModel::onBackspace,
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .testTag("calc_key_backspace")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete"
                            )
                        }
                        CalcKeyButton(
                            label = "%",
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = viewModel::onPercent,
                            testTag = "calc_key_percent",
                            modifier = Modifier.weight(1f)
                        )
                        CalcKeyButton(
                            label = "÷",
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = { viewModel.onOperator("÷") },
                            testTag = "calc_key_divide",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    CalcNumberOperatorRow(
                        nums = listOf("7", "8", "9"),
                        op = "×",
                        opTestTag = "calc_key_multiply",
                        onDigit = viewModel::onDigit,
                        onOperator = viewModel::onOperator
                    )

                    CalcNumberOperatorRow(
                        nums = listOf("4", "5", "6"),
                        op = "-",
                        opTestTag = "calc_key_minus",
                        onDigit = viewModel::onDigit,
                        onOperator = viewModel::onOperator
                    )

                    CalcNumberOperatorRow(
                        nums = listOf("1", "2", "3"),
                        op = "+",
                        opTestTag = "calc_key_plus",
                        onDigit = viewModel::onDigit,
                        onOperator = viewModel::onOperator
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CalcKeyButton(
                            label = "00",
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = viewModel::onDoubleZero,
                            testTag = "calc_key_00",
                            modifier = Modifier.weight(1f)
                        )
                        CalcKeyButton(
                            label = "0",
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = { viewModel.onDigit("0") },
                            testTag = "calc_key_0",
                            modifier = Modifier.weight(1f)
                        )
                        CalcKeyButton(
                            label = ".",
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            onClick = viewModel::onDecimal,
                            testTag = "calc_key_dot",
                            modifier = Modifier.weight(1f)
                        )
                        CalcKeyButton(
                            label = "=",
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            onClick = { viewModel.onEquals() },
                            testTag = "calc_key_equals",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcNumberOperatorRow(
    nums: List<String>,
    op: String,
    opTestTag: String,
    onDigit: (String) -> Unit,
    onOperator: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        nums.forEach { d ->
            CalcKeyButton(
                label = d,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = { onDigit(d) },
                testTag = "calc_key_$d",
                modifier = Modifier.weight(1f)
            )
        }
        CalcKeyButton(
            label = op,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = { onOperator(op) },
            testTag = opTestTag,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CalcKeyButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    ElevatedButton(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = 1.dp),
        modifier = modifier
            .height(64.dp)
            .testTag(testTag)
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
