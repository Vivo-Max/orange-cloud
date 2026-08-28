//
//  ZoneSSLCertsViewModel.swift
//  Orange Cloud
//
//  SSL 证书展示 + Universal SSL 开关 + 删除证书包（仅非 Universal）。
//

import Foundation
import Observation

@Observable
@MainActor
final class ZoneSSLCertsViewModel {

    private(set) var packs: [SSLCertificatePack] = []
    private(set) var universalEnabled = true
    private(set) var universalLoaded = false
    private(set) var isLoading = false
    private(set) var loaded = false
    var isTogglingUniversal = false
    var isDeleting = false
    var error: String?

    private let service: SSLCertificateService
    private let zoneId: String

    init(service: SSLCertificateService, zoneId: String) {
        self.service = service
        self.zoneId = zoneId
    }

    func load() async {
        guard !isLoading else { return }
        isLoading = true
        defer { isLoading = false }
        error = nil
        do {
            packs = try await service.certificatePacks(zoneId: zoneId)
            loaded = true
        } catch {
            self.error = error.localizedDescription
        }
        // Universal 状态是附加信息，读不到不影响证书列表
        if let enabled = try? await service.universalSSLEnabled(zoneId: zoneId) {
            universalEnabled = enabled
            universalLoaded = true
        }
    }

    func setUniversal(_ enabled: Bool) async {
        guard !isTogglingUniversal else { return }
        isTogglingUniversal = true
        error = nil
        defer { isTogglingUniversal = false }
        do {
            universalEnabled = try await service.setUniversalSSL(zoneId: zoneId, enabled: enabled)
        } catch {
            self.error = error.localizedDescription
        }
    }

    func deletePack(_ pack: SSLCertificatePack) async {
        guard !pack.isUniversal, !isDeleting else { return }
        isDeleting = true
        error = nil
        defer { isDeleting = false }
        do {
            try await service.deleteCertificatePack(zoneId: zoneId, packId: pack.id)
            packs.removeAll { $0.id == pack.id }
        } catch {
            self.error = error.localizedDescription
        }
    }
}
