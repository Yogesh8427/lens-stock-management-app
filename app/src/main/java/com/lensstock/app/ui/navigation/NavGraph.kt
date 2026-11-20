package com.lensstock.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lensstock.app.ui.screens.fullstock.FullStockScreen
import com.lensstock.app.ui.screens.history.HistoryScreen
import com.lensstock.app.ui.screens.home.HomeScreen
import com.lensstock.app.ui.screens.login.LoginScreen
import com.lensstock.app.ui.screens.scan.ScanMode
import com.lensstock.app.ui.screens.scan.ScanScreen
import com.lensstock.app.ui.screens.search.SearchScreen
import com.lensstock.app.ui.screens.settings.SettingsScreen
import com.lensstock.app.ui.screens.toorder.ToOrderScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun LensStockNavGraph(navController: NavHostController = rememberNavController()) {
    val startDestination = if (Firebase.auth.currentUser != null) Routes.HOME else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(onSignedIn = { navController.navigateToHome() })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onScanIn = { navController.navigate(Routes.SCAN_IN) },
                onScanOut = { navController.navigate(Routes.SCAN_OUT) },
                onSearch = { navController.navigate(Routes.SEARCH) },
                onToOrder = { navController.navigate(Routes.TO_ORDER) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onFullStock = { navController.navigate(Routes.FULL_STOCK) },
            )
        }
        composable(Routes.SEARCH) {
            SearchScreen(onBack = navController::popBackStack)
        }
        composable(Routes.FULL_STOCK) {
            FullStockScreen(onBack = navController::popBackStack)
        }
        composable(Routes.SCAN_IN) {
            ScanScreen(mode = ScanMode.IN, onBack = navController::popBackStack)
        }
        composable(Routes.SCAN_OUT) {
            ScanScreen(mode = ScanMode.OUT, onBack = navController::popBackStack)
        }
        composable(Routes.TO_ORDER) {
            ToOrderScreen(onBack = navController::popBackStack)
        }
        composable(Routes.ACTIVITY) {
            HistoryScreen(onBack = navController::popBackStack)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = navController::popBackStack,
                onActivity = { navController.navigate(Routes.ACTIVITY) },
            )
        }
    }
}

private fun NavHostController.navigateToHome() {
    navigate(Routes.HOME) {
        popUpTo(Routes.LOGIN) { inclusive = true }
    }
}
