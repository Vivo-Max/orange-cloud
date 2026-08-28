//
//  ManagedHeaderModels.swift
//  Orange Cloud
//
//  托管请求/响应头（Managed Transforms）。GET/PATCH /zones/{zone_id}/managed_headers
//
//  由 Cloudflare 维护的一组开关式头部改写（如 add_security_headers、
//  add_bot_protection_headers、add_true_client_ip_headers），无需自己写 Transform 规则。
//
//  PATCH 是**分区合并**：两个数组都可选，只更新给出的那一节；但给出的那一节内
//  会按 id 匹配更新，所以只发要改的那一条即可。
//

import Foundation

nonisolated struct ManagedTransforms: Codable, Sendable {
    let managedRequestHeaders:  [ManagedTransform]?
    let managedResponseHeaders: [ManagedTransform]?

    enum CodingKeys: String, CodingKey {
        case managedRequestHeaders  = "managed_request_headers"
        case managedResponseHeaders = "managed_response_headers"
    }
}

nonisolated struct ManagedTransform: Codable, Identifiable, Hashable, Sendable {
    let id:      String
    let enabled: Bool?
    /// 与之冲突的其它托管头（只读）。开启前应提示用户会互斥。
    let conflictsWith: [String]?

    enum CodingKeys: String, CodingKey {
        case id, enabled
        case conflictsWith = "conflicts_with"
    }

    /// 接口给的是 add_security_headers 这类蛇形 id，直接展示不友好
    var displayName: String {
        id.replacingOccurrences(of: "_", with: " ")
            .split(separator: " ")
            .map { $0.prefix(1).uppercased() + $0.dropFirst() }
            .joined(separator: " ")
    }
}

/// PATCH 体：只带要改的那一节
nonisolated struct ManagedTransformsPatch: Codable, Sendable {
    var managedRequestHeaders:  [ManagedTransformToggle]?
    var managedResponseHeaders: [ManagedTransformToggle]?

    enum CodingKeys: String, CodingKey {
        case managedRequestHeaders  = "managed_request_headers"
        case managedResponseHeaders = "managed_response_headers"
    }
}

nonisolated struct ManagedTransformToggle: Codable, Sendable {
    let id:      String
    let enabled: Bool
}
