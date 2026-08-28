//
//  AppLog.swift
//  Orange Cloud
//
//  客户端统一日志门面：每条日志同时进 os.Logger（Console 实时调试）与 App 内滚动日志文件
//  （随「设置 → 帮助与反馈」导出/作邮件附件）。按类别分流，方便排查。
//
//  脱敏铁律（调用方负责）：绝不把 access/refresh token、Cookie、完整 Authorization 头、
//  KV/密钥的「值」、用户隐私写进消息。需要标识某枚令牌时用 AuthDiagnostics.fingerprint
//  （不可逆指纹）。路径里的资源 ID（account/zone/记录 ID）可记，便于定位。
//
//  用法：AppLog.network.info("GET /zones -> 200 (123ms)")
//

import Foundation
import os

nonisolated struct AppLog: Sendable {

    enum Category: String, Sendable {
        case app, auth, network, websocket, purchase, background, sync
    }

    enum Level: String, Sendable {
        case debug, info, notice, error
    }

    // 各类别单例（subsystem 与 EntitlementStore / 旧 AuthDiagnostics 一致）
    static let app        = AppLog(.app)
    static let auth       = AppLog(.auth)
    static let network    = AppLog(.network)
    static let websocket  = AppLog(.websocket)
    static let purchase   = AppLog(.purchase)
    static let background = AppLog(.background)
    static let sync       = AppLog(.sync)

    private let category: Category
    private let logger: Logger

    private init(_ category: Category) {
        self.category = category
        self.logger = Logger(subsystem: "jiamin.chen.orange-cloud", category: category.rawValue)
    }

    func debug(_ message: @autoclosure () -> String)  { emit(.debug, message()) }
    func info(_ message: @autoclosure () -> String)   { emit(.info, message()) }
    func notice(_ message: @autoclosure () -> String) { emit(.notice, message()) }
    func error(_ message: @autoclosure () -> String)  { emit(.error, message()) }

    private func emit(_ level: Level, _ message: String) {
        // 消息已由调用方脱敏 → 整条 .public，确保 Console 与导出文件可读
        switch level {
        case .debug:  logger.debug("\(message, privacy: .public)")
        case .info:   logger.info("\(message, privacy: .public)")
        case .notice: logger.notice("\(message, privacy: .public)")
        case .error:  logger.error("\(message, privacy: .public)")
        }
        LogFileStore.shared.append(level: level, category: category, message: message)
        // 体验者计划（opt-in）：auth 时间线 + 全类别 error 镜像到 Sentry。
        // 消息已按上方脱敏铁律清洗，未开启时此调用是空转。
        TelemetryReporter.forward(category: category.rawValue, level: level.rawValue, message: message)
    }

    // MARK: - 启动环境头

    /// 启动时打一条环境快照，让每份导出日志自描述（版本 / 系统 / 机型 / 语言 / 登录态）。
    /// 绝不含 token 或账号标识，仅数量。
    static func logLaunch(loggedIn: Bool, sessionCount: Int) {
        let info    = Bundle.main.infoDictionary
        let version = info?["CFBundleShortVersionString"] as? String ?? "?"
        let build   = info?["CFBundleVersion"] as? String ?? "?"
        let os      = ProcessInfo.processInfo.operatingSystemVersion
        let lang    = Locale.preferredLanguages.first ?? "?"
        app.notice(
            "launch · v\(version)(\(build)) · iOS \(os.majorVersion).\(os.minorVersion).\(os.patchVersion)"
            + " · \(DiagnosticsInfo.deviceModel()) · lang=\(lang) locale=\(Locale.current.identifier)"
            + " · loggedIn=\(loggedIn) sessions=\(sessionCount)"
        )
    }
}
