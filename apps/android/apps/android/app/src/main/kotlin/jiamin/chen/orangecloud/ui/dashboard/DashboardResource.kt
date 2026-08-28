package jiamin.chen.orangecloud.ui.dashboard

/**
 * Dashboard 跨资源类型的统一条目（置顶 / 命令搜索 / 告警跳转共用）。
 *
 * 设计要点：**持久化只存 type + id**（见 [pinKey]），title / subtitle 是展示态，
 * 每次都从当前账号的资源目录现查。资源改名后置顶仍然有效，只是标题跟着变新名；
 * 目录里查不到（已删除 / 无权限 / 还没拉到）时用 id 兜底展示，并允许取消置顶。
 */
data class DashboardResource(
    val type: DashboardResourceType,
    val id: String,
    val title: String,
    val subtitle: String? = null,
    /** false = 目录里没查到，title 只是 id 兜底（UI 会弱化显示）。 */
    val resolved: Boolean = true,
) {
    /** 持久化键：`TYPE|id`（id 中的 `\` 与 `|` 已转义）。 */
    val pinKey: String get() = encodePinKey(type, id)
}

/** 可置顶 / 可搜索的六类资源。枚举名参与持久化键，**不要改名**。 */
enum class DashboardResourceType {
    ZONE,
    WORKER,
    R2_BUCKET,
    D1_DATABASE,
    KV_NAMESPACE,
    TUNNEL,
}

/** `TYPE|id`，id 内的 `\` 与 `|` 转义，保证解码时第一个裸 `|` 就是分隔符。 */
fun encodePinKey(type: DashboardResourceType, id: String): String = type.name + "|" + escapePinId(id)

/**
 * 解码持久化键为「未解析」占位条目（title = id）。
 * 键损坏 / 类型已不存在（老版本遗留）时返回 null，调用方直接忽略该键。
 */
fun decodePinKey(key: String): DashboardResource? {
    val sep = key.indexOf('|')
    if (sep <= 0) return null
    val type = DashboardResourceType.entries.firstOrNull { it.name == key.substring(0, sep) } ?: return null
    val id = unescapePinId(key.substring(sep + 1))
    if (id.isEmpty()) return null
    return DashboardResource(type = type, id = id, title = id, subtitle = null, resolved = false)
}

private fun escapePinId(raw: String): String = buildString(raw.length) {
    raw.forEach { c ->
        when (c) {
            '\\' -> append("\\\\")
            '|' -> append("\\|")
            else -> append(c)
        }
    }
}

private fun unescapePinId(raw: String): String = buildString(raw.length) {
    var i = 0
    while (i < raw.length) {
        val c = raw[i]
        if (c == '\\' && i + 1 < raw.length) {
            append(raw[i + 1])
            i += 2
        } else {
            append(c)
            i += 1
        }
    }
}

/** 命令搜索过滤（纯函数）：空查询给前 30 条，有查询最多 50 条，title/subtitle 不区分大小写包含匹配。 */
fun filterResources(all: List<DashboardResource>, query: String): List<DashboardResource> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return all.take(EMPTY_QUERY_LIMIT)
    return all.asSequence()
        .filter { it.title.lowercase().contains(q) || it.subtitle.orEmpty().lowercase().contains(q) }
        .take(QUERY_LIMIT)
        .toList()
}

private const val EMPTY_QUERY_LIMIT = 30
private const val QUERY_LIMIT = 50
