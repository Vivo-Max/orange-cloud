//
//  FirewallAccessRuleModels.swift
//  Orange Cloud
//
//  IP 访问规则（legacy firewall access rules）CRUD。
//  /zones/{id}/firewall/access_rules/rules，读 firewall-services.read，写 .write。
//  注意：configuration（target+value）创建后不可改，编辑仅改 mode + notes。
//

import Foundation

nonisolated struct FirewallAccessRule: Codable, Identifiable, Sendable {
    let id:            String
    let mode:          String?
    let configuration: AccessRuleConfig?
    let notes:         String?

    var modeLabel: String {
        switch mode {
        case "block":             String(localized: "拦截")
        case "challenge":         String(localized: "质询")
        case "js_challenge":      String(localized: "JS 质询")
        case "managed_challenge": String(localized: "托管质询")
        case "whitelist":         String(localized: "允许")
        default:                  mode ?? "—"
        }
    }
}

nonisolated struct AccessRuleConfig: Codable, Sendable {
    let target: String?
    let value:  String?

    var targetLabel: String {
        switch target {
        case "ip":       "IP"
        case "ip6":      "IPv6"
        case "ip_range": String(localized: "IP 段")
        case "asn":      "ASN"
        case "country":  String(localized: "国家/地区")
        default:         target ?? "—"
        }
    }
}

// MARK: - 写入载荷

nonisolated struct AccessRuleConfigInput: Codable, Sendable {
    let target: String
    let value:  String
}

nonisolated struct AccessRuleCreate: Codable, Sendable {
    let mode:          String
    let configuration: AccessRuleConfigInput
    let notes:         String?
}

/// 编辑只动 mode + notes（configuration 不可变）
nonisolated struct AccessRuleUpdate: Codable, Sendable {
    let mode:  String
    let notes: String?
}

// MARK: - 编辑器选项

nonisolated enum AccessRuleMode: String, CaseIterable, Identifiable, Sendable {
    case block
    case managedChallenge = "managed_challenge"
    case jsChallenge      = "js_challenge"
    case challenge
    case whitelist

    var id: String { rawValue }

    var label: String {
        switch self {
        case .block:            String(localized: "拦截")
        case .managedChallenge: String(localized: "托管质询")
        case .jsChallenge:      String(localized: "JS 质询")
        case .challenge:        String(localized: "质询")
        case .whitelist:        String(localized: "允许")
        }
    }
}

nonisolated enum AccessRuleTarget: String, CaseIterable, Identifiable, Sendable {
    case ip
    case ip6
    case ipRange = "ip_range"
    case asn
    case country

    var id: String { rawValue }

    var label: String {
        switch self {
        case .ip:      "IP"
        case .ip6:     "IPv6"
        case .ipRange: String(localized: "IP 段")
        case .asn:     "ASN"
        case .country: String(localized: "国家/地区")
        }
    }

    var placeholder: String {
        switch self {
        case .ip:      "192.0.2.1"
        case .ip6:     "2001:db8::1"
        case .ipRange: "192.0.2.0/24"
        case .asn:     "AS13335"
        case .country: String(localized: "国家代码，如 US")
        }
    }
}
