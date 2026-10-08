package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Calendar : Screen("calendar")
    object Analytics : Screen("analytics")
    object History : Screen("history")
    object Settings : Screen("settings")
    object Search : Screen("search")
    object SmsSync : Screen("sms_sync")
}
