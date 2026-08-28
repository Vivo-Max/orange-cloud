//
//  WatchBridge.swift
//  Orange Cloud Watch Watch App
//
//  Watch 侧 WatchConnectivity 客户端：接收 iPhone 推来的 token + 快照，
//  落地（token → 共享钥匙串，快照 → App Group）并驱动界面。
//
//  纯消费者：绝不自行刷新 token（Cloudflare refresh_token 轮转，多处刷新会互相作废）。
//  token 临期且手机可达时，经 sendMessage 向 iPhone 索取——刷新只在 iPhone 那一处发生。
//

import Foundation
import SwiftUI
import WidgetKit
import WatchConnectivity

@MainActor
@Observable
final class WatchBridge: NSObject {

    var zones:       [WidgetZoneMetrics] = []
    var usage:       WidgetUsageData?
    var accountAnalyticsUnavailable: Bool = false   // 账户级数据无权限（免费账号）
    var accountName: String = ""
    var lastUpdated: Date?
    var hasToken:    Bool = false
    var isReachable: Bool = false

    private static let accountNameKey = "watchAccountName"

    override init() {
        super.init()
        loadFromStore()
    }

    func activate() {
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        session.delegate = self
        session.activate()
    }

    // MARK: - 派生数据

    /// 全账号 24h 请求合计
    var totalRequests: Int { zones.reduce(0) { $0 + $1.requests } }

    /// 各 Zone 24h 序列按「距今相同小时」对齐求和（供概览迷你折线）
    var aggregatedSeries: [Int] {
        guard !zones.isEmpty else { return [] }
        let length = zones.map(\.requestsSeries.count).max() ?? 0
        guard length > 1 else { return [] }
        var summed = [Int](repeating: 0, count: length)
        for zone in zones {
            let offset = length - zone.requestsSeries.count
            for (index, value) in zone.requestsSeries.enumerated() where offset + index < length {
                summed[offset + index] += value
            }
        }
        return summed
    }

    // MARK: - 持久化

    private func loadFromStore() {
        zones = WidgetDataStore.loadZones()
        usage = WidgetDataStore.loadUsage()
        accountAnalyticsUnavailable = !WidgetDataStore.loadAccountAnalyticsAvailable()
        let defaults = UserDefaults(suiteName: WidgetSnapshot.appGroupID)
        accountName = defaults?.string(forKey: Self.accountNameKey) ?? ""
        hasToken = SharedAuth.currentValidAccessToken() != nil
    }

    private func apply(_ payload: WatchBridgePayload) {
        if let token = payload.accessToken, let expiresAt = payload.expiresAt, let sessionId = payload.sessionId {
            WatchTokenStore.save(accessToken: token, expiresAt: expiresAt, sessionId: sessionId)
            hasToken = expiresAt.timeIntervalSinceNow > 60
        }
        WidgetDataStore.saveAllZones(payload.zones)
        if let usage = payload.usage { WidgetDataStore.saveUsage(usage, accountId: "") }
        let unavailable = payload.accountAnalyticsUnavailable ?? false
        WidgetDataStore.saveAccountAnalyticsAvailable(!unavailable)
        accountAnalyticsUnavailable = unavailable
        if let name = payload.accountName {
            UserDefaults(suiteName: WidgetSnapshot.appGroupID)?.set(name, forKey: Self.accountNameKey)
            accountName = name
        }
        zones = payload.zones
        usage = payload.usage
        lastUpdated = payload.updatedAt
        WidgetCenter.shared.reloadAllTimelines()
    }

    // MARK: - 主动刷新

    /// 进入界面/可达性变化时调用：token 临期且手机可达就索取新 token（绝不自行刷新）
    func requestFreshTokenIfNeeded() {
        guard SharedAuth.currentValidAccessToken() == nil else { return }
        let session = WCSession.default
        guard session.activationState == .activated, session.isReachable else { return }
        session.sendMessage(["request": "freshToken"], replyHandler: { reply in
            let payload = WatchBridgePayload.from(context: reply)   // 后台线程解码出 Sendable 载荷
            Task { @MainActor in
                if let payload { self.apply(payload) }
            }
        }, errorHandler: { _ in })
    }

    /// 单 Zone 实时刷新（详情页用），失败保持原样
    func refreshZone(id: String, name: String) async {
        guard let fresh = await WatchFetcher.freshZone(zoneId: id, name: name) else { return }
        if let index = zones.firstIndex(where: { $0.id == id }) {
            zones[index] = fresh
        }
        WidgetDataStore.saveAllZones(zones)
        WidgetCenter.shared.reloadAllTimelines()
    }
}

extension WatchBridge: WCSessionDelegate {

    nonisolated func session(_ session: WCSession,
                             activationDidCompleteWith activationState: WCSessionActivationState,
                             error: Error?) {
        let payload = WatchBridgePayload.from(context: session.receivedApplicationContext)
        let reachable = session.isReachable
        Task { @MainActor in
            self.isReachable = reachable
            if let payload { self.apply(payload) }
            self.requestFreshTokenIfNeeded()
        }
    }

    nonisolated func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        let payload = WatchBridgePayload.from(context: applicationContext)
        Task { @MainActor in
            if let payload { self.apply(payload) }
        }
    }

    nonisolated func sessionReachabilityDidChange(_ session: WCSession) {
        let reachable = session.isReachable
        Task { @MainActor in
            self.isReachable = reachable
            self.requestFreshTokenIfNeeded()
        }
    }
}
