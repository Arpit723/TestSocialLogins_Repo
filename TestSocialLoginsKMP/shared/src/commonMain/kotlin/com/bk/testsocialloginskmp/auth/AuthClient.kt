package com.bk.testsocialloginskmp.auth

import kotlinx.coroutines.flow.StateFlow

data class AppUser(
    val uid: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
)

/** Thrown when the user deliberately dismisses the sign-in sheet; not an error to surface. */
class SignInCancelledException : Exception("Sign-in cancelled by user")

interface AuthClient {
    val isSupported: Boolean
    val currentUser: StateFlow<AppUser?>
    suspend fun signInWithGoogle()
    suspend fun signOut()
}

expect val authClient: AuthClient
