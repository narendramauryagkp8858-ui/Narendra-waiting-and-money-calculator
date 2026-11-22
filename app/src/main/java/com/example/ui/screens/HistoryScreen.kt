package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.CalculationEntity
import com.example.data.local.ShopProfile
import com.example.domain.CalculationType
import com.example.localization.LocalStrings
import com.example.receipt.ReceiptFormatter
import com.example.ui.components.ConfirmClearHistoryDialog
import com.example.ui.components.NarendraTopBar
import com.example.ui.components.ResultDetailRow
import com.example.ui.theme.WhatsAppGreen

@Composable
fun HistoryScreen(
    historyItems: List<CalculationEntity>,
    shopProfile: ShopProfile,
    onDeleteItem: (Long) -> Unit,
    onClearAll: () -> Unit,
    onOpenReceipt: (Long) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf<CalculationType?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    val filteredItems = remember(historyItems, selectedFilter) {
        if (selectedFilter == null) {
            historyItems
        } else {
            historyItems.filter { it.calculationType == selectedFilter?.key }
        }
    }

    Scaffold(
        topBar = {
            NarendraTopBar(
                title = strings.historyTitle,
                onBack = onBack,
                actions = {
                    if (historyItems.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("history_clear_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = strings.clearAllHistory,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = strings.confirmDelete,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
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
                    .widthIn(max = 680.dp)
            ) {
                // Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("${strings.allFilter} (${historyItems.size})") },
                        modifier = Modifier.testTag("history_filter_all")
                    )
                    FilterChip(
                        selected = selectedFilter == CalculationType.WEIGHT_TO_MONEY,
                        onClick = { selectedFilter = CalculationType.WEIGHT_TO_MONEY },
                        label = { Text(strings.weightToMoneyTitle) },
                        modifier = Modifier.testTag("history_filter_w2m")
                    )
                    FilterChip(
                        selected = selectedFilter == CalculationType.MONEY_TO_WEIGHT,
                        onClick = { selectedFilter = CalculationType.MONEY_TO_WEIGHT },
                        label = { Text(strings.moneyToWeightTitle) },
                        modifier = Modifier.testTag("history_filter_m2w")
                    )
                    FilterChip(
                        selected = selectedFilter == CalculationType.COMBINED_BILL,
                        onClick = { selectedFilter = CalculationType.COMBINED_BILL },
                        label = { Text(strings.combinedBillTitle) },
                        modifier = Modifier.testTag("history_filter_bill")
                    )
                    FilterChip(
                        selected = selectedFilter == CalculationType.NORMAL,
                        onClick = { selectedFilter = CalculationType.NORMAL },
                        label = { Text(strings.normalCalculatorTitle) },
                        modifier = Modifier.testTag("history_filter_normal")
                    )
                }

                if (filteredItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(24.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(76.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(38.dp)
                                    )
                                }
                            }
                            Text(
                                text = strings.emptyHistoryTitle,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = strings.emptyHistorySubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { item ->
                            HistoryItemCard(
                                item = item,
                                onDelete = { onDeleteItem(item.id) },
                                onViewReceipt = { onOpenReceipt(item.id) },
                                onShareWhatsApp = {
                                    val text = ReceiptFormatter.buildWhatsAppTextReceipt(
                                        item, shopProfile, strings
                                    )
                                    ReceiptFormatter.shareDirectlyToWhatsApp(
                                        context, text, strings
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        ConfirmClearHistoryDialog(
            onConfirm = {
                onClearAll()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }
}

@Composable
private fun HistoryItemCard(
    item: CalculationEntity,
    onDelete: () -> Unit,
    onViewReceipt: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    val strings = LocalStrings.current
    val calcType = CalculationType.fromKey(item.calculationType)
    val badgeColor = when (calcType) {
        CalculationType.WEIGHT_TO_MONEY -> MaterialTheme.colorScheme.primaryContainer
        CalculationType.MONEY_TO_WEIGHT -> MaterialTheme.colorScheme.secondaryContainer
        CalculationType.COMBINED_BILL -> MaterialTheme.colorScheme.primaryContainer
        CalculationType.NORMAL -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val badgeTextColor = when (calcType) {
        CalculationType.WEIGHT_TO_MONEY -> MaterialTheme.colorScheme.onPrimaryContainer
        CalculationType.MONEY_TO_WEIGHT -> MaterialTheme.colorScheme.onSecondaryContainer
        CalculationType.COMBINED_BILL -> MaterialTheme.colorScheme.onPrimaryContainer
        CalculationType.NORMAL -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    Card(
        onClick = onViewReceipt,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_card_${item.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = ReceiptFormatter.calculationTypeLabel(item.calculationType, strings),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeTextColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ReceiptFormatter.formatTimestamp(item.timestamp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("history_delete_${item.id}")
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

            if (item.itemName.isNotBlank()) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (item.ratePerKg.isNotBlank()) {
                ResultDetailRow(label = strings.ratePerKgLabel, value = item.ratePerKg)
            }
            if (item.weightDisplay.isNotBlank()) {
                ResultDetailRow(label = strings.weightLabel, value = item.weightDisplay)
            }
            if (item.amountDisplay.isNotBlank()) {
                ResultDetailRow(label = strings.amountLabel, value = item.amountDisplay)
            }
            if (item.detailsSummary.isNotBlank() && (calcType == CalculationType.NORMAL || calcType == CalculationType.COMBINED_BILL)) {
                Text(
                    text = item.detailsSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.finalResultLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.finalResult,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onViewReceipt,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.viewReceipt)
                    }

                    IconButton(
                        onClick = onShareWhatsApp,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = strings.shareWhatsApp,
                            tint = WhatsAppGreen
                        )
                    }
                }
            }
        }
    }
}
