package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.SanaDatabase
import com.example.data.repository.LocalChatRepository
import com.example.data.repository.SanaAiRepository
import com.example.data.repository.SanaFirestoreRepository
import com.example.data.repository.UserPreferencesRepository
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.ui.SanaAppShell
import com.example.ui.SanaViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.SanaTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent before any UI or ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                SanaTheme {
                    SanaAppRoot()
                }
            }
        }
    }
}

@Composable
fun SanaAppRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { Firebase.auth }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val userPrefsRepo = remember(context) { UserPreferencesRepository(context) }

    val hasCompletedOnboarding by userPrefsRepo.hasCompletedOnboarding
        .collectAsStateWithLifecycle(initialValue = null)
    var showOnboardingReplay by remember { mutableStateOf(false) }

    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)

    var autoSignInChecked by remember { mutableStateOf(auth.currentUser != null) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { autoSignInChecked = true },
                onUnauthenticated = { autoSignInChecked = true },
                scope = this
            )
        }
    }

    val user = currentUser
    val onboardingDone = hasCompletedOnboarding
    if (onboardingDone == null || (!autoSignInChecked && user == null)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (!onboardingDone || showOnboardingReplay) {
        OnboardingScreen(
            onFinish = {
                showOnboardingReplay = false
                scope.launch {
                    userPrefsRepo.setOnboardingCompleted(true)
                }
            }
        )
    } else if (user == null) {
        AuthScreen(onAuthSuccess = { })
    } else {
        val uid = user.uid
        key(uid) {
            val viewModel: SanaViewModel = viewModel(
                key = uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        val sanaDb = SanaDatabase.getInstance(app)
                        val localChatRepo = LocalChatRepository(
                            chatMessageDao = sanaDb.chatMessageDao(),
                            conversationDao = sanaDb.chatConversationDao()
                        )
                        SanaViewModel(
                            firestoreRepo = SanaFirestoreRepository(db = db, auth = auth),
                            aiRepo = SanaAiRepository(),
                            localChatRepo = localChatRepo,
                            currentUserId = uid,
                            userPreferencesRepo = userPrefsRepo
                        )
                    }
                }
            )
            SanaAppShell(
                viewModel = viewModel,
                userDisplayName = user.displayName ?: user.email,
                onSignedOut = { },
                onReplayOnboarding = { showOnboardingReplay = true }
            )
        }
    }
}
