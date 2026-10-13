package com.bk.testsocialloginskmp.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

actual val authClient: AuthClient by lazy { IosAuthClient() }

/** Google tokens returned by the native GoogleSignIn SDK. */
class GoogleSignInTokens(
    val idToken: String,
    val accessToken: String?,
)

/** Implemented on the Swift side with GIDSignIn and registered from iOSApp.init. */
interface GoogleSignInBridge {
    fun signIn(onSuccess: (GoogleSignInTokens) -> Unit, onError: (String) -> Unit)
    fun signOut()
}

object GoogleSignInBridgeHolder {
    var bridge: GoogleSignInBridge? = null
}

class IosAuthClient : AuthClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override val isSupported: Boolean
        get() = GoogleSignInBridgeHolder.bridge != null

    override val currentUser: StateFlow<AppUser?> =
        Firebase.auth.authStateChanged
            .map { it?.toAppUser() }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    override suspend fun signInWithGoogle(): AppUser? {
        val bridge = GoogleSignInBridgeHolder.bridge
            ?: throw IllegalStateException("GoogleSignInBridge not registered; call GoogleSignInBridgeHolder.shared.bridge from iOSApp.init")
        val tokens = bridge.awaitTokens()
        val credential = GoogleAuthProvider.credential(idToken = tokens.idToken, accessToken = tokens.accessToken)
        val result = Firebase.auth.signInWithCredential(credential)
        return result.user?.toAppUser()
    }

    override suspend fun signOut() {
        GoogleSignInBridgeHolder.bridge?.signOut()
        Firebase.auth.signOut()
    }

    private suspend fun GoogleSignInBridge.awaitTokens(): GoogleSignInTokens =
        suspendCancellableCoroutine { cont ->
            signIn(
                onSuccess = { cont.resume(it) },
                onError = { cont.resumeWith(Result.failure(RuntimeException(it))) },
            )
        }

    private fun FirebaseUser.toAppUser() = AppUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoURL,
    )
}
