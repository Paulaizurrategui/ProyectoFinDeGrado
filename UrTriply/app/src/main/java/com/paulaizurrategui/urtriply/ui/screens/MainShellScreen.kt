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

data class BottomTab(
    val route: String,
    val labelRes: Int,
    val icon: @Composable () -> Unit
)

@Composable
fun MainShellScreen(
    mode: String,
    authViewModel: AuthViewModel,
    onRequireLogin: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val navController = rememberNavController()

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: MainTabs.HOME

    val isGuest = mode.lowercase() == "guest"

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
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
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
            modifier = Modifier.padding(innerPadding),
            navController = navController,
            startDestination = MainTabs.HOME
        ) {
            composable(MainTabs.HOME) {
                InicioTabScreen(isGuest = isGuest, onRequireLogin = onRequireLogin)
            }

            composable(MainTabs.PLAN) {
                PlanTabScreen(
                    isGuest = isGuest,
                    onNavigateToResult = { navController.navigate(PlanRoutes.RESULT) }
                )
            }

            composable(PlanRoutes.RESULT) {
                PlanResultScreen(
                    isGuest = isGuest,
                    onBack = { navController.popBackStack() },
                    onRequireLogin = onRequireLogin
                )
            }

            composable(MainTabs.COMMUNITY) {
                if (isGuest) RequireLoginScreen(onRequireLogin = onRequireLogin)
                else CommunityScreen()
            }

            composable(MainTabs.PROFILE) {
                if (isGuest) {
                    RequireLoginScreen(onRequireLogin = onRequireLogin)
                } else {
                    ProfileTabScreen(
                        authViewModel = authViewModel,
                        onLoggedOut = onLoggedOut,
                        onEditProfile = { navController.navigate("profile/edit") },
                        onEditTrip = { tripId -> navController.navigate("trip/edit/$tripId") },
                        onNavigateToFindFriends = { navController.navigate("find_friends") }
                    )
                }
            }

            composable("profile/edit") {
                EditProfileScreen(onBack = { navController.popBackStack() })
            }

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

            composable("find_friends") {
                FindFriendsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}