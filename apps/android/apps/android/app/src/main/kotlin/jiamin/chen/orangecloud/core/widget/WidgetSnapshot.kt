package jiamin.chen.orangecloud.core.widget

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

/**
 * 桌面小组件快照（对应 iOS App Group 共享的 WidgetSnapshot）。
 * App 侧（Dashboard 算完账号总览后）写入，Glance 在 provideGlance 读取。
 * 同进程共享一个 Preferences DataStore，无需 App Group。
 */
private val Context.widgetDataStore by preferencesDataStore("widget_snapshot")

data class WidgetSnapshot(
    val accountName: String = "",
    val todayRequests: String = "—",
    val zoneCount: String = "—",
)

object WidgetSnapshotStore {
    private val KEY_ACCOUNT = stringPreferencesKey("account")
    private val KEY_REQUESTS = stringPreferencesKey("requests")
    private val KEY_ZONES = stringPreferencesKey("zones")

    suspend fun write(context: Context, snapshot: WidgetSnapshot) {
        context.widgetDataStore.edit { p ->
            p[KEY_ACCOUNT] = snapshot.accountName
            p[KEY_REQUESTS] = snapshot.todayRequests
            p[KEY_ZONES] = snapshot.zoneCount
        }
    }

    suspend fun read(context: Context): WidgetSnapshot {
        val p = context.widgetDataStore.data.first()
        return WidgetSnapshot(
            accountName = p[KEY_ACCOUNT].orEmpty(),
            todayRequests = p[KEY_REQUESTS] ?: "—",
            zoneCount = p[KEY_ZONES] ?: "—",
        )
    }
}
