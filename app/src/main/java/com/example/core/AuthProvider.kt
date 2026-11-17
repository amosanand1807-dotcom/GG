package com.example.core

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.AuthUiState
import com.example.viewmodel.AuthViewModel

data class AuthController(
    val state: AuthUiState,
    val attemptAutoSignIn: () -> Unit,
    val signInWithGoogle: () -> Unit,
    val signOut: ((() -> Unit)) -> Unit,
    val clearError: () -> Unit
)

val LocalAuthController = staticCompositionLocalOf<AuthController> {
    error("AuthProvider must wrap the application composable hierarchy before accessing LocalAuthController.")
}

@Composable
fun AuthProvider(
    authViewModel: AuthViewModel = viewModel(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activityContext = remember(context) { context.findActivity() ?: context }
    val credentialManager = remember(activityContext) { CredentialManager.create(activityContext) }
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    val controller = remember(authState, authViewModel, activityContext, credentialManager) {
        AuthController(
            state = authState,
            attemptAutoSignIn = {
                authViewModel.attemptAutoSignIn(activityContext, credentialManager)
            },
            signInWithGoogle = {
                authViewModel.signInWithGoogle(activityContext, credentialManager)
            },
            signOut = { onComplete ->
                authViewModel.signOut(credentialManager, onComplete)
            },
            clearError = {
                authViewModel.clearError()
            }
        )
    }

    CompositionLocalProvider(LocalAuthController provides controller) {
        content()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
