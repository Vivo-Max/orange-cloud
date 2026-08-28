//
//  DashboardAlerts.swift
//  Orange Cloud
//
//  概览页告警中心的规则集：只用**已经在手上的数据**推导（域名缓存 / Tunnel 列表 / Workers 缓存），
//  不额外发任何请求。纯函数，方便后续补测试。
//

import Foundation
import SwiftData

/// 一条告警。`route` 为空表示无处可跳（如「还没有部署 Worker」）。
struct DashboardAlert: Identifiable, Hashable {

    /// 严重度（rawValue 越小越靠前；排序统一比 rawValue，不额外做 Comparable conformance）
    enum Severity: Int, Hashable {
        case critical = 0
        case warn     = 1
        case info     = 2
        case ok       = 3
    }

    let id: String
    let severity: Severity
    let title: String
    let detail: String
    let route: DashboardResourceRoute?
}

enum DashboardAlerts {

    /// 最多返回 `limit` 条，按严重度排序（同级保持输入顺序）。全部正常时返回空数组，由卡片显示「暂无告警」。
    static func build(
        zones: [CachedZone],
        tunnels: [Tunnel],
        workerCount: Int?,
        limit: Int = 5
    ) -> [DashboardAlert] {
        var alerts: [DashboardAlert] = []

        // ① 未激活 / 已暂停的域名（暂停是用户主动操作，单独给一条可读的说明）
        for zone in zones where zone.displayStatus != "active" {
            let pendingLike = zone.status == "pending" || zone.status == "initializing"
            let detail: String
            if zone.paused {
                detail = String(localized: "已暂停 Cloudflare，流量当前不经过代理")
            } else if pendingLike {
                detail = String(localized: "域名待激活，检查域名服务器是否已指向 Cloudflare")
            } else {
                detail = String(localized: "域名未处于启用状态（\(zone.status)）")
            }
            alerts.append(DashboardAlert(
                id: "zone|\(zone.id)",
                severity: pendingLike ? .warn : .critical,
                title: zone.name,
                detail: detail,
                route: .zone(zone)
            ))
        }

        // ② 非 healthy 的 Tunnel
        for tunnel in tunnels where (tunnel.status ?? "") != "healthy" {
            let severity: DashboardAlert.Severity
            switch tunnel.status {
            case "down":     severity = .critical
            case "degraded": severity = .warn
            default:         severity = .info      // inactive / 未知
            }
            alerts.append(DashboardAlert(
                id: "tunnel|\(tunnel.id)",
                severity: severity,
                title: tunnel.name,
                detail: String(localized: "隧道状态：\(tunnel.statusText)"),
                route: .tunnel(tunnel)
            ))
        }

        // ③ 一个 Worker 都没有（有权限读取时才提示，nil = 未知/无权限）
        if let workerCount, workerCount == 0 {
            alerts.append(DashboardAlert(
                id: "workers|empty",
                severity: .info,
                title: String(localized: "还没有部署 Worker"),
                detail: String(localized: "当前账号下没有 Workers 脚本"),
                route: nil
            ))
        }

        return Array(
            alerts
                .enumerated()
                .sorted { lhs, rhs in
                    lhs.element.severity == rhs.element.severity
                        ? lhs.offset < rhs.offset
                        : lhs.element.severity.rawValue < rhs.element.severity.rawValue
                }
                .map(\.element)
                .prefix(limit)
        )
    }
}
