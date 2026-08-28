//
//  HealthCheckModels.swift
//  Orange Cloud
//
//  独立健康检查（Standalone Health Checks）。/zones/{zone_id}/healthchecks
//
//  与负载均衡的 monitor 是两回事：LB monitor 只在负载均衡语境里生效，
//  这里是独立监控单个 IP / 主机名，单源站也能用。别把两者混进同一个页面。
//
//  套餐：免费版不可用（0 个），Pro 10 / Business 50 / Enterprise 1000。
//

import Foundation

nonisolated struct HealthCheck: Codable, Identifiable, Hashable, Sendable {
    let id:            String
    let name:          String
    /// 被监控的源站主机名或 IP
    let address:       String?
    let description:   String?
    /// HTTP / HTTPS / TCP
    let type:          String?
    /// healthy / unhealthy / unknown / suspended
    let status:        String?
    /// status 为 unhealthy 时的失败原因
    let failureReason: String?
    /// 暂停后不再向源站发送检查
    let suspended:     Bool?
    /// 检查间隔（秒）
    let interval:      Int?
    let timeout:       Int?
    let retries:       Int?
    let consecutiveFails:     Int?
    let consecutiveSuccesses: Int?
    /// 发起检查的区域；nil 表示由 Cloudflare 自选
    let checkRegions:  [String]?
    let createdOn:     String?
    let modifiedOn:    String?

    enum CodingKeys: String, CodingKey {
        case id, name, address, description, type, status, suspended, interval, timeout, retries
        case failureReason         = "failure_reason"
        case consecutiveFails      = "consecutive_fails"
        case consecutiveSuccesses  = "consecutive_successes"
        case checkRegions          = "check_regions"
        case createdOn             = "created_on"
        case modifiedOn            = "modified_on"
    }

    /// 暂停优先于 status——暂停时 CF 不再探测，status 会停在最后一次结果上，直接显示会误导。
    var displayStatus: HealthCheckStatus {
        if suspended == true { return .suspended }
        switch status {
        case "healthy":   return .healthy
        case "unhealthy": return .unhealthy
        default:          return .unknown
        }
    }
}

nonisolated enum HealthCheckStatus: Sendable {
    case healthy, unhealthy, suspended, unknown

    var label: String {
        switch self {
        case .healthy:   String(localized: "正常")
        case .unhealthy: String(localized: "异常")
        case .suspended: String(localized: "已暂停")
        case .unknown:   String(localized: "未知")
        }
    }
}

/// PATCH 体：只发要改的字段（暂停 / 恢复）
nonisolated struct HealthCheckSuspendUpdate: Codable, Sendable {
    let suspended: Bool
}
