package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ShopProfile
import com.example.localization.LocalStrings
import com.example.ui.components.NarendraTopBar
import com.example.ui.components.ShopLogoAvatar
import kotlinx.coroutines.launch

@Composable
fun ShopProfileScreen(
    shopProfile: ShopProfile,
    onSaveProfile: (ShopProfile) -> Unit,
    onPickLogoUri: (android.net.Uri) -> Unit,
    onRemoveLogo: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var shopName by remember { mutableStateOf(shopProfile.shopName) }
    var ownerName by remember { mutableStateOf(shopProfile.ownerName) }
    var mobileNumber by remember { mutableStateOf(shopProfile.mobileNumber) }
    var address by remember { mutableStateOf(shopProfile.address) }

    LaunchedEffect(shopProfile) {
        if (shopName.isEmpty() && shopProfile.shopName.isNotEmpty()) shopName = shopProfile.shopName
        if (ownerName.isEmpty() && shopProfile.ownerName.isNotEmpty()) ownerName = shopProfile.ownerName
        if (mobileNumber.isEmpty() && shopProfile.mobileNumber.isNotEmpty()) mobileNumber = shopProfile.mobileNumber
        if (address.isEmpty() && shopProfile.address.isNotEmpty()) address = shopProfile.address
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickLogoUri(uri)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            NarendraTopBar(
                title = strings.shopProfileTitle,
                onBack = onBack
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
                // Logo Picker Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ShopLogoAvatar(
                            logoPath = shopProfile.logoPath,
                            shopName = shopName.ifBlank { shopProfile.effectiveShopName },
                            size = 92.dp
                        )
                        Text(
                            text = strings.shopLogoLabel,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("shop_upload_logo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (shopProfile.logoPath.isNotBlank()) strings.changeLogo else strings.uploadLogo
                                )
                            }

                            if (shopProfile.logoPath.isNotBlank()) {
                                TextButton(
                                    onClick = onRemoveLogo,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("shop_remove_logo_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(strings.removeLogo, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                // Form Fields Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text(strings.shopNameLabel) },
                            placeholder = { Text(strings.shopNameHint) },
                            leadingIcon = {
                                Icon(Icons.Filled.Storefront, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_name_input")
                        )

                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text(strings.ownerNameLabel) },
                            placeholder = { Text(strings.ownerNameHint) },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("owner_name_input")
                        )

                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = { mobileNumber = it.filter { ch -> ch.isDigit() || ch == '+' || ch == ' ' } },
                            label = { Text(strings.mobileNumberLabel) },
                            placeholder = { Text(strings.mobileNumberHint) },
                            leadingIcon = {
                                Icon(Icons.Filled.Phone, contentDescription = null)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_mobile_input")
                        )

                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text(strings.shopAddressLabel) },
                            placeholder = { Text(strings.shopAddressHint) },
                            leadingIcon = {
                                Icon(Icons.Filled.LocationOn, contentDescription = null)
                            },
                            minLines = 2,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_address_input")
                        )

                        Button(
                            onClick = {
                                onSaveProfile(
                                    ShopProfile(
                                        shopName = shopName.trim(),
                                        ownerName = ownerName.trim(),
                                        mobileNumber = mobileNumber.trim(),
                                        address = address.trim(),
                                        logoPath = shopProfile.logoPath
                                    )
                                )
                                scope.launch {
                                    snackbarHostState.showSnackbar(strings.shopProfileSaved)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("save_shop_profile_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.saveShopProfile,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }

                // Branding Note Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = strings.shopBrandingNote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}
