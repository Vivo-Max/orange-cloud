//
//  LogFileStore.swift
//  Orange Cloud
//
//  AppLog 的文件落地：所有日志写进 Caches/Logs/app.log，超上限滚动一代（app.1.log）。
//  导出时合并上一代 + 当前（旧在前），约束总体积便于作邮件附件。
//  全部读写都在串行队列上，对外线程安全（@unchecked Sendable）。
//

import Foundation

nonisolated final class LogFileStore: @unchecked Sendable {

    static let shared = LogFileStore()

    private let queue = DispatchQueue(label: "jiamin.chen.orange-cloud.logfile")
    private let fileManager = FileManager.default
    private let maxBytes = 256 * 1024          // 单文件上限，两代约 512KB
    private let isoFormatter: ISO8601DateFormatter

    private var handle: FileHandle?
    private var currentBytes = 0
    private var isSetup = false

    private init() {
        isoFormatter = ISO8601DateFormatter()
        isoFormatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
    }

    private var directory: URL {
        fileManager.urls(for: .cachesDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("Logs", isDirectory: true)
    }
    private var currentURL: URL { directory.appendingPathComponent("app.log") }
    private var previousURL: URL { directory.appendingPathComponent("app.1.log") }

    // MARK: - 写入

    func append(level: AppLog.Level, category: AppLog.Category, message: String) {
        let timestamp = Date()
        queue.async { [weak self] in
            guard let self else { return }
            // 时间格式化放队列内：ISO8601DateFormatter 非线程安全
            let line = "\(self.isoFormatter.string(from: timestamp)) [\(level.rawValue)] [\(category.rawValue)] \(message)\n"
            self.write(line)
        }
    }

    private func write(_ line: String) {
        setupIfNeeded()
        guard let data = line.data(using: .utf8) else { return }
        if currentBytes + data.count > maxBytes {
            rotate()
        }
        _ = try? handle?.write(contentsOf: data)
        currentBytes += data.count
    }

    private func setupIfNeeded() {
        guard !isSetup else { return }
        isSetup = true
        try? fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
        if !fileManager.fileExists(atPath: currentURL.path) {
            fileManager.createFile(atPath: currentURL.path, contents: nil)
        }
        let attrs = try? fileManager.attributesOfItem(atPath: currentURL.path)
        currentBytes = (attrs?[.size] as? Int) ?? 0
        openHandle()
    }

    private func openHandle() {
        handle = try? FileHandle(forWritingTo: currentURL)
        _ = try? handle?.seekToEnd()
    }

    private func rotate() {
        _ = try? handle?.close()
        handle = nil
        try? fileManager.removeItem(at: previousURL)
        try? fileManager.moveItem(at: currentURL, to: previousURL)
        fileManager.createFile(atPath: currentURL.path, contents: nil)
        currentBytes = 0
        openHandle()
    }

    // MARK: - 导出 / 清空

    /// 上一代 + 当前合并文本（旧在前），若有未清理的上次崩溃则置于最前。
    /// 供反馈附件与导出用。
    func exportedText() -> String {
        queue.sync {
            _ = try? handle?.synchronize()
            return formattedCrashReport() + combinedLogText()
        }
    }

    /// 最近 maxCharacters 字的日志尾巴（崩溃报告内嵌用，不含崩溃段避免自引用）。
    func recentText(maxCharacters: Int) -> String {
        queue.sync {
            _ = try? handle?.synchronize()
            return String(combinedLogText().suffix(maxCharacters))
        }
    }

    private func formattedCrashReport() -> String {
        guard let report = CrashReporter.currentReportTextForExport() else { return "" }
        return "===== Last Crash =====\n\(report)\n\n===== App Log =====\n"
    }

    private func combinedLogText() -> String {
        let prev = (try? String(contentsOf: previousURL, encoding: .utf8)) ?? ""
        let curr = (try? String(contentsOf: currentURL, encoding: .utf8)) ?? ""
        return prev + curr
    }

    /// 写到临时文件返回 URL（邮件附件 / 系统分享用）。无内容时返回 nil。
    func exportedFileURL() -> URL? {
        let text = exportedText()
        guard !text.isEmpty else { return nil }
        let url = fileManager.temporaryDirectory.appendingPathComponent("OrangeCloud-logs.txt")
        do {
            try Data(text.utf8).write(to: url)
            return url
        } catch {
            return nil
        }
    }

    func clear() {
        queue.sync {
            _ = try? handle?.close()
            handle = nil
            try? fileManager.removeItem(at: currentURL)
            try? fileManager.removeItem(at: previousURL)
            currentBytes = 0
            isSetup = false
        }
    }
}
