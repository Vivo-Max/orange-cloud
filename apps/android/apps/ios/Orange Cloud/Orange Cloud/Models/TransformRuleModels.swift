//
//  TransformRuleModels.swift
//  Orange Cloud
//
//  Transform Rules（Rulesets API）三个 phase：
//  http_request_transform（URL 重写）/ http_request_late_transform（请求头）/
//  http_response_headers_transform（响应头）。读 zone-transform-rules.read，写 .write。
//  动作恒为 "rewrite"，差异在 action_parameters（uri 或 headers）。
//

import Foundation

nonisolated struct TransformRuleset: Codable, Sendable {
    let id:    String
    let name:  String?
    let phase: String?
    let rules: [TransformRule]?
}

nonisolated struct TransformRule: Codable, Identifiable, Sendable {
    let id:          String
    let expression:  String?
    let description: String?
    let enabled:     Bool?
    let action:      String?
    let actionParameters: TransformActionParameters?

    enum CodingKeys: String, CodingKey {
        case id, expression, description, enabled, action
        case actionParameters = "action_parameters"
    }

    /// 给列表一句话摘要（重写了什么 / 改了哪些头）
    func summary(for phase: TransformPhase) -> String? {
        guard let p = actionParameters else { return nil }
        if phase == .requestURL {
            var parts: [String] = []
            if let v = p.uri?.path?.value { parts.append(String(localized: "路径 → \(v)")) }
            if let v = p.uri?.query?.value { parts.append(String(localized: "查询串 → \(v)")) }
            return parts.isEmpty ? nil : parts.joined(separator: "  ·  ")
        } else if let headers = p.headers, !headers.isEmpty {
            let names = headers.keys.sorted().joined(separator: ", ")
            return String(localized: "头：\(names)")
        }
        return nil
    }
}

// MARK: - action_parameters

nonisolated struct TransformActionParameters: Codable, Sendable {
    var uri: URIRewrite?
    var headers: [String: HeaderTransform]?
}

nonisolated struct URIRewrite: Codable, Sendable {
    var path:  RewriteTarget?
    var query: RewriteTarget?
}

nonisolated struct RewriteTarget: Codable, Sendable {
    var value:      String?
    var expression: String?
}

nonisolated struct HeaderTransform: Codable, Sendable {
    var operation:  String        // "set" | "add" | "remove"
    var value:      String?
    var expression: String?
}

// MARK: - 写入载荷

/// POST rules / PATCH rule / PUT entrypoint 共用
nonisolated struct TransformRuleCreate: Codable, Sendable {
    let action:           String        // 恒为 "rewrite"
    let expression:       String
    let description:      String?
    let enabled:          Bool
    let actionParameters: TransformActionParameters?

    enum CodingKeys: String, CodingKey {
        case action, expression, description, enabled
        case actionParameters = "action_parameters"
    }
}

nonisolated struct TransformRuleToggle: Codable, Sendable {
    let enabled: Bool
}

nonisolated struct TransformEntrypointUpdate: Codable, Sendable {
    let rules: [TransformRuleCreate]
}

// MARK: - phase

nonisolated enum TransformPhase: String, CaseIterable, Identifiable, Sendable {
    case requestURL   = "http_request_transform"
    case requestHead  = "http_request_late_transform"
    case responseHead = "http_response_headers_transform"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .requestURL:   String(localized: "URL 重写")
        case .requestHead:  String(localized: "请求头修改")
        case .responseHead: String(localized: "响应头修改")
        }
    }

    /// URL 重写用 uri 参数；两个 header phase 用 headers 参数
    var isURLRewrite: Bool { self == .requestURL }
}

/// 请求/响应头编辑的操作类型
nonisolated enum HeaderOperation: String, CaseIterable, Identifiable, Sendable {
    case set, add, remove

    var id: String { rawValue }

    var label: String {
        switch self {
        case .set:    String(localized: "设置")
        case .add:    String(localized: "追加")
        case .remove: String(localized: "删除")
        }
    }
}
