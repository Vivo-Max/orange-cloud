//
//  Telemetry.swift
//  Orange Cloud
//
//  体验者计划（opt-in 遥测）：Sentry 封装。**默认完全不启动**——仅当用户在
//  弹窗或「设置」里明确加入后才初始化并上报；拒绝/未选择时零网络、零采集。
//
//  隐私边界（与 AppLog 脱敏铁律同源）：只转发日志门面里的匿名诊断消息
//  （令牌只有不可逆指纹、无 Cookie、无密钥值），重点是 auth 类别（身份认证排查）；
//  sendDefaultPii 关闭、关闭 swizzling（不自动采集网络请求/UI 事件）、
//  不采集截图与视图层级、不设置任何用户身份。崩溃采集开启（Sentry 会链式保留
//  先安装的 CrashReporter handler，两者可共存；Apple 崩溃报告不受影响）。
//

import Foundation
import os
import Sentry

// MARK: - 上报入口（nonisolated，供 AppLog 等任意线程转发）

nonisolated enum TelemetryReporter {

    private static let dsn =
        "https://0731592c0467b3fc21b8ddbc245d16cf@o4511298316337152.ingest.us.sentry.io/4511680020676608"

    /// AppLog 从任意线程读，用锁保证可见性；只在用户同意后置 true
    private static let enabledFlag = OSAllocatedUnfairLock(initialState: false)

    static var isEnabled: Bool { enabledFlag.withLock { $0 } }

    static func start() {
        guard !isEnabled else { return }
        SentrySDK.start { options in
            options.dsn = dsn
            #if DEBUG
            options.environment = "debug"
            #else
            options.environment = "release"
            #endif
            options.sendDefaultPii = false        // 不带 IP / 用户身份
            options.attachScreenshot = false
            options.attachViewHierarchy = false
            options.enableSwizzling = false       // 不自动抓网络/UI（API URL 含账号、域名 ID）
            options.tracesSampleRate = 0          // 不做性能采样，只要日志 / 事件 / 崩溃
        }
        enabledFlag.withLock { $0 = true }
        addBreadcrumb(category: "telemetry", message: "telemetry started")
    }

    static func stop() {
        guard isEnabled else { return }
        enabledFlag.withLock { $0 = false }
        SentrySDK.close()
    }

    /// AppLog 转发面：auth 类别全级别进 breadcrumb（认证排查的时间线），
    /// 任何类别的 error 升级为 Sentry 事件（带前面积累的面包屑上下文一起上报）。
    static func forward(category: String, level: String, message: String) {
        guard isEnabled else { return }
        if level == "error" {
            let event = Event(level: .error)
            event.message = SentryMessage(formatted: message)
            event.logger = category
            SentrySDK.capture(event: event)
        } else if category == "auth" {
            addBreadcrumb(category: category, message: message)
        }
    }

    private static func addBreadcrumb(category: String, message: String) {
        let crumb = Breadcrumb(level: .info, category: category)
        crumb.message = message
        SentrySDK.addBreadcrumb(crumb)
    }
}

// MARK: - 同意状态（MainActor，驱动弹窗与设置开关）

@Observable
@MainActor
final class TelemetryStore {

    static let shared = TelemetryStore()

    enum Consent: String {
        case unasked, granted, declined
    }

    private static let consentKey = "telemetryConsent"

    private(set) var consent: Consent

    /// 设置页 Toggle 绑定
    var isOptedIn: Bool {
        get { consent == .granted }
        set { setConsent(newValue ? .granted : .declined) }
    }

    private init() {
        consent = Consent(rawValue: UserDefaults.standard.string(forKey: Self.consentKey) ?? "") ?? .unasked
        // 「默认不初始化」：只有历史上已同意过才在启动时拉起 SDK
        if consent == .granted {
            TelemetryReporter.start()
        }
    }

    func setConsent(_ value: Consent) {
        guard value != consent else { return }
        consent = value
        UserDefaults.standard.set(value.rawValue, forKey: Self.consentKey)
        switch value {
        case .granted:
            AppLog.app.notice("telemetry consent granted")
            TelemetryReporter.start()
        case .declined, .unasked:
            AppLog.app.notice("telemetry consent revoked/declined")
            TelemetryReporter.stop()
        }
    }
}
