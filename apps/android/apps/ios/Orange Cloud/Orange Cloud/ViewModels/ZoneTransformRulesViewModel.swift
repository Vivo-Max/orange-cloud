//
//  ZoneTransformRulesViewModel.swift
//  Orange Cloud
//
//  Transform Rules CRUD：三个 phase 各持一份 entrypoint ruleset，按 phase 增删改查。
//

import Foundation
import Observation

@Observable
@MainActor
final class ZoneTransformRulesViewModel {

    /// 按 phase rawValue 存当前 entrypoint ruleset（无规则集的 phase 不入表）
    private(set) var rulesetByPhase: [String: TransformRuleset] = [:]
    private(set) var loaded = false
    var isLoading = false
    var isSaving = false
    var togglingRuleId: String?
    var error: String?

    private let service: TransformRuleService
    private let zoneId: String

    init(service: TransformRuleService, zoneId: String) {
        self.service = service
        self.zoneId = zoneId
    }

    func rules(for phase: TransformPhase) -> [TransformRule] {
        rulesetByPhase[phase.rawValue]?.rules ?? []
    }

    var hasAnyRule: Bool {
        TransformPhase.allCases.contains { !rules(for: $0).isEmpty }
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            var map: [String: TransformRuleset] = [:]
            for phase in TransformPhase.allCases {
                if let rs = try await service.ruleset(zoneId: zoneId, phase: phase.rawValue) {
                    map[phase.rawValue] = rs
                }
            }
            rulesetByPhase = map
            loaded = true
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func toggle(phase: TransformPhase, rule: TransformRule, enabled: Bool) async {
        guard let rulesetId = rulesetByPhase[phase.rawValue]?.id, togglingRuleId == nil else { return }
        togglingRuleId = rule.id
        error = nil
        do {
            rulesetByPhase[phase.rawValue] = try await service.setRuleEnabled(
                zoneId: zoneId, rulesetId: rulesetId, ruleId: rule.id, enabled: enabled
            )
        } catch {
            self.error = error.localizedDescription
        }
        togglingRuleId = nil
    }

    /// 新建（ruleId == nil）或更新。成功返回 true。
    func save(phase: TransformPhase, ruleId: String?, draft: TransformRuleCreate) async -> Bool {
        guard !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            if let ruleId {
                guard let rulesetId = rulesetByPhase[phase.rawValue]?.id else { return false }
                rulesetByPhase[phase.rawValue] = try await service.updateRule(
                    zoneId: zoneId, rulesetId: rulesetId, ruleId: ruleId, rule: draft
                )
            } else if let rulesetId = rulesetByPhase[phase.rawValue]?.id {
                rulesetByPhase[phase.rawValue] = try await service.addRule(
                    zoneId: zoneId, rulesetId: rulesetId, rule: draft
                )
            } else {
                rulesetByPhase[phase.rawValue] = try await service.createEntrypoint(
                    zoneId: zoneId, phase: phase.rawValue, rule: draft
                )
            }
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func delete(phase: TransformPhase, rule: TransformRule) async {
        guard let rulesetId = rulesetByPhase[phase.rawValue]?.id else { return }
        error = nil
        do {
            try await service.deleteRule(zoneId: zoneId, rulesetId: rulesetId, ruleId: rule.id)
            // 删除响应即更新后的 ruleset，但统一重读该 phase 更稳
            rulesetByPhase[phase.rawValue] = try await service.ruleset(zoneId: zoneId, phase: phase.rawValue)
        } catch {
            self.error = error.localizedDescription
        }
    }
}
