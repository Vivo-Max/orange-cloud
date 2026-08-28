//
//  WorkersAIService.swift
//  Orange Cloud
//
//  Workers AI（account 级）。模型目录浏览 ai.read；试运行（文本生成 / 文生图）需 ai.read + ai.write。
//

import Foundation

struct WorkersAIService {

    private let client: CFAPIClient

    init(client: CFAPIClient) { self.client = client }

    /// 可用模型目录（GET /accounts/{id}/ai/models/search，按页拉满 per_page）
    func listModels(accountId: String) async throws -> [AIModel] {
        let response: CFAPIResponseArray<AIModel> = try await client.get(
            "accounts/\(accountId)/ai/models/search",
            queryItems: [URLQueryItem(name: "per_page", value: "100")]
        )
        guard response.success else { throw response.toAPIError() }
        return response.result ?? []
    }

    /// 文本生成试运行（POST /accounts/{id}/ai/run/{model}）。model 形如 `@cf/meta/llama-3.1-8b-instruct`，
    /// 其中的 `/` `@` `.` `-` 都是合法 path 字符，percentEncodedPath 无需额外编码。返回生成文本。
    func runTextGeneration(accountId: String, model: String, messages: [AIChatMessage]) async throws -> String {
        let response: CFAPIResponse<AITextGenResult> = try await client.post(
            "accounts/\(accountId)/ai/run/\(model)", body: AITextGenRequest(messages: messages)
        )
        guard response.success, let result = response.result else { throw response.toAPIError() }
        return result.response ?? ""
    }

    /// 文生图试运行（POST /accounts/{id}/ai/run/{model}）。model 路径同上，无需额外编码。
    /// 返回体有两种形态：多数模型直接回图片二进制（PNG/JPEG），少数回 CF 信封 + base64 `image`。
    /// 两种都不匹配时抛出明确错误，不静默返回空图。
    func runTextToImage(accountId: String, model: String, prompt: String) async throws -> Data {
        let raw = try await client.postRaw(
            "accounts/\(accountId)/ai/run/\(model)", body: AIImageGenRequest(prompt: prompt)
        )

        if Self.looksLikeImage(raw) { return raw }

        // 信封 + base64 形态
        if let envelope = try? JSONDecoder().decode(CFAPIResponse<AIImageGenResult>.self, from: raw) {
            guard envelope.success else { throw envelope.toAPIError() }
            if let base64 = envelope.result?.image,
               let decoded = Data(base64Encoded: base64, options: [.ignoreUnknownCharacters]),
               Self.looksLikeImage(decoded) {
                return decoded
            }
        }

        AppLog.network.error("ai/run text-to-image: unexpected payload (\(raw.count) bytes)")
        throw APIError.cloudflareError(
            code: 0,
            message: String(localized: "该模型返回的不是图片数据，暂不支持在 App 内试运行")
        )
    }

    /// 按魔数判断是否图片（PNG / JPEG / GIF / WEBP）
    private static func looksLikeImage(_ data: Data) -> Bool {
        guard data.count > 12 else { return false }
        let head = [UInt8](data.prefix(12))
        if head[0] == 0x89, head[1] == 0x50, head[2] == 0x4E, head[3] == 0x47 { return true }   // PNG
        if head[0] == 0xFF, head[1] == 0xD8, head[2] == 0xFF { return true }                    // JPEG
        if head[0] == 0x47, head[1] == 0x49, head[2] == 0x46 { return true }                    // GIF
        if head[0] == 0x52, head[1] == 0x49, head[2] == 0x46, head[3] == 0x46,
           head[8] == 0x57, head[9] == 0x45, head[10] == 0x42, head[11] == 0x50 { return true } // WEBP
        return false
    }
}
