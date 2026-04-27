package com.paulaizurrategui.urtriply.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.paulaizurrategui.urtriply.ui.auth.AuthViewModel
import com.paulaizurrategui.urtriply.ui.screens.LoginScreen
import com.paulaizurrategui.urtriply.ui.screens.MainShellScreen
import com.paulaizurrategui.urtriply.ui.screens.RegisterScreen
import com.paulaizurrategui.urtriply.ui.screens.WelcomeScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val authViewModel = remember { AuthViewModel() }

    val start = Routes.WELCOME

    NavHost(navController = navController, startDestination = start) {

        composable(Routes.WELCOME) {
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

            WelcomeScreen(
                isLoggedIn = isLoggedIn,
                onGoHome = {
                    navController.navigate("main/auth") {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
                onGoLogin = { navController.navigate(Routes.LOGIN) },
                onGoRegister = { navController.navigate(Routes.REGISTER) },
                onContinueGuest = {
                    navController.navigate("main/guest") {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel,
                onGoToRegister = { navController.navigate(Routes.REGISTER) },
                onLoginSuccess = {
                    navController.navigate("main/auth") {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                authViewModel = authViewModel,
                onGoToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate("main/auth") {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.MAIN,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "guest"

            MainShellScreen(
                mode = mode,
                authViewModel = authViewModel,
                onRequireLogin = { navController.navigate(Routes.LOGIN) },
                onLoggedOut = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}