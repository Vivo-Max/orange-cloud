//
//  RateLimitViewModel.swift
//  Orange Cloud
//
//  Rate Limiting：加载 http_ratelimit entrypoint、启停 / 删除 / 新建·编辑规则。
//

import Foundation
import Observation

@Observable
@MainActor
final class RateLimitViewModel {

    private(set) var rules: [RateLimitRule] = []
    private(set) var rulesetId: String?
    private(set) var loaded = false
    var isLoading = false
    var isMutating = false
    var error: String?

    private let service: RateLimitService
    private let zoneId: String

    init(service: RateLimitService, zoneId: String) {
        self.service = service
        self.zoneId = zoneId
    }

    func load() async {
        guard !isLoading else { return }
        isLoading = true
        error = nil
        do {
            let ruleset = try await service.ruleset(zoneId: zoneId)
            apply(ruleset)
            loaded = true
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func setEnabled(_ rule: RateLimitRule, enabled: Bool) async {
        guard let rulesetId else { return }
        await mutate {
            let rs = try await service.setRuleEnabled(
                zoneId: zoneId, rulesetId: rulesetId, ruleId: rule.id, enabled: enabled
            )
            apply(rs)
        }
    }

    func delete(_ rule: RateLimitRule) async {
        guard let rulesetId else { return }
        await mutate {
            try await service.deleteRule(zoneId: zoneId, rulesetId: rulesetId, ruleId: rule.id)
            rules.removeAll { $0.id == rule.id }
        }
    }

    /// 新建（existing == nil）或全量更新一条规则
    func save(existing: RateLimitRule?, rule: RateLimitRuleCreate) async {
        await mutate {
            if let rulesetId {
                let rs: RateLimitRuleset
                if let existing {
                    rs = try await service.updateRule(
                        zoneId: zoneId, rulesetId: rulesetId, ruleId: existing.id, rule: rule
                    )
                } else {
                    rs = try await service.addRule(zoneId: zoneId, rulesetId: rulesetId, rule: rule)
                }
                apply(rs)
            } else {
                // 该 phase 还没有 entrypoint，首条规则创建之
                let rs = try await service.createEntrypoint(zoneId: zoneId, rule: rule)
                apply(rs)
            }
        }
    }

    private func apply(_ ruleset: RateLimitRuleset?) {
        rulesetId = ruleset?.id
        rules = ruleset?.rules ?? []
    }

    private func mutate(_ op: () async throws -> Void) async {
        guard !isMutating else { return }
        isMutating = true
        error = nil
        do {
            try await op()
        } catch {
            self.error = error.localizedDescription
        }
        isMutating = false
    }
}
