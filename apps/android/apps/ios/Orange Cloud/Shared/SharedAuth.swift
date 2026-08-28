//
//  SharedAuth.swift
//  Orange Cloud（主 App 与 Widget Extension 共享）
//
//  Widget 侧的只读 token 访问：经共享钥匙串组读当前身份的 access_token。
//  重要：Widget 绝不刷新 token——Cloudflare 的 refresh_token 是轮转式的，
//  并发刷新会作废主 App 手里的凭据。token 过期时返回 nil，由调用方回退快照。
//

import Foundation
import Security

nonisolated enum SharedAuth {

    private static let service = "app.orangecloud.oauth"

    private struct SharedToken: Codable {
        let accessToken: String
        let expiresAt:   Date
    }

    /// 当前身份的有效 access_token；不存在或已过期（含 60s 余量）返回 nil
    static func currentValidAccessToken() -> String? {
        guard let defaults = UserDefaults(suiteName: WidgetSnapshot.appGroupID),
              let sessionId = defaults.string(forKey: "currentSessionId") else { return nil }
        return validAccessToken(sessionId: sessionId)
    }

    /// 指定身份的有效 access_token（Widget 固定某账号时取其所属身份的 token）；
    /// 不存在或已过期（含 60s 余量）返回 nil。只读，绝不刷新。
    static func validAccessToken(sessionId: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String:              kSecClassGenericPassword,
            kSecAttrService as String:        service,
            kSecAttrAccount as String:        sessionId,
            kSecAttrSynchronizable as String: kSecAttrSynchronizableAny,
            kSecReturnData as String:         true,
            kSecMatchLimit as String:         kSecMatchLimitOne,
        ]
        // 不指定 access group：自动匹配本目标可访问的组（含共享组）
        var item: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
              let data = item as? Data,
              let token = try? JSONDecoder().decode(SharedToken.self, from: data) else {
            return nil
        }
        guard token.expiresAt.timeIntervalSinceNow > 60 else { return nil }
        return token.accessToken
    }
}
