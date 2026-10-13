package com.bk.testsocialloginskmp.auth

import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuthException
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

actual val authClient: AuthClient by lazy { AndroidAuthClient() }

private const val TAG = "AndroidAuthClient"

private class AndroidAuthClient : AuthClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val isSupported: Boolean = true

    override val currentUser: StateFlow<AppUser?> =
        Firebase.auth.authStateChanged
            .map { it?.toAppUser() }
            .stateIn(scope, SharingStarted.WhileSubscribed(5_000), null)

    override suspend fun signInWithGoogle(): AppUser? {
        val activity = AndroidAuthHost.activity
        Log.i(TAG, "signInWithGoogle: started")
        logPlayServicesState()
        try {
            val serverClientId = serverClientId()
            Log.i(TAG, "signInWithGoogle: serverClientId=$serverClientId")

            val credentialManager = CredentialManager.create(activity)
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setAutoSelectEnabled(false)
                .setServerClientId(serverClientId)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            Log.i(TAG, "signInWithGoogle: calling CredentialManager.getCredential()")
            val response = credentialManager.getCredential(activity, request)
            val credential = response.credential
            Log.i(TAG, "signInWithGoogle: credential returned, type=${credential.type}")

            check(credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                "Unexpected credential type: ${credential.type}"
            }

            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            Log.i(TAG, "signInWithGoogle: Google ID token received (length=${idToken.length}), signing in to Firebase")

            val firebaseCredential = GoogleAuthProvider.credential(idToken, null)
            val result = Firebase.auth.signInWithCredential(firebaseCredential)
            Log.i(TAG, "signInWithGoogle: Firebase sign-in succeeded, uid=${result.user?.uid}")
            return result.user?.toAppUser()
        } catch (e: GetCredentialException) {
            Log.e(
                TAG,
                "signInWithGoogle: CredentialManager failed. type=${e.type} message=${e.message} cause=${e.cause}",
                e,
            )
            throw e
        } catch (e: FirebaseAuthException) {
            Log.e(TAG, "signInWithGoogle: Firebase sign-in failed. ${e::class.java.simpleName}: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "signInWithGoogle: unexpected failure: ${e::class.java.name}: ${e.message}", e)
            throw e
        }
    }

    override suspend fun signOut() {
        val activity = AndroidAuthHost.activity
        Log.i(TAG, "signOut: started")
        try {
            Firebase.auth.signOut()
            CredentialManager.create(activity).clearCredentialState(ClearCredentialStateRequest())
            Log.i(TAG, "signOut: done")
        } catch (e: Exception) {
            Log.e(TAG, "signOut: failed", e)
            throw e
        }
    }

    private fun logPlayServicesState() {
        val context = AndroidAuthHost.activity
        val availability = GoogleApiAvailability.getInstance()
        val status = availability.isGooglePlayServicesAvailable(context)
        if (status == ConnectionResult.SUCCESS) {
            Log.i(TAG, "Google Play services: available")
        } else {
            Log.e(TAG, "Google Play services: NOT available, code=$status (${availability.getErrorString(status)})")
        }
    }

    private fun serverClientId(): String {
        val context = AndroidAuthHost.activity
        val id = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
        if (id == 0) {
            Log.e(TAG, "serverClientId: default_web_client_id resource missing; google-services.json not applied or package mismatch")
        }
        check(id != 0) { "default_web_client_id resource not found; regenerate google-services.json" }
        return context.getString(id)
    }

    private fun FirebaseUser.toAppUser() = AppUser(
        uid = uid,
        displayName = displayName,
        email = email,
        photoUrl = photoURL,
    )
}
