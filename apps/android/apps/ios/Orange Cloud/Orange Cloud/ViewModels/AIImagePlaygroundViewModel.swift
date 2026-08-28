//
//  AIImagePlaygroundViewModel.swift
//  Orange Cloud
//
//  Workers AI 文生图试运行（POST /accounts/{id}/ai/run/{model}，需 ai.read + ai.write）。
//  返回的是图片二进制：只保留一张结果，重新运行即释放上一张，避免多张大图叠在内存里。
//

import Foundation
import Observation
#if canImport(UIKit)
import UIKit
#endif

@Observable
@MainActor
final class AIImagePlaygroundViewModel {

    var prompt = ""
    var isRunning = false
    var error: String?

    private(set) var imageData: Data?
    #if canImport(UIKit)
    /// 解码后的图片（只在拿到新数据时解一次，body 里不重复解码）
    private(set) var image: UIImage?
    #endif

    /// 结果大小上限：超过就不解码，给明确提示而不是让内存尖峰
    static let maxImageBytes = 24 * 1024 * 1024

    private let service: WorkersAIService
    let accountId: String?
    let model: AIModel

    init(service: WorkersAIService, accountId: String?, model: AIModel) {
        self.service = service
        self.accountId = accountId
        self.model = model
    }

    var trimmedPrompt: String { prompt.trimmingCharacters(in: .whitespacesAndNewlines) }
    var canRun: Bool { !trimmedPrompt.isEmpty && !isRunning && accountId != nil }

    func run() async {
        guard let accountId, canRun else { return }
        isRunning = true
        error = nil
        clearImage()
        defer { isRunning = false }
        do {
            let data = try await service.runTextToImage(
                accountId: accountId,
                model: model.name ?? model.id,
                prompt: trimmedPrompt
            )
            guard data.count <= Self.maxImageBytes else {
                error = String(localized: "生成的图片过大，无法在 App 内预览")
                return
            }
            #if canImport(UIKit)
            guard let decoded = UIImage(data: data) else {
                error = String(localized: "图片数据无法解码")
                return
            }
            image = decoded
            #endif
            imageData = data
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func clearImage() {
        imageData = nil
        #if canImport(UIKit)
        image = nil
        #endif
    }
}
