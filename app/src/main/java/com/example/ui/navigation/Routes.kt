package com.example.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val DASHBOARD = "dashboard"
    const val WEIGHT_TO_MONEY = "weight_to_money"
    const val MONEY_TO_WEIGHT = "money_to_weight"
    const val PIECE_TO_MONEY = "piece_to_money"
    const val COMBINED_BILL = "combined_bill"
    const val NORMAL_CALCULATOR = "normal_calculator"
    const val HISTORY = "history"
    const val SHOP_PROFILE = "shop_profile"
    const val SETTINGS = "settings"
    const val RECEIPT = "receipt/{calculationId}"

    fun receiptRoute(calculationId: Long): String = "receipt/$calculationId"
}
