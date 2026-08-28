package jiamin.chen.orangecloud.ui.storage

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/** 收藏切换结果（用于向用户解释为什么没收藏上）。 */
enum class D1FavoriteResult { ADDED, REMOVED, TOO_LONG, FULL }

/**
 * D1 查询历史 / SQL 收藏的落盘（**按 databaseId 分键**，进程被杀不丢）。
 *
 * 复用全局那一个 Preferences DataStore（与 AppPrefs 同实例，由 AppModule 提供），
 * 键统一带 `d1q_` 前缀避免与其它模块撞车。为防 DataStore 膨胀：
 * 单条 SQL 超 [MAX_SQL_CHARS] 不落盘；历史上限 [MAX_HISTORY] 条、收藏上限 [MAX_FAVORITES] 条。
 */
@Singleton
class D1QueryPrefs @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    /** 最近成功执行的 SQL，新的在前。 */
    fun history(databaseId: String): Flow<List<String>> =
        dataStore.data.map { decode(it[historyKey(databaseId)]) }

    /** 收藏的 SQL，新的在前。 */
    fun favorites(databaseId: String): Flow<List<String>> =
        dataStore.data.map { decode(it[favoritesKey(databaseId)]) }

    /** 记一条历史：去重（同句提到最前）、超长丢弃、超上限截尾。 */
    suspend fun addHistory(databaseId: String, sql: String) {
        val entry = sql.trim()
        if (entry.isEmpty() || entry.length > MAX_SQL_CHARS) return
        dataStore.edit { prefs ->
            val key = historyKey(databaseId)
            val next = (listOf(entry) + decode(prefs[key]).filterNot { it == entry }).take(MAX_HISTORY)
            prefs[key] = encode(next)
        }
    }

    /** 收藏 / 取消收藏。超长或已满时不改动，返回原因供 UI 提示。 */
    suspend fun toggleFavorite(databaseId: String, sql: String): D1FavoriteResult {
        val entry = sql.trim()
        if (entry.isEmpty()) return D1FavoriteResult.TOO_LONG
        var result = D1FavoriteResult.ADDED
        dataStore.edit { prefs ->
            val key = favoritesKey(databaseId)
            val current = decode(prefs[key])
            when {
                current.contains(entry) -> {
                    prefs[key] = encode(current.filterNot { it == entry })
                    result = D1FavoriteResult.REMOVED
                }

                entry.length > MAX_SQL_CHARS -> result = D1FavoriteResult.TOO_LONG

                current.size >= MAX_FAVORITES -> result = D1FavoriteResult.FULL

                else -> {
                    prefs[key] = encode(listOf(entry) + current)
                    result = D1FavoriteResult.ADDED
                }
            }
        }
        return result
    }

    private fun decode(raw: String?): List<String> {
        if (raw.isNullOrEmpty()) return emptyList()
        return runCatching { json.decodeFromString(listSerializer, raw) }.getOrDefault(emptyList())
    }

    private fun encode(items: List<String>): String = json.encodeToString(listSerializer, items)

    private fun historyKey(databaseId: String) = stringPreferencesKey("d1q_history_$databaseId")

    private fun favoritesKey(databaseId: String) = stringPreferencesKey("d1q_favorites_$databaseId")

    companion object {
        /** 历史条数上限。 */
        const val MAX_HISTORY = 12

        /** 收藏条数上限。 */
        const val MAX_FAVORITES = 20

        /** 单条 SQL 落盘的字符上限，超出不记（截断会产生不可执行的残句）。 */
        const val MAX_SQL_CHARS = 4000

        private val json = Json { ignoreUnknownKeys = true }
        private val listSerializer = ListSerializer(String.serializer())
    }
}
