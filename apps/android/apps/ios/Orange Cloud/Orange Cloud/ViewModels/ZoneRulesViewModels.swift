//
//  ZoneRulesViewModels.swift
//  Orange Cloud
//
//  「规则」入口下的三组 ViewModel：phase 泛化规则（查看/启停/删除）、
//  Page Rules（传统）、URL Normalization。
//

import Foundation
import Observation

// MARK: - Rulesets phase 泛化

@Observable
@MainActor
final class ZonePhaseRulesViewModel {

    private(set) var ruleset: ZoneRuleset?
    private(set) var rules: [ZoneRule] = []
    var isLoading = false
    var loaded = false
    var isMutating = false
    var isSaving = false
    var error: String?
    var didMutate = false

    private let service: ZoneRulesetService
    private let zoneId: String
    let phase: ZoneRulePhase

    init(service: ZoneRulesetService, zoneId: String, phase: ZoneRulePhase) {
        self.service = service
        self.zoneId = zoneId
        self.phase = phase
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            let result = try await service.ruleset(zoneId: zoneId, phase: phase)
            ruleset = result
            rules = result?.rules ?? []
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func setEnabled(_ rule: ZoneRule, enabled: Bool) async {
        guard !isMutating, let rulesetId = ruleset?.id else { return }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            let updated = try await service.setRuleEnabled(
                zoneId: zoneId, rulesetId: rulesetId, ruleId: rule.id, enabled: enabled
            )
            ruleset = updated
            rules = updated.rules ?? []
            didMutate.toggle()
        } catch {
            self.error = error.localizedDescription
        }
    }

    func delete(_ rule: ZoneRule) async {
        guard !isMutating, let rulesetId = ruleset?.id else { return }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            try await service.deleteRule(zoneId: zoneId, rulesetId: rulesetId, ruleId: rule.id)
            rules.removeAll { $0.id == rule.id }
            didMutate.toggle()
        } catch {
            self.error = error.localizedDescription
        }
    }

    /// 新建（ruleId nil）或更新规则；zone 首条规则时自动建 entrypoint
    func save(ruleId: String?, payload: ZoneRulePayload) async -> Bool {
        guard !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let updated: ZoneRuleset
            if let ruleId, let rulesetId = ruleset?.id {
                updated = try await service.updateRule(zoneId: zoneId, rulesetId: rulesetId, ruleId: ruleId, rule: payload)
            } else if let rulesetId = ruleset?.id {
                updated = try await service.addRule(zoneId: zoneId, rulesetId: rulesetId, rule: payload)
            } else {
                updated = try await service.createEntrypoint(zoneId: zoneId, phase: phase, rule: payload)
            }
            ruleset = updated
            rules = updated.rules ?? []
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }
}

// MARK: - Page Rules（传统）

@Observable
@MainActor
final class PageRulesViewModel {

    private(set) var rules: [PageRule] = []
    var isLoading = false
    var loaded = false
    var isMutating = false
    var error: String?
    var didMutate = false

    private let service: ZoneRulesetService
    private let zoneId: String

    init(service: ZoneRulesetService, zoneId: String) {
        self.service = service
        self.zoneId = zoneId
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            rules = try await service.listPageRules(zoneId: zoneId)
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func setActive(_ rule: PageRule, active: Bool) async {
        guard !isMutating else { return }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            let updated = try await service.setPageRuleStatus(zoneId: zoneId, ruleId: rule.id, active: active)
            if let index = rules.firstIndex(where: { $0.id == rule.id }) {
                rules[index] = updated
            }
            didMutate.toggle()
        } catch {
            self.error = error.localizedDescription
        }
    }

    func delete(_ rule: PageRule) async {
        guard !isMutating else { return }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            try await service.deletePageRule(zoneId: zoneId, ruleId: rule.id)
            rules.removeAll { $0.id == rule.id }
            didMutate.toggle()
        } catch {
            self.error = error.localizedDescription
        }
    }
}

// MARK: - URL Normalization

@Observable
@MainActor
final class URLNormalizationViewModel {

    var value: URLNormalization?
    var isLoading = false
    var isMutating = false
    var error: String?
    var didMutate = false

    private let service: ZoneRulesetService
    private let zoneId: String

    init(service: ZoneRulesetService, zoneId: String) {
        self.service = service
        self.zoneId = zoneId
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            value = try await service.urlNormalization(zoneId: zoneId)
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func update(type: String? = nil, scope: String? = nil) async {
        guard !isMutating, var updated = value else { return }
        if let type { updated.type = type }
        if let scope { updated.scope = scope }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            value = try await service.setURLNormalization(zoneId: zoneId, value: updated)
            didMutate.toggle()
        } catch {
            self.error = error.localizedDescription
            // 回读服务器真值，避免 UI 停在未生效的选择上
            await load()
        }
    }
}
