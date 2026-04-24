package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.paulaizurrategui.urtriply.R
import com.paulaizurrategui.urtriply.ui.auth.AuthViewModel
import com.paulaizurrategui.urtriply.ui.navigation.MainTabs
import com.paulaizurrategui.urtriply.ui.navigation.PlanRoutes

// Modelo simple para definir cada item del BottomNavigation (ruta + texto + icono)
data class BottomTab(
    val route: String, // Ruta interna del NavHost de tabs (ej. "tab_home")
    val labelRes: Int, // Texto del tab en strings.xml (multi-idioma)
    val icon: @Composable () -> Unit // Icono del tab como composable (Icon(...))
)

@Composable
fun MainShellScreen(
    mode: String, // Modo de ejecución: "auth" (logueado) o "guest" (invitado)
    authViewModel: AuthViewModel, // ViewModel de auth para poder hacer logout desde Perfil
    onRequireLogin: () -> Unit, // Callback: se usa cuando un invitado intenta acceder a algo restringido
    onLoggedOut: () -> Unit // Callback: se usa cuando el usuario hace logout (volver a Welcome y limpiar backstack)
) {
    // NavController interno para navegar ENTRE tabs (y pantallas internas como plan_result)
    val navController = rememberNavController()

    // Observa la ruta actual para marcar el tab seleccionado
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: MainTabs.HOME

    // Si es invitado, aplicamos restricciones en COMMUNITY y PROFILE
    val isGuest = mode.lowercase() == "guest"

    // Definición de tabs del bottom bar (rutas + etiquetas + iconos)
    val tabs = listOf(
        BottomTab(MainTabs.HOME, R.string.tab_home) { Icon(Icons.Default.Home, contentDescription = null) },
        BottomTab(MainTabs.PLAN, R.string.tab_plan) { Icon(Icons.Default.Place, contentDescription = null) },
        BottomTab(MainTabs.COMMUNITY, R.string.tab_community) { Icon(Icons.Default.People, contentDescription = null) },
        BottomTab(MainTabs.PROFILE, R.string.tab_profile) { Icon(Icons.Default.Person, contentDescription = null) }
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    val selected = currentRoute == tab.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                // Mantiene estado de tabs previas
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                // Evita duplicar la misma pantalla si vuelves a pulsar el tab
                                launchSingleTop = true
                                // Restaura estado guardado (scroll, etc.) al volver a un tab
                                restoreState = true
                            }
                        },
                        icon = tab.icon,
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            modifier = Modifier.padding(innerPadding), // Respeta padding del Scaffold para no tapar contenido con el bottom bar
            navController = navController,
            startDestination = MainTabs.HOME // Tab por defecto al entrar en MainShell
        ) {
            // TAB: HOME
            composable(MainTabs.HOME) {
                InicioTabScreen(isGuest = isGuest, onRequireLogin = onRequireLogin)
            }

            // TAB: PLAN (Pantalla 4)
            composable(MainTabs.PLAN) {
                PlanTabScreen(
                    isGuest = isGuest,
                    onNavigateToResult = { navController.navigate(PlanRoutes.RESULT) }
                )
            }

            // PANTALLA 5: RESULTADO DE PROPUESTA (se mantiene bottom bar visible)
            composable(PlanRoutes.RESULT) {
                PlanResultScreen(
                    isGuest = isGuest,
                    onBack = { navController.popBackStack() },
                    onRequireLogin = onRequireLogin
                )
            }

            // TAB: COMMUNITY
            composable(MainTabs.COMMUNITY) {
                if (isGuest) RequireLoginScreen(onRequireLogin = onRequireLogin)
                else CommunityTabScreen()
            }

            // TAB: PROFILE
            composable(MainTabs.PROFILE) {
                if (isGuest) RequireLoginScreen(onRequireLogin = onRequireLogin)
                else ProfileTabScreen(
                    authViewModel = authViewModel,
                    onLoggedOut = onLoggedOut,
                    onEditProfile = { navController.navigate("profile/edit") },
                    onEditTrip = { tripId -> navController.navigate("trip/edit/$tripId") }
                )
            }

            // PANTALLA: EDITAR PERFIL
            composable("profile/edit") {
                EditProfileScreen(onBack = { navController.popBackStack() })
            }

            // PANTALLA: EDITAR VIAJE
            composable(
                route = "trip/edit/{tripId}",
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
                EditTripScreen(
                    tripId = tripId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}