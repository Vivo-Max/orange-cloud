//
//  BulkRedirectModels.swift
//  Orange Cloud
//
//  Bulk Redirects（account 级，三段式）：
//  1) 重定向列表 Rules List（kind=redirect）—— /accounts/{id}/rules/lists
//  2) 列表条目 Items —— /…/lists/{id}/items（增删改为**异步**，返回 operation_id 需轮询）
//  3) 启用规则 —— account ruleset phase http_request_redirect，rule.action=redirect + from_list
//  端点 / 字段核对自 Cloudflare 官方 SDK 与文档。读 mass-url-redirects.read + account-rule-lists.read。
//

import Foundation

// MARK: - 重定向列表

nonisolated struct RedirectList: Codable, Identifiable, Sendable {
    let id:          String
    var name:        String?
    var kind:        String?       // "redirect"
    var description: String?
    var numItems:    Int?
    let createdOn:   String?
    let modifiedOn:  String?

    enum CodingKeys: String, CodingKey {
        case id, name, kind, description
        case numItems   = "num_items"
        case createdOn  = "created_on"
        case modifiedOn = "modified_on"
    }
}

nonisolated struct RedirectListCreate: Codable, Sendable {
    let name:        String
    let kind:        String        // 恒 "redirect"
    let description: String?
}

nonisolated struct RedirectListUpdate: Codable, Sendable {
    let description: String?
}

// MARK: - 条目（重定向）

nonisolated struct RedirectListItem: Codable, Identifiable, Sendable {
    let id:         String
    let redirect:   RedirectRule?
    let createdOn:  String?
    let modifiedOn: String?

    enum CodingKeys: String, CodingKey {
        case id, redirect
        case createdOn  = "created_on"
        case modifiedOn = "modified_on"
    }
}

nonisolated struct RedirectRule: Codable, Sendable {
    var sourceUrl:           String
    var targetUrl:           String
    var statusCode:          Int?  = nil
    var includeSubdomains:   Bool? = nil
    var subpathMatching:     Bool? = nil
    var preserveQueryString: Bool? = nil
    var preservePathSuffix:  Bool? = nil

    enum CodingKeys: String, CodingKey {
        case sourceUrl           = "source_url"
        case targetUrl           = "target_url"
        case statusCode          = "status_code"
        case includeSubdomains   = "include_subdomains"
        case subpathMatching     = "subpath_matching"
        case preserveQueryString = "preserve_query_string"
        case preservePathSuffix  = "preserve_path_suffix"
    }
}

/// POST /items 的数组元素
nonisolated struct RedirectItemInput: Codable, Sendable {
    let redirect: RedirectRule
}

/// DELETE /items 的体：{ items: [{ id }] }
nonisolated struct ItemDeleteBody: Codable, Sendable {
    let items: [ItemRef]
}
nonisolated struct ItemRef: Codable, Sendable {
    let id: String
}

// MARK: - 异步批量操作

nonisolated struct BulkOperationRef: Codable, Sendable {
    let operationId: String
    enum CodingKeys: String, CodingKey { case operationId = "operation_id" }
}

nonisolated struct BulkOperation: Codable, Sendable {
    let id:     String?
    let status: String?     // pending | running | completed | failed
    let error:  String?
}

// MARK: - 启用规则（account ruleset，phase http_request_redirect）

nonisolated struct RedirectRuleset: Codable, Sendable {
    let id:    String
    let rules: [RedirectRulesetRule]?
}

nonisolated struct RedirectRulesetRule: Codable, Identifiable, Sendable {
    let id:               String
    let expression:       String?
    let action:           String?
    let enabled:          Bool?
    let description:      String?
    let actionParameters: RedirectActionParameters?

    enum CodingKeys: String, CodingKey {
        case id, expression, action, enabled, description
        case actionParameters = "action_parameters"
    }
}

nonisolated struct RedirectActionParameters: Codable, Sendable {
    let fromList: FromList?
    enum CodingKeys: String, CodingKey { case fromList = "from_list" }
}

nonisolated struct FromList: Codable, Sendable {
    let name: String?
    let key:  String?
}

// 写入载荷
nonisolated struct RedirectRuleCreate: Codable, Sendable {
    let action:           String        // 恒 "redirect"
    let expression:       String
    let description:      String?
    let enabled:          Bool
    let actionParameters: RedirectActionParametersInput

    enum CodingKeys: String, CodingKey {
        case action, expression, description, enabled
        case actionParameters = "action_parameters"
    }
}

nonisolated struct RedirectActionParametersInput: Codable, Sendable {
    let fromList: FromListInput
    enum CodingKeys: String, CodingKey { case fromList = "from_list" }
}
nonisolated struct FromListInput: Codable, Sendable {
    let name: String
    let key:  String
}

nonisolated struct RedirectRuleToggle: Codable, Sendable { let enabled: Bool }
nonisolated struct RedirectEntrypointUpdate: Codable, Sendable { let rules: [RedirectRuleCreate] }

extension RedirectRuleCreate {
    /// 启用某重定向列表的标准规则：http.request.full_uri in $<name> + from_list
    static func enabling(listName: String) -> RedirectRuleCreate {
        RedirectRuleCreate(
            action: "redirect",
            expression: "http.request.full_uri in $\(listName)",
            description: "Bulk Redirect: \(listName)",
            enabled: true,
            actionParameters: RedirectActionParametersInput(
                fromList: FromListInput(name: listName, key: "http.request.full_uri")
            )
        )
    }
}

// MARK: - 状态码

nonisolated enum RedirectStatusCode: Int, CaseIterable, Identifiable, Sendable {
    case movedPermanently = 301
    case found            = 302
    case temporary        = 307
    case permanent        = 308

    var id: Int { rawValue }

    var label: String {
        switch self {
        case .movedPermanently: String(localized: "301 永久")
        case .found:            String(localized: "302 临时")
        case .temporary:        String(localized: "307 临时（保留方法）")
        case .permanent:        String(localized: "308 永久（保留方法）")
        }
    }
}
