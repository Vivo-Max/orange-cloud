//
//  CFAlertingModels.swift
//  Orange Cloud
//
//  Cloudflare Alerting / Notifications（account 级，/alerting/v3/*）。
//  字段按 v3 API 实测可能微调——读侧字段尽量可选，容错解码。
//

import Foundation

// MARK: - 可用告警类型（available_alerts：result 是 {分类: [告警]} ）

nonisolated struct CFAvailableAlert: Codable, Identifiable, Sendable {
    var id: String { type }
    let type: String
    let displayName: String?
    let name: String?
    let description: String?

    var label: String { displayName ?? name ?? type }

    enum CodingKeys: String, CodingKey {
        case type
        case displayName = "display_name"
        case name
        case description
    }
}

// MARK: - Webhook 投递目标

nonisolated struct CFWebhookDestination: Codable, Identifiable, Sendable {
    let id: String
    let name: String?
    let url: String?
    let type: String?
}

nonisolated struct CFWebhookCreate: Codable, Sendable {
    let name: String
    let url: String
}

// MARK: - 告警策略

nonisolated struct CFMechanismRef: Codable, Sendable {
    let id: String
}

nonisolated struct CFAlertMechanisms: Codable, Sendable {
    let webhooks: [CFMechanismRef]?
    let email: [CFMechanismRef]?
}

nonisolated struct CFAlertPolicy: Codable, Identifiable, Sendable {
    let id: String
    let name: String?
    let alertType: String?
    let enabled: Bool?
    let mechanisms: CFAlertMechanisms?

    enum CodingKeys: String, CodingKey {
        case id, name, enabled, mechanisms
        case alertType = "alert_type"
    }
}

nonisolated struct CFAlertPolicyCreate: Codable, Sendable {
    let name: String
    let alertType: String
    let enabled: Bool
    let mechanisms: CFAlertMechanisms
    let filters: [String: [String]]   // 空 {} = 不限定

    enum CodingKeys: String, CodingKey {
        case name, enabled, mechanisms, filters
        case alertType = "alert_type"
    }
}

/// 创建类接口的返回（{ id }）
nonisolated struct CFAlertingIDResult: Codable, Sendable {
    let id: String
}
