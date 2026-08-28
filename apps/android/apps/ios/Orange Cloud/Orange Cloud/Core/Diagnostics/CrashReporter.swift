//
//  CrashReporter.swift
//  Orange Cloud
//
//  尽力而为的崩溃捕获，用于排查只在真机复现的故障（尤其是启动期崩溃）。
//  iOS 在致命信号后会终止进程，所以崩溃报告写盘后留到下次启动再呈现：
//  下次冷启动时 LogFileStore 导出（反馈邮件 / 设置页导出日志）会自动把上次
//  崩溃附在最前面，配合 recordBreadcrumb 的启动面包屑可定位崩在哪一步。
//
//  注意：信号处理器里做的是 Foundation 文件 IO，并非严格 async-signal-safe，
//  仅作调试取证用途（best-effort）；致命信号记录后会以默认处置重新抛出，
//  让系统照常生成它自己的 crash log。
//

import Darwin
import Foundation

nonisolated struct CrashReport: Identifiable, Sendable {
    let id = UUID()
    let text: String
}

nonisolated enum CrashReporter {

    private static let directoryName = "Logs"
    private static let reportFileName = "last-crash.txt"
    private static let breadcrumbFileName = "crash-breadcrumbs.txt"
    private static let maxReportCharacters = 24_000
    private static let maxLogCharacters = 12_000
    private static let maxBreadcrumbCharacters = 8_000

    // 仅捕获“真致命”的信号。SIGPIPE 不在内：默认处置是终止进程，但断开的
    // socket 本应是无害的——这里显式忽略（见 install()），避免把它升级成崩溃。
    private static let handledSignals: [Int32] = [
        SIGABRT, SIGILL, SIGSEGV, SIGFPE, SIGBUS, SIGTRAP,
    ]

    static func install() {
        signal(SIGPIPE, SIG_IGN)   // 断管不致命，交给框架层（SO_NOSIGPIPE）兜底
        NSSetUncaughtExceptionHandler(crashExceptionHandler)
        for signalCode in handledSignals {
            signal(signalCode, crashSignalHandler)
        }
    }

    static func pendingReport() -> CrashReport? {
        guard let text = currentReportText(), !text.isEmpty else { return nil }
        return CrashReport(text: text)
    }

    static func currentReportText() -> String? {
        currentReportText(includeRecentLog: true)
    }

    static func currentReportTextForExport() -> String? {
        currentReportText(includeRecentLog: false)
    }

    private static func currentReportText(includeRecentLog: Bool) -> String? {
        guard let crash = crashReportText() else { return nil }
        let log = includeRecentLog ? logSection() : nil
        return [crash, breadcrumbSection(), log]
            .compactMap { $0 }
            .joined(separator: "\n\n")
    }

    static func clearPendingReport() {
        try? FileManager.default.removeItem(at: reportURL)
        try? FileManager.default.removeItem(at: breadcrumbURL)
    }

    /// 启动 / 关键路径埋点：崩溃报告会带上最后几条面包屑，定位崩在哪一步。
    static func recordBreadcrumb(_ message: String) {
        let timestamp = ISO8601DateFormatter().string(from: Date())
        appendBreadcrumb("\(timestamp) \(message)\n")
    }

    fileprivate static func record(exception: NSException) {
        writeReport(
            title: "Uncaught NSException",
            details: [
                "name: \(exception.name.rawValue)",
                "reason: \(exception.reason ?? "nil")",
            ],
            stack: exception.callStackSymbols
        )
    }

    fileprivate static func record(signal signalCode: Int32) {
        writeReport(
            title: "Fatal Signal",
            details: [
                "signal: \(signalCode)",
                "name: \(signalName(signalCode))",
            ],
            stack: Thread.callStackSymbols
        )
    }

    private static var reportURL: URL {
        logDirectory.appendingPathComponent(reportFileName)
    }

    private static var breadcrumbURL: URL {
        logDirectory.appendingPathComponent(breadcrumbFileName)
    }

    private static var logDirectory: URL {
        FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent(directoryName, isDirectory: true)
    }

    private static func writeReport(title: String, details: [String], stack: [String]) {
        let report = formatReport(title: title, details: details, stack: stack)
        do {
            try createLogDirectory()
            try Data(report.utf8).write(to: reportURL, options: .atomic)
        } catch {
            NSLog("Orange Cloud crash report write failed: %@", error.localizedDescription)
        }
    }

    private static func appendBreadcrumb(_ line: String) {
        do {
            try createLogDirectory()
            let previous = (try? String(contentsOf: breadcrumbURL, encoding: .utf8)) ?? ""
            let text = String((previous + line).suffix(maxBreadcrumbCharacters))
            try Data(text.utf8).write(to: breadcrumbURL, options: .atomic)
        } catch {
            NSLog("Orange Cloud breadcrumb write failed: %@", error.localizedDescription)
        }
    }

    private static func createLogDirectory() throws {
        try FileManager.default.createDirectory(
            at: logDirectory,
            withIntermediateDirectories: true
        )
    }

    private static func crashReportText() -> String? {
        let text = try? String(contentsOf: reportURL, encoding: .utf8)
        guard let text, !text.isEmpty else { return nil }
        return String(text.prefix(maxReportCharacters))
    }

    private static func breadcrumbSection() -> String? {
        let text = try? String(contentsOf: breadcrumbURL, encoding: .utf8)
        guard let text, !text.isEmpty else { return nil }
        return "Breadcrumbs:\n\(text)"
    }

    private static func logSection() -> String? {
        let text = LogFileStore.shared.recentText(maxCharacters: maxLogCharacters)
        guard !text.isEmpty else { return nil }
        return "Recent AppLog:\n\(text)"
    }

    private static func formatReport(title: String, details: [String], stack: [String]) -> String {
        let header = [
            "Orange Cloud Crash Report",
            "capturedAt: \(ISO8601DateFormatter().string(from: Date()))",
            "type: \(title)",
        ]
        return (header + details + ["", "Call Stack:"] + stack).joined(separator: "\n")
    }

    private static func signalName(_ signalCode: Int32) -> String {
        switch signalCode {
        case SIGABRT: return "SIGABRT"
        case SIGILL:  return "SIGILL"
        case SIGSEGV: return "SIGSEGV"
        case SIGFPE:  return "SIGFPE"
        case SIGBUS:  return "SIGBUS"
        case SIGTRAP: return "SIGTRAP"
        default:      return "UNKNOWN"
        }
    }
}

// 工程开启 SWIFT_DEFAULT_ACTOR_ISOLATION = MainActor，全局函数默认会被隔离到
// MainActor，而 MainActor 隔离的函数无法转成 @convention(c) 函数指针（C 指针带不了
// actor 隔离）。这两个要喂给 NSSetUncaughtExceptionHandler / signal，必须显式 nonisolated。
private nonisolated func crashExceptionHandler(_ exception: NSException) {
    CrashReporter.record(exception: exception)
}

private nonisolated func crashSignalHandler(_ signalCode: Int32) {
    Darwin.signal(signalCode, SIG_DFL)
    CrashReporter.record(signal: signalCode)
    raise(signalCode)
}
