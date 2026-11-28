package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.AppLanguage
import com.example.data.local.CalculationEntity
import com.example.data.local.ShopProfile
import com.example.data.local.ThemeMode
import com.example.localization.LocalStrings
import com.example.ui.components.ShopLogoAvatar
import com.example.ui.theme.WhatsAppGreen
import com.example.ui.theme.translucentCardBorderColor
import com.example.ui.theme.translucentCardSurfaceColor

private data class DashboardMenuOption(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val badgeText: String? = null,
    val testTag: String,
    val onClick: () -> Unit
)

@Composable
fun DashboardScreen(
    shopProfile: ShopProfile,
    historyCount: Int,
    billCount: Int,
    latestCalculation: CalculationEntity?,
    currentLanguage: AppLanguage,
    currentThemeMode: ThemeMode,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onNavigateWeightToMoney: () -> Unit,
    onNavigateMoneyToWeight: () -> Unit,
    onNavigatePieceToMoney: () -> Unit,
    onNavigateCombinedBill: () -> Unit,
    onNavigateNormalCalculator: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateShopProfile: () -> Unit,
    onOpenReceipt: (Long) -> Unit,
    onNavigateSettings: () -> Unit
) {
    val strings = LocalStrings.current
    var showNoReceiptDialog by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val neutralTint = MaterialTheme.colorScheme.onSurfaceVariant

    val latestCalcId = latestCalculation?.id
    val menuOptions = remember(
        strings,
        billCount,
        historyCount,
        latestCalcId,
        primaryColor,
        secondaryColor,
        tertiaryColor,
        neutralTint
    ) {
        listOf(
            DashboardMenuOption(
                title = strings.weightToMoneyTitle,
                subtitle = strings.weightToMoneyDesc,
                icon = Icons.Filled.Scale,
                accentColor = primaryColor,
                testTag = "menu_weight_to_money",
                onClick = onNavigateWeightToMoney
            ),
            DashboardMenuOption(
                title = strings.moneyToWeightTitle,
                subtitle = strings.moneyToWeightDesc,
                icon = Icons.Filled.CurrencyRupee,
                accentColor = secondaryColor,
                testTag = "menu_money_to_weight",
                onClick = onNavigateMoneyToWeight
            ),
            DashboardMenuOption(
                title = strings.pieceToMoneyTitle,
                subtitle = strings.pieceToMoneyDesc,
                icon = Icons.Filled.Inventory2,
                accentColor = primaryColor,
                testTag = "menu_piece_to_money",
                onClick = onNavigatePieceToMoney
            ),
            DashboardMenuOption(
                title = strings.combinedBillTitle,
                subtitle = strings.combinedBillDesc,
                icon = Icons.Filled.AddShoppingCart,
                accentColor = tertiaryColor,
                badgeText = if (billCount > 0) billCount.toString() else null,
                testTag = "menu_combined_bill",
                onClick = onNavigateCombinedBill
            ),
            DashboardMenuOption(
                title = strings.normalCalculatorTitle,
                subtitle = strings.normalCalculatorDesc,
                icon = Icons.Filled.Calculate,
                accentColor = tertiaryColor,
                testTag = "menu_normal_calculator",
                onClick = onNavigateNormalCalculator
            ),
            DashboardMenuOption(
                title = strings.historyTitle,
                subtitle = strings.historyDesc,
                icon = Icons.Filled.History,
                accentColor = primaryColor,
                badgeText = if (historyCount > 0) historyCount.toString() else null,
                testTag = "menu_history",
                onClick = onNavigateHistory
            ),
            DashboardMenuOption(
                title = strings.shopProfileTitle,
                subtitle = strings.shopProfileDesc,
                icon = Icons.Filled.Storefront,
                accentColor = secondaryColor,
                testTag = "menu_shop_profile",
                onClick = onNavigateShopProfile
            ),
            DashboardMenuOption(
                title = strings.whatsappShareTitle,
                subtitle = strings.whatsappShareDesc,
                icon = Icons.Filled.Share,
                accentColor = WhatsAppGreen,
                testTag = "menu_whatsapp_share",
                onClick = {
                    if (latestCalcId != null) {
                        onOpenReceipt(latestCalcId)
                    } else {
                        showNoReceiptDialog = true
                    }
                }
            ),
            DashboardMenuOption(
                title = strings.settingsTitle,
                subtitle = strings.settingsDesc,
                icon = Icons.Filled.Settings,
                accentColor = neutralTint,
                testTag = "menu_settings",
                onClick = onNavigateSettings
            )
        )
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
            ) {
                val columns = if (maxWidth >= 560.dp) 3 else 2
                val rows = remember(menuOptions, columns) { menuOptions.chunked(columns) }
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 28.dp
                        )
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Brand Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Narendra Weight & Money Calculator",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.developerTagline,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                onClick = onToggleLanguage,
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dashboard_language_toggle")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Translate,
                                        contentDescription = strings.languageLabel,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = if (currentLanguage == AppLanguage.ENGLISH) "हिं" else "EN",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = onToggleTheme,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dashboard_theme_toggle")
                            ) {
                                Icon(
                                    imageVector = if (currentThemeMode == ThemeMode.DARK) {
                                        Icons.Filled.LightMode
                                    } else {
                                        Icons.Filled.DarkMode
                                    },
                                    contentDescription = strings.appThemeLabel,
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }

                    // Shop Hero Banner Card (translucent merchant card)
                    Card(
                        onClick = onNavigateShopProfile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dashboard_shop_banner"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.84f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
                                        )
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ShopLogoAvatar(
                                    logoPath = shopProfile.logoPath,
                                    shopName = shopProfile.effectiveShopName,
                                    size = 52.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (shopProfile.hasCustomName) {
                                            shopProfile.shopName
                                        } else {
                                            strings.setupShopPrompt
                                        },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (shopProfile.hasCustomName) {
                                            listOfNotNull(
                                                shopProfile.ownerName.takeIf { it.isNotBlank() },
                                                shopProfile.mobileNumber.takeIf { it.isNotBlank() }
                                            ).joinToString(" • ").ifBlank { strings.tapToEditShop }
                                        } else {
                                            strings.tapToEditShop
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.90f),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Uniform Translucent Grid of All App Options
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { option ->
                                DashboardGridTile(
                                    option = option,
                                    isWideSingleTile = columns == 2 && rowItems.size == 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNoReceiptDialog) {
        AlertDialog(
            onDismissRequest = { showNoReceiptDialog = false },
            title = { Text(strings.noCalculationForShareTitle) },
            text = { Text(strings.noCalculationForShareMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        showNoReceiptDialog = false
                        onNavigateWeightToMoney()
                    }
                ) {
                    Text(strings.goToWeightToMoney)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showNoReceiptDialog = false
                        onNavigateMoneyToWeight()
                    }
                ) {
                    Text(strings.goToMoneyToWeight)
                }
            }
        )
    }
}

@Composable
private fun DashboardGridTile(
    option: DashboardMenuOption,
    isWideSingleTile: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBg = translucentCardSurfaceColor(lightAlpha = 0.56f, darkAlpha = 0.52f)
    val borderColor = if (option.testTag == "menu_settings") {
        translucentCardBorderColor()
    } else {
        option.accentColor.copy(alpha = 0.34f)
    }

    Card(
        onClick = option.onClick,
        modifier = modifier
            .height(if (isWideSingleTile) 86.dp else 142.dp)
            .testTag(option.testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        if (isWideSingleTile) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(option.accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = option.icon,
                        contentDescription = null,
                        tint = option.accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = option.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = option.accentColor
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(option.accentColor.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = option.accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (option.badgeText != null) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Text(
                                text = option.badgeText,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Column {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = option.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
