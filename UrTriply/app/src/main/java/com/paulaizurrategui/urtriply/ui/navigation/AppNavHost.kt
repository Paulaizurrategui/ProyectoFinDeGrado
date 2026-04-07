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
    val navController = rememberNavController() // Controla la navegación entre pantallas (NavHost)
    val authViewModel = remember { AuthViewModel() } // ViewModel compartido para login/register/logout en toda la app

    val start = Routes.WELCOME // Según Hito 1: siempre empezamos en la pantalla de bienvenida (Welcome)

    NavHost(navController = navController, startDestination = start) {

        composable(Routes.WELCOME) {
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null // true si hay sesión activa en Firebase

            WelcomeScreen(
                isLoggedIn = isLoggedIn,
                onGoHome = {
                    navController.navigate("main/auth") { // Entrar en "modo autenticado"
                        popUpTo(Routes.WELCOME) { inclusive = true } // Quita Welcome del backstack (no vuelve atrás)
                    }
                },
                onGoLogin = { navController.navigate(Routes.LOGIN) }, // Ir a Login
                onGoRegister = { navController.navigate(Routes.REGISTER) }, // Ir a Registro
                onContinueGuest = {
                    navController.navigate("main/guest") { // Entrar en "modo invitado"
                        popUpTo(Routes.WELCOME) { inclusive = true } // Quita Welcome del backstack
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                authViewModel = authViewModel, // Usa el mismo ViewModel para actualizar estado/loading/errores
                onGoToRegister = { navController.navigate(Routes.REGISTER) }, // Desde login -> registro
                onLoginSuccess = {
                    navController.navigate("main/auth") { // Si login OK -> Main autenticado
                        popUpTo(Routes.WELCOME) { inclusive = true } // Limpia el flujo de acceso
                    }
                }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                authViewModel = authViewModel,
                onGoToLogin = { navController.popBackStack() }, // Volver a la pantalla anterior
                onRegisterSuccess = {
                    navController.navigate("main/auth") { // Si registro OK -> Main autenticado
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.MAIN, // Ruta con argumento: "main/{mode}"
            arguments = listOf(navArgument("mode") { type = NavType.StringType }) // mode = "guest" o "auth"
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "guest" // Si no viene, asumimos invitado

            MainShellScreen(
                mode = mode, // Define permisos (guest limitado / auth completo)
                authViewModel = authViewModel,
                onRequireLogin = {
                    navController.navigate(Routes.LOGIN) // Si invitado intenta algo restringido -> Login
                },
                onLoggedOut = {
                    navController.navigate(Routes.WELCOME) { // Logout -> volver a Welcome
                        popUpTo(0) { inclusive = true } // Reset total del backstack
                    }
                }
            )
        }
    }
}