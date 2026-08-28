package jiamin.chen.orangecloud.ui.dashboard

import jiamin.chen.orangecloud.data.model.Tunnel
import jiamin.chen.orangecloud.data.model.Zone

/** 告警级别（配色见 DashboardHubUi 的 AlertCenterCard）。 */
enum class AlertSeverity { CRITICAL, WARN, INFO, OK }

/**
 * 告警类型。文案不在这里落地（纯逻辑层不碰 Context/资源），
 * 由 UI 按 kind 取 string 资源、把 [DashboardAlert.name] / [DashboardAlert.detail] 填进去。
 */
enum class AlertKind {
    ZONE_INACTIVE,     // 域名未激活（status != active）
    TUNNEL_UNHEALTHY,  // 隧道状态非 healthy
    NO_WORKERS,        // 账号下一个 Worker 都没有
    ALL_CLEAR,         // 全部正常
}

/** 一条告警。target 非空即可点击跳转到对应资源。 */
data class DashboardAlert(
    val kind: AlertKind,
    val severity: AlertSeverity,
    val name: String = "",
    val detail: String = "",
    val target: DashboardResource? = null,
) {
    /** 列表 key（同类同资源唯一）。 */
    val id: String get() = kind.name + ":" + (target?.pinKey ?: name)
}

/**
 * 告警输入。**null 一律表示「还没拉到 / 无权限」，不产生告警**——
 * 避免冷启动空缓存瞬间误报（例如 workerCount 还没刷新就喊「没有 Worker」）。
 */
data class AlertInput(
    val zones: List<Zone> = emptyList(),
    val tunnels: List<Tunnel>? = null,
    val workerCount: Int? = null,
)

const val MAX_ALERTS = 5

/**
 * 构造告警列表（纯函数，方便加测试）。
 * 规则只用已有数据，不引入任何额外 API 调用：
 *  - 域名 status != active → WARN
 *  - 隧道 status != healthy（down → CRITICAL，其余 → WARN）
 *  - Worker 数为 0 → INFO
 * 全部正常时返回单条 ALL_CLEAR。按 严重度 → 类型 排序后截断到 [MAX_ALERTS]。
 */
fun buildAlerts(input: AlertInput, limit: Int = MAX_ALERTS): List<DashboardAlert> {
    val alerts = mutableListOf<DashboardAlert>()

    input.zones.filter { !it.isActive }.forEach { zone ->
        alerts += DashboardAlert(
            kind = AlertKind.ZONE_INACTIVE,
            severity = AlertSeverity.WARN,
            name = zone.name,
            detail = zone.displayStatus,
            target = DashboardResource(DashboardResourceType.ZONE, zone.id, zone.name, zone.displayStatus),
        )
    }

    input.tunnels?.filter { !it.status.equals("healthy", ignoreCase = true) }?.forEach { tunnel ->
        val status = tunnel.status.orEmpty()
        alerts += DashboardAlert(
            kind = AlertKind.TUNNEL_UNHEALTHY,
            severity = if (status.equals("down", ignoreCase = true)) AlertSeverity.CRITICAL else AlertSeverity.WARN,
            name = tunnel.name,
            detail = status,
            target = DashboardResource(DashboardResourceType.TUNNEL, tunnel.id, tunnel.name, status),
        )
    }

    if (input.workerCount == 0) {
        alerts += DashboardAlert(kind = AlertKind.NO_WORKERS, severity = AlertSeverity.INFO)
    }

    if (alerts.isEmpty()) return listOf(DashboardAlert(kind = AlertKind.ALL_CLEAR, severity = AlertSeverity.OK))

    return alerts
        .sortedWith(
            compareBy<DashboardAlert> { it.severity.ordinal }
                .thenBy { it.kind.ordinal }
                .thenBy { it.name },
        )
        .take(limit)
}
