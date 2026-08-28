//
//  APIError.swift
//  Orange Cloud
//

import Foundation

nonisolated enum APIError: LocalizedError {
    case unauthorized                    // 401，需要重新登录
    case forbidden                       // 403，权限不足
    case notFound                        // 404
    case rateLimited                     // 429
    case serverError(statusCode: Int)    // 5xx
    case cloudflareError(code: Int, message: String)  // CF 业务错误
    case decodingError(Error)            // 解析失败
    case networkError(Error)             // 网络错误
    case accountNotAuthorized            // 账户级数据集未授权（GraphQL authz；免费账号常态，无法区分无权限/计划未开通）

    var errorDescription: String? {
        switch self {
        case .unauthorized:                return String(localized: "登录已过期，请重新登录")
        case .forbidden:                   return String(localized: "权限不足，请检查 OAuth Scope")
        case .notFound:                    return String(localized: "资源不存在")
        case .rateLimited:                 return String(localized: "请求太频繁，请稍后再试")
        case .serverError(let code):       return String(localized: "服务器错误（\(code)）")
        case .cloudflareError(_, let msg): return msg
        case .decodingError:               return String(localized: "数据解析失败")
        case .networkError(let e):         return String(localized: "网络错误：\(e.localizedDescription)")
        case .accountNotAuthorized:        return String(localized: "此账号暂无账户级数据查询权限")
        }
    }

    /// 是否为账户级数据集未授权（用于 UI 降级到「免费账号无账户级数据」态）
    var isAccountNotAuthorized: Bool {
        if case .accountNotAuthorized = self { return true }
        return false
    }

    /// 是否为「token 权限不足」的确定性拒绝（403，或 CF 业务错误 10000 Authentication error）。
    /// 与网络/5xx 等瞬时失败区分：命中它说明同一 token 重试注定同样失败，可按账号记住不再探测。
    var isPermissionDenied: Bool {
        switch self {
        case .forbidden:                    return true
        case .cloudflareError(let code, _): return code == 10000
        default:                            return false
        }
    }
}
