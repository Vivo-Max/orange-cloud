//
//  DNSSettingsViewModel.swift
//  Orange Cloud
//
//  Zone DNS 设置 + DNSSEC。两条链路权限不同（zone-dns-settings.* / dns.*），
//  任一读失败不连累另一条。
//

import Foundation
import Observation

@Observable
@MainActor
final class DNSSettingsViewModel {

    private(set) var settings: ZoneDNSSettings?
    private(set) var dnssec: ZoneDNSSEC?
    var isLoading = false
    var isMutating = false
    var error: String?
    var didMutate = false

    private let service: DNSSettingsService
    private let zoneId: String

    init(service: DNSSettingsService, zoneId: String) {
        self.service = service
        self.zoneId = zoneId
    }

    var settingsLoaded: Bool { settings != nil }
    var dnssecLoaded: Bool { dnssec != nil }

    func load() async {
        isLoading = true
        error = nil
        // 两条链路各自独立：DNSSEC 无权限时不该让 DNS 设置也不显示
        async let settingsTask = service.settings(zoneId: zoneId)
        async let dnssecTask = service.dnssec(zoneId: zoneId)
        settings = try? await settingsTask
        dnssec = try? await dnssecTask
        isLoading = false
    }

    private func apply(_ update: ZoneDNSSettingsUpdate) async {
        guard !isMutating else { return }
        isMutating = true
        error = nil
        do {
            settings = try await service.updateSettings(zoneId: zoneId, update: update)
            didMutate.toggle()
        } catch {
            self.error = error.localizedDescription
        }
        isMutating = false
    }

    func setFlattenAllCnames(_ on: Bool) async {
        await apply(ZoneDNSSettingsUpdate(flattenAllCnames: on))
    }

    func setMultiProvider(_ on: Bool) async {
        await apply(ZoneDNSSettingsUpdate(multiProvider: on))
    }

    func setSecondaryOverrides(_ on: Bool) async {
        await apply(ZoneDNSSettingsUpdate(secondaryOverrides: on))
    }

    func setZoneMode(_ mode: ZoneMode) async {
        await apply(ZoneDNSSettingsUpdate(zoneMode: mode.rawValue))
    }

    /// 高级名称服务器：用 nameservers.type 而非已弃用的 foundation_dns
    func setAdvancedNameservers(_ on: Bool) async {
        await apply(ZoneDNSSettingsUpdate(
            nameservers: ZoneNameserversUpdate(type: on ? "cloudflare.advanced" : "cloudflare.standard")
        ))
    }

    func setDNSSEC(_ on: Bool) async {
        guard !isMutating else { return }
        isMutating = true
        error = nil
        do {
            dnssec = try await service.setDNSSEC(zoneId: zoneId, enabled: on)
            didMutate.toggle()
        } catch {
            self.error = error.localizedDescription
        }
        isMutating = false
    }
}
