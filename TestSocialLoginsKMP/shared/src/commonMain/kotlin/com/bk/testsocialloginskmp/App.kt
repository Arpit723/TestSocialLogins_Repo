package com.bk.testsocialloginskmp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bk.testsocialloginskmp.auth.AuthViewModel
import com.bk.testsocialloginskmp.auth.AppUser
import com.bk.testsocialloginskmp.auth.authClient
import org.jetbrains.compose.resources.painterResource

import testsocialloginskmp.shared.generated.resources.Res
import testsocialloginskmp.shared.generated.resources.compose_multiplatform

@Composable
@Preview
fun App() {
    MaterialTheme {
        val viewModel: AuthViewModel = viewModel { AuthViewModel() }
        val uiState by viewModel.uiState.collectAsState()
        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val user = uiState.user
            if (user != null) {
                SignedInSection(
                    user = user,
                    isLoading = uiState.isLoading,
                    onSignOut = viewModel::signOut,
                )
            } else {
                Button(
                    onClick = viewModel::signIn,
                    enabled = authClient.isSupported && !uiState.isLoading,
                ) {
                    Text(
                        if (authClient.isSupported) "Sign in with Google"
                        else "Sign in with Google — iOS support coming soon"
                    )
                }
            }
            if (uiState.isLoading) {
                CircularProgressIndicator()
            }
            uiState.errorMessage?.let {
                Text("Error: $it", color = MaterialTheme.colorScheme.error)
            }
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}

@Composable
private fun SignedInSection(
    user: AppUser,
    isLoading: Boolean,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        user.photoUrl?.let { Text("Photo: $it") }
        user.displayName?.let { Text("Signed in as ${it}") }
        user.email?.let { Text(it) }
        Text("UID: ${user.uid}")
        Button(onClick = onSignOut, enabled = !isLoading) {
            Text("Sign out")
        }
    }
}
