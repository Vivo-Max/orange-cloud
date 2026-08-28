//
//  DNSRecord.swift
//  Orange Cloud
//

import Foundation

nonisolated struct DNSRecord: Codable, Identifiable, Hashable, Sendable {
    let id:        String
    let type:      String          // A, AAAA, CNAME, TXT, MX 等
    let name:      String
    let content:   String
    let proxied:   Bool?
    let ttl:       Int
    let priority:  Int?            // MX / SRV 记录需要
    let comment:   String?
    let createdOn: String?

    enum CodingKeys: String, CodingKey {
        case id, type, name, content, proxied, ttl, priority, comment
        case createdOn = "created_on"
    }

    var isProxied: Bool { proxied ?? false }
}

nonisolated struct CreateDNSRecord: Codable, Sendable {
    let type:     String
    let name:     String
    let content:  String
    let proxied:  Bool
    let ttl:      Int
    let priority: Int?             // 仅 MX / SRV，其他类型传 nil（不编码）
    let comment:  String?
}
