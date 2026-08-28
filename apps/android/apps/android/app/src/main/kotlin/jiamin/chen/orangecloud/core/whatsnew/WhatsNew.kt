package jiamin.chen.orangecloud.core.whatsnew

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import jiamin.chen.orangecloud.BuildConfig
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** 单条「新功能」条目（title/detail 为 strings.xml 资源 id）。 */
data class WhatsNewItem(val titleRes: Int, val detailRes: Int)

/** 一个版本的「新功能」集合。version 须 == MARKETING_VERSION 基号。 */
data class WhatsNewRelease(val version: String, val items: List<WhatsNewItem>)

/**
 * 按版本 curated 的「新功能」内容（对应 iOS WhatsNewContent.releases）。
 * ⚠️ 内容是单一数据源：改 packages/changelog/android.json 后运行 `pnpm changelog:gen`，
 *    会重新生成 WhatsNewReleases.generated.kt 与各 locale 的 whatsnew.xml 资源（勿手改）。
 */
object WhatsNewContent {
    val releases: List<WhatsNewRelease> = whatsNewReleases

    fun releaseFor(version: String): WhatsNewRelease? = releases.firstOrNull { it.version == version }
}

/**
 * 「新功能」弹窗触发判定（对应 iOS WhatsNewModifier）：
 * 全新安装静默（记下当前版本不弹）；老用户升级且该版本有 curated 内容，补看一次。
 */
@Singleton
class WhatsNewManager @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    /** 当前版本号基号（去掉 -oss 等 versionNameSuffix）。 */
    private val currentVersion: String = BuildConfig.VERSION_NAME.substringBefore("-")

    /** 返回需要补看的 release；无则 null。会顺带把 lastSeen 推进到当前版本。 */
    suspend fun pendingRelease(): WhatsNewRelease? {
        val prefs = dataStore.data.firstOrNull()
        val lastSeen = prefs?.get(KEY_LAST_SEEN)
        markSeen()
        if (lastSeen == null) return null                  // 全新安装：静默
        if (lastSeen == currentVersion) return null
        return WhatsNewContent.releaseFor(currentVersion)  // 升级且有内容才弹
    }

    private suspend fun markSeen() {
        dataStore.edit { it[KEY_LAST_SEEN] = currentVersion }
    }

    private companion object {
        val KEY_LAST_SEEN = stringPreferencesKey("whatsnew_last_seen_version")
    }
}
