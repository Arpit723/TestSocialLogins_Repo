package com.bk.testsocialloginskmp.auth

import kotlinx.coroutines.flow.StateFlow

data class AppUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
)

interface AuthClient {
    val isSupported: Boolean
    val currentUser: StateFlow<AppUser?>
    suspend fun signInWithGoogle(): AppUser?
    suspend fun signOut()
}

expect val authClient: AuthClient
