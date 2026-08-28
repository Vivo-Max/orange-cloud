//
//  AddZoneViewModel.swift
//  Orange Cloud
//
//  添加域名（新建 Zone）。成功后把新 Zone 单条 upsert 进缓存，
//  列表 @Query 即时可见；createdZone 非空时表单页切换到「名称服务器」结果页。
//

import Foundation
import Observation
import SwiftData

@Observable
@MainActor
final class AddZoneViewModel {

    var isSaving = false
    var error: String?
    /// 创建成功后非空——View 据此从表单切换到结果页（展示 NS + 后续步骤）
    var createdZone: Zone?

    private let zoneService: ZoneService

    init(zoneService: ZoneService) {
        self.zoneService = zoneService
    }

    func create(name: String, accountId: String, context: ModelContext) async {
        guard !isSaving else { return }
        isSaving = true
        error = nil
        do {
            let zone = try await zoneService.createZone(name: name, accountId: accountId)
            // 只 upsert 这一条：不能用 CacheSync.syncZones（它会删除不在传入列表里的其它 zone，
            // 单条传入会清空该账号下的全部已有域名）。新 zone 一般不存在，做存在性兜底即可。
            upsert(zone, accountId: accountId, context: context)
            createdZone = zone
        } catch {
            self.error = error.localizedDescription
        }
        isSaving = false
    }

    private func upsert(_ zone: Zone, accountId: String, context: ModelContext) {
        let zoneId = zone.id
        SafeCache.perform("新 Zone 缓存 upsert") {
            let descriptor = FetchDescriptor<CachedZone>(predicate: #Predicate { $0.id == zoneId })
            if let existing = try context.fetch(descriptor).first {
                existing.update(from: zone)
            } else {
                context.insert(CachedZone(from: zone, accountId: accountId))
            }
            try context.save()
        }
    }
}
