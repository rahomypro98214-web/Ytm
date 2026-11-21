package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.DEFAULT_CONVERSATION_ID
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.sanaUserPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sana_user_preferences"
)

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.sanaUserPreferencesDataStore)

    val preferredName: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
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
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
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
        val PREFERRED_NAME_KEY = stringPreferencesKey("preferred_name")
        const val MAX_PREFERRED_NAME_LENGTH = 40
    }
}
