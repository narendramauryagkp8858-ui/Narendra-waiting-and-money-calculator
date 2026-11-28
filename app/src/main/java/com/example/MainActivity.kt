package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.localization.LocalStrings
import com.example.localization.stringsFor
import com.example.ui.navigation.Routes
import com.example.ui.screens.CombinedBillScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MoneyToWeightScreen
import com.example.ui.screens.NormalCalculatorScreen
import com.example.ui.screens.PieceToMoneyScreen
import com.example.ui.screens.ReceiptScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShopProfileScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.WeightToMoneyScreen
import com.example.ui.theme.NarendraCalculatorTheme
import com.example.ui.theme.ThemedWatermarkBackground
import com.example.ui.viewmodel.CalculatorViewModelFactory
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.MoneyToWeightViewModel
import com.example.ui.viewmodel.NormalCalculatorViewModel
import com.example.ui.viewmodel.PieceToMoneyViewModel
import com.example.ui.viewmodel.WeightToMoneyViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val mainViewModel: MainViewModel = viewModel()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val appLanguage by mainViewModel.appLanguage.collectAsStateWithLifecycle()
            val strings = remember(appLanguage) { stringsFor(appLanguage) }

            NarendraCalculatorTheme(themeMode = themeMode) {
                CompositionLocalProvider(LocalStrings provides strings) {
                    ThemedWatermarkBackground {
                        AppNavGraph(mainViewModel = mainViewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavGraph(mainViewModel: MainViewModel) {
    val navController = rememberNavController()
    val calcFactory = remember(mainViewModel.calculationRepository) {
        CalculatorViewModelFactory(mainViewModel.calculationRepository)
    }

    val navigateSingleTop = remember(navController) {
        { route: String ->
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }
    val onBack: () -> Unit = remember(navController) {
        { navController.popBackStack() }
    }
    val onOpenReceipt: (Long) -> Unit = remember(navController) {
        { calcId -> navigateSingleTop(Routes.receiptRoute(calcId)) }
    }
    val onNavigateWeightToMoney: () -> Unit = remember(navController) {
        { navigateSingleTop(Routes.WEIGHT_TO_MONEY) }
    }
    val onNavigateMoneyToWeight: () -> Unit = remember(navController) {
        { navigateSingleTop(Routes.MONEY_TO_WEIGHT) }
    }
    val onNavigatePieceToMoney: () -> Unit = remember(navController) {
        { navigateSingleTop(Routes.PIECE_TO_MONEY) }
    }
    val onNavigateCombinedBill: () -> Unit = remember(navController) {
        { navigateSingleTop(Routes.COMBINED_BILL) }
    }
    val onNavigateNormalCalculator: () -> Unit = remember(navController) {
        { navigateSingleTop(Routes.NORMAL_CALCULATOR) }
    }
    val onNavigateHistory: () -> Unit = remember(navController) {
        { navigateSingleTop(Routes.HISTORY) }
    }
    val onNavigateShopProfile: () -> Unit = remember(navController) {
        { navigateSingleTop(Routes.SHOP_PROFILE) }
    }
    val onNavigateSettings: () -> Unit = remember(navController) {
        { navigateSingleTop(Routes.SETTINGS) }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onFinished = {
                    navigateSplashToDashboard(navController)
                }
            )
        }

        composable(Routes.DASHBOARD) {
            val shopProfile by mainViewModel.shopProfile.collectAsStateWithLifecycle()
            val historyCount by mainViewModel.historyCount.collectAsStateWithLifecycle()
            val latestCalc by mainViewModel.latestCalculation.collectAsStateWithLifecycle()
            val billItems by mainViewModel.billItems.collectAsStateWithLifecycle()
            val appLanguage by mainViewModel.appLanguage.collectAsStateWithLifecycle()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()

            DashboardScreen(
                shopProfile = shopProfile,
                historyCount = historyCount,
                billCount = billItems.size,
                latestCalculation = latestCalc,
                currentLanguage = appLanguage,
                currentThemeMode = themeMode,
                onToggleLanguage = mainViewModel::toggleLanguage,
                onToggleTheme = mainViewModel::toggleTheme,
                onNavigateWeightToMoney = onNavigateWeightToMoney,
                onNavigateMoneyToWeight = onNavigateMoneyToWeight,
                onNavigatePieceToMoney = onNavigatePieceToMoney,
                onNavigateCombinedBill = onNavigateCombinedBill,
                onNavigateNormalCalculator = onNavigateNormalCalculator,
                onNavigateHistory = onNavigateHistory,
                onNavigateShopProfile = onNavigateShopProfile,
                onOpenReceipt = onOpenReceipt,
                onNavigateSettings = onNavigateSettings
            )
        }

        composable(Routes.WEIGHT_TO_MONEY) {
            val w2mViewModel: WeightToMoneyViewModel = viewModel(factory = calcFactory)
            val shopProfile by mainViewModel.shopProfile.collectAsStateWithLifecycle()
            val billItems by mainViewModel.billItems.collectAsStateWithLifecycle()
            WeightToMoneyScreen(
                viewModel = w2mViewModel,
                shopProfile = shopProfile,
                billItems = billItems,
                onAddToBill = mainViewModel::addWeightItemToBill,
                onOpenCombinedBill = onNavigateCombinedBill,
                onBack = onBack,
                onOpenReceipt = onOpenReceipt
            )
        }

        composable(Routes.MONEY_TO_WEIGHT) {
            val m2wViewModel: MoneyToWeightViewModel = viewModel(factory = calcFactory)
            val shopProfile by mainViewModel.shopProfile.collectAsStateWithLifecycle()
            val billItems by mainViewModel.billItems.collectAsStateWithLifecycle()
            MoneyToWeightScreen(
                viewModel = m2wViewModel,
                shopProfile = shopProfile,
                billItems = billItems,
                onAddToBill = mainViewModel::addExactItemToBill,
                onOpenCombinedBill = onNavigateCombinedBill,
                onBack = onBack,
                onOpenReceipt = onOpenReceipt
            )
        }

        composable(Routes.PIECE_TO_MONEY) {
            val p2mViewModel: PieceToMoneyViewModel = viewModel(factory = calcFactory)
            val shopProfile by mainViewModel.shopProfile.collectAsStateWithLifecycle()
            val billItems by mainViewModel.billItems.collectAsStateWithLifecycle()
            PieceToMoneyScreen(
                viewModel = p2mViewModel,
                shopProfile = shopProfile,
                billItems = billItems,
                onAddToBill = mainViewModel::addPieceItemToBill,
                onOpenCombinedBill = onNavigateCombinedBill,
                onBack = onBack,
                onOpenReceipt = onOpenReceipt
            )
        }

        composable(Routes.COMBINED_BILL) {
            val shopProfile by mainViewModel.shopProfile.collectAsStateWithLifecycle()
            val billItems by mainViewModel.billItems.collectAsStateWithLifecycle()
            CombinedBillScreen(
                billItems = billItems,
                shopProfile = shopProfile,
                onAddWeightItem = mainViewModel::addWeightItemToBill,
                onAddPieceItem = mainViewModel::addPieceItemToBill,
                onRemoveItem = mainViewModel::removeBillItem,
                onClearBill = mainViewModel::clearBillItems,
                onSaveBillToHistory = mainViewModel::saveCombinedBillToHistory,
                onOpenReceipt = onOpenReceipt,
                onBack = onBack
            )
        }

        composable(Routes.NORMAL_CALCULATOR) {
            val normalViewModel: NormalCalculatorViewModel = viewModel(factory = calcFactory)
            val shopProfile by mainViewModel.shopProfile.collectAsStateWithLifecycle()
            NormalCalculatorScreen(
                viewModel = normalViewModel,
                shopProfile = shopProfile,
                onBack = onBack,
                onOpenReceipt = onOpenReceipt
            )
        }

        composable(Routes.HISTORY) {
            val history by mainViewModel.calculationHistory.collectAsStateWithLifecycle()
            val shopProfile by mainViewModel.shopProfile.collectAsStateWithLifecycle()
            HistoryScreen(
                historyItems = history,
                shopProfile = shopProfile,
                onDeleteItem = mainViewModel::deleteHistoryItem,
                onRestoreItem = mainViewModel::restoreHistoryItem,
                onClearAll = mainViewModel::clearAllHistory,
                onOpenReceipt = onOpenReceipt,
                onBack = onBack
            )
        }

        composable(Routes.SHOP_PROFILE) {
            val shopProfile by mainViewModel.shopProfile.collectAsStateWithLifecycle()
            ShopProfileScreen(
                shopProfile = shopProfile,
                onSaveProfile = mainViewModel::saveShopProfile,
                onPickLogoUri = mainViewModel::updateShopLogo,
                onRemoveLogo = mainViewModel::removeShopLogo,
                onBack = onBack
            )
        }

        composable(
            route = Routes.RECEIPT,
            arguments = listOf(
                navArgument("calculationId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val calcId = backStackEntry.arguments?.getLong("calculationId") ?: -1L
            val shopProfile by mainViewModel.shopProfile.collectAsStateWithLifecycle()
            ReceiptScreen(
                calculationId = calcId,
                shopProfile = shopProfile,
                loadCalculation = mainViewModel::getCalculationById,
                onEditShopProfile = onNavigateShopProfile,
                onBack = onBack
            )
        }

        composable(Routes.SETTINGS) {
            val shopProfile by mainViewModel.shopProfile.collectAsStateWithLifecycle()
            val themeMode by mainViewModel.themeMode.collectAsStateWithLifecycle()
            val appLanguage by mainViewModel.appLanguage.collectAsStateWithLifecycle()
            SettingsScreen(
                shopProfile = shopProfile,
                currentThemeMode = themeMode,
                currentLanguage = appLanguage,
                onSelectThemeMode = mainViewModel::setThemeMode,
                onSelectLanguage = mainViewModel::setLanguage,
                onEditShopProfile = onNavigateShopProfile,
                onClearHistory = mainViewModel::clearAllHistory,
                onBack = onBack
            )
        }
    }
}

private fun navigateSplashToDashboard(navController: NavHostController) {
    navController.navigate(Routes.DASHBOARD) {
        popUpTo(Routes.SPLASH) { inclusive = true }
        launchSingleTop = true
    }
}
