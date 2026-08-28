package jiamin.chen.orangecloud.core.system

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import jiamin.chen.orangecloud.data.model.AccountUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** 外观模式（对应 iOS AppAppearance）。 */
enum class AppAppearance(val value: Int) {
    SYSTEM(0), LIGHT(1), DARK(2);

    companion object {
        fun from(value: Int): AppAppearance = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}

/** 资源列表排序（Workers / Pages 等通用，对应 iOS ResourceSort）。 */
enum class ResourceSort(val value: Int) {
    NAME(0),       // 默认：名称字母序（列表原有顺序）
    CREATED(1),    // 创建日期，新的在前
    MODIFIED(2);   // 最近更新，新的在前

    companion object {
        fun from(value: Int): ResourceSort = entries.firstOrNull { it.value == value } ?: NAME
    }
}

/**
 * Dashboard 用量套餐设置（对应 iOS AccountPrefs 的手动套餐兜底）。
 * OAuth token 无 billing scope，套餐/账单日靠用户手动设置，按账号存。
 */
data class UsagePlanPrefs(
    val workersPaid: Boolean = false,
    val r2Paid: Boolean = false,
    val billingDay: Int = 1,   // 1 = 自然月；否则按账单日划周期
)

/** App 偏好（外观 + 通知开关 + 列表排序 + 用量套餐），存共享 DataStore。 */
@Singleton
class AppPrefs @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val appearance: Flow<AppAppearance> = dataStore.data.map { AppAppearance.from(it[KEY_APPEARANCE] ?: 0) }
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_NOTIF_MASTER] ?: false }
    val notifyZoneStatus: Flow<Boolean> = dataStore.data.map { it[KEY_NOTIF_ZONE] ?: true }
    val notifyWorkerErrors: Flow<Boolean> = dataStore.data.map { it[KEY_NOTIF_WORKER] ?: true }

    suspend fun setAppearance(appearance: AppAppearance) {
        dataStore.edit { it[KEY_APPEARANCE] = appearance.value }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIF_MASTER] = enabled }
    }

    suspend fun setNotifyZoneStatus(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIF_ZONE] = enabled }
    }

    suspend fun setNotifyWorkerErrors(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIF_WORKER] = enabled }
    }

    /**
     * 某个登录身份下默认进入的 Cloudflare 账号（按身份分键）。
     * 用户切一次账号就记住它，下次冷启动直接进这个账号而不是列表第一个（issue #71）。
     */
    suspend fun defaultAccountId(sessionId: String): String? {
        if (sessionId.isEmpty()) return null
        return dataStore.data.first()[defaultAccountKey(sessionId)]?.takeIf { it.isNotEmpty() }
    }

    suspend fun setDefaultAccountId(sessionId: String, accountId: String) {
        if (sessionId.isEmpty() || accountId.isEmpty()) return
        dataStore.edit { it[defaultAccountKey(sessionId)] = accountId }
    }

    private fun defaultAccountKey(sessionId: String) = stringPreferencesKey("pref_default_account_$sessionId")

    /** 某个资源列表的排序偏好（key 如 "workers" / "pages"）。 */
    fun listSort(key: String): Flow<ResourceSort> =
        dataStore.data.map { ResourceSort.from(it[intPreferencesKey("pref_sort_$key")] ?: 0) }

    suspend fun setListSort(key: String, sort: ResourceSort) {
        dataStore.edit { it[intPreferencesKey("pref_sort_$key")] = sort.value }
    }

    /** 某账号的用量套餐设置（Workers/R2 付费 + 账单日）。 */
    fun usagePlan(accountId: String): Flow<UsagePlanPrefs> = dataStore.data.map { p ->
        UsagePlanPrefs(
            workersPaid = p[booleanPreferencesKey("pref_usage_w_paid_$accountId")] ?: false,
            r2Paid = p[booleanPreferencesKey("pref_usage_r2_paid_$accountId")] ?: false,
            billingDay = (p[intPreferencesKey("pref_usage_bday_$accountId")] ?: 1).coerceIn(1, 28),
        )
    }

    suspend fun setUsageWorkersPaid(accountId: String, paid: Boolean) {
        dataStore.edit { it[booleanPreferencesKey("pref_usage_w_paid_$accountId")] = paid }
    }

    suspend fun setUsageR2Paid(accountId: String, paid: Boolean) {
        dataStore.edit { it[booleanPreferencesKey("pref_usage_r2_paid_$accountId")] = paid }
    }

    suspend fun setUsageBillingDay(accountId: String, day: Int) {
        dataStore.edit { it[intPreferencesKey("pref_usage_bday_$accountId")] = day.coerceIn(1, 28) }
    }

    /**
     * Dashboard 置顶资源键集合（按账号分键——一身份多账号，账号级视图必须各自隔离）。
     * 值形如 `ZONE|abc123`，**只存类型与 id**，标题由 UI 现查，资源改名后置顶不失效。
     */
    fun pinnedResources(accountId: String): Flow<Set<String>> =
        dataStore.data.map { it[pinnedResourcesKey(accountId)] ?: emptySet() }

    /** 置顶 / 取消置顶（幂等切换）。 */
    suspend fun togglePinnedResource(accountId: String, key: String) {
        dataStore.edit { prefs ->
            val current = prefs[pinnedResourcesKey(accountId)] ?: emptySet()
            prefs[pinnedResourcesKey(accountId)] = if (key in current) current - key else current + key
        }
    }

    private fun pinnedResourcesKey(accountId: String) = stringSetPreferencesKey("pref_pinned_res_$accountId")

    /** 用量快照落盘（按账号，JSON），供下次冷启动/切回即时回显（对齐 iOS UsageCache）。 */
    suspend fun loadUsageCache(accountId: String): AccountUsage? {
        val raw = dataStore.data.first()[stringPreferencesKey("usage_cache_$accountId")] ?: return null
        return runCatching { usageJson.decodeFromString<AccountUsage>(raw) }.getOrNull()
    }

    suspend fun saveUsageCache(accountId: String, usage: AccountUsage) {
        val raw = runCatching { usageJson.encodeToString(usage) }.getOrNull() ?: return
        dataStore.edit { it[stringPreferencesKey("usage_cache_$accountId")] = raw }
    }

    private companion object {
        val usageJson = Json { ignoreUnknownKeys = true }

        val KEY_APPEARANCE = intPreferencesKey("pref_appearance")
        val KEY_NOTIF_MASTER = booleanPreferencesKey("pref_notif_master")
        val KEY_NOTIF_ZONE = booleanPreferencesKey("pref_notif_zone_status")
        val KEY_NOTIF_WORKER = booleanPreferencesKey("pref_notif_worker_errors")
    }
}
