package com.sxplus.rider.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.sxplus.rider.ui.screens.dashboard.DashboardScreen
import com.sxplus.rider.ui.screens.login.AuthViewModel
import com.sxplus.rider.ui.screens.login.LoginScreen
import com.sxplus.rider.ui.screens.otp.OtpScreen
import com.sxplus.rider.ui.screens.scaffold.FeatureScaffoldScreen
import com.sxplus.rider.ui.screens.scan.ScanScreen

object Routes {
    const val AUTH = "auth"
    const val LOGIN = "login"
    const val OTP = "otp"
    const val HOME = "home"
    const val FEATURE = "feature"
    const val SCAN = "scan"
}

@Composable
fun AppNav(startLoggedIn: Boolean) {
    val nav = rememberNavController()
    val start = if (startLoggedIn) Routes.HOME else Routes.AUTH

    NavHost(navController = nav, startDestination = start) {

        navigation(startDestination = Routes.LOGIN, route = Routes.AUTH) {
            composable(Routes.LOGIN) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.AUTH) }
                val vm: AuthViewModel = hiltViewModel(parent)
                LoginScreen(
                    vm = vm,
                    onOtpSent = { nav.navigate(Routes.OTP) },
                    // Go to the dashboard; it reconnects if already paired, or opens
                    // the scan list directly if nothing is paired yet.
                    onConnectDirectly = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.OTP) { entry ->
                val parent = remember(entry) { nav.getBackStackEntry(Routes.AUTH) }
                val vm: AuthViewModel = hiltViewModel(parent)
                OtpScreen(
                    vm = vm,
                    onVerified = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }
                    },
                    onBack = { nav.popBackStack() },
                )
            }
        }

        composable(Routes.HOME) {
            DashboardScreen(
                onOpenFeature = { key -> nav.navigate("${Routes.FEATURE}/$key") },
                onOpenScan = { nav.navigate(Routes.SCAN) },
                onLoggedOut = {
                    nav.navigate(Routes.AUTH) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.SCAN) {
            ScanScreen(
                onBack = { nav.popBackStack() },
                onPaired = {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.SCAN) { inclusive = true }
                    }
                },
            )
        }

        composable(
            "${Routes.FEATURE}/{key}",
            arguments = listOf(navArgument("key") { type = NavType.StringType }),
        ) { entry ->
            val key = entry.arguments?.getString("key") ?: ""
            val onBack: () -> Unit = { nav.popBackStack() }
            when (key) {
                "telemetry" -> com.sxplus.rider.ui.screens.features.TelemetryScreen(onBack)
                "crash" -> com.sxplus.rider.ui.screens.features.CrashAlertScreen(onBack)
                "geofence" -> com.sxplus.rider.ui.screens.features.FenceScreen(onBack)
                "rides" -> com.sxplus.rider.ui.screens.features.RideHistoryScreen(onBack)
                "charging" -> com.sxplus.rider.ui.screens.features.ChargingScreen(onBack)
                "share" -> com.sxplus.rider.ui.screens.features.ShareVehicleScreen(onBack)
                "cluster" -> com.sxplus.rider.ui.screens.features.ClusterControlScreen(onBack)
                "settings" -> com.sxplus.rider.ui.screens.features.VehicleSettingsScreen(onBack)
                "profile" -> com.sxplus.rider.ui.screens.features.ProfileScreen(onBack)
                else -> FeatureScaffoldScreen(featureKey = key, title = featureTitle(key), onBack = onBack)
            }
        }
    }
}

private fun featureTitle(key: String): String = when (key) {
    "telemetry" -> "Live Telemetry"
    "crash" -> "Crash Alert"
    "geofence" -> "Geo / Time Fence"
    "rides" -> "Ride History"
    "charging" -> "Charging"
    "share" -> "Share Vehicle"
    else -> "Feature"
}
