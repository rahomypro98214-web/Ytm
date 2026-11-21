package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.DEFAULT_CONVERSATION_ID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.sanaUserPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sana_user_preferences",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.sanaUserPreferencesDataStore)

    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is CancellationException) throw exception
            emit(emptyPreferences())
        }
        .map { prefs ->
            prefs[ONBOARDING_COMPLETED_KEY] ?: false
        }

    suspend fun setOnboardingCompleted(completed: Boolean = true) {
        dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED_KEY] = completed
        }
    }

    val preferredName: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is CancellationException) throw exception
            emit(emptyPreferences())
        }
        .map { prefs ->
            prefs[PREFERRED_NAME_KEY]
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        }

    fun activeConversationId(userId: String): Flow<String> {
        val key = activeConversationKey(userId)
        return dataStore.data
            .catch { exception ->
                if (exception is CancellationException) throw exception
                emit(emptyPreferences())
            }
            .map { prefs ->
                prefs[key]?.takeIf { it.isNotBlank() } ?: DEFAULT_CONVERSATION_ID
            }
    }

    suspend fun setActiveConversationId(userId: String, conversationId: String) {
        val key = activeConversationKey(userId)
        dataStore.edit { prefs ->
            prefs[key] = conversationId.ifBlank { DEFAULT_CONVERSATION_ID }
        }
    }

    suspend fun setPreferredName(rawName: String) {
        val cleaned = rawName.trim().take(MAX_PREFERRED_NAME_LENGTH)
        dataStore.edit { prefs ->
            if (cleaned.isEmpty()) {
                prefs.remove(PREFERRED_NAME_KEY)
            } else {
                prefs[PREFERRED_NAME_KEY] = cleaned
            }
        }
    }

    suspend fun clearPreferredName() {
        dataStore.edit { prefs ->
            prefs.remove(PREFERRED_NAME_KEY)
        }
    }

    private fun activeConversationKey(userId: String): Preferences.Key<String> =
        stringPreferencesKey("active_conversation_id_${userId.ifBlank { "local" }}")

    companion object {
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        val PREFERRED_NAME_KEY = stringPreferencesKey("preferred_name")
        const val MAX_PREFERRED_NAME_LENGTH = 40
    }
}
