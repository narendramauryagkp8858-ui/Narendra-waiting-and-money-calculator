package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppLanguage
import com.example.data.local.ThemeMode
import com.example.localization.LocalStrings
import com.example.localization.stringsFor
import com.example.ui.navigation.Routes
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MoneyToWeightScreen
import com.example.ui.screens.NormalCalculatorScreen
import com.example.ui.screens.ReceiptScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopProfileScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WeightToMoneyScreen
import com.example.ui.theme.NarendraCalculatorTheme
import com.example.ui.viewmodel.CalculatorViewModelFactory
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MoneyToWeightViewModel
import com.example.ui.viewmodel.NormalCalculatorViewModel
import com.example.ui.viewmodel.WeightToMoneyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val prefs by mainViewModel.userPreferences.collectAsStateWithLifecycle()
            val history by mainViewModel.calculationHistory.collectAsStateWithLifecycle()
            val latestCalc by mainViewModel.latestCalculation.collectAsStateWithLifecycle()

            val calcFactory = CalculatorViewModelFactory(mainViewModel.calculationRepository)
            val strings = stringsFor(prefs.appLanguage)

            NarendraCalculatorTheme(themeMode = prefs.themeMode) {
                CompositionLocalProvider(LocalStrings provides strings) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Routes.SPLASH
                    ) {
                        composable(Routes.SPLASH) {
                            SplashScreen(
                                onFinished = {
                                    navController.navigate(Routes.DASHBOARD) {
                                        popUpTo(Routes.SPLASH) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }

                        composable(Routes.DASHBOARD) {
                            DashboardScreen(
                                shopProfile = prefs.shopProfile,
                                historyCount = history.size,
                                latestCalculation = latestCalc,
                                currentLanguage = prefs.appLanguage,
                                currentThemeMode = prefs.themeMode,
                                onToggleLanguage = {
                                    val next = if (prefs.appLanguage == AppLanguage.ENGLISH) {
                                        AppLanguage.HINDI
                                    } else {
                                        AppLanguage.ENGLISH
                                    }
                                    mainViewModel.setLanguage(next)
                                },
                                onToggleTheme = {
                                    val next = if (prefs.themeMode == ThemeMode.DARK) {
                                        ThemeMode.LIGHT
                                    } else {
                                        ThemeMode.DARK
                                    }
                                    mainViewModel.setThemeMode(next)
                                },
                                onNavigateWeightToMoney = {
                                    navController.navigate(Routes.WEIGHT_TO_MONEY)
                                },
                                onNavigateMoneyToWeight = {
                                    navController.navigate(Routes.MONEY_TO_WEIGHT)
                                },
                                onNavigateNormalCalculator = {
                                    navController.navigate(Routes.NORMAL_CALCULATOR)
                                },
                                onNavigateHistory = {
                                    navController.navigate(Routes.HISTORY)
                                },
                                onNavigateShopProfile = {
                                    navController.navigate(Routes.SHOP_PROFILE)
                                },
                                onOpenReceipt = { calcId ->
                                    navController.navigate(Routes.receiptRoute(calcId))
                                },
                                onNavigateSettings = {
                                    navController.navigate(Routes.SETTINGS)
                                }
                            )
                        }

                        composable(Routes.WEIGHT_TO_MONEY) {
                            val w2mViewModel: WeightToMoneyViewModel = viewModel(factory = calcFactory)
                            WeightToMoneyScreen(
                                viewModel = w2mViewModel,
                                shopProfile = prefs.shopProfile,
                                onBack = { navController.popBackStack() },
                                onOpenReceipt = { calcId ->
                                    navController.navigate(Routes.receiptRoute(calcId))
                                }
                            )
                        }

                        composable(Routes.MONEY_TO_WEIGHT) {
                            val m2wViewModel: MoneyToWeightViewModel = viewModel(factory = calcFactory)
                            MoneyToWeightScreen(
                                viewModel = m2wViewModel,
                                shopProfile = prefs.shopProfile,
                                onBack = { navController.popBackStack() },
                                onOpenReceipt = { calcId ->
                                    navController.navigate(Routes.receiptRoute(calcId))
                                }
                            )
                        }

                        composable(Routes.NORMAL_CALCULATOR) {
                            val normalViewModel: NormalCalculatorViewModel = viewModel(factory = calcFactory)
                            NormalCalculatorScreen(
                                viewModel = normalViewModel,
                                shopProfile = prefs.shopProfile,
                                onBack = { navController.popBackStack() },
                                onOpenReceipt = { calcId ->
                                    navController.navigate(Routes.receiptRoute(calcId))
                                }
                            )
                        }

                        composable(Routes.HISTORY) {
                            HistoryScreen(
                                historyItems = history,
                                shopProfile = prefs.shopProfile,
                                onDeleteItem = mainViewModel::deleteHistoryItem,
                                onClearAll = mainViewModel::clearAllHistory,
                                onOpenReceipt = { calcId ->
                                    navController.navigate(Routes.receiptRoute(calcId))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.SHOP_PROFILE) {
                            ShopProfileScreen(
                                shopProfile = prefs.shopProfile,
                                onSaveProfile = mainViewModel::saveShopProfile,
                                onPickLogoUri = mainViewModel::updateShopLogo,
                                onRemoveLogo = mainViewModel::removeShopLogo,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(
                            route = Routes.RECEIPT,
                            arguments = listOf(
                                navArgument("calculationId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val calcId = backStackEntry.arguments?.getLong("calculationId") ?: -1L
                            ReceiptScreen(
                                calculationId = calcId,
                                shopProfile = prefs.shopProfile,
                                loadCalculation = { id -> mainViewModel.getCalculationById(id) },
                                onEditShopProfile = {
                                    navController.navigate(Routes.SHOP_PROFILE)
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                shopProfile = prefs.shopProfile,
                                currentThemeMode = prefs.themeMode,
                                currentLanguage = prefs.appLanguage,
                                onSelectThemeMode = mainViewModel::setThemeMode,
                                onSelectLanguage = mainViewModel::setLanguage,
                                onEditShopProfile = {
                                    navController.navigate(Routes.SHOP_PROFILE)
                                },
                                onClearHistory = mainViewModel::clearAllHistory,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
