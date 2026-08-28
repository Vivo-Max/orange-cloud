//
//  ZoneRulesetService.swift
//  Orange Cloud
//
//  Zone 级规则族 API：
//  - 五个 Rulesets entrypoint phase 的查看 / 启停 / 删除（v1 不做参数编辑，
//    PATCH 只带 enabled，绝不回写 action_parameters）。
//  - Page Rules（传统 REST）列表 / 启停 / 删除。
//  - URL Normalization 读写。
//  与 Cache/WAF 同样容错：zone 从未建过该类规则时 entrypoint 返回 404/错误信封，视为 nil。
//

import Foundation

struct ZoneRulesetService {

    private let client: CFAPIClient

    init(client: CFAPIClient) {
        self.client = client
    }

    // MARK: - Rulesets phase 泛化

    /// 该 phase 的 entrypoint ruleset；zone 还没建过此类规则时返回 nil
    func ruleset(zoneId: String, phase: ZoneRulePhase) async throws -> ZoneRuleset? {
        do {
            let response: CFAPIResponse<ZoneRuleset> = try await client.get(
                "zones/\(zoneId)/rulesets/phases/\(phase.rawValue)/entrypoint"
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
    func setRuleEnabled(zoneId: String, rulesetId: String, ruleId: String, enabled: Bool) async throws -> ZoneRuleset {
        let response: CFAPIResponse<ZoneRuleset> = try await client.patch(
            "zones/\(zoneId)/rulesets/\(rulesetId)/rules/\(ruleId)",
            body: ZoneRuleToggle(enabled: enabled)
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

    /// 向已有规则集追加规则
    func addRule(zoneId: String, rulesetId: String, rule: ZoneRulePayload) async throws -> ZoneRuleset {
        let response: CFAPIResponse<ZoneRuleset> = try await client.post(
            "zones/\(zoneId)/rulesets/\(rulesetId)/rules",
            body: rule
        )
        guard response.success, let ruleset = response.result else {
            throw response.toAPIError()
        }
        return ruleset
    }

    /// 更新单条规则（PATCH 覆盖所列字段；action_parameters 由调用方按合并策略构建）
    func updateRule(zoneId: String, rulesetId: String, ruleId: String, rule: ZoneRulePayload) async throws -> ZoneRuleset {
        let response: CFAPIResponse<ZoneRuleset> = try await client.patch(
            "zones/\(zoneId)/rulesets/\(rulesetId)/rules/\(ruleId)",
            body: rule
        )
        guard response.success, let ruleset = response.result else {
            throw response.toAPIError()
        }
        return ruleset
    }

    /// zone 还没有该类规则集时，用首条规则创建 entrypoint
    func createEntrypoint(zoneId: String, phase: ZoneRulePhase, rule: ZoneRulePayload) async throws -> ZoneRuleset {
        let response: CFAPIResponse<ZoneRuleset> = try await client.put(
            "zones/\(zoneId)/rulesets/phases/\(phase.rawValue)/entrypoint",
            body: ZoneEntrypointUpdate(rules: [rule])
        )
        guard response.success, let ruleset = response.result else {
            throw response.toAPIError()
        }
        return ruleset
    }

    // MARK: - Page Rules（传统）

    func listPageRules(zoneId: String) async throws -> [PageRule] {
        let response: CFAPIResponse<[PageRule]> = try await client.get("zones/\(zoneId)/pagerules")
        guard response.success, let rules = response.result else {
            throw response.toAPIError()
        }
        return rules
    }

    func setPageRuleStatus(zoneId: String, ruleId: String, active: Bool) async throws -> PageRule {
        let response: CFAPIResponse<PageRule> = try await client.patch(
            "zones/\(zoneId)/pagerules/\(ruleId)",
            body: PageRuleStatusUpdate(status: active ? "active" : "disabled")
        )
        guard response.success, let rule = response.result else {
            throw response.toAPIError()
        }
        return rule
    }

    func deletePageRule(zoneId: String, ruleId: String) async throws {
        try await client.delete("zones/\(zoneId)/pagerules/\(ruleId)")
    }

    // MARK: - URL Normalization

    func urlNormalization(zoneId: String) async throws -> URLNormalization {
        let response: CFAPIResponse<URLNormalization> = try await client.get("zones/\(zoneId)/url_normalization")
        guard response.success, let value = response.result else {
            throw response.toAPIError()
        }
        return value
    }

    func setURLNormalization(zoneId: String, value: URLNormalization) async throws -> URLNormalization {
        let response: CFAPIResponse<URLNormalization> = try await client.put(
            "zones/\(zoneId)/url_normalization",
            body: value
        )
        guard response.success, let updated = response.result else {
            throw response.toAPIError()
        }
        return updated
    }
}
