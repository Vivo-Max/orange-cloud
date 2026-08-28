//
//  RateLimitService.swift
//  Orange Cloud
//
//  Rate Limiting CRUD（Rulesets http_ratelimit phase entrypoint）。
//  与 TransformRuleService 同构；读写复用 zone-waf.read/.write。
//  phase 没有规则时 entrypoint 返回 404 / “could not find entrypoint”，视为 nil。
//

import Foundation

struct RateLimitService {

    private let client: CFAPIClient
    private let phase = "http_ratelimit"

    init(client: CFAPIClient) {
        self.client = client
    }

    /// 取 http_ratelimit 的 entrypoint ruleset；没有规则时返回 nil
    func ruleset(zoneId: String) async throws -> RateLimitRuleset? {
        do {
            let response: CFAPIResponse<RateLimitRuleset> = try await client.get(
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

    /// 启停单条规则（PATCH 只更 enabled）
    func setRuleEnabled(zoneId: String, rulesetId: String, ruleId: String, enabled: Bool) async throws -> RateLimitRuleset {
        let response: CFAPIResponse<RateLimitRuleset> = try await client.patch(
            "zones/\(zoneId)/rulesets/\(rulesetId)/rules/\(ruleId)",
            body: RateLimitToggle(enabled: enabled)
        )
        guard response.success, let ruleset = response.result else {
            throw response.toAPIError()
        }
        return ruleset
    }

    /// 向已有规则集追加规则
    func addRule(zoneId: String, rulesetId: String, rule: RateLimitRuleCreate) async throws -> RateLimitRuleset {
        let response: CFAPIResponse<RateLimitRuleset> = try await client.post(
            "zones/\(zoneId)/rulesets/\(rulesetId)/rules",
            body: rule
        )
        guard response.success, let ruleset = response.result else {
            throw response.toAPIError()
        }
        return ruleset
    }

    /// 全量更新单条规则
    func updateRule(zoneId: String, rulesetId: String, ruleId: String, rule: RateLimitRuleCreate) async throws -> RateLimitRuleset {
        let response: CFAPIResponse<RateLimitRuleset> = try await client.patch(
            "zones/\(zoneId)/rulesets/\(rulesetId)/rules/\(ruleId)",
            body: rule
        )
        guard response.success, let ruleset = response.result else {
            throw response.toAPIError()
        }
        return ruleset
    }

    /// phase 还没有规则集时，用首条规则创建 entrypoint
    func createEntrypoint(zoneId: String, rule: RateLimitRuleCreate) async throws -> RateLimitRuleset {
        let response: CFAPIResponse<RateLimitRuleset> = try await client.put(
            "zones/\(zoneId)/rulesets/phases/\(phase)/entrypoint",
            body: RateLimitEntrypointUpdate(rules: [rule])
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
