//
//  ZonePerformanceViewModel.swift
//  Orange Cloud
//
//  Zone 详情页「性能与缓存」面板：网络优化开关（Brotli / HTTP2 / HTTP3 / 0-RTT /
//  Early Hints / WebSockets / IPv6）+ 缓存控制（缓存级别 / Always Online / 查询字符串排序）。
//  全部走通用 zone 设置端点，读 zone-settings.read，写 zone-settings.write。
//

import Foundation
import Observation

/// 缓存级别（zone setting `cache_level` 的取值）
nonisolated enum CacheLevel: String, CaseIterable, Identifiable, Sendable {
    case basic, simplified, aggressive

    var id: String { rawValue }

    var title: String {
        switch self {
        case .basic:      String(localized: "无查询字符串")
        case .simplified: String(localized: "忽略查询字符串")
        case .aggressive: String(localized: "标准")
        }
    }
}

@Observable
@MainActor
final class ZonePerformanceViewModel {

    /// 网络优化里的 on/off 设置，按 Cloudflare 仪表盘顺序
    static let networkToggles = ["brotli", "http2", "http3", "0rtt", "early_hints", "websockets", "ipv6"]

    private(set) var values: [String: String] = [:]
    private(set) var loaded = false
    private(set) var isLoading = false
    /// 正在写入的 setting ID
    var updating: Set<String> = []
    var error: String?

    private let service: ZoneSettingsService
    private let zoneId: String

    init(service: ZoneSettingsService, zoneId: String) {
        self.service = service
        self.zoneId = zoneId
    }

    func isOn(_ id: String) -> Bool { values[id] == "on" }
    var cacheLevel: CacheLevel { CacheLevel(rawValue: values["cache_level"] ?? "") ?? .aggressive }

    func load() async {
        guard !loaded, !isLoading else { return }
        isLoading = true
        defer { isLoading = false }

        async let brotli      = service.getSetting(zoneId: zoneId, setting: "brotli")
        async let http2       = service.getSetting(zoneId: zoneId, setting: "http2")
        async let http3       = service.getSetting(zoneId: zoneId, setting: "http3")
        async let zeroRTT     = service.getSetting(zoneId: zoneId, setting: "0rtt")
        async let earlyHints  = service.getSetting(zoneId: zoneId, setting: "early_hints")
        async let websockets  = service.getSetting(zoneId: zoneId, setting: "websockets")
        async let ipv6        = service.getSetting(zoneId: zoneId, setting: "ipv6")
        async let cacheLevel  = service.getSetting(zoneId: zoneId, setting: "cache_level")
        async let alwaysOnline = service.getSetting(zoneId: zoneId, setting: "always_online")
        async let sortQS      = service.getSetting(zoneId: zoneId, setting: "sort_query_string_for_cache")

        var acc: [String: String] = [:]
        if let v = try? await brotli       { acc["brotli"] = v }
        if let v = try? await http2        { acc["http2"] = v }
        if let v = try? await http3        { acc["http3"] = v }
        if let v = try? await zeroRTT      { acc["0rtt"] = v }
        if let v = try? await earlyHints   { acc["early_hints"] = v }
        if let v = try? await websockets   { acc["websockets"] = v }
        if let v = try? await ipv6         { acc["ipv6"] = v }
        if let v = try? await cacheLevel   { acc["cache_level"] = v }
        if let v = try? await alwaysOnline { acc["always_online"] = v }
        if let v = try? await sortQS       { acc["sort_query_string_for_cache"] = v }

        // 一项都读不到（通常是无 zone-settings.read）→ 保持未加载态，UI 显示锁定
        guard !acc.isEmpty else { return }
        values = acc
        loaded = true
    }

    func setToggle(_ id: String, _ on: Bool) async {
        await update(id, value: on ? "on" : "off")
    }

    func setCacheLevel(_ level: CacheLevel) async {
        await update("cache_level", value: level.rawValue)
    }

    private func update(_ id: String, value: String) async {
        guard !updating.contains(id) else { return }
        updating.insert(id)
        error = nil
        do {
            let applied = try await service.setSetting(zoneId: zoneId, setting: id, value: value)
            values[id] = applied
        } catch {
            self.error = error.localizedDescription
        }
        updating.remove(id)
    }
}
