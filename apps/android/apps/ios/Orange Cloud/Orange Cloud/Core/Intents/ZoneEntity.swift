//
//  ZoneEntity.swift
//  Orange Cloud
//
//  Zone 的 App Intents 实体：数据来自 SwiftData 本地缓存，Siri/快捷指令可离线查询。
//

import Foundation
import AppIntents
import SwiftData

nonisolated struct ZoneEntity: AppEntity, Identifiable {

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "域名"
    static let defaultQuery = ZoneEntityQuery()

    let id:       String
    let name:     String
    let status:   String
    let planName: String

    var displayRepresentation: DisplayRepresentation {
        DisplayRepresentation(
            title: "\(name)",
            subtitle: "\(status) · \(planName)"
        )
    }

    @MainActor
    init(from cached: CachedZone) {
        self.id       = cached.id
        self.name     = cached.name
        self.status   = cached.status
        self.planName = cached.planName
    }
}

nonisolated struct ZoneEntityQuery: EntityQuery {

    // fetch 一律走 SafeCache（iOS 17.x CoreData 会抛 Swift 接不住的 NSException，
    // 系统在冷启动就可能来查实体，异常按无缓存处理，绝不让 Intents 查询崩掉 App）。

    func entities(for identifiers: [ZoneEntity.ID]) async throws -> [ZoneEntity] {
        await MainActor.run {
            let context = ModelContext(CacheContainer.shared)
            let zones = SafeCache.fetch(FetchDescriptor<CachedZone>(), context: context) ?? []
            return zones
                .filter { identifiers.contains($0.id) }
                .map(ZoneEntity.init(from:))
        }
    }

    func suggestedEntities() async throws -> [ZoneEntity] {
        await MainActor.run {
            let context = ModelContext(CacheContainer.shared)
            let zones = SafeCache.fetch(FetchDescriptor<CachedZone>(), context: context) ?? []
            // 内存排序：iOS 17.0 的 CoreData 对带 sortBy 的 fetch 更易触发 NSException（缓存行数小，内存排序足够）
            return zones
                .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
                .map(ZoneEntity.init(from:))
        }
    }
}
