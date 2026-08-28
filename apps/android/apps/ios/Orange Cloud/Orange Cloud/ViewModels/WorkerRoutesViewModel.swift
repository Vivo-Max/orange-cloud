//
//  WorkerRoutesViewModel.swift
//  Orange Cloud
//
//  Worker 的域名/路由：workers.dev 子域开关 + 自定义域（挂/卸）+ Zone 路由（加/删）。
//  Zone 路由按账号下各 zone 逐个查询、过滤到本脚本（routes 端点是 zone 级）。
//

import Foundation
import Observation

@Observable
@MainActor
final class WorkerRoutesViewModel {

    private(set) var subdomain:     WorkerSubdomain?
    private(set) var accountSubdomain: String?      // 账号级 workers.dev 前缀，用于拼完整地址
    private(set) var customDomains: [WorkerCustomDomain] = []
    private(set) var routes:        [ScopedWorkerRoute] = []
    private(set) var zones:         [Zone] = []
    private(set) var loaded = false
    var isLoading = false
    var isSaving  = false
    var togglingSubdomain = false
    var error: String?

    private let service:     WorkerService
    private let zoneService: ZoneService
    let accountId:  String
    let scriptName: String

    init(service: WorkerService, zoneService: ZoneService, accountId: String, scriptName: String) {
        self.service     = service
        self.zoneService = zoneService
        self.accountId   = accountId
        self.scriptName  = scriptName
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            zones         = try await zoneService.listZones(accountId: accountId)
            customDomains = try await service.customDomains(accountId: accountId, scriptName: scriptName)
            // 子域可能未在账号开通，单独容错，不阻断整页
            subdomain        = try? await service.subdomain(accountId: accountId, scriptName: scriptName)
            accountSubdomain = try? await service.accountSubdomain(accountId: accountId)
            await loadRoutes()
            loaded = true
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    /// 逐 zone 查路由并过滤本脚本（顺序请求，避免跨 actor 的 Sendable 复杂度）
    private func loadRoutes() async {
        var collected: [ScopedWorkerRoute] = []
        for zone in zones {
            let zoneRoutes = (try? await service.routes(zoneId: zone.id)) ?? []
            for route in zoneRoutes where route.script == scriptName {
                collected.append(ScopedWorkerRoute(zoneId: zone.id, zoneName: zone.name, route: route))
            }
        }
        routes = collected.sorted { $0.route.pattern < $1.route.pattern }
    }

    // MARK: - workers.dev 子域

    /// 子域已开启且账号前缀已知时的完整访问地址：https://<脚本名>.<前缀>.workers.dev
    var workersDevURL: URL? {
        guard subdomain?.enabled == true, let prefix = accountSubdomain, !prefix.isEmpty else { return nil }
        return URL(string: "https://\(scriptName).\(prefix).workers.dev")
    }

    func toggleSubdomain(_ enabled: Bool) async {
        guard !togglingSubdomain else { return }
        togglingSubdomain = true
        error = nil
        do {
            try await service.setSubdomain(accountId: accountId, scriptName: scriptName, enabled: enabled)
            subdomain = try? await service.subdomain(accountId: accountId, scriptName: scriptName)
        } catch {
            self.error = error.localizedDescription
        }
        togglingSubdomain = false
    }

    // MARK: - 自定义域

    func attachDomain(hostname: String, zoneId: String) async -> Bool {
        guard !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            try await service.attachDomain(accountId: accountId, scriptName: scriptName, hostname: hostname, zoneId: zoneId)
            customDomains = (try? await service.customDomains(accountId: accountId, scriptName: scriptName)) ?? customDomains
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func detachDomain(_ domain: WorkerCustomDomain) async {
        error = nil
        do {
            try await service.deleteDomain(accountId: accountId, domainId: domain.id)
            customDomains.removeAll { $0.id == domain.id }
        } catch {
            self.error = error.localizedDescription
        }
    }

    // MARK: - Zone 路由

    func addRoute(zoneId: String, pattern: String) async -> Bool {
        guard !isSaving else { return false }
        let trimmed = pattern.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            try await service.createRoute(zoneId: zoneId, pattern: trimmed, scriptName: scriptName)
            await loadRoutes()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func deleteRoute(_ scoped: ScopedWorkerRoute) async {
        error = nil
        do {
            try await service.deleteRoute(zoneId: scoped.zoneId, routeId: scoped.route.id)
            routes.removeAll { $0.id == scoped.id }
        } catch {
            self.error = error.localizedDescription
        }
    }
}
