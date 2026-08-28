//
//  PushStore.swift
//  Orange Cloud
//
//  推送中心的共享存储（App Group UserDefaults）：收件箱 + 端点配置 + E2E 密钥。
//  与 NSE（notification/PushExtensionStore.swift）共用同一 App Group、storeKey 与默认 JSON 编码。
//

import Foundation

// MARK: - 收到的推送

nonisolated struct PushMessage: Codable, Identifiable, Sendable {
    var id = UUID()
    var date = Date()
    var sourceID: String?      // = APNs request.identifier，用于 NSE 与前台去重
    var title: String?
    var subtitle: String?
    var body: String
    var group: String?
    var url: String?
}

nonisolated enum PushInbox {
    static let storeKey = "push.inbox.messages"
    private static let cap = 200

    private static func defaults() -> UserDefaults? {
        UserDefaults(suiteName: WidgetSnapshot.appGroupID)
    }

    static func all() -> [PushMessage] {
        guard let data = defaults()?.data(forKey: storeKey),
              let msgs = try? JSONDecoder().decode([PushMessage].self, from: data) else { return [] }
        return msgs
    }

    static func append(_ message: PushMessage) {
        guard let defaults = defaults() else { return }
        var msgs = all()
        if let sid = message.sourceID, msgs.contains(where: { $0.sourceID == sid }) { return }   // 去重
        msgs.insert(message, at: 0)
        if msgs.count > cap { msgs = Array(msgs.prefix(cap)) }
        if let data = try? JSONEncoder().encode(msgs) { defaults.set(data, forKey: storeKey) }
    }

    static func clear() {
        defaults()?.removeObject(forKey: storeKey)
    }

    /// 从 APNs payload 提取一条消息（前台 delegate / NSE 共用）
    static func message(
        from userInfo: [AnyHashable: Any],
        fallbackTitle: String?,
        fallbackBody: String?,
        sourceID: String? = nil
    ) -> PushMessage {
        let aps = userInfo["aps"] as? [AnyHashable: Any]
        let alert = aps?["alert"] as? [AnyHashable: Any]
        let title = alert?["title"] as? String ?? (fallbackTitle?.isEmpty == false ? fallbackTitle : nil)
        let body = alert?["body"] as? String ?? fallbackBody ?? ""
        return PushMessage(
            sourceID: sourceID,
            title: title,
            subtitle: alert?["subtitle"] as? String,
            body: body,
            group: userInfo["group"] as? String,
            url: userInfo["url"] as? String
        )
    }
}

// MARK: - 端点配置 + E2E 密钥

nonisolated enum PushConfig {
    static let defaultServer = "https://push.90dd.adsl8.workers.dev"
    private static let serverKey = "push.serverURL"
    private static let keyKey = "push.deviceKey"
    private static let e2eKeyKey = "push.e2eKey"

    private static func defaults() -> UserDefaults? {
        UserDefaults(suiteName: WidgetSnapshot.appGroupID)
    }

    static var serverURL: String {
        get { defaults()?.string(forKey: serverKey) ?? defaultServer }
        set { defaults()?.set(newValue, forKey: serverKey) }
    }

    static var deviceKey: String? {
        get { defaults()?.string(forKey: keyKey) }
        set {
            let defaults = defaults()
            if let newValue { defaults?.set(newValue, forKey: keyKey) } else { defaults?.removeObject(forKey: keyKey) }
        }
    }

    /// E2E 密钥（NSE 用它解密 ciphertext；与 NSE 同 App Group key "push.e2eKey"）
    static var e2eKey: String? {
        get { defaults()?.string(forKey: e2eKeyKey) }
        set {
            let defaults = defaults()
            if let newValue, !newValue.isEmpty { defaults?.set(newValue, forKey: e2eKeyKey) } else { defaults?.removeObject(forKey: e2eKeyKey) }
        }
    }

    static var endpointURL: String? {
        guard let key = deviceKey else { return nil }
        return "\(serverURL)/\(key)"
    }
}
