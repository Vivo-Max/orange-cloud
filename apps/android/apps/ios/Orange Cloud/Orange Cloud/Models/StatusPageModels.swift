//
//  StatusPageModels.swift
//  Orange Cloud
//
//  Cloudflare 官方状态页（cloudflarestatus.com，Statuspage v2 API）。
//  公开接口无 CF 信封（没有 result/success 包装），日期为 ISO8601 字符串。
//

import Foundation

/// GET /api/v2/summary.json
nonisolated struct StatusPageSummary: Codable, Sendable {
    let status:                StatusPageOverall
    let components:            [StatusPageComponent]
    let incidents:             [StatusPageIncident]
    let scheduledMaintenances: [StatusPageIncident]

    enum CodingKeys: String, CodingKey {
        case status, components, incidents
        case scheduledMaintenances = "scheduled_maintenances"
    }
}

/// GET /api/v2/incidents.json（含已解决的历史事件，最近 50 条）
nonisolated struct StatusPageIncidentList: Codable, Sendable {
    let incidents: [StatusPageIncident]
}

/// 总体状态
nonisolated struct StatusPageOverall: Codable, Sendable {
    let indicator:   String   // "none" | "minor" | "major" | "critical" | "maintenance"
    let description: String

    /// 总体状态的本地化描述（未知 indicator 时透出官方英文原文）
    var localizedText: String {
        switch indicator {
        case "none":        String(localized: "所有系统正常运行")
        case "minor":       String(localized: "部分服务轻微异常")
        case "major":       String(localized: "部分服务严重异常")
        case "critical":    String(localized: "重大服务中断")
        case "maintenance": String(localized: "维护进行中")
        default:            description
        }
    }
}

nonisolated struct StatusPageComponent: Codable, Identifiable, Sendable {
    let id:      String
    let name:    String
    let status:  String   // "operational" | "degraded_performance" | "partial_outage" | "major_outage" | "under_maintenance"
    let group:   Bool?    // true = 分组容器（如各大洲 PoP 分组），本身不是服务
    let groupId: String?

    enum CodingKeys: String, CodingKey {
        case id, name, status, group
        case groupId = "group_id"
    }

    var statusText: String {
        switch status {
        case "operational":          String(localized: "正常")
        case "degraded_performance": String(localized: "性能下降")
        case "partial_outage":       String(localized: "部分中断")
        case "major_outage":         String(localized: "大面积中断")
        case "under_maintenance":    String(localized: "维护中")
        default:                     status
        }
    }
}

/// 边缘网络大区汇总（由 ViewModel 按分组聚合，非 API 原始结构）
nonisolated struct StatusPageRegion: Identifiable, Sendable {
    let id:       String
    let name:     String   // API 原文（英文）
    let total:    Int
    let impacted: Int      // 非 operational 的节点数

    var localizedName: String {
        switch name {
        case "Africa":                        String(localized: "非洲")
        case "Asia":                          String(localized: "亚洲")
        case "Europe":                        String(localized: "欧洲")
        case "Latin America & the Caribbean": String(localized: "拉丁美洲和加勒比")
        case "Middle East":                   String(localized: "中东")
        case "North America":                 String(localized: "北美")
        case "Oceania":                       String(localized: "大洋洲")
        default:                              name
        }
    }
}

/// 事件与计划维护共用结构（维护多 scheduled_for 字段）
nonisolated struct StatusPageIncident: Codable, Identifiable, Sendable {
    let id:              String
    let name:            String
    let status:          String   // 事件 "investigating"… / 维护 "scheduled"…
    let impact:          String   // "none" | "minor" | "major" | "critical" | "maintenance"
    let createdAt:       String?
    let updatedAt:       String?
    let scheduledFor:    String?
    let shortlink:       String?
    let incidentUpdates: [StatusPageIncidentUpdate]?

    enum CodingKeys: String, CodingKey {
        case id, name, status, impact, shortlink
        case createdAt       = "created_at"
        case updatedAt       = "updated_at"
        case scheduledFor    = "scheduled_for"
        case incidentUpdates = "incident_updates"
    }

    static func statusText(_ status: String) -> String {
        switch status {
        case "investigating": String(localized: "调查中")
        case "identified":    String(localized: "已定位")
        case "monitoring":    String(localized: "监控中")
        case "resolved":      String(localized: "已解决")
        case "postmortem":    String(localized: "事后分析")
        case "scheduled":     String(localized: "已排期")
        case "in_progress":   String(localized: "进行中")
        case "verifying":     String(localized: "验证中")
        case "completed":     String(localized: "已完成")
        default:              status
        }
    }

    var statusText: String { Self.statusText(status) }

    var impactText: String {
        switch impact {
        case "critical":    String(localized: "重大")
        case "major":       String(localized: "严重")
        case "minor":       String(localized: "轻微")
        case "maintenance": String(localized: "维护")
        case "none":        String(localized: "无影响")
        default:            impact
        }
    }
}

nonisolated struct StatusPageIncidentUpdate: Codable, Identifiable, Sendable {
    let id:        String
    let status:    String
    let body:      String
    let displayAt: String?

    enum CodingKeys: String, CodingKey {
        case id, status, body
        case displayAt = "display_at"
    }

    var statusText: String { StatusPageIncident.statusText(status) }
}
