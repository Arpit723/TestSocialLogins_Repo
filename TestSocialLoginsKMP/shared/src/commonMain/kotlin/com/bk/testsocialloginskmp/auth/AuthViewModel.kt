package com.bk.testsocialloginskmp.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

data class AuthUiState(
    val user: AppUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class AuthViewModel : ViewModel() {

    private val client: AuthClient = authClient

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            client.currentUser.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun signIn() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                client.signInWithGoogle()
            } catch (e: CancellationException) {
                throw e
            } catch (e: SignInCancelledException) {
                // User dismissed the sign-in sheet; not an error.
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: e.toString()) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                client.signOut()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: e.toString()) }
            }
        }
    }
}
