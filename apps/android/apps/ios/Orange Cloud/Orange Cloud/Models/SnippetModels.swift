//
//  SnippetModels.swift
//  Orange Cloud
//
//  Cloudflare Snippets（边缘轻量 JS，zone 级，Rules 下）。
//  GET    /zones/{id}/snippets                         列表
//  GET    /zones/{id}/snippets/{name}/content          原始 JS
//  PUT    /zones/{id}/snippets/{name}                  创建/更新（multipart）
//  DELETE /zones/{id}/snippets/{name}                  删除
//  GET    /zones/{id}/snippets/snippet_rules           触发规则
//  PUT    /zones/{id}/snippets/snippet_rules           整组回写规则
//

import Foundation

/// 单个 snippet（列表 / 元数据）。id 用 snippet_name。
nonisolated struct Snippet: Codable, Identifiable, Hashable, Sendable {
    let snippetName: String
    let createdOn:   String?
    let modifiedOn:  String?

    var id: String { snippetName }

    enum CodingKeys: String, CodingKey {
        case snippetName = "snippet_name"
        case createdOn   = "created_on"
        case modifiedOn  = "modified_on"
    }
}

/// 触发规则（GET 解码用）。服务端分配 id / last_updated。
nonisolated struct SnippetRule: Codable, Identifiable, Hashable, Sendable {
    let ruleID:      String?
    let snippetName: String
    let expression:  String
    let description: String?
    let enabled:     Bool?
    let lastUpdated: String?

    /// 服务端有 id 用 id，新建/本地态用 snippet_name+expression 兜底，保证 List 稳定
    var id: String { ruleID ?? "\(snippetName)|\(expression)" }

    enum CodingKeys: String, CodingKey {
        case ruleID      = "id"
        case expression, description, enabled
        case snippetName = "snippet_name"
        case lastUpdated = "last_updated"
    }

    /// 回写时转成请求体（丢弃 id / last_updated；可覆盖 enabled）
    func toInput(enabled override: Bool? = nil) -> SnippetRuleInput {
        SnippetRuleInput(
            snippetName: snippetName,
            expression:  expression,
            description: description,
            enabled:     override ?? enabled ?? true
        )
    }
}

/// PUT snippet_rules 的单条规则（不含 id / last_updated）
nonisolated struct SnippetRuleInput: Codable, Sendable {
    let snippetName: String
    let expression:  String
    let description: String?
    let enabled:     Bool

    enum CodingKeys: String, CodingKey {
        case expression, description, enabled
        case snippetName = "snippet_name"
    }
}

/// PUT snippet_rules 的请求体——必须携带 zone 下全部规则（整组替换）
nonisolated struct SnippetRulesUpdate: Codable, Sendable {
    let rules: [SnippetRuleInput]
}

/// snippet_rules 的响应 result 形态不固定（裸数组 / {rules:[...]} / {}），统一兼容解码
nonisolated struct SnippetRulesResult: Codable, Sendable {
    let rules: [SnippetRule]

    enum CodingKeys: String, CodingKey { case rules }

    init(from decoder: Decoder) throws {
        if let array = try? [SnippetRule](from: decoder) {
            rules = array                                   // result 直接是数组
        } else if let container = try? decoder.container(keyedBy: CodingKeys.self),
                  let array = try? container.decode([SnippetRule].self, forKey: .rules) {
            rules = array                                   // result 是 {rules:[...]}
        } else {
            rules = []                                      // result 是 {} 或无规则
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(rules, forKey: .rules)
    }
}
