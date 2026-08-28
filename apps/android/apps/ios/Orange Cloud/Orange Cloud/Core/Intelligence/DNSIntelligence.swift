//
//  DNSIntelligence.swift
//  Orange Cloud
//
//  设备端模型（Foundation Models，iOS 26+）把一句大白话变成一条 DNS 记录草稿：
//   "给 blog 加个指向 1.2.3.4 的 A 记录" → 结构化草稿（类型/名称/值/代理 为白名单/受控字段）
//   → 由 Swift 确定性渲染成 `CreateDNSRecord`，再填入表单，提交前由人核对。
//  模型只挑字段、不拼协议串，从根上杜绝非法记录类型与字段越界。
//
//  全部离线、免费、不出设备。基线 iOS 17：FoundationModels 调用走 #available(iOS 26) 守卫，
//  老设备保留手填表单。
//

import Foundation
import FoundationModels

// MARK: - 对外纯数据类型（不依赖 FoundationModels，iOS 17 也可引用）

/// 渲染完成的记录草稿：record 已可直接提交，summary 是给用户核对的自然语言回读。
nonisolated struct GeneratedDNSRecord: Sendable {
    let record:  CreateDNSRecord
    let summary: String
}

// MARK: - 门面

nonisolated enum DNSAssistant {

    /// 设备端模型此刻是否真的可用——AI 入口的唯一判据，详见 `OnDeviceAI.isReady`。
    static var isReady: Bool { OnDeviceAI.isReady }

    /// 自然语言 → 结构化草稿 → 确定性渲染成 CreateDNSRecord。永不直接吐协议字段串。
    static func generateRecord(from naturalLanguage: String, locale: Locale = .current) async throws -> GeneratedDNSRecord {
        guard #available(iOS 26.0, *) else { throw OnDeviceAIError.unsupported }
        let language = locale.localizedString(forIdentifier: locale.identifier) ?? locale.identifier
        let session = LanguageModelSession(instructions: """
            You translate a natural-language request into a single Cloudflare DNS record. Use only the \
            record types offered by the schema; never invent fields. Guidance:
            • "name" is the subdomain label only (e.g. "blog", "www", "mail") or "@" for the root domain. \
              Do not include the zone/domain itself.
            • A records take a dotted IPv4 address; AAAA an IPv6 address; CNAME, MX and NS a hostname; \
              TXT the literal text content.
            • "proxied" only matters for A/AAAA/CNAME; default it to true unless the user clearly wants \
              DNS-only (unproxied).
            • "priority" applies to MX only (lower is higher priority, e.g. 10).
            • Write "summary" as one short sentence in the user's language (\(language)).
            """)
        let draft: DNSRecordDraftAI
        do {
            draft = try await session.respond(
                to: naturalLanguage,
                generating: DNSRecordDraftAI.self,
                options: GenerationOptions(temperature: 0.1)
            ).content
        } catch let error as LanguageModelSession.GenerationError {
            throw OnDeviceAIError.generation(OnDeviceAI.friendlyMessage(for: error))
        }
        guard let result = draft.render() else { throw OnDeviceAIError.emptyResult }
        return result
    }
}

// MARK: - 结构化草稿（@Generable，iOS 26+）

/// 记录类型——白名单，与表单的 recordTypes 对齐。模型选不出不存在的类型。
@available(iOS 26.0, *)
@Generable
nonisolated enum DNSTypeDraft {
    case a
    case aaaa
    case cname
    case txt
    case mx
    case ns

    nonisolated var token: String {
        switch self {
        case .a:     "A"
        case .aaaa:  "AAAA"
        case .cname: "CNAME"
        case .txt:   "TXT"
        case .mx:    "MX"
        case .ns:    "NS"
        }
    }

    /// 仅 A/AAAA/CNAME 支持 Cloudflare 代理
    nonisolated var supportsProxy: Bool {
        switch self {
        case .a, .aaaa, .cname: true
        default:                false
        }
    }

    nonisolated var isMX: Bool { self == .mx }
}

@available(iOS 26.0, *)
@Generable
nonisolated struct DNSRecordDraftAI {
    @Guide(description: "The DNS record type.")
    var type: DNSTypeDraft

    @Guide(description: "The record name: the subdomain label only (e.g. \"blog\", \"www\"), or \"@\" for the root domain. Never include the zone/domain itself.")
    var name: String

    @Guide(description: "The record value. A: dotted IPv4. AAAA: IPv6. CNAME/MX/NS: a hostname. TXT: the literal text.")
    var content: String

    @Guide(description: "Whether to route through Cloudflare's proxy (orange cloud). Only meaningful for A/AAAA/CNAME; true unless the user asks for DNS-only.")
    var proxied: Bool

    @Guide(description: "Mail server priority for MX records (lower is higher priority, e.g. 10). Use 10 when unsure; ignored for other types.")
    var priority: Int

    @Guide(description: "One short sentence, in the user's language, describing the record being created.")
    var summary: String

    /// 确定性渲染：代理仅对支持类型生效，TTL 取自动，优先级只给 MX。
    nonisolated func render() -> GeneratedDNSRecord? {
        let trimmedContent = content.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedContent.isEmpty else { return nil }
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let record = CreateDNSRecord(
            type:     type.token,
            name:     trimmedName.isEmpty ? "@" : trimmedName,
            content:  trimmedContent,
            proxied:  type.supportsProxy && proxied,
            ttl:      1,                                    // 自动 TTL（代理开启时本就强制自动）
            priority: type.isMX ? max(0, min(priority, 65535)) : nil,
            comment:  nil
        )
        return GeneratedDNSRecord(
            record: record,
            summary: summary.trimmingCharacters(in: .whitespacesAndNewlines)
        )
    }
}
