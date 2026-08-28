package jiamin.chen.orangecloud.core.push

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.buildWatchStore by preferencesDataStore("orange_cloud_build_watch")

/** 「盯哪些 Worker 的构建」与「已通知过的 build_uuid」。均为本地偏好，不入云。 */
@Singleton
class BuildWatchStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val watchedKey = stringSetPreferencesKey("watchedBuildScripts")
    private val notifiedKey = stringSetPreferencesKey("notifiedBuildUuids")

    val watched = context.buildWatchStore.data.map { it[watchedKey] ?: emptySet() }

    suspend fun watchedNow(): Set<String> = watched.first()

    suspend fun isWatching(script: String): Boolean = watchedNow().contains(script)

    suspend fun setWatching(script: String, on: Boolean) {
        context.buildWatchStore.edit { prefs ->
            val current = prefs[watchedKey] ?: emptySet()
            prefs[watchedKey] = if (on) current + script else current - script
        }
    }

    suspend fun notifiedNow(): Set<String> =
        context.buildWatchStore.data.map { it[notifiedKey] ?: emptySet() }.first()

    suspend fun setNotified(uuids: List<String>) {
        context.buildWatchStore.edit { it[notifiedKey] = uuids.toSet() }
    }
}
