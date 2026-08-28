//
//  AnalyticsIntelligence.swift
//  Orange Cloud
//
//  设备端模型（Foundation Models，iOS 26+）把一段流量分析数据读成大白话：
//   一句话要点（"本周请求 +32%，主要来自日本"）+ 2–4 条亮点/异常提示。
//  只读、零风险：把确定性聚合好的数字喂给模型，明确要求「只用给定数字、不得编造」，
//  模型只负责措辞与挑重点（大幅涨跌、威胁集中、命中率偏低、来源集中等）。
//
//  全部离线、免费、不出设备。基线 iOS 17：FoundationModels 调用走 #available(iOS 26) 守卫。
//

import Foundation
import FoundationModels

// MARK: - 对外纯数据类型（不依赖 FoundationModels，iOS 17 也可引用）

/// 喂给模型的确定性事实集——全部由 ViewModel 的聚合值拼成，模型不接触原始数据。
nonisolated struct TrafficSummaryInput: Sendable {
    let periodLabel:     String        // 本地化时间范围（"过去 24 小时"），供模型回读
    let totalRequests:   Int
    let requestsTrend:   Double?        // 环比百分比；nil 表示无前一周期可比
    let totalBytes:      Int
    let bytesTrend:      Double?
    let totalThreats:    Int
    let threatsTrend:    Double?
    let totalUniques:    Int
    let uniquesTrend:    Double?
    let cacheHitRate:    Double?        // 0–100
    let cacheHitTrendPt: Double?        // 百分点差
    let topCountries:    [TopCountry]   // 请求量降序，已截断

    nonisolated struct TopCountry: Sendable {
        let name:     String
        let requests: Int
        let threats:  Int
    }

    /// 拼成一段结构化、稳定的事实清单（英文标签 + 具体数字），供模型据此措辞。
    var factSheet: String {
        var lines: [String] = []
        lines.append("Time window: \(periodLabel)")
        lines.append("Total requests: \(totalRequests.formatted(.number.grouping(.automatic)))\(Self.trend(requestsTrend))")
        let bytes = Int64(totalBytes).ocBytes
        lines.append("Data served: \(bytes)\(Self.trend(bytesTrend))")
        lines.append("Unique visitors: \(totalUniques.formatted(.number.grouping(.automatic)))\(Self.trend(uniquesTrend))")
        lines.append("Threats blocked: \(totalThreats.formatted(.number.grouping(.automatic)))\(Self.trend(threatsTrend))")
        if let rate = cacheHitRate {
            lines.append("Cache hit rate: \(String(format: "%.1f%%", rate))\(Self.trendPoints(cacheHitTrendPt))")
        }
        if !topCountries.isEmpty {
            lines.append("Top sources by requests:")
            for country in topCountries {
                let threats = country.threats > 0 ? ", \(country.threats) threats" : ""
                lines.append("- \(country.name): \(country.requests.formatted(.number.grouping(.automatic))) requests\(threats)")
            }
        }
        return lines.joined(separator: "\n")
    }

    private static func trend(_ value: Double?) -> String {
        guard let value, value.isFinite, abs(value) >= 0.05 else { return "" }
        let sign = value >= 0 ? "+" : ""
        return " (\(sign)\(String(format: "%.1f", value))% vs the previous equal period)"
    }

    private static func trendPoints(_ value: Double?) -> String {
        guard let value, value.isFinite, abs(value) >= 0.05 else { return "" }
        let sign = value >= 0 ? "+" : ""
        return " (\(sign)\(String(format: "%.1f", value)) percentage points vs the previous period)"
    }
}

/// 模型产出的要点摘要（已剥离 FoundationModels 类型，iOS 17 也可持有）。
nonisolated struct TrafficInsight: Sendable {
    let summary:    String     // 一句话要点
    let highlights: [String]   // 2–4 条亮点/异常
}

// MARK: - 门面

nonisolated enum AnalyticsAssistant {

    /// 设备端模型此刻是否真的可用——AI 入口的唯一判据，详见 `OnDeviceAI.isReady`。
    static var isReady: Bool { OnDeviceAI.isReady }

    /// 把一段流量数据读成一句话要点 + 几条亮点。只读、零风险。
    static func summarize(_ input: TrafficSummaryInput, locale: Locale = .current) async throws -> TrafficInsight {
        guard #available(iOS 26.0, *) else { throw OnDeviceAIError.unsupported }
        let language = locale.localizedString(forIdentifier: locale.identifier) ?? locale.identifier
        let session = LanguageModelSession(instructions: """
            You are a concise web-traffic analyst summarizing Cloudflare analytics for a website owner. \
            You are given a fixed set of figures. Rules:
            • Use ONLY the numbers provided. Never invent or estimate any figure.
            • "summary": one short sentence with the single most important takeaway (e.g. a big change \
              or the dominant traffic source).
            • "highlights": 2 to 4 very short bullet points calling out what's notable — large increases \
              or drops, concentration of threats, a low cache hit rate, or a dominant source country. \
              Skip anything unremarkable; do not pad to four.
            • Be specific and plain; no code, no raw metric names, no markdown.
            • Write everything in the user's language (\(language)).
            """)
        let draft: TrafficInsightAI
        do {
            draft = try await session.respond(
                to: input.factSheet,
                generating: TrafficInsightAI.self,
                options: GenerationOptions(temperature: 0.3)
            ).content
        } catch let error as LanguageModelSession.GenerationError {
            throw OnDeviceAIError.generation(OnDeviceAI.friendlyMessage(for: error))
        }
        let summary = draft.summary.trimmingCharacters(in: .whitespacesAndNewlines)
        let highlights = draft.highlights
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        guard !summary.isEmpty || !highlights.isEmpty else { throw OnDeviceAIError.emptyResult }
        return TrafficInsight(summary: summary, highlights: highlights)
    }
}

// MARK: - 结构化产出（@Generable，iOS 26+）

@available(iOS 26.0, *)
@Generable
nonisolated struct TrafficInsightAI {
    @Guide(description: "One short sentence with the single most important takeaway, in the user's language.")
    var summary: String

    @Guide(description: "2 to 4 very short highlight bullets, each a brief phrase in the user's language. Omit any that are not notable.")
    var highlights: [String]
}
