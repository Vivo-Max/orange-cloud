package jiamin.chen.orangecloud.core.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Token 持久化：按身份 UUID 加密存 DataStore（Keystore 包裹密钥）。
 * 对应 iOS TokenStore，去掉 iCloud 同步 / Keychain 共享组（Glance 共享在 Phase F 另议）。
 */
@Singleton
class TokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
) {
    private fun key(sessionId: String) = stringPreferencesKey("token_$sessionId")

    suspend fun save(sessionId: String, token: StoredToken) {
        val encrypted = Crypto.encrypt(json.encodeToString(StoredToken.serializer(), token))
        dataStore.edit { it[key(sessionId)] = encrypted }
    }

    suspend fun load(sessionId: String): StoredToken? {
        val encoded = dataStore.data.firstOrNull()?.get(key(sessionId)) ?: return null
        val decoded = Crypto.decrypt(encoded) ?: return null
        return runCatching { json.decodeFromString(StoredToken.serializer(), decoded) }.getOrNull()
    }

    suspend fun clear(sessionId: String) {
        dataStore.edit { it.remove(key(sessionId)) }
    }
}
