package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "narendra_shop_prefs")

enum class ThemeMode(val key: String) {
    LIGHT("LIGHT"),
    DARK("DARK"),
    SYSTEM("SYSTEM");

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.find { it.key == key } ?: SYSTEM
    }
}

enum class AppLanguage(val key: String) {
    ENGLISH("EN"),
    HINDI("HI");

    companion object {
        fun fromKey(key: String?): AppLanguage =
            entries.find { it.key == key } ?: ENGLISH
    }
}

data class ShopProfile(
    val shopName: String = "",
    val ownerName: String = "",
    val mobileNumber: String = "",
    val address: String = "",
    val logoPath: String = ""
) {
    val hasCustomName: Boolean get() = shopName.isNotBlank()
    val effectiveShopName: String get() = shopName.trim().ifEmpty { "My Shop" }
}

data class UserPreferences(
    val shopProfile: ShopProfile = ShopProfile(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.ENGLISH
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val SHOP_NAME = stringPreferencesKey("shop_name")
        val OWNER_NAME = stringPreferencesKey("owner_name")
        val MOBILE_NUMBER = stringPreferencesKey("mobile_number")
        val ADDRESS = stringPreferencesKey("shop_address")
        val LOGO_PATH = stringPreferencesKey("shop_logo_path")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            shopProfile = ShopProfile(
                shopName = prefs[Keys.SHOP_NAME] ?: "",
                ownerName = prefs[Keys.OWNER_NAME] ?: "",
                mobileNumber = prefs[Keys.MOBILE_NUMBER] ?: "",
                address = prefs[Keys.ADDRESS] ?: "",
                logoPath = prefs[Keys.LOGO_PATH] ?: ""
            ),
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE]),
            appLanguage = AppLanguage.fromKey(prefs[Keys.APP_LANGUAGE])
        )
    }

    suspend fun saveShopProfile(profile: ShopProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOP_NAME] = profile.shopName.trim()
            prefs[Keys.OWNER_NAME] = profile.ownerName.trim()
            prefs[Keys.MOBILE_NUMBER] = profile.mobileNumber.trim()
            prefs[Keys.ADDRESS] = profile.address.trim()
            prefs[Keys.LOGO_PATH] = profile.logoPath
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.key
        }
    }

    suspend fun saveLanguage(language: AppLanguage) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = language.key
        }
    }

    suspend fun copyLogoToInternalStorage(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val destFile = File(context.filesDir, "shop_logo_${System.currentTimeMillis()}.jpg")

            val wroteCompressed = saveSampledLogo(uri, destFile, maxDimPx = 512)
            if (!wroteCompressed) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                context.dataStore.edit { prefs ->
                    prefs[Keys.LOGO_PATH] = destFile.absolutePath
                }
                // Remove older logos only after the new logo is safely persisted
                context.filesDir.listFiles()?.filter {
                    it.name.startsWith("shop_logo_") && it.name != destFile.name
                }?.forEach { it.delete() }
                destFile.absolutePath
            } else {
                if (destFile.exists()) destFile.delete()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun removeLogo() = withContext(Dispatchers.IO) {
        context.filesDir.listFiles()?.filter {
            it.name.startsWith("shop_logo_")
        }?.forEach { it.delete() }
        context.dataStore.edit { prefs ->
            prefs[Keys.LOGO_PATH] = ""
        }
    }

    private fun saveSampledLogo(uri: Uri, destFile: File, maxDimPx: Int): Boolean {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return false

            var sampleSize = 1
            while ((bounds.outWidth / sampleSize) > maxDimPx * 2 || (bounds.outHeight / sampleSize) > maxDimPx * 2) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return false

            val scale = minOf(
                1f,
                maxDimPx.toFloat() / decoded.width.coerceAtLeast(1),
                maxDimPx.toFloat() / decoded.height.coerceAtLeast(1)
            )
            val finalBitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(
                    decoded,
                    (decoded.width * scale).toInt().coerceAtLeast(1),
                    (decoded.height * scale).toInt().coerceAtLeast(1),
                    true
                ).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            try {
                destFile.outputStream().use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
            } finally {
                finalBitmap.recycle()
            }
            destFile.exists() && destFile.length() > 0
        } catch (_: Exception) {
            false
        }
    }
}
