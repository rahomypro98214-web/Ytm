package com.example.firebase

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val WEB_CLIENT_ID =
    "1025768180782-24ie3jkoh5n4lbv1pv6sjb4b0fbl6mf3.apps.googleusercontent.com"

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@SuppressLint("DiscouragedApi")
internal fun resolveWebClientId(context: Context): String {
    val resId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )
    val fromRes = if (resId != 0) runCatching { context.getString(resId) }.getOrNull() else null
    return fromRes?.takeIf { it.isNotBlank() } ?: WEB_CLIENT_ID
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

private suspend fun signInFirebaseWithCredential(credential: Credential): FirebaseUser {
    if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val result = Firebase.auth.signInWithCredential(
            GoogleAuthProvider.getCredential(token, null)
        ).await()
        return result.user
            ?: Firebase.auth.currentUser
            ?: throw IllegalStateException("تعذر إتمام الجلسة بعد تسجيل الدخول.")
    } else {
        throw IllegalArgumentException("نوع بيانات الاعتماد غير متوقع.")
    }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = resolveWebClientId(context)
    val requestContext = context.findActivity() ?: context
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val response = withTimeoutOrNull(2500L) {
                credentialManager.getCredential(requestContext, request)
            }
            val cred = response?.credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                signInFirebaseWithCredential(cred)
                withContext(Dispatchers.Main) {
                    onAuthSuccess()
                }
            } else {
                withContext(Dispatchers.Main) {
                    onUnauthenticated()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (oom: OutOfMemoryError) {
            throw oom
        } catch (t: Throwable) {
            Log.w("Auth", "Auto sign-in skipped: ${t.message}")
            withContext(Dispatchers.Main) {
                onUnauthenticated()
            }
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val activity = context.findActivity()
    if (activity == null) {
        onAuthError("تعذر بدء تسجيل الدخول من هذا السياق.")
        return
    }
    val clientId = resolveWebClientId(activity)
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val primaryRequest = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    val fallbackGoogleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(false)
        .setServerClientId(clientId)
        .build()
    val fallbackRequest = GetCredentialRequest.Builder()
        .addCredentialOption(fallbackGoogleIdOption)
        .build()

    scope.launch {
        try {
            val cred = try {
                credentialManager.getCredential(activity, primaryRequest).credential
            } catch (cancelled: GetCredentialCancellationException) {
                throw cancelled
            } catch (primaryError: GetCredentialException) {
                Log.w(
                    "Auth",
                    "Primary Google Sign-In option failed (${primaryError::class.simpleName}); retrying with GetGoogleIdOption",
                    primaryError
                )
                credentialManager.getCredential(activity, fallbackRequest).credential
            }

            signInFirebaseWithCredential(cred)
            withContext(Dispatchers.Main) {
                onAuthSuccess()
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            withContext(Dispatchers.Main) {
                onAuthCancelled()
            }
        } catch (e: NoCredentialException) {
            Log.w("Auth", "No Google credentials available on device", e)
            withContext(Dispatchers.Main) {
                onAuthError("لا يوجد حساب Google مسجّل أو متاح على هذا الجهاز حاليًا.")
            }
        } catch (e: GetCredentialUnsupportedException) {
            Log.w("Auth", "Credential Manager unsupported on device", e)
            withContext(Dispatchers.Main) {
                onAuthError("خدمة تسجيل الدخول بحساب Google غير مدعومة على هذا الجهاز.")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (oom: OutOfMemoryError) {
            throw oom
        } catch (t: Throwable) {
            Log.e("Auth", "Google Sign-In failed", t)
            withContext(Dispatchers.Main) {
                onAuthError(t.localizedMessage ?: "فشل تسجيل الدخول بحساب Google.")
            }
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                withContext(Dispatchers.Main) {
                    onSignOutComplete()
                }
            }
        }
    }
}
