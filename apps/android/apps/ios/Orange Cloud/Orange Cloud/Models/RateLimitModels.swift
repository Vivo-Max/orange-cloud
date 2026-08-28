//
//  RateLimitModels.swift
//  Orange Cloud
//
//  Rate Limiting（现代版）：Rulesets 的 http_ratelimit phase entrypoint。
//  与 Transform Rules 同构（rulesets/phases/{phase}/entrypoint + rules CRUD）。
//  复用 zone-waf.read/.write（与 WAF 自定义规则同一权限组）。
//

import Foundation

nonisolated struct RateLimitRuleset: Codable, Sendable {
    let id:    String
    let name:  String?
    let phase: String?
    let rules: [RateLimitRule]?
}

nonisolated struct RateLimitRule: Codable, Identifiable, Sendable {
    let id:          String
    let action:      String?
    let expression:  String?
    let description: String?
    let enabled:     Bool?
    let ratelimit:   RateLimitConfig?

    var isEnabled: Bool { enabled ?? false }
}

nonisolated struct RateLimitConfig: Codable, Sendable {
    let characteristics:    [String]?
    let period:             Int?
    let requestsPerPeriod:  Int?
    let mitigationTimeout:  Int?
    let countingExpression: String?

    enum CodingKeys: String, CodingKey {
        case characteristics, period
        case requestsPerPeriod  = "requests_per_period"
        case mitigationTimeout  = "mitigation_timeout"
        case countingExpression = "counting_expression"
    }
}

// MARK: - 写入载荷

nonisolated struct RateLimitRuleCreate: Codable, Sendable {
    let action:      String
    let expression:  String
    let description: String?
    let enabled:     Bool
    let ratelimit:   RateLimitConfigInput

    /// 便捷构造：按 IP（按 colo 本地计数）在 period 秒内超过 requests 次即触发 action
    static func make(
        expression: String,
        requests: Int,
        period: Int,
        action: String,
        mitigationTimeout: Int,
        description: String?,
        enabled: Bool
    ) -> RateLimitRuleCreate {
        .init(
            action: action,
            expression: expression,
            description: description,
            enabled: enabled,
            ratelimit: .init(
                characteristics: ["ip.src", "cf.colo.id"],
                period: period,
                requestsPerPeriod: requests,
                mitigationTimeout: mitigationTimeout
            )
        )
    }
}

nonisolated struct RateLimitConfigInput: Codable, Sendable {
    let characteristics:   [String]
    let period:            Int
    let requestsPerPeriod: Int
    let mitigationTimeout: Int

    enum CodingKeys: String, CodingKey {
        case characteristics, period
        case requestsPerPeriod = "requests_per_period"
        case mitigationTimeout = "mitigation_timeout"
    }
}

nonisolated struct RateLimitToggle: Codable, Sendable {
    let enabled: Bool
}

nonisolated struct RateLimitEntrypointUpdate: Codable, Sendable {
    let rules: [RateLimitRuleCreate]
}

// MARK: - 选项

/// 触发后的处置动作
nonisolated enum RateLimitAction: String, CaseIterable, Identifiable, Sendable {
    case block
    case managedChallenge = "managed_challenge"
    case jsChallenge      = "js_challenge"
    case log

    var id: String { rawValue }

    var label: String {
        switch self {
        case .block:            String(localized: "阻止")
        case .managedChallenge: String(localized: "托管质询")
        case .jsChallenge:      String(localized: "JS 质询")
        case .log:              String(localized: "仅记录")
        }
    }
}

/// 时间窗 / 封禁时长备选（秒）
nonisolated enum RateLimitPeriod: Int, CaseIterable, Identifiable, Sendable {
    case s10 = 10, s60 = 60, s600 = 600, s3600 = 3600

    var id: Int { rawValue }

    var label: String {
        switch self {
        case .s10:   String(localized: "10 秒")
        case .s60:   String(localized: "1 分钟")
        case .s600:  String(localized: "10 分钟")
        case .s3600: String(localized: "1 小时")
        }
    }
}
