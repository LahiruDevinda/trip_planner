package com.devinda.tripplanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devinda.tripplanner.ui.theme.screens.CreateTripPage
import com.devinda.tripplanner.ui.theme.screens.HomePage
import com.devinda.tripplanner.ui.theme.screens.LoginPage
import com.devinda.tripplanner.ui.theme.screens.SignupPage
import com.devinda.tripplanner.ui.theme.screens.StartTripMapScreen
import com.devinda.tripplanner.ui.theme.screens.TripDetailsPage

@Composable
fun MyAppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {


    val navController = rememberNavController()
    val startDestination = if (authViewModel.isUserLoggedIn()) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") { LoginPage(navController= navController, authViewModel= authViewModel) }
        composable("signup") { SignupPage(navController = navController, authViewModel= authViewModel) }
        composable("home") { HomePage(Modifier, navController, authViewModel) }
        composable("create_trip") { CreateTripPage(navController) }


        composable(
            route = "trip_details/{tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            TripDetailsPage(tripId = tripId, navController = navController)
        }

        composable(
            route = "trip_route/{tripId}",
            arguments = listOf(navArgument("tripId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tripId = backStackEntry.arguments?.getString("tripId") ?: ""
            StartTripMapScreen(tripId = tripId, navController = navController)
        }
    }

}

