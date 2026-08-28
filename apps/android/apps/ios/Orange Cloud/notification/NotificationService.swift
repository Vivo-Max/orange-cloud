//
//  NotificationService.swift
//  notification
//
//  通知服务扩展：每条推送（服务端恒设 mutable-content）先过这里。
//  把消息写入 App Group 收件箱（后台到达也能收）；有 ciphertext + 本机密钥则解密覆盖。
//

import UserNotifications

class NotificationService: UNNotificationServiceExtension {

    var contentHandler: ((UNNotificationContent) -> Void)?
    var bestAttemptContent: UNMutableNotificationContent?

    override func didReceive(
        _ request: UNNotificationRequest,
        withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void
    ) {
        self.contentHandler = contentHandler
        let best = request.content.mutableCopy() as? UNMutableNotificationContent
        bestAttemptContent = best

        let userInfo = request.content.userInfo
        let sourceID = request.identifier

        var message = ExtPushInbox.message(
            from: userInfo,
            fallbackTitle: best?.title,
            fallbackBody: best?.body,
            sourceID: sourceID
        )

        // 端到端加密：有密文且本机存了密钥 → 解密并覆盖展示内容
        if let ciphertext = userInfo["ciphertext"] as? String, let key = ExtE2E.key() {
            let iv = userInfo["iv"] as? String ?? ""
            if let decoded = ExtE2E.decrypt(ciphertextBase64: ciphertext, ivString: iv, key: key) {
                let title = decoded["title"] as? String
                let subtitle = decoded["subtitle"] as? String
                let body = decoded["body"] as? String ?? ""
                if let title { best?.title = title }
                if let subtitle { best?.subtitle = subtitle }
                best?.body = body
                message = ExtPushMessage(
                    sourceID: sourceID,
                    title: title,
                    subtitle: subtitle,
                    body: body,
                    group: decoded["group"] as? String,
                    url: decoded["url"] as? String
                )
            } else {
                best?.body = "🔒 " + (best?.body ?? "")   // 解密失败（密钥不匹配）
            }
        }

        ExtPushInbox.append(message)
        contentHandler(best ?? request.content)
    }

    override func serviceExtensionTimeWillExpire() {
        if let contentHandler, let bestAttemptContent {
            contentHandler(bestAttemptContent)
        }
    }
}
