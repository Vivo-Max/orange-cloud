//
//  GraphQLModels.swift
//  Orange Cloud
//
//  GraphQL Analytics API 的通用请求/响应信封（与 REST 的 CFAPIResponse 不同）。
//

import Foundation

nonisolated struct GraphQLRequest<V: Codable & Sendable>: Codable, Sendable {
    let query:     String
    let variables: V
}

nonisolated struct GraphQLResponse<D: Codable & Sendable>: Codable, Sendable {
    let data:   D?
    let errors: [GraphQLError]?
}

nonisolated struct GraphQLError: Codable, Sendable {
    let message: String
    let extensions: Extensions?

    nonisolated struct Extensions: Codable, Sendable {
        let code: String?
    }

    /// 账户级数据集未授权：CF 用同一个 authz 码兼指「token 无权限」与「计划未开通该数据集」
    /// （Cloudflare 支持已确认两者无法区分），免费账号查账户级 analytics 即命中。
    var isAuthz: Bool {
        extensions?.code == "authz" || message == "not authorized for that account"
    }
}
