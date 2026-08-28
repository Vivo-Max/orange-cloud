//
//  CachePolicy.swift
//  Orange Cloud
//
//  缓存有效期（stale-while-revalidate）：SwiftData 缓存在 TTL 内视为新鲜——冷启动 / 切 Tab
//  直接用缓存（@Query 已即时渲染），不再发同一份请求；下拉刷新与切账号始终强制重拉（force）。
//  后台静默刷新（BackgroundRefresh）负责把数据预热到最新，用户切回前台直接看到新数据。
//

import Foundation
import SwiftData

@MainActor
enum CachePolicy {

    /// 域名 / 资产列表有效期
    static let zones: TimeInterval = 10 * 60
    /// DNS 记录有效期
    static let dns: TimeInterval = 10 * 60

    static func isFresh(_ date: Date?, ttl: TimeInterval) -> Bool {
        guard let date else { return false }
        let age = Date().timeIntervalSince(date)
        return age >= 0 && age < ttl
    }

    // ⚠️ 这里只允许「纯谓词」fetch，禁止 sortBy + fetchLimit：该组合在 iOS 17.0 的
    // CoreData 层抛 ObjC 异常（TF 崩溃点 D8tiH4pqdctLgx_nCLGnZ，1.8.1/1.8.2 实测）。
    // 1.8.2(24) 改纯谓词后同一台设备仍在崩（build 24/27 日志砸在纯谓词 fetch 本身），
    // 说明该机缓存库已损坏——Swift 的 try? 接不住 CoreData 的 NSException，必须经
    // SafeCache 的 ObjC @try 垫片兜底：异常按「缓存不新鲜」处理（触发正常重拉），
    // 连续异常由 CacheContainer 标记下次启动清库重建。
    // 单账号/单域名下的缓存行数很小，内存里取 max(updatedAt) 足够。

    /// 某账号缓存的域名是否仍在有效期内（用于冷启动免重拉）
    static func zonesFresh(accountId: String, context: ModelContext) -> Bool {
        let descriptor = FetchDescriptor<CachedZone>(
            predicate: #Predicate { $0.accountId == accountId }
        )
        guard let cached = SafeCache.fetch(descriptor, context: context), !cached.isEmpty else { return false }
        return isFresh(cached.map(\.updatedAt).max(), ttl: zones)
    }

    /// 某域名缓存的 DNS 记录是否仍在有效期内
    static func dnsFresh(zoneId: String, context: ModelContext) -> Bool {
        let descriptor = FetchDescriptor<CachedDNSRecord>(
            predicate: #Predicate { $0.zoneId == zoneId }
        )
        guard let cached = SafeCache.fetch(descriptor, context: context), !cached.isEmpty else { return false }
        return isFresh(cached.map(\.updatedAt).max(), ttl: dns)
    }
}
