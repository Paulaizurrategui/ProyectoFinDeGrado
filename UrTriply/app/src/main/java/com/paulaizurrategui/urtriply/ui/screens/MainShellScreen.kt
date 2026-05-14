package com.paulaizurrategui.urtriply.ui.screens

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.paulaizurrategui.urtriply.ui.navigation.Routes

data class BottomTab( // modelo de cada tab
    val route: String, // ruta navegación
    val labelRes: Int, // texto
    val icon: @Composable () -> Unit // icono
)

@Composable
fun MainShellScreen( // pantalla principal con navegación
    mode: String, // modo usuario
    authViewModel: AuthViewModel,
    onRequireLogin: () -> Unit, // pedir login
    onLoggedOut: () -> Unit, // logout
    onNavigateToAdmin: () -> Unit
) {
    val navController = rememberNavController() // creo controlador

    val currentBackStackEntry by navController.currentBackStackEntryAsState() // observo ruta
    val currentRoute = currentBackStackEntry?.destination?.route ?: MainTabs.HOME // ruta actual o home

    val isGuest = mode.lowercase() == "guest" // compruebo si es invitado

    val tabs = listOf( // lista tabs abajo
        BottomTab(MainTabs.HOME, R.string.tab_home) { Icon(Icons.Default.Home, contentDescription = null) },
        BottomTab(MainTabs.PLAN, R.string.tab_plan) { Icon(Icons.Default.Place, contentDescription = null) },
        BottomTab(MainTabs.COMMUNITY, R.string.tab_community) { Icon(Icons.Default.People, contentDescription = null) },
        BottomTab(MainTabs.PROFILE, R.string.tab_profile) { Icon(Icons.Default.Person, contentDescription = null) }
    )

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        // CLAVE: gestionamos insets una sola vez a nivel app
        contentWindowInsets = WindowInsets.safeDrawing, // evita solaparse con sistema
        bottomBar = {
            // CLAVE: evitamos que NavigationBar meta padding extra "de más"
            NavigationBar(
                windowInsets = WindowInsets(0, 0, 0, 0), // quito padding extra
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                tabs.forEach { tab -> // recorro tabs
                    val selected = currentRoute == tab.route // si esta seleccionada
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navigateToTab(tab.route)
                        },
                        icon = tab.icon,
                        label = { Text(stringResource(tab.labelRes)) } // texto tab
                    )
                }
            }
        }
    ) { innerPadding -> // padding interno del scaffold
        NavHost(
            modifier = Modifier.padding(innerPadding), // aplico padding
            navController = navController,
            startDestination = MainTabs.HOME // inicio en home
        ) {

            composable(MainTabs.HOME) {
                InicioTabScreen(
                    isGuest = isGuest,
                    onRequireLogin = onRequireLogin,
                    onGoPlan = { navigateToTab(MainTabs.PLAN) },
                    onGoCommunity = { navigateToTab(MainTabs.COMMUNITY) },
                    onGoProfile = { navigateToTab(MainTabs.PROFILE) }
                )
            }

            composable(MainTabs.PLAN) {
                PlanTabScreen(
                    isGuest = isGuest,
                    onNavigateToResult = { navController.navigate(PlanRoutes.RESULT) } // ir a resultado
                )
            }

            composable(PlanRoutes.RESULT) {
                PlanResultScreen(
                    isGuest = isGuest,
                    onBack = { navController.popBackStack() }, // volver atrás
                    onRequireLogin = onRequireLogin
                )
            }

            composable(MainTabs.COMMUNITY) {
                if (isGuest) RequireLoginScreen(onRequireLogin = onRequireLogin) // si invitado, bloqueo
                else CommunityTabScreen(onPostClick = { postId ->
                    // Navigate to post details
                    navController.navigate("community/post/$postId")
                }, onNavigateToFindFriends = {
                    navController.navigate("find_friends")
                }, onNavigateToUserProfile = { userId ->
                    navController.navigate("profile/public/$userId")
                })
            }

            composable(
                route = "community/post/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                PostDetailScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(MainTabs.PROFILE) {
                if (isGuest) {
                    RequireLoginScreen(onRequireLogin = onRequireLogin) // perfil requiere login
                } else {
                    ProfileTabScreen(
                        authViewModel = authViewModel,
                        onLoggedOut = onLoggedOut,
                        onEditProfile = { navController.navigate("profile/edit") }, // editar perfil
                        onEditTrip = { tripId -> navController.navigate("trip/edit/$tripId") }, // editar viaje
                        onNavigateToFindFriends = { navController.navigate("find_friends") }, // buscar amigos
                        onNavigateToAdmin = onNavigateToAdmin
                    )
                }
            }

            composable("profile/edit") {
                EditProfileScreen(onBack = { navController.popBackStack() }) // pantalla editar perfil
            }

            composable(
                route = "trip/edit/{tripId}", // ruta con parametro
                arguments = listOf(navArgument("tripId") { type = NavType.StringType })
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable // saco id
                EditTripScreen(
                    tripId = tripId,
                    onBack = { navController.popBackStack() } // volver atrás
                )
            }

            composable("find_friends") {
                FindFriendsScreen(onBack = { navController.popBackStack() }) // pantalla amigos
            }

            composable(
                route = "profile/public/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
                PublicProfileScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}