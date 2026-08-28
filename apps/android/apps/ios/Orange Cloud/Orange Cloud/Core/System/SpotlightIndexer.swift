//
//  SpotlightIndexer.swift
//  Orange Cloud
//
//  CoreSpotlight 索引：Zone 与 DNS 记录可在系统搜索中直达。
//  数据刷新时增量重建对应 domain，索引失败静默忽略（非关键路径）。
//

import Foundation
import CoreSpotlight

nonisolated enum SpotlightIndexer {

    static let zoneDomain = "zones"
    static let dnsDomain  = "dns"

    /// Zone 列表刷新后全量重建 zone 索引
    static func indexZones(_ zones: [Zone]) {
        let items = zones.map { zone in
            let attributes = CSSearchableItemAttributeSet(contentType: .item)
            attributes.title = zone.name
            attributes.contentDescription = String(localized: "Cloudflare 域名 · \(zone.status)")
            attributes.keywords = ["Cloudflare", "Zone", "域名", zone.name]
            return CSSearchableItem(
                uniqueIdentifier: "zone-\(zone.id)",
                domainIdentifier: zoneDomain,
                attributeSet: attributes
            )
        }
        // 默认索引按提交顺序处理操作，先删后建无需嵌套回调
        let index = CSSearchableIndex.default()
        index.deleteSearchableItems(withDomainIdentifiers: [zoneDomain])
        index.indexSearchableItems(items)
    }

    /// 单个 Zone 的 DNS 记录刷新后重建该 Zone 的 DNS 索引
    static func indexDNSRecords(_ records: [DNSRecord], zoneId: String, zoneName: String) {
        let domain = "\(dnsDomain)-\(zoneId)"
        let items = records.map { record in
            let attributes = CSSearchableItemAttributeSet(contentType: .item)
            attributes.title = record.name
            attributes.contentDescription = String(localized: "\(record.type) 记录 · \(zoneName) · \(record.content)")
            attributes.keywords = ["DNS", record.type, zoneName, record.name]
            return CSSearchableItem(
                uniqueIdentifier: "dns-\(record.id)",
                domainIdentifier: domain,
                attributeSet: attributes
            )
        }
        let index = CSSearchableIndex.default()
        index.deleteSearchableItems(withDomainIdentifiers: [domain])
        index.indexSearchableItems(items)
    }

    /// 登出时清空全部索引
    static func deleteAll() {
        CSSearchableIndex.default().deleteAllSearchableItems()
    }
}
