package com.lensstock.app.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginScreen(
    onSignedIn: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state) {
        if (state is LoginUiState.SignedIn) onSignedIn()
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Lens Stock", style = MaterialTheme.typography.headlineMedium)

            when (val s = state) {
                is LoginUiState.Loading -> CircularProgressIndicator(modifier = Modifier.padding(top = 24.dp))
                is LoginUiState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
                    Button(onClick = viewModel::signIn, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Sign in with Google")
                    }
                }
                else -> {
                    Button(onClick = viewModel::signIn, modifier = Modifier.padding(top = 24.dp)) {
                        Text("Sign in with Google")
                    }
                }
            }
        }
    }
}
