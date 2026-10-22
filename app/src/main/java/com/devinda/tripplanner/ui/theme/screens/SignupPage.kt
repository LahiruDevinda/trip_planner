package com.devinda.tripplanner.ui.theme.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.devinda.tripplanner.AuthState
import com.devinda.tripplanner.AuthViewModel
import com.devinda.tripplanner.R

@Composable
fun SignupPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.observeAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }


    LaunchedEffect(Unit) {
        authViewModel.resetAuthState()
    }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.SignUpSuccess -> {
                Toast.makeText(
                    context.applicationContext,
                    "Account created successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                navController.navigate("home") {
                    popUpTo("signup") { inclusive = true }
                }
            }
            is AuthState.Error -> Unit
            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(Color(0xFF161433)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.auth_banner),
                contentDescription = "Header Illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "Sign Up",
                    fontSize = 30.sp,
                    color = colorResource(R.color.text_primary))
                Spacer(modifier = Modifier.height(16.dp))

                if (authState is AuthState.Error) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(text = "Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(

                        focusedTextColor = colorResource(R.color.focused_text_color),
                        unfocusedTextColor = colorResource(R.color.unfocused_text_color),

                        focusedLabelColor = colorResource(R.color.focused_label_color),
                        unfocusedLabelColor = colorResource(R.color.unfocused_label_color),

                        focusedBorderColor = colorResource(R.color.focused_border_color),
                        unfocusedBorderColor = colorResource(R.color.unfocused_border_color),

                        cursorColor = colorResource(R.color.cursor_color)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(text = "Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(

                        focusedTextColor = colorResource(R.color.focused_text_color),
                        unfocusedTextColor = colorResource(R.color.unfocused_text_color),

                        focusedLabelColor = colorResource(R.color.focused_label_color),
                        unfocusedLabelColor = colorResource(R.color.unfocused_label_color),

                        focusedBorderColor = colorResource(R.color.focused_border_color),
                        unfocusedBorderColor = colorResource(R.color.unfocused_border_color),

                        cursorColor = colorResource(R.color.cursor_color)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(text = "Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(

                        focusedTextColor = colorResource(R.color.focused_text_color),
                        unfocusedTextColor = colorResource(R.color.unfocused_text_color),

                        focusedLabelColor = colorResource(R.color.focused_label_color),
                        unfocusedLabelColor = colorResource(R.color.unfocused_label_color),

                        focusedBorderColor = colorResource(R.color.focused_border_color),
                        unfocusedBorderColor = colorResource(R.color.unfocused_border_color),

                        cursorColor = colorResource(R.color.cursor_color)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(text = "Confirm password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(

                        focusedTextColor = colorResource(R.color.focused_text_color),
                        unfocusedTextColor = colorResource(R.color.unfocused_text_color),

                        focusedLabelColor = colorResource(R.color.focused_label_color),
                        unfocusedLabelColor = colorResource(R.color.unfocused_label_color),

                        focusedBorderColor = colorResource(R.color.focused_border_color),
                        unfocusedBorderColor = colorResource(R.color.unfocused_border_color),

                        cursorColor = colorResource(R.color.cursor_color)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        authViewModel.signUp(name, email, password, confirmPassword, onSuccess = {
                            Toast.makeText(
                                context.applicationContext,
                                "Account created successfully!",
                                Toast.LENGTH_LONG
                            ).show()

                            navController.navigate("login") {
                                popUpTo("signup") { inclusive = true }
                            }
                        })
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.button_container),
                        contentColor = colorResource(R.color.button_text),
                        disabledContainerColor = colorResource(R.color.button_disabled_container),
                        disabledContentColor = colorResource(R.color.button_disabled_text)
                    ),
                    enabled = authState !is AuthState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = "Create Account", fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                TextButton(
                    onClick = {
                        navController.navigate("login") {
                            popUpTo("signup") { inclusive = true }
                        }
                    }
                ) {
                    Text(
                        text = "Already have an account? Log in",
                        fontSize = 14.sp,
                        color= colorResource(R.color.text_primary))
                }
            }

        }
    }
}
