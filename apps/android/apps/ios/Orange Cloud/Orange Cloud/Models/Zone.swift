//
//  Zone.swift
//  Orange Cloud
//

import Foundation

nonisolated struct Zone: Codable, Identifiable, Hashable, Sendable {
    let id:          String
    let name:        String
    let status:      String          // "active" | "pending" | "initializing" | "moved" 等
    /// 是否暂停 Cloudflare 代理（Dashboard 的「Pause Cloudflare on Site」）。
    /// 与 status 正交：暂停时 status 多半仍是 "active"，展示态要以本字段优先。
    let paused:      Bool?
    let plan:        ZonePlan?
    let nameServers: [String]?

    enum CodingKeys: String, CodingKey {
        case id, name, status, paused, plan
        case nameServers = "name_servers"
    }
}

nonisolated struct ZonePlan: Codable, Hashable, Sendable {
    let name: String
}

/// POST /zones 请求体。type 为 "full"（Cloudflare 作权威 DNS，需在注册商换 NS）
/// 或 "partial"（CNAME 接入，Business+ 才可用）；本 App 仅走 full。
nonisolated struct CreateZoneRequest: Codable, Sendable {
    let name:    String
    let type:    String
    let account: AccountRef

    nonisolated struct AccountRef: Codable, Sendable {
        let id: String
    }
}

/// PATCH /zones/{id} 请求体：暂停 / 恢复 Cloudflare 代理
nonisolated struct PauseZoneRequest: Codable, Sendable {
    let paused: Bool
}
