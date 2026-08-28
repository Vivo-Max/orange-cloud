//
//  BulkRedirectService.swift
//  Orange Cloud
//
//  Bulk Redirects（account 级）：重定向列表 + 条目（异步批量操作 + 轮询）+ 启用规则（ruleset）。
//  端点核对自 Cloudflare 官方 SDK / 文档。读 account-rule-lists.read + mass-url-redirects.read。
//

import Foundation

struct BulkRedirectService {

    private let client: CFAPIClient
    private let redirectPhase = "http_request_redirect"

    init(client: CFAPIClient) {
        self.client = client
    }

    // MARK: - 重定向列表（同步）

    /// 仅返回 kind == "redirect" 的列表（账号下可能还有 ip / hostname / asn 类列表）
    func listRedirectLists(accountId: String) async throws -> [RedirectList] {
        let response: CFAPIResponse<[RedirectList]> = try await client.get(
            "accounts/\(accountId)/rules/lists"
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result.filter { $0.kind == "redirect" }
    }

    func createList(accountId: String, name: String, description: String?) async throws -> RedirectList {
        let response: CFAPIResponse<RedirectList> = try await client.post(
            "accounts/\(accountId)/rules/lists",
            body: RedirectListCreate(name: name, kind: "redirect", description: description)
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    func updateList(accountId: String, listId: String, description: String?) async throws -> RedirectList {
        let response: CFAPIResponse<RedirectList> = try await client.put(
            "accounts/\(accountId)/rules/lists/\(listId)",
            body: RedirectListUpdate(description: description)
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    func deleteList(accountId: String, listId: String) async throws {
        try await client.delete("accounts/\(accountId)/rules/lists/\(listId)")
    }

    // MARK: - 条目（异步：create/delete 返回 operation_id 需轮询）

    func listItems(accountId: String, listId: String) async throws -> [RedirectListItem] {
        let response: CFAPIResponse<[RedirectListItem]> = try await client.get(
            "accounts/\(accountId)/rules/lists/\(listId)/items"
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    /// 追加条目，返回 operation_id
    func createItems(accountId: String, listId: String, items: [RedirectItemInput]) async throws -> String {
        let response: CFAPIResponse<BulkOperationRef> = try await client.post(
            "accounts/\(accountId)/rules/lists/\(listId)/items",
            body: items
        )
        guard response.success, let ref = response.result else { throw response.toAPIError() }
        return ref.operationId
    }

    /// 删除条目（按 id），返回 operation_id
    func deleteItems(accountId: String, listId: String, itemIds: [String]) async throws -> String {
        let response: CFAPIResponse<BulkOperationRef> = try await client.delete(
            "accounts/\(accountId)/rules/lists/\(listId)/items",
            body: ItemDeleteBody(items: itemIds.map { ItemRef(id: $0) })
        )
        guard response.success, let ref = response.result else { throw response.toAPIError() }
        return ref.operationId
    }

    func operationStatus(accountId: String, operationId: String) async throws -> BulkOperation {
        let response: CFAPIResponse<BulkOperation> = try await client.get(
            "accounts/\(accountId)/rules/lists/bulk_operations/\(operationId)"
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result
    }

    /// 轮询批量操作直至完成；failed / 超时抛错
    func waitForOperation(accountId: String, operationId: String, timeout: TimeInterval = 30) async throws {
        let deadline = Date().addingTimeInterval(timeout)
        while Date() < deadline {
            let op = try await operationStatus(accountId: accountId, operationId: operationId)
            switch op.status {
            case "completed":
                return
            case "failed":
                throw APIError.cloudflareError(code: -1, message: op.error ?? String(localized: "批量操作失败"))
            default:
                try await Task.sleep(nanoseconds: 800_000_000)   // 0.8s 后再查
            }
        }
        throw APIError.cloudflareError(code: -1, message: String(localized: "批量操作超时，请稍后刷新查看结果"))
    }

    // MARK: - 启用规则（ruleset entrypoint，phase http_request_redirect）

    func redirectEntrypoint(accountId: String) async throws -> RedirectRuleset? {
        do {
            let response: CFAPIResponse<RedirectRuleset> = try await client.get(
                "accounts/\(accountId)/rulesets/phases/\(redirectPhase)/entrypoint"
            )
            guard response.success, let ruleset = response.result else { throw response.toAPIError() }
            return ruleset
        } catch APIError.notFound {
            return nil
        } catch let APIError.cloudflareError(code, message) {
            if message.localizedCaseInsensitiveContains("could not find entrypoint") { return nil }
            throw APIError.cloudflareError(code: code, message: message)
        }
    }

    func addRule(accountId: String, rulesetId: String, rule: RedirectRuleCreate) async throws -> RedirectRuleset {
        let response: CFAPIResponse<RedirectRuleset> = try await client.post(
            "accounts/\(accountId)/rulesets/\(rulesetId)/rules",
            body: rule
        )
        guard response.success, let ruleset = response.result else { throw response.toAPIError() }
        return ruleset
    }

    func createEntrypoint(accountId: String, rule: RedirectRuleCreate) async throws -> RedirectRuleset {
        let response: CFAPIResponse<RedirectRuleset> = try await client.put(
            "accounts/\(accountId)/rulesets/phases/\(redirectPhase)/entrypoint",
            body: RedirectEntrypointUpdate(rules: [rule])
        )
        guard response.success, let ruleset = response.result else { throw response.toAPIError() }
        return ruleset
    }

    func setRuleEnabled(accountId: String, rulesetId: String, ruleId: String, enabled: Bool) async throws -> RedirectRuleset {
        let response: CFAPIResponse<RedirectRuleset> = try await client.patch(
            "accounts/\(accountId)/rulesets/\(rulesetId)/rules/\(ruleId)",
            body: RedirectRuleToggle(enabled: enabled)
        )
        guard response.success, let ruleset = response.result else { throw response.toAPIError() }
        return ruleset
    }
}
