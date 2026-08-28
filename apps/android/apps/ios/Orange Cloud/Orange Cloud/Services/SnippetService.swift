//
//  SnippetService.swift
//  Orange Cloud
//
//  Cloudflare Snippets：zone 级边缘 JS。列表/正文/增删 + 触发规则整组回写。
//  snippet 名受限于 [a-zA-Z0-9_]，可直接拼进 path，无需百分号编码。
//

import Foundation

struct SnippetService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    // MARK: - Snippets

    /// zone 下全部 snippet（该端点不分页）
    func list(zoneId: String) async throws -> [Snippet] {
        let response: CFAPIResponseArray<Snippet> = try await client.get(
            "zones/\(zoneId)/snippets"
        )
        guard response.success else { throw response.toAPIError() }
        return response.result ?? []
    }

    /// snippet 的原始 JS 正文（非 JSON 信封）
    func content(zoneId: String, name: String) async throws -> String {
        let data = try await client.getRaw("zones/\(zoneId)/snippets/\(name)/content")
        return String(decoding: data, as: UTF8.self)
    }

    /// 创建或更新 snippet（multipart：metadata + JS 模块），返回更新后的元数据
    @discardableResult
    func put(
        zoneId: String,
        name: String,
        code: String,
        mainModule: String = "snippet.js"
    ) async throws -> Snippet {
        let response: CFAPIResponse<Snippet> = try await client.putMultipartFile(
            "zones/\(zoneId)/snippets/\(name)",
            metadata: ["main_module": mainModule],
            fileName: mainModule,
            fileContent: Data(code.utf8),
            fileContentType: "application/javascript+module"
        )
        guard response.success, let snippet = response.result else { throw response.toAPIError() }
        return snippet
    }

    /// 删除 snippet
    func delete(zoneId: String, name: String) async throws {
        try await client.delete("zones/\(zoneId)/snippets/\(name)")
    }

    // MARK: - 触发规则

    /// zone 下全部 snippet 触发规则
    func rules(zoneId: String) async throws -> [SnippetRule] {
        let response: CFAPIResponse<SnippetRulesResult> = try await client.get(
            "zones/\(zoneId)/snippets/snippet_rules"
        )
        guard response.success else { throw response.toAPIError() }
        return response.result?.rules ?? []
    }

    /// 整组回写规则——调用方必须传 zone 下「全部」规则，PUT 会替换全部（漏传即删除）
    func putRules(zoneId: String, rules: [SnippetRuleInput]) async throws {
        let response: CFAPIResponse<SnippetRulesResult> = try await client.put(
            "zones/\(zoneId)/snippets/snippet_rules",
            body: SnippetRulesUpdate(rules: rules)
        )
        guard response.success else { throw response.toAPIError() }
    }
}
