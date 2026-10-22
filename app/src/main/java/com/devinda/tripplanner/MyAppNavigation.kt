package com.devinda.tripplanner

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devinda.tripplanner.ui.theme.screens.HomePage
import com.devinda.tripplanner.ui.theme.screens.LoginPage
import com.devinda.tripplanner.ui.theme.screens.SignupPage

@Composable
fun MyAppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {


    val navController = rememberNavController()
    val startDestination = if (authViewModel.isUserLoggedIn()) "home" else "login"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") { LoginPage(navController = navController, authViewModel = authViewModel) }
        composable("signup") { SignupPage(navController = navController, authViewModel = authViewModel) }
        composable("home") { HomePage(navController = navController, authViewModel = authViewModel) }
    }

}

