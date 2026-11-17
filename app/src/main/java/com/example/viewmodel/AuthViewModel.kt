package com.example.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.attemptAutoSignIn as runAutoSignIn
import com.example.data.authStateFlow
import com.example.data.onGoogleSignInClicked
import com.example.data.performSignOut
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

data class AuthUser(
    val uid: String,
    val displayName: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val isEmailVerified: Boolean = false
)

private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
    uid = uid,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl?.toString(),
    isEmailVerified = isEmailVerified
)

sealed interface AuthStatus {
    data object Loading : AuthStatus
    data object Unauthenticated : AuthStatus
    data class Authenticated(val user: AuthUser) : AuthStatus
}

data class AuthUiState(
    val status: AuthStatus = AuthStatus.Loading,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val currentUser: AuthUser?
        get() = (status as? AuthStatus.Authenticated)?.user

    val userId: String?
        get() = currentUser?.uid

    val isAuthenticated: Boolean
        get() = status is AuthStatus.Authenticated
}

class AuthViewModel(
    authUserFlow: Flow<AuthUser?>,
    initialUser: AuthUser? = null
) : ViewModel() {

    constructor() : this(
        authUserFlow = Firebase.auth.authStateFlow().map { it?.toAuthUser() },
        initialUser = Firebase.auth.currentUser?.toAuthUser()
    )

    private val _isSubmitting = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private var autoSignInAttempted = false

    val authState: StateFlow<AuthUiState> = combine(
        authUserFlow.onStart { emit(initialUser) },
        _isSubmitting,
        _errorMessage
    ) { user, submitting, error ->
        val status = if (user != null) {
            AuthStatus.Authenticated(user)
        } else {
            AuthStatus.Unauthenticated
        }
        AuthUiState(
            status = status,
            isSubmitting = submitting,
            errorMessage = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AuthUiState(
            status = if (initialUser != null) {
                AuthStatus.Authenticated(initialUser)
            } else {
                AuthStatus.Unauthenticated
            }
        )
    )

    fun attemptAutoSignIn(context: Context, credentialManager: CredentialManager) {
        if (autoSignInAttempted || authState.value.isAuthenticated || _isSubmitting.value) return
        autoSignInAttempted = true
        runAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                _errorMessage.value = null
            },
            onUnauthenticated = {},
            scope = viewModelScope
        )
    }

    fun signInWithGoogle(context: Context, credentialManager: CredentialManager) {
        if (_isSubmitting.value) return
        _errorMessage.value = null
        _isSubmitting.value = true
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                _isSubmitting.value = false
                _errorMessage.value = null
            },
            onAuthError = { msg ->
                _isSubmitting.value = false
                _errorMessage.value = msg
            },
            scope = viewModelScope,
            onAuthCancelled = {
                _isSubmitting.value = false
            }
        )
    }

    fun signOut(credentialManager: CredentialManager, onComplete: () -> Unit = {}) {
        _errorMessage.value = null
        _isSubmitting.value = true
        autoSignInAttempted = false
        performSignOut(
            credentialManager = credentialManager,
            scope = viewModelScope,
            onComplete = {
                _isSubmitting.value = false
                onComplete()
            }
        )
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
