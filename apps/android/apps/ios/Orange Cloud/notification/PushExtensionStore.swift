//
//  PushExtensionStore.swift
//  notification
//
//  NSE 自包含的收件箱写入 + AES-CBC 解密。
//  ⚠️ JSON 形状必须与主 App 的 `PushStore.swift`（PushMessage / 同 App Group key）保持一致。
//

import Foundation
import CommonCrypto

nonisolated struct ExtPushMessage: Codable {
    var id = UUID()
    var date = Date()
    var sourceID: String?
    var title: String?
    var subtitle: String?
    var body: String
    var group: String?
    var url: String?
}

nonisolated enum ExtPushInbox {
    static let appGroupID = "group.jiamin.chen.Orange-Cloud"
    static let storeKey = "push.inbox.messages"
    private static let cap = 200

    private static func defaults() -> UserDefaults? { UserDefaults(suiteName: appGroupID) }

    static func message(
        from userInfo: [AnyHashable: Any],
        fallbackTitle: String?,
        fallbackBody: String?,
        sourceID: String?
    ) -> ExtPushMessage {
        let aps = userInfo["aps"] as? [AnyHashable: Any]
        let alert = aps?["alert"] as? [AnyHashable: Any]
        let title = alert?["title"] as? String ?? (fallbackTitle?.isEmpty == false ? fallbackTitle : nil)
        let body = alert?["body"] as? String ?? fallbackBody ?? ""
        return ExtPushMessage(
            sourceID: sourceID,
            title: title,
            subtitle: alert?["subtitle"] as? String,
            body: body,
            group: userInfo["group"] as? String,
            url: userInfo["url"] as? String
        )
    }

    static func append(_ message: ExtPushMessage) {
        guard let defaults = defaults() else { return }
        var msgs: [ExtPushMessage] = defaults.data(forKey: storeKey)
            .flatMap { try? JSONDecoder().decode([ExtPushMessage].self, from: $0) } ?? []
        if let sid = message.sourceID, msgs.contains(where: { $0.sourceID == sid }) { return }   // 去重（与前台 delegate 共用 request.identifier）
        msgs.insert(message, at: 0)
        if msgs.count > cap { msgs = Array(msgs.prefix(cap)) }
        if let data = try? JSONEncoder().encode(msgs) { defaults.set(data, forKey: storeKey) }
    }
}

nonisolated enum ExtE2E {
    /// E2E 密钥（与主 App 共用 App Group；密钥字符串的 UTF8 字节即 AES key）
    static func key() -> Data? {
        guard let k = UserDefaults(suiteName: ExtPushInbox.appGroupID)?.string(forKey: "push.e2eKey"),
              !k.isEmpty else { return nil }
        return k.data(using: .utf8)
    }

    /// AES-CBC/PKCS7 解密（key/iv 取字符串 UTF8 字节、ciphertext 为 base64），返回 JSON 字典
    static func decrypt(ciphertextBase64: String, ivString: String, key: Data) -> [String: Any]? {
        guard [16, 24, 32].contains(key.count) else { return nil }
        let iv = ivString.data(using: .utf8) ?? Data()
        guard iv.count == 16, let cipher = Data(base64Encoded: ciphertextBase64) else { return nil }
        guard let plain = aesCBCDecrypt(cipher, key: key, iv: iv) else { return nil }
        return (try? JSONSerialization.jsonObject(with: plain)) as? [String: Any]
    }

    private static func aesCBCDecrypt(_ data: Data, key: Data, iv: Data) -> Data? {
        let bufferSize = data.count + kCCBlockSizeAES128
        var buffer = Data(count: bufferSize)
        var moved = 0
        let status = buffer.withUnsafeMutableBytes { bufPtr in
            data.withUnsafeBytes { dataPtr in
                key.withUnsafeBytes { keyPtr in
                    iv.withUnsafeBytes { ivPtr in
                        CCCrypt(
                            CCOperation(kCCDecrypt), CCAlgorithm(kCCAlgorithmAES), CCOptions(kCCOptionPKCS7Padding),
                            keyPtr.baseAddress, key.count, ivPtr.baseAddress,
                            dataPtr.baseAddress, data.count,
                            bufPtr.baseAddress, bufferSize, &moved
                        )
                    }
                }
            }
        }
        guard status == Int32(kCCSuccess) else { return nil }
        return buffer.prefix(moved)
    }
}
