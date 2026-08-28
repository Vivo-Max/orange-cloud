//
//  ZeroTrustViewModels.swift
//  Orange Cloud
//
//  Zero Trust 只读：Access 应用 / Gateway 策略列表加载。
//

import Foundation
import Observation

@Observable
@MainActor
final class AccessAppsViewModel {

    private(set) var apps: [AccessApp] = []
    private(set) var loaded = false
    var isLoading = false
    var isSaving = false
    var error: String?
    var didChange = false       // sensoryFeedback 触发器

    private let service: ZeroTrustService
    let accountId: String?

    init(service: ZeroTrustService, accountId: String?) {
        self.service = service
        self.accountId = accountId
    }

    func load() async {
        guard !isLoading, let accountId else { return }
        isLoading = true
        error = nil
        do {
            apps = try await service.accessApps(accountId: accountId)
            loaded = true
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    /// 取应用详情（含完整策略规则），编辑前用
    func detail(appId: String) async -> AccessApp? {
        guard let accountId else { return nil }
        do {
            return try await service.accessApp(accountId: accountId, appId: appId)
        } catch {
            self.error = error.localizedDescription
            return nil
        }
    }

    /// 新建：先建可复用策略，再建应用引用它。hostnames 为全部公共主机名（保序，第一个作主 domain）。
    func create(name: String, hostnames: [String], sessionDuration: String, decision: String, include: [AccessRule]) async -> Bool {
        guard let accountId, !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let policy = try await service.createAccessPolicy(
                accountId: accountId,
                body: AccessPolicyInput(name: String(localized: "\(name) 策略"), decision: decision, include: include)
            )
            guard let policyId = policy.id else {
                error = String(localized: "策略创建未返回 ID")
                return false
            }
            do {
                _ = try await service.createAccessApp(
                    accountId: accountId,
                    body: AccessAppInput(
                        name: name,
                        domain: hostnames.first ?? "",
                        destinations: hostnames.map { AccessDestinationInput(type: "public", uri: $0) },
                        type: "self_hosted", sessionDuration: sessionDuration, policies: [policyId]
                    )
                )
            } catch {
                // 应用创建失败：补偿删掉刚建的可复用策略，避免账号里堆积孤儿策略
                try? await service.deleteAccessPolicy(accountId: accountId, policyId: policyId)
                throw error
            }
            await load()
            didChange.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    /// 编辑：可选先 PATCH 策略规则，再 PUT 应用（policyIds 为现有引用，保持不变）。
    /// hostnames 为编辑后的全部公共主机名；preserved 为原应用的非 public 目标，原样回写避免误删。
    /// PUT 全量替换——必须把全部主机名一起回写，否则会把没提交的主机名删掉。
    func update(
        appId: String,
        policyIds: [String],
        name: String,
        hostnames: [String],
        preservedDestinations: [AccessDestination],
        sessionDuration: String,
        policyPatch: (id: String, decision: String, include: [AccessRule])?
    ) async -> Bool {
        guard let accountId, !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            if let patch = policyPatch {
                try await service.updateAccessPolicy(
                    accountId: accountId, policyId: patch.id,
                    body: AccessPolicyInput(name: String(localized: "\(name) 策略"), decision: patch.decision, include: patch.include)
                )
            }
            let destinations = hostnames.map { AccessDestinationInput(type: "public", uri: $0) }
                + preservedDestinations.compactMap { dest in
                    dest.uri.map { AccessDestinationInput(type: dest.type ?? "private", uri: $0) }
                }
            _ = try await service.updateAccessApp(
                accountId: accountId, appId: appId,
                body: AccessAppInput(
                    name: name, domain: hostnames.first ?? "",
                    destinations: destinations,
                    type: "self_hosted", sessionDuration: sessionDuration, policies: policyIds
                )
            )
            await load()
            didChange.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func delete(_ app: AccessApp) async {
        guard let accountId, !isSaving else { return }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            try await service.deleteAccessApp(accountId: accountId, appId: app.id)
            apps.removeAll { $0.id == app.id }
            didChange.toggle()
        } catch {
            self.error = error.localizedDescription
        }
    }
}

@Observable
@MainActor
final class GatewayRulesViewModel {

    private(set) var rules: [GatewayRule] = []
    private(set) var loaded = false
    var isLoading = false
    var isSaving = false
    var error: String?
    var didChange = false       // sensoryFeedback 触发器

    private let service: ZeroTrustService
    let accountId: String?

    init(service: ZeroTrustService, accountId: String?) {
        self.service = service
        self.accountId = accountId
    }

    func load() async {
        guard !isLoading, let accountId else { return }
        isLoading = true
        error = nil
        do {
            rules = try await service.gatewayRules(accountId: accountId)
            loaded = true
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    /// 新建：成功后插到列表顶端
    func create(_ input: GatewayRuleInput) async -> Bool {
        guard let accountId, !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let created = try await service.createGatewayRule(accountId: accountId, body: input)
            rules.insert(created, at: 0)
            didChange.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    /// 编辑：成功后替换列表中对应项
    func update(ruleId: String, _ input: GatewayRuleInput) async -> Bool {
        guard let accountId, !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let updated = try await service.updateGatewayRule(accountId: accountId, ruleId: ruleId, body: input)
            if let idx = rules.firstIndex(where: { $0.id == ruleId }) { rules[idx] = updated }
            didChange.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    /// 启停：PUT 全量回写，仅翻转 enabled，保留其余字段
    func toggle(_ rule: GatewayRule) async {
        guard let accountId, !isSaving else { return }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let input = GatewayRuleInput(from: rule, enabledOverride: !rule.isEnabled)
            let updated = try await service.updateGatewayRule(accountId: accountId, ruleId: rule.id, body: input)
            if let idx = rules.firstIndex(where: { $0.id == rule.id }) { rules[idx] = updated }
            didChange.toggle()
        } catch {
            self.error = error.localizedDescription
        }
    }

    /// 删除：成功后从列表移除
    func delete(_ rule: GatewayRule) async {
        guard let accountId, !isSaving else { return }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            try await service.deleteGatewayRule(accountId: accountId, ruleId: rule.id)
            rules.removeAll { $0.id == rule.id }
            didChange.toggle()
        } catch {
            self.error = error.localizedDescription
        }
    }
}
