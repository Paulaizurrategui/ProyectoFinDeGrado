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
    // nav principal (welcome/login/register/main)
    val navController = rememberNavController()

    // vm compartido entre login y register
    val authViewModel = remember { AuthViewModel() }

    // pantalla inicial (si quieres auto-login, aqui podrias cambiarlo segun currentuser)
    val start = Routes.WELCOME

    NavHost(navController = navController, startDestination = start) {

        composable(Routes.WELCOME) {
            // miro si hay sesion (para cambiar botones en welcome)
            val isLoggedIn = FirebaseAuth.getInstance().currentUser != null

            WelcomeScreen(
                isLoggedIn = isLoggedIn,

                // entro a modo auth (tabs + pantallas)
                onGoHome = {
                    navController.navigate("main/auth") {
                        popUpTo(Routes.WELCOME) { inclusive = true } // quito welcome del backstack
                    }
                },

                // navegacion normal
                onGoLogin = { navController.navigate(Routes.LOGIN) },
                onGoRegister = { navController.navigate(Routes.REGISTER) },

                // modo invitado (sin comunidad/perfil)
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

                // al loguear, entro al main en modo auth
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

                // vuelvo a login (back)
                onGoToLogin = { navController.popBackStack() },

                // al registrar, entro al main en modo auth
                onRegisterSuccess = {
                    navController.navigate("main/auth") {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                }
            )
        }

        // main recibe un argumento {mode} = auth o guest
        composable(
            route = Routes.MAIN,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "guest"

            MainShellScreen(
                mode = mode,
                authViewModel = authViewModel,

                // si en modo guest intento entrar a algo bloqueado, mando a login
                onRequireLogin = { navController.navigate(Routes.LOGIN) },

                // al cerrar sesion vuelvo al welcome y limpio backstack
                onLoggedOut = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}