//
//  TransformRuleService.swift
//  Orange Cloud
//
//  Transform Rules CRUD（Rulesets entrypoint，按 phase）。
//  读 zone-transform-rules.read，写 zone-transform-rules.write。
//  与 WAFService 同样容错：phase 还没有规则时 entrypoint 返回 404/错误信封，视为 nil。
//

import Foundation

struct TransformRuleService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 取某个 transform phase 的 entrypoint ruleset；该 phase 没有规则集时返回 nil
    func ruleset(zoneId: String, phase: String) async throws -> TransformRuleset? {
        do {
            let response: CFAPIResponse<TransformRuleset> = try await client.get(
                "zones/\(zoneId)/rulesets/phases/\(phase)/entrypoint"
            )
            guard response.success, let ruleset = response.result else {
                throw response.toAPIError()
            }
            return ruleset
        } catch APIError.notFound {
            return nil
        } catch let APIError.cloudflareError(code, message) {
            if message.localizedCaseInsensitiveContains("could not find entrypoint") {
                return nil
            }
            throw APIError.cloudflareError(code: code, message: message)
        }
    }

    /// 启停单条规则（PATCH 只更 enabled），返回更新后的整个 ruleset
    func setRuleEnabled(zoneId: String, rulesetId: String, ruleId: String, enabled: Bool) async throws -> TransformRuleset {
        let response: CFAPIResponse<TransformRuleset> = try await client.patch(
            "zones/\(zoneId)/rulesets/\(rulesetId)/rules/\(ruleId)",
            body: TransformRuleToggle(enabled: enabled)
        )
        guard response.success, let ruleset = response.result else {
            throw response.toAPIError()
        }
        return ruleset
    }

    /// 向已有规则集追加规则
    func addRule(zoneId: String, rulesetId: String, rule: TransformRuleCreate) async throws -> TransformRuleset {
        let response: CFAPIResponse<TransformRuleset> = try await client.post(
            "zones/\(zoneId)/rulesets/\(rulesetId)/rules",
            body: rule
        )
        guard response.success, let ruleset = response.result else {
            throw response.toAPIError()
        }
        return ruleset
    }

    /// 全量更新单条规则（PATCH，覆盖该规则的字段）
    func updateRule(zoneId: String, rulesetId: String, ruleId: String, rule: TransformRuleCreate) async throws -> TransformRuleset {
        let response: CFAPIResponse<TransformRuleset> = try await client.patch(
            "zones/\(zoneId)/rulesets/\(rulesetId)/rules/\(ruleId)",
            body: rule
        )
        guard response.success, let ruleset = response.result else {
            throw response.toAPIError()
        }
        return ruleset
    }

    /// phase 还没有规则集时，用首条规则创建 entrypoint
    func createEntrypoint(zoneId: String, phase: String, rule: TransformRuleCreate) async throws -> TransformRuleset {
        let response: CFAPIResponse<TransformRuleset> = try await client.put(
            "zones/\(zoneId)/rulesets/phases/\(phase)/entrypoint",
            body: TransformEntrypointUpdate(rules: [rule])
        )
        guard response.success, let ruleset = response.result else {
            throw response.toAPIError()
        }
        return ruleset
    }

    /// 删除规则
    func deleteRule(zoneId: String, rulesetId: String, ruleId: String) async throws {
        try await client.delete("zones/\(zoneId)/rulesets/\(rulesetId)/rules/\(ruleId)")
    }
}
