//
//  ZoneAccessRulesViewModel.swift
//  Orange Cloud
//
//  IP 访问规则 CRUD。编辑仅改 mode + notes（匹配对象不可变）。
//

import Foundation
import Observation

@Observable
@MainActor
final class ZoneAccessRulesViewModel {

    private(set) var rules: [FirewallAccessRule] = []
    private(set) var isLoading = false
    private(set) var loaded = false
    var isSaving = false
    var error: String?

    private let service: FirewallAccessRuleService
    private let zoneId: String

    init(service: FirewallAccessRuleService, zoneId: String) {
        self.service = service
        self.zoneId = zoneId
    }

    func load() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        error = nil
        do {
            rules = try await service.rules(zoneId: zoneId)
            loaded = true
        } catch is CancellationError {
            // 下拉刷新 / searchable 取消，不算加载失败
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
    }

    func create(mode: String, target: AccessRuleTarget, value: String, notes: String?) async -> Bool {
        guard !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let created = try await service.createRule(
                zoneId: zoneId,
                draft: AccessRuleCreate(
                    mode: mode,
                    configuration: AccessRuleConfigInput(target: target.rawValue, value: value),
                    notes: notes
                )
            )
            rules.insert(created, at: 0)
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func update(ruleId: String, mode: String, notes: String?) async -> Bool {
        guard !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let updated = try await service.updateRule(
                zoneId: zoneId, ruleId: ruleId,
                update: AccessRuleUpdate(mode: mode, notes: notes)
            )
            if let i = rules.firstIndex(where: { $0.id == ruleId }) { rules[i] = updated }
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func delete(_ rule: FirewallAccessRule) async {
        error = nil
        do {
            try await service.deleteRule(zoneId: zoneId, ruleId: rule.id)
            rules.removeAll { $0.id == rule.id }
        } catch {
            self.error = error.localizedDescription
        }
    }
}
