//
//  CFAPIResponse.swift
//  Orange Cloud
//
//  Cloudflare API 通用响应包装 { result, success, errors, messages }
//

import Foundation

nonisolated struct CFAPIResponse<T: Codable & Sendable>: Codable, Sendable {
    let result:   T?
    let success:  Bool
    let errors:   [CFAPIError]
    let messages: [CFAPIMessage]?

    enum CodingKeys: String, CodingKey { case result, success, errors, messages }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        result   = try c.decodeIfPresent(T.self, forKey: .result)
        success  = try c.decode(Bool.self, forKey: .success)
        // 部分端点（如 workers/domains）回 errors:null，宽容降级为空数组
        errors   = (try? c.decode([CFAPIError].self, forKey: .errors)) ?? []
        messages = try c.decodeIfPresent([CFAPIMessage].self, forKey: .messages)
    }
}

nonisolated struct CFAPIResponseArray<T: Codable & Sendable>: Codable, Sendable {
    let result:     [T]?
    let success:    Bool
    let errors:     [CFAPIError]
    let resultInfo: ResultInfo?

    enum CodingKeys: String, CodingKey {
        case result, success, errors
        case resultInfo = "result_info"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        result     = try c.decodeIfPresent([T].self, forKey: .result)
        success    = try c.decode(Bool.self, forKey: .success)
        // 部分端点回 errors:null，宽容降级为空数组
        errors     = (try? c.decode([CFAPIError].self, forKey: .errors)) ?? []
        resultInfo = try c.decodeIfPresent(ResultInfo.self, forKey: .resultInfo)
    }
}

nonisolated struct CFAPIError: Codable, Sendable {
    let code:    Int
    let message: String
}

nonisolated struct CFAPIMessage: Codable, Sendable {
    let code:    Int
    let message: String
}

nonisolated struct ResultInfo: Codable, Sendable {
    // 页码分页（Zone/DNS 等）
    let page:       Int?
    let perPage:    Int?
    let totalPages: Int?
    let count:      Int?
    let totalCount: Int?
    // 游标分页（R2 对象、KV keys 等）
    let cursor:      String?
    // R2 list 传 delimiter 时回的「折叠前缀」（即子文件夹），key 就叫 delimited
    let delimited:   [String]?
    let isTruncated: Bool?

    enum CodingKeys: String, CodingKey {
        case page, count, cursor, delimited
        case perPage     = "per_page"
        case totalPages  = "total_pages"
        case totalCount  = "total_count"
        case isTruncated = "is_truncated"
    }
}

// 用于 DELETE 等只关心 success 的请求
nonisolated struct EmptyResponse: Codable, Sendable {}

extension CFAPIResponse {
    func toAPIError() -> APIError {
        let err = errors.first
        return .cloudflareError(code: err?.code ?? 0, message: err?.message ?? String(localized: "未知错误"))
    }
}

extension CFAPIResponseArray {
    func toAPIError() -> APIError {
        let err = errors.first
        return .cloudflareError(code: err?.code ?? 0, message: err?.message ?? String(localized: "未知错误"))
    }
}
