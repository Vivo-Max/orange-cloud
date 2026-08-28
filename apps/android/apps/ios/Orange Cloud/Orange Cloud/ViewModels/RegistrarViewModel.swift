//
//  RegistrarViewModel.swift
//  Orange Cloud
//
//  在 Cloudflare 注册的域名。只读 + 自动续费开关——
//  转移锁在新版 API 里是只读的，域名注册是花钱操作、移动端不做。
//

import Foundation
import Observation

@Observable
@MainActor
final class RegistrarViewModel {

    private(set) var registrations: [DomainRegistration] = []
    var isLoading = false
    var loaded = false
    var isMutating = false
    var error: String?
    var didMutate = false

    private let service: RegistrarService
    private let accountId: String

    init(service: RegistrarService, accountId: String) {
        self.service = service
        self.accountId = accountId
    }

    /// 快到期又没开自动续费的排最前，其余按到期日升序
    private func sorted(_ items: [DomainRegistration]) -> [DomainRegistration] {
        items.sorted { lhs, rhs in
            if lhs.needsAttention != rhs.needsAttention { return lhs.needsAttention }
            switch (lhs.expiryDate, rhs.expiryDate) {
            case let (l?, r?): return l < r
            case (nil, _?):    return false
            case (_?, nil):    return true
            default:           return lhs.domainName < rhs.domainName
            }
        }
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            registrations = sorted(try await service.registrations(accountId: accountId))
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    /// 接口是异步 workflow，返回后回读列表拿真实状态
    func setAutoRenew(_ registration: DomainRegistration, enabled: Bool) async {
        guard !isMutating else { return }
        isMutating = true
        error = nil
        do {
            try await service.setAutoRenew(
                accountId: accountId, domainName: registration.domainName, enabled: enabled
            )
            didMutate.toggle()
            await load()
        } catch {
            self.error = error.localizedDescription
        }
        isMutating = false
    }
}
