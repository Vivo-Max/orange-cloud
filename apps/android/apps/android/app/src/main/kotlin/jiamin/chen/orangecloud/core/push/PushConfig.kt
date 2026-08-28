package jiamin.chen.orangecloud.core.push

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 官方中继默认端点（可在设置里改成自托管）。 */
const val DEFAULT_PUSH_SERVER = "https://push.90dd.adsl8.workers.dev"

private val Context.pushDataStore by preferencesDataStore("orange_cloud_push")

private object PushKeys {
    val SERVER = stringPreferencesKey("server_url")
    val DEVICE_KEY = stringPreferencesKey("device_key")
    val ENABLED = booleanPreferencesKey("enabled")
}

/** 推送配置：server URL（默认 push.90dd.adsl8.workers.dev）/ device_key（端点凭证）/ 是否启用。device_key 非机密，存普通 DataStore。 */
@Singleton
class PushPrefs @Inject constructor(@ApplicationContext private val context: Context) {

    val serverUrl: Flow<String> = context.pushDataStore.data.map { it[PushKeys.SERVER] ?: DEFAULT_PUSH_SERVER }
    val deviceKey: Flow<String?> = context.pushDataStore.data.map { it[PushKeys.DEVICE_KEY] }
    val enabled: Flow<Boolean> = context.pushDataStore.data.map { it[PushKeys.ENABLED] ?: false }

    suspend fun serverUrlNow(): String = serverUrl.first()
    suspend fun deviceKeyNow(): String? = deviceKey.first()
    suspend fun enabledNow(): Boolean = enabled.first()

    suspend fun setServer(url: String) {
        context.pushDataStore.edit { it[PushKeys.SERVER] = url.trim().trimEnd('/') }
    }

    suspend fun setDeviceKey(key: String) {
        context.pushDataStore.edit { it[PushKeys.DEVICE_KEY] = key }
    }

    suspend fun setEnabled(on: Boolean) {
        context.pushDataStore.edit { it[PushKeys.ENABLED] = on }
    }

    suspend fun reset() {
        context.pushDataStore.edit { it.remove(PushKeys.DEVICE_KEY); it[PushKeys.ENABLED] = false }
    }
}
