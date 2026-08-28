//
//  FirewallAccessRuleService.swift
//  Orange Cloud
//
//  IP 访问规则 CRUD（legacy firewall access rules）。读 firewall-services.read，写 .write。
//

import Foundation

struct FirewallAccessRuleService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 列出该 Zone 的 IP 访问规则（最多 100 条，足够展示）
    func rules(zoneId: String) async throws -> [FirewallAccessRule] {
        let response: CFAPIResponse<[FirewallAccessRule]> = try await client.get(
            "zones/\(zoneId)/firewall/access_rules/rules",
            queryItems: [URLQueryItem(name: "per_page", value: "100")]
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    func createRule(zoneId: String, draft: AccessRuleCreate) async throws -> FirewallAccessRule {
        let response: CFAPIResponse<FirewallAccessRule> = try await client.post(
            "zones/\(zoneId)/firewall/access_rules/rules",
            body: draft
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    /// 仅改 mode + notes（target/value 不可变）
    func updateRule(zoneId: String, ruleId: String, update: AccessRuleUpdate) async throws -> FirewallAccessRule {
        let response: CFAPIResponse<FirewallAccessRule> = try await client.patch(
            "zones/\(zoneId)/firewall/access_rules/rules/\(ruleId)",
            body: update
        )
        guard response.success, let result = response.result else {
            throw response.toAPIError()
        }
        return result
    }

    func deleteRule(zoneId: String, ruleId: String) async throws {
        try await client.delete("zones/\(zoneId)/firewall/access_rules/rules/\(ruleId)")
    }
}
