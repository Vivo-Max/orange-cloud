//
//  CacheRulesViewModel.swift
//  Orange Cloud
//
//  Cache Rules CRUD：单个 entrypoint ruleset，增删改查 + 启停。
//

import Foundation
import Observation

@Observable
@MainActor
final class CacheRulesViewModel {

    private(set) var ruleset: CacheRuleset?
    private(set) var loaded = false        // 区分「未加载」与「加载过但没有规则」
    var isLoading = false
    var isSaving = false
    var togglingRuleId: String?
    var error: String?

    var rules: [CacheRule] { ruleset?.rules ?? [] }

    private let service: CacheRuleService
    private let zoneId: String

    init(service: CacheRuleService, zoneId: String) {
        self.service = service
        self.zoneId = zoneId
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            ruleset = try await service.ruleset(zoneId: zoneId)
            loaded = true
        } catch is CancellationError {
            // 下拉刷新 / searchable 取消，不算加载失败
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func toggle(rule: CacheRule, enabled: Bool) async {
        guard let rulesetId = ruleset?.id, togglingRuleId == nil else { return }
        togglingRuleId = rule.id
        error = nil
        do {
            ruleset = try await service.setRuleEnabled(
                zoneId: zoneId, rulesetId: rulesetId, ruleId: rule.id, enabled: enabled
            )
        } catch {
            self.error = error.localizedDescription
        }
        togglingRuleId = nil
    }

    /// 新建（ruleId == nil）或更新。成功返回 true。
    func save(ruleId: String?, draft: CacheRuleCreate) async -> Bool {
        guard !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            if let ruleId {
                guard let rulesetId = ruleset?.id else { return false }
                ruleset = try await service.updateRule(
                    zoneId: zoneId, rulesetId: rulesetId, ruleId: ruleId, rule: draft
                )
            } else if let rulesetId = ruleset?.id {
                ruleset = try await service.addRule(zoneId: zoneId, rulesetId: rulesetId, rule: draft)
            } else {
                ruleset = try await service.createEntrypoint(zoneId: zoneId, rule: draft)
            }
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func delete(rule: CacheRule) async {
        guard let rulesetId = ruleset?.id else { return }
        error = nil
        do {
            try await service.deleteRule(zoneId: zoneId, rulesetId: rulesetId, ruleId: rule.id)
            await load()
        } catch {
            self.error = error.localizedDescription
        }
    }
}
