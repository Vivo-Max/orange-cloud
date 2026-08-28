//
//  HealthCheckViewModel.swift
//  Orange Cloud
//
//  独立健康检查列表：查看源站状态 + 暂停/恢复 + 删除。
//  新建检查字段较多（协议、区域、重试、超时、HTTP 配置），属桌面场景，暂不在移动端提供。
//

import Foundation
import Observation

@Observable
@MainActor
final class HealthCheckViewModel {

    private(set) var checks: [HealthCheck] = []
    var isLoading = false
    var loaded = false
    var isMutating = false
    var error: String?
    var didMutate = false

    private let service: HealthCheckService
    let zoneId: String

    init(service: HealthCheckService, zoneId: String) {
        self.service = service
        self.zoneId = zoneId
    }

    /// 异常的排在前面——进页第一眼就该看到出问题的源站
    private func sorted(_ items: [HealthCheck]) -> [HealthCheck] {
        items.sorted { lhs, rhs in
            func rank(_ c: HealthCheck) -> Int {
                switch c.displayStatus {
                case .unhealthy: 0
                case .unknown:   1
                case .healthy:   2
                case .suspended: 3
                }
            }
            let (l, r) = (rank(lhs), rank(rhs))
            return l == r ? lhs.name.localizedStandardCompare(rhs.name) == .orderedAscending : l < r
        }
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            checks = sorted(try await service.list(zoneId: zoneId))
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func setSuspended(_ check: HealthCheck, suspended: Bool) async {
        guard !isMutating else { return }
        isMutating = true
        error = nil
        do {
            let updated = try await service.setSuspended(
                zoneId: zoneId, checkId: check.id, suspended: suspended
            )
            if let index = checks.firstIndex(where: { $0.id == check.id }) {
                checks[index] = updated
                checks = sorted(checks)
            }
            didMutate.toggle()
        } catch {
            self.error = error.localizedDescription
        }
        isMutating = false
    }

    @discardableResult
    func delete(_ check: HealthCheck) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            try await service.delete(zoneId: zoneId, checkId: check.id)
            checks.removeAll { $0.id == check.id }
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }
}
