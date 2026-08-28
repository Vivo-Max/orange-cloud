//
//  OnDeviceAI.swift
//  Orange Cloud
//
//  设备端模型（Foundation Models，iOS 26+）的共享门面：可用性判据 + 生成错误归一。
//  WAF / 分析摘要 / DNS 生成等各处 AI 能力共用，避免重复同一套 #available 守卫与错误映射。
//
//  全部离线、免费、不出设备，与本 App「不用贴 API Token」的隐私定位一致。
//  基线 iOS 17：所有 FoundationModels API 走 #available(iOS 26) 守卫，老设备整套 AI 入口静默隐藏。
//

import Foundation
import FoundationModels

// MARK: - 共享门面

nonisolated enum OnDeviceAI {

    /// 设备端模型此刻是否真的可用——所有 AI 入口的唯一判据。
    ///
    /// Foundation Models 用的就是 Apple 智能的端侧模型，因此它**继承 Apple 智能的地区限制**：
    /// 在中国大陆（Apple 智能暂未开放）等受限地区，即便是 iOS 26 的兼容机型，`isAvailable`
    /// 也会返回 false。所以这里不靠 `#available(iOS 26)`、更不手动判地区/语言，而是直接以框架的
    /// `SystemLanguageModel.isAvailable` 为准——它已把「系统版本 / 机型 / 地区 / 用户开关 /
    /// 模型下载状态」全部收进去。不可用时整套 AI 入口静默隐藏，手敲入口始终保留。
    static var isReady: Bool {
        if #available(iOS 26.0, *) {
            return SystemLanguageModel.default.isAvailable
        }
        return false
    }

    /// 把框架的生成错误翻成给用户看的本地化文案。
    @available(iOS 26.0, *)
    static func friendlyMessage(for error: LanguageModelSession.GenerationError) -> String {
        switch error {
        case .guardrailViolation:
            return String(localized: "这条描述被安全过滤拦下了，换个说法再试。")
        case .unsupportedLanguageOrLocale:
            return String(localized: "当前语言暂不被设备端模型支持，可改用英文描述。")
        case .exceededContextWindowSize:
            return String(localized: "描述太长了，精简后再试。")
        case .assetsUnavailable, .rateLimited:
            return String(localized: "设备端模型暂时不可用，请稍后再试。")
        default:
            return String(localized: "没能生成结果，换个说法再试试。")
        }
    }
}

// MARK: - 共享错误

nonisolated enum OnDeviceAIError: LocalizedError {
    case unsupported
    case emptyResult
    case generation(String)

    var errorDescription: String? {
        switch self {
        case .unsupported:       String(localized: "此设备不支持设备端 AI（需要 iOS 26 及支持 Apple 智能的机型）。")
        case .emptyResult:       String(localized: "没能理解这条描述，换个说法再试试。")
        case .generation(let m): m
        }
    }
}
