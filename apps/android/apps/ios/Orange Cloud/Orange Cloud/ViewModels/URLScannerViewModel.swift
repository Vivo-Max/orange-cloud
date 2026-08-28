//
//  URLScannerViewModel.swift
//  Orange Cloud
//
//  扫描是异步的：提交 → 轮询结果。轮询有上限，避免无限转圈。
//

import Foundation
import Observation

@Observable
@MainActor
final class URLScannerViewModel {

    var urlText = ""
    private(set) var result: URLScanResult?
    private(set) var scanId: String?
    var isScanning = false
    var error: String?
    var didFinish = false

    private let service: URLScannerService
    private let accountId: String

    /// 轮询上限：2 秒一次、最多 30 次（约 1 分钟）。超时按「还没出结果」处理而非报错。
    private let maxPolls = 30
    private let pollInterval: UInt64 = 2_000_000_000

    init(service: URLScannerService, accountId: String) {
        self.service = service
        self.accountId = accountId
    }

    var canScan: Bool {
        let trimmed = urlText.trimmingCharacters(in: .whitespaces)
        return !trimmed.isEmpty && trimmed.contains(".") && !isScanning
    }

    func screenshotPath() -> String? {
        scanId.map { service.screenshotPath(accountId: accountId, scanId: $0) }
    }

    func scan() async {
        guard canScan else { return }
        isScanning = true
        error = nil
        result = nil
        scanId = nil
        do {
            var target = urlText.trimmingCharacters(in: .whitespaces)
            // 用户多半只输域名，补上协议再提交
            if !target.contains("://") { target = "https://\(target)" }
            guard let id = try await service.submit(accountId: accountId, url: target) else {
                error = String(localized: "提交成功但没拿到扫描 ID。")
                isScanning = false
                return
            }
            scanId = id
            for _ in 0..<maxPolls {
                try? await Task.sleep(nanoseconds: pollInterval)
                if Task.isCancelled { break }
                if let report = try await service.result(accountId: accountId, scanId: id) {
                    result = report
                    didFinish.toggle()
                    break
                }
            }
            if result == nil {
                error = String(localized: "扫描仍在进行，请稍后用同一个链接再查一次。")
            }
        } catch {
            self.error = error.localizedDescription
        }
        isScanning = false
    }
}
