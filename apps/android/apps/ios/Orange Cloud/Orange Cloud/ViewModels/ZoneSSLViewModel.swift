//
//  ZoneSSLViewModel.swift
//  Orange Cloud
//
//  Zone 详情页「SSL/TLS」面板：加密模式 / 始终使用 HTTPS / 自动 HTTPS 重写 /
//  最低 TLS 版本 / TLS 1.3。全部走通用 zone 设置端点（zone-settings.read/.write）。
//

import Foundation
import Observation

/// SSL/TLS 加密模式（zone setting `ssl` 的取值）
nonisolated enum SSLMode: String, CaseIterable, Identifiable, Sendable {
    case off, flexible, full, strict

    var id: String { rawValue }

    var title: String {
        switch self {
        case .off:      String(localized: "关闭")
        case .flexible: String(localized: "灵活")
        case .full:     String(localized: "完全")
        case .strict:   String(localized: "完全（严格）")
        }
    }

    var blurb: String {
        switch self {
        case .off:      String(localized: "不加密访客与 Cloudflare 之间的连接。")
        case .flexible: String(localized: "访客到 Cloudflare 加密，Cloudflare 到源站不加密。")
        case .full:     String(localized: "全程加密，但不校验源站证书。")
        case .strict:   String(localized: "全程加密并校验源站证书，最安全。")
        }
    }
}

/// 最低 TLS 版本（zone setting `min_tls_version` 的取值）
nonisolated enum MinTLSVersion: String, CaseIterable, Identifiable, Sendable {
    case v1_0 = "1.0"
    case v1_1 = "1.1"
    case v1_2 = "1.2"
    case v1_3 = "1.3"

    var id: String { rawValue }
    var title: String { "TLS \(rawValue)" }
}

@Observable
@MainActor
final class ZoneSSLViewModel {

    private(set) var sslMode: SSLMode = .full
    private(set) var alwaysUseHTTPS = false
    private(set) var autoHTTPSRewrites = false
    private(set) var minTLS: MinTLSVersion = .v1_0
    private(set) var tls13 = false

    private(set) var loaded = false
    private(set) var isLoading = false
    /// 正在写入的 setting ID（行内 ProgressView / 禁用其它控件）
    var updating: Set<String> = []
    var error: String?

    private let service: ZoneSettingsService
    private let zoneId: String

    init(service: ZoneSettingsService, zoneId: String) {
        self.service = service
        self.zoneId = zoneId
    }

    func load() async {
        guard !loaded, !isLoading else { return }
        isLoading = true
        defer { isLoading = false }

        async let sslT = service.getSetting(zoneId: zoneId, setting: "ssl")
        async let ahT  = service.getSetting(zoneId: zoneId, setting: "always_use_https")
        async let arT  = service.getSetting(zoneId: zoneId, setting: "automatic_https_rewrites")
        async let tlsT = service.getSetting(zoneId: zoneId, setting: "min_tls_version")
        async let t13T = service.getSetting(zoneId: zoneId, setting: "tls_1_3")

        let ssl = try? await sslT
        let ah  = try? await ahT
        let ar  = try? await arT
        let tls = try? await tlsT
        let t13 = try? await t13T

        // 一项都读不到（通常是无 zone-settings.read）→ 保持未加载态，UI 显示锁定
        guard ssl != nil || ah != nil || tls != nil else { return }
        if let ssl, let mode = SSLMode(rawValue: ssl) { sslMode = mode }
        if let ah  { alwaysUseHTTPS = ah == "on" }
        if let ar  { autoHTTPSRewrites = ar == "on" }
        if let tls, let v = MinTLSVersion(rawValue: tls) { minTLS = v }
        if let t13 { tls13 = (t13 == "on" || t13 == "zrt") }
        loaded = true
    }

    func setSSLMode(_ mode: SSLMode) async {
        await update("ssl", value: mode.rawValue) { applied in
            if let m = SSLMode(rawValue: applied) { self.sslMode = m }
        }
    }

    func setAlwaysUseHTTPS(_ on: Bool) async {
        await update("always_use_https", value: on ? "on" : "off") { applied in
            self.alwaysUseHTTPS = applied == "on"
        }
    }

    func setAutoHTTPSRewrites(_ on: Bool) async {
        await update("automatic_https_rewrites", value: on ? "on" : "off") { applied in
            self.autoHTTPSRewrites = applied == "on"
        }
    }

    func setMinTLS(_ v: MinTLSVersion) async {
        await update("min_tls_version", value: v.rawValue) { applied in
            if let mv = MinTLSVersion(rawValue: applied) { self.minTLS = mv }
        }
    }

    func setTLS13(_ on: Bool) async {
        await update("tls_1_3", value: on ? "on" : "off") { applied in
            self.tls13 = (applied == "on" || applied == "zrt")
        }
    }

    /// 写单项设置：乐观锁（同一 setting 写入中再点忽略），成功后用服务端返回值回填
    private func update(_ setting: String, value: String, apply: (String) -> Void) async {
        guard !updating.contains(setting) else { return }
        updating.insert(setting)
        error = nil
        do {
            let applied = try await service.setSetting(zoneId: zoneId, setting: setting, value: value)
            apply(applied)
        } catch {
            self.error = error.localizedDescription
        }
        updating.remove(setting)
    }
}
