//
//  LoadBalancerViewModels.swift
//  Orange Cloud
//
//  负载均衡：LB 列表（zone，附带 account 池供编辑器选择）/ 池列表（account + 健康）/ 监测列表（account）。
//

import Foundation
import Observation

// MARK: - Load Balancer（zone）

@Observable
@MainActor
final class LoadBalancerListViewModel {

    private(set) var loadBalancers: [LoadBalancer] = []
    private(set) var pools: [Pool] = []       // 供编辑器选择 default/fallback 池 + 名称解析
    var isLoading = false
    var loaded = false
    var isMutating = false
    var error: String?
    var didMutate = false

    private let service: LoadBalancerService
    let zoneId: String
    let accountId: String

    init(service: LoadBalancerService, zoneId: String, accountId: String) {
        self.service = service
        self.zoneId = zoneId
        self.accountId = accountId
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            async let lbs = service.listLoadBalancers(zoneId: zoneId)
            async let pls = service.listPools(accountId: accountId)
            loadBalancers = try await lbs
            pools = (try? await pls) ?? []     // 池获取失败不连累 LB 列表
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func poolName(_ id: String) -> String {
        pools.first { $0.id == id }?.name ?? id
    }

    /// 新建（lbId == nil）或编辑
    func save(lbId: String?, body: LoadBalancerUpdate) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            if let lbId {
                _ = try await service.updateLoadBalancer(zoneId: zoneId, lbId: lbId, body: body)
            } else {
                _ = try await service.createLoadBalancer(zoneId: zoneId, body: body)
            }
            await reloadLBs()
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func toggle(_ lb: LoadBalancer, enabled: Bool) async {
        guard !isMutating else { return }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            _ = try await service.updateLoadBalancer(zoneId: zoneId, lbId: lb.id, body: LoadBalancerUpdate(enabled: enabled))
            await reloadLBs()
            didMutate.toggle()
        } catch {
            self.error = error.localizedDescription
        }
    }

    func delete(_ lb: LoadBalancer) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            try await service.deleteLoadBalancer(zoneId: zoneId, lbId: lb.id)
            loadBalancers.removeAll { $0.id == lb.id }
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    private func reloadLBs() async {
        if let lbs = try? await service.listLoadBalancers(zoneId: zoneId) { loadBalancers = lbs }
    }
}

// MARK: - 源站池（account）

@Observable
@MainActor
final class PoolListViewModel {

    private(set) var pools: [Pool] = []
    private(set) var monitors: [Monitor] = []       // 供池编辑器选择监测
    private(set) var healthByPool: [String: PoolHealthResponse] = [:]
    var isLoading = false
    var loaded = false
    var isMutating = false
    var error: String?
    var didMutate = false

    private let service: LoadBalancerService
    let accountId: String

    init(service: LoadBalancerService, accountId: String) {
        self.service = service
        self.accountId = accountId
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            async let pls = service.listPools(accountId: accountId)
            async let mons = service.listMonitors(accountId: accountId)
            pools = try await pls
            monitors = (try? await mons) ?? []
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
        await loadHealth()
    }

    private func loadHealth() async {
        let service = self.service
        let accountId = self.accountId
        let ids = pools.map(\.id)
        let map = await withTaskGroup(of: (String, PoolHealthResponse?).self) { group in
            for id in ids {
                group.addTask { (id, try? await service.poolHealth(accountId: accountId, poolId: id)) }
            }
            var result: [String: PoolHealthResponse] = [:]
            for await (id, health) in group {
                if let health { result[id] = health }
            }
            return result
        }
        healthByPool = map
    }

    func monitorName(_ id: String?) -> String {
        guard let id, !id.isEmpty else { return String(localized: "无") }
        return monitors.first { $0.id == id }?.summary ?? id
    }

    func healthText(for pool: Pool) -> String? {
        guard let h = healthByPool[pool.id], h.totalCount > 0 else { return nil }
        return String(localized: "\(h.healthyCount)/\(h.totalCount) 数据中心健康")
    }

    func save(poolId: String?, body: PoolUpdate) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            if let poolId {
                _ = try await service.updatePool(accountId: accountId, poolId: poolId, body: body)
            } else {
                _ = try await service.createPool(accountId: accountId, body: body)
            }
            await reloadPools()
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func toggle(_ pool: Pool, enabled: Bool) async {
        guard !isMutating else { return }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            _ = try await service.updatePool(accountId: accountId, poolId: pool.id, body: PoolUpdate(enabled: enabled))
            await reloadPools()
            didMutate.toggle()
        } catch {
            self.error = error.localizedDescription
        }
    }

    func delete(_ pool: Pool) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            try await service.deletePool(accountId: accountId, poolId: pool.id)
            pools.removeAll { $0.id == pool.id }
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    private func reloadPools() async {
        if let pls = try? await service.listPools(accountId: accountId) { pools = pls }
    }
}

// MARK: - 健康监测（account）

@Observable
@MainActor
final class MonitorListViewModel {

    private(set) var monitors: [Monitor] = []
    var isLoading = false
    var loaded = false
    var isMutating = false
    var error: String?
    var didMutate = false

    private let service: LoadBalancerService
    let accountId: String

    init(service: LoadBalancerService, accountId: String) {
        self.service = service
        self.accountId = accountId
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            monitors = try await service.listMonitors(accountId: accountId)
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func save(monitorId: String?, body: MonitorUpdate) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            if let monitorId {
                _ = try await service.updateMonitor(accountId: accountId, monitorId: monitorId, body: body)
            } else {
                _ = try await service.createMonitor(accountId: accountId, body: body)
            }
            await reload()
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func delete(_ monitor: Monitor) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            try await service.deleteMonitor(accountId: accountId, monitorId: monitor.id)
            monitors.removeAll { $0.id == monitor.id }
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    private func reload() async {
        if let mons = try? await service.listMonitors(accountId: accountId) { monitors = mons }
    }
}
