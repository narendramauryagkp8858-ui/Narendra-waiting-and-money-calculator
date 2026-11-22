package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.AppLanguage
import com.example.data.local.CalculationEntity
import com.example.data.local.ShopProfile
import com.example.data.local.ThemeMode
import com.example.localization.LocalStrings
import com.example.ui.components.ShopLogoAvatar
import com.example.ui.theme.WhatsAppGreen

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
    onNavigateCombinedBill: () -> Unit,
    onNavigateNormalCalculator: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateShopProfile: () -> Unit,
    onOpenReceipt: (Long) -> Unit,
    onNavigateSettings: () -> Unit
) {
    val strings = LocalStrings.current
    var showNoReceiptDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
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
                    .widthIn(max = 720.dp)
            ) {
                val isWide = maxWidth >= 560.dp

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header Brand Bar
                    item {
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
                                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                    }

                    // Shop Hero Banner Card
                    item {
                        Card(
                            onClick = onNavigateShopProfile,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_shop_banner"),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_merchant_banner),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.16f,
                                    modifier = Modifier
                                        .matchParentSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
                                                )
                                            )
                                        )
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ShopLogoAvatar(
                                        logoPath = shopProfile.logoPath,
                                        shopName = shopProfile.effectiveShopName,
                                        size = 56.dp
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
                                        Spacer(modifier = Modifier.height(3.dp))
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
                                            color = Color.White.copy(alpha = 0.88f),
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
                    }

                    // Primary 2 Calculators (Highlighted Row)
                    item {
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PrimaryFeatureCard(
                                    title = strings.weightToMoneyTitle,
                                    subtitle = strings.weightToMoneyDesc,
                                    icon = Icons.Filled.Scale,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    onClick = onNavigateWeightToMoney,
                                    testTag = "menu_weight_to_money",
                                    modifier = Modifier.weight(1f)
                                )
                                PrimaryFeatureCard(
                                    title = strings.moneyToWeightTitle,
                                    subtitle = strings.moneyToWeightDesc,
                                    icon = Icons.Filled.CurrencyRupee,
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    onClick = onNavigateMoneyToWeight,
                                    testTag = "menu_money_to_weight",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                PrimaryFeatureCard(
                                    title = strings.weightToMoneyTitle,
                                    subtitle = strings.weightToMoneyDesc,
                                    icon = Icons.Filled.Scale,
                                    accentColor = MaterialTheme.colorScheme.primary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    onClick = onNavigateWeightToMoney,
                                    testTag = "menu_weight_to_money",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                PrimaryFeatureCard(
                                    title = strings.moneyToWeightTitle,
                                    subtitle = strings.moneyToWeightDesc,
                                    icon = Icons.Filled.CurrencyRupee,
                                    accentColor = MaterialTheme.colorScheme.secondary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    onClick = onNavigateMoneyToWeight,
                                    testTag = "menu_money_to_weight",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Multi-Weight Combined Bill Card (for Loha / multiple weights in 1 bill)
                    item {
                        PrimaryFeatureCard(
                            title = if (billCount > 0) "${strings.combinedBillTitle} ($billCount)" else strings.combinedBillTitle,
                            subtitle = strings.combinedBillDesc,
                            icon = Icons.Filled.AddShoppingCart,
                            accentColor = MaterialTheme.colorScheme.tertiary,
                            containerColor = MaterialTheme.colorScheme.surface,
                            onClick = onNavigateCombinedBill,
                            testTag = "menu_combined_bill",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Secondary Grid: Normal Calculator & History
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CompactGridCard(
                                title = strings.normalCalculatorTitle,
                                subtitle = strings.normalCalculatorDesc,
                                icon = Icons.Filled.Calculate,
                                iconTint = MaterialTheme.colorScheme.tertiary,
                                badgeText = null,
                                onClick = onNavigateNormalCalculator,
                                testTag = "menu_normal_calculator",
                                modifier = Modifier.weight(1f)
                            )
                            CompactGridCard(
                                title = strings.historyTitle,
                                subtitle = strings.historyDesc,
                                icon = Icons.Filled.History,
                                iconTint = MaterialTheme.colorScheme.primary,
                                badgeText = if (historyCount > 0) historyCount.toString() else null,
                                onClick = onNavigateHistory,
                                testTag = "menu_history",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Shop Profile & WhatsApp Share
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CompactGridCard(
                                title = strings.shopProfileTitle,
                                subtitle = strings.shopProfileDesc,
                                icon = Icons.Filled.Storefront,
                                iconTint = MaterialTheme.colorScheme.secondary,
                                badgeText = null,
                                onClick = onNavigateShopProfile,
                                testTag = "menu_shop_profile",
                                modifier = Modifier.weight(1f)
                            )
                            CompactGridCard(
                                title = strings.whatsappShareTitle,
                                subtitle = strings.whatsappShareDesc,
                                icon = Icons.Filled.Share,
                                iconTint = WhatsAppGreen,
                                badgeText = null,
                                onClick = {
                                    if (latestCalculation != null) {
                                        onOpenReceipt(latestCalculation.id)
                                    } else {
                                        showNoReceiptDialog = true
                                    }
                                },
                                testTag = "menu_whatsapp_share",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Settings Full-Width Card
                    item {
                        Card(
                            onClick = onNavigateSettings,
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .testTag("menu_settings"),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.settingsTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = strings.settingsDesc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun PrimaryFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = accentColor
            )
        }
    }
}

@Composable
private fun CompactGridCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    badgeText: String?,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .height(146.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
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
                        .background(iconTint.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                if (badgeText != null) {
                    Badge(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Text(
                            text = badgeText,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
