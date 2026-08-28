//
//  WatchTokenStore.swift
//  Orange Cloud Watch Watch App
//
//  Watch 侧 token 落地：写进共享钥匙串组（与 complication 同机共享），
//  结构与 iPhone 的 TokenStore 兼容，让 Shared 里的 SharedAuth 能直接读出。
//  Watch 绝不刷新 token——只接收 iPhone 桥过来的，过期就向 iPhone 索取。
//

import Foundation
import Security

nonisolated enum WatchTokenStore {

    private static let service = "app.orangecloud.oauth"
    /// 与 iPhone TokenStore.sharedAccessGroup 一致；watch 本机的同名共享组
    private static let accessGroup = "6G78MMY657.jiamin.chen.orange-cloud.shared"

    /// SharedAuth 解码的最小结构（多余字段会被 Codable 忽略，故与 StoredToken 兼容）
    private struct SharedToken: Codable {
        let accessToken: String
        let expiresAt:   Date
    }

    /// 存当前身份 token，并把 currentSessionId 写进 App Group 供 SharedAuth 定位
    static func save(accessToken: String, expiresAt: Date, sessionId: String) {
        UserDefaults(suiteName: WidgetSnapshot.appGroupID)?
            .set(sessionId, forKey: "currentSessionId")

        guard let data = try? JSONEncoder().encode(SharedToken(accessToken: accessToken, expiresAt: expiresAt)) else { return }

        let base: [String: Any] = [
            kSecClass as String:       kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: sessionId,
        ]
        SecItemDelete(base as CFDictionary)

        var add = base
        add[kSecValueData as String]      = data
        add[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        add[kSecAttrAccessGroup as String] = accessGroup
        if SecItemAdd(add as CFDictionary, nil) != errSecSuccess {
            // entitlement 缺失时回退默认组，保证 token 不丢
            add.removeValue(forKey: kSecAttrAccessGroup as String)
            SecItemAdd(add as CFDictionary, nil)
        }
    }
}
