//
//  RequestTracerModels.swift
//  Orange Cloud
//
//  Cloudflare Trace —— 模拟一个请求打过 Cloudflare，看它走了哪些规则、被谁拦的。
//  POST /accounts/{account_id}/request-tracer/trace
//
//  本质是「模拟」而非真实流量回放，文案上别让用户误解。
//  全套餐可用，但要求 Administrator / Super Administrator 角色。
//

import Foundation

nonisolated struct TraceRequest: Codable, Sendable {
    let method:   String
    let url:      String
    let protocolVersion: String

    enum CodingKeys: String, CodingKey {
        case method, url
        case protocolVersion = "protocol"
    }
}

nonisolated struct TraceResult: Codable, Sendable {
    let statusCode: Int?
    let trace:      [TraceStep]?

    enum CodingKeys: String, CodingKey {
        case trace
        case statusCode = "status_code"
    }
}

/// 单个求值步骤。**是递归结构**：ruleset 步骤的 trace 里装着它的各条 rule。
nonisolated struct TraceStep: Codable, Hashable, Sendable, Identifiable {
    /// rule / ruleset / …
    let type:       String?
    let stepName:   String?
    /// 命中时执行的动作（type 为 rule 时）
    let action:     String?
    let description: String?
    /// 匹配表达式（type 为 rule 时）
    let expression: String?
    /// ruleset 的种类（zone / managed 等）
    let kind:       String?
    let name:       String?
    /// 该步骤是否真的影响了请求——UI 的重点就是把 true 的挑出来
    let matched:    Bool?
    let trace:      [TraceStep]?

    var id: String {
        // 同一层可能有同名步骤，拼上表达式与子树规模降低碰撞
        "\(stepName ?? "")|\(name ?? "")|\(expression ?? "")|\(trace?.count ?? 0)"
    }

    enum CodingKeys: String, CodingKey {
        case type, action, description, expression, kind, name, matched, trace
        case stepName = "step_name"
    }

    /// 展示名：ruleset 用 name，rule 用 description，都没有才退回 step_name
    var title: String {
        if let name, !name.isEmpty { return name }
        if let description, !description.isEmpty { return description }
        return stepName ?? String(localized: "未命名步骤")
    }
}

/// 把递归树摊平成带层级的行，便于用 List 直接渲染
nonisolated struct FlatTraceStep: Identifiable {
    let step:  TraceStep
    let depth: Int
    var id: String { "\(depth)-\(step.id)" }
}

nonisolated extension Array where Element == TraceStep {
    /// 深度优先摊平；depth 用于缩进
    func flattened(depth: Int = 0) -> [FlatTraceStep] {
        flatMap { step -> [FlatTraceStep] in
            [FlatTraceStep(step: step, depth: depth)] + (step.trace ?? []).flattened(depth: depth + 1)
        }
    }
}
