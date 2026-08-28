//
//  WatchBridgePayload.swift
//  Orange Cloud（主 App 与 watch target 共享）
//
//  iPhone → Watch 经 WatchConnectivity applicationContext 传输的载荷。
//  纯数据、不 import WatchConnectivity，可在任意 target 编译。
//  token 只在传输链路与各自设备的 Keychain 落地，绝不写 UserDefaults。
//

import Foundation

nonisolated struct WatchBridgePayload: Codable, Sendable {

    var accessToken: String?
    var expiresAt:   Date?
    var sessionId:   String?            // 当前身份 UUID 字符串
    var accountName: String?
    var zones:       [WidgetZoneMetrics] // 复用 Widget 快照模型
    var usage:       WidgetUsageData?
    var accountAnalyticsUnavailable: Bool? = nil   // 账户级数据无权限（免费账号）→ watch 显示提示，可选兼容旧载荷
    var updatedAt:   Date

    /// applicationContext 载荷：单个 Data blob（plist 兼容，避免逐字段类型转换）
    func asContext() -> [String: Any] {
        guard let data = try? JSONEncoder().encode(self) else { return [:] }
        return [Self.contextKey: data]
    }

    static func from(context: [String: Any]) -> WatchBridgePayload? {
        guard let data = context[contextKey] as? Data else { return nil }
        return try? JSONDecoder().decode(WatchBridgePayload.self, from: data)
    }

    private static let contextKey = "payload"
}
