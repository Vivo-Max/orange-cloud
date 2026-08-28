//
//  ZoneRuleModels.swift
//  Orange Cloud
//
//  Zone 级规则族（对齐 developers.cloudflare.com/rules/）：
//  - 五个 Rulesets entrypoint phase（Single Redirects / Origin / Configuration /
//    Compression / Custom Errors）共用一套泛化模型：action_parameters 各 phase
//    schema 不同，用 TunnelJSONValue 原样解码保留（v1 查看/启停/删除，不做编辑器，
//    PATCH 只碰 enabled，绝不回写参数、不丢配置）。
//  - Page Rules（传统）走独立 REST /zones/{id}/pagerules。
//  - URL Normalization 是单一设置对象 GET/PUT。
//

import Foundation

// MARK: - Rulesets phase 泛化

/// 收进「规则」入口的五个 zone 级 entrypoint phase
nonisolated enum ZoneRulePhase: String, CaseIterable, Identifiable, Sendable {
    case singleRedirect = "http_request_dynamic_redirect"
    case origin         = "http_request_origin"
    case config         = "http_config_settings"
    case compression    = "http_response_compression"
    case customErrors   = "http_custom_errors"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .singleRedirect: String(localized: "单条重定向")
        case .origin:         String(localized: "源站规则")
        case .config:         String(localized: "配置规则")
        case .compression:    String(localized: "压缩规则")
        case .customErrors:   String(localized: "自定义错误")
        }
    }

    /// OAuth scope（读 / 写），ID 以 dash 实列为准（cf-oauth-scopes）
    var readScope: String {
        switch self {
        case .singleRedirect: "dynamic-redirect.read"
        case .origin:         "origin.read"
        case .config:         "config-settings.read"
        case .compression:    "response-compression.read"
        case .customErrors:   "custom-errors.read"
        }
    }

    var writeScope: String {
        switch self {
        case .singleRedirect: "dynamic-redirect.write"
        case .origin:         "origin.write"
        case .config:         "config-settings.write"
        case .compression:    "response-compression.write"
        case .customErrors:   "custom-errors.write"
        }
    }

    var systemImage: String {
        switch self {
        case .singleRedirect: "arrow.uturn.right"
        case .origin:         "server.rack"
        case .config:         "slider.horizontal.3"
        case .compression:    "rectangle.compress.vertical"
        case .customErrors:   "exclamationmark.bubble"
        }
    }
}

nonisolated struct ZoneRuleset: Codable, Sendable {
    let id:    String
    let name:  String?
    let phase: String?
    let rules: [ZoneRule]?
}

nonisolated struct ZoneRule: Codable, Identifiable, Sendable {
    let id:          String
    let expression:  String?
    let description: String?
    let enabled:     Bool?
    let action:      String?
    /// 各 phase 参数 schema 不同：原样保留，仅用于展示与摘要
    let actionParameters: TunnelJSONValue?

    enum CodingKeys: String, CodingKey {
        case id, expression, description, enabled, action
        case actionParameters = "action_parameters"
    }

    /// 列表行一句话摘要（能识别的常见形态给友好文案，其余回退参数键名列表）
    var summary: String {
        guard case .object(let params)? = actionParameters else {
            return action ?? String(localized: "查看详情")
        }
        // Single Redirect：target_url + status_code
        if case .object(let from)? = params["from_value"] {
            var parts: [String] = []
            if case .object(let target)? = from["target_url"] {
                if case .string(let url)? = target["value"] { parts.append(url) }
                if case .string(let expr)? = target["expression"] { parts.append(expr) }
            }
            if case .int(let code)? = from["status_code"] { parts.append("\(code)") }
            if !parts.isEmpty { return parts.joined(separator: " · ") }
        }
        // Origin：host_header / origin
        if case .string(let host)? = params["host_header"] {
            return String(localized: "Host：\(host)")
        }
        if case .object(let origin)? = params["origin"] {
            if case .string(let host)? = origin["host"] { return String(localized: "源站：\(host)") }
            if case .int(let port)? = origin["port"] { return String(localized: "源站端口：\(port)") }
        }
        // Compression：algorithms
        if case .array(let algos)? = params["algorithms"] {
            let names = algos.compactMap { v -> String? in
                if case .object(let o) = v, case .string(let n)? = o["name"] { return n }
                return nil
            }
            if !names.isEmpty { return names.joined(separator: " · ") }
        }
        // 兜底：列出改动的参数键
        return params.keys.sorted().joined(separator: " · ")
    }
}

/// 启停 PATCH 只带 enabled（其余字段不回写，避免覆盖丢配置）
nonisolated struct ZoneRuleToggle: Codable, Sendable {
    let enabled: Bool
}

extension ZoneRulePhase {
    /// 各 phase 的固定 action 名（OpenAPI schema 已核实）
    var action: String {
        switch self {
        case .singleRedirect: "redirect"
        case .origin:         "route"
        case .config:         "set_config"
        case .compression:    "compress_response"
        case .customErrors:   "serve_error"
        }
    }
}

/// 新建 / 更新规则的请求体。action_parameters 泛化为 TunnelJSONValue：
/// 编辑既有规则时以其 raw 参数为底叠加改动（可合并的 phase），未建模字段原样保留。
nonisolated struct ZoneRulePayload: Codable, Sendable {
    let action:      String
    let expression:  String
    let description: String
    let enabled:     Bool
    let actionParameters: TunnelJSONValue

    enum CodingKeys: String, CodingKey {
        case action, expression, description, enabled
        case actionParameters = "action_parameters"
    }
}

/// 首条规则建 entrypoint 的 PUT 体
nonisolated struct ZoneEntrypointUpdate: Codable, Sendable {
    let rules: [ZoneRulePayload]
}

// MARK: - Page Rules（传统）

nonisolated struct PageRule: Codable, Identifiable, Sendable {
    let id:       String
    let targets:  [PageRuleTarget]?
    let actions:  [PageRuleAction]?
    let priority: Int?
    let status:   String?     // active / disabled

    var isActive: Bool { status == "active" }

    /// 目标 URL 模式（列表行主标题）
    var targetLabel: String {
        targets?.first?.constraint?.value ?? id
    }

    /// 动作摘要（键名列表）
    var actionsLabel: String {
        let names = (actions ?? []).map(\.id)
        return names.isEmpty ? String(localized: "无动作") : names.joined(separator: " · ")
    }
}

nonisolated struct PageRuleTarget: Codable, Sendable {
    let target: String?
    let constraint: PageRuleConstraint?
}

nonisolated struct PageRuleConstraint: Codable, Sendable {
    let `operator`: String?
    let value: String?
}

nonisolated struct PageRuleAction: Codable, Sendable {
    let id: String
    let value: TunnelJSONValue?
}

nonisolated struct PageRuleStatusUpdate: Codable, Sendable {
    let status: String
}

// MARK: - URL Normalization

nonisolated struct URLNormalization: Codable, Sendable {
    var type:  String    // cloudflare / rfc3986
    var scope: String    // incoming / both / none
}
