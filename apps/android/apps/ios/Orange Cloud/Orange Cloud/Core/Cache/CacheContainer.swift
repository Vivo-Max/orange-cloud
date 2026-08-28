//
//  CacheContainer.swift
//  Orange Cloud
//
//  全局共享的 SwiftData 容器：App 主界面与 App Intents 共用同一存储。
//

import Foundation
import SwiftData

nonisolated enum CacheContainer {

    static let shared: ModelContainer = {
        let schema = Schema([
            CachedZone.self,
            CachedDNSRecord.self,
            CachedWorkerScript.self,
        ])
        // cloudKitDatabase 必须显式 .none：App 带 iCloud entitlement 时 .automatic 会
        // 强制开启 CloudKit 同步，而 CloudKit 不允许非可选属性和 @Attribute(.unique)。
        // 缓存数据本就按账号实时拉取，无需跨设备同步。
        let configuration = ModelConfiguration(
            schema: schema,
            isStoredInMemoryOnly: false,
            cloudKitDatabase: .none
        )
        // 上一轮运行中 fetch 连续抛 ObjC 异常（见 CachePolicy.safeFetch）说明本机缓存库
        // 大概率已损坏——容器活着时不能动店文件，标记留到此刻（容器创建前）清库重建。
        if UserDefaults.standard.bool(forKey: rebuildFlagKey) {
            AppLog.app.error("缓存库带损坏标记，启动前清库重建")
            destroyStoreFiles(at: configuration.url)
            UserDefaults.standard.removeObject(forKey: rebuildFlagKey)
            UserDefaults.standard.removeObject(forKey: fetchExceptionCountKey)
        }
        do {
            return try ModelContainer(for: schema, configurations: [configuration])
        } catch {
            // 缓存是可随时按账号从 API 重拉的非关键数据，绝不让它的损坏 / 不兼容把 App 在
            // 启动瞬间崩掉（旧写法在此 fatalError）。先清掉磁盘存储重建；仍失败则退到内存
            // 容器（本次不落盘），保证一定能启动。
            AppLog.app.error("ModelContainer 创建失败，尝试清库重建：\(error.localizedDescription)")
            Self.destroyStoreFiles(at: configuration.url)
            if let rebuilt = try? ModelContainer(for: schema, configurations: [configuration]) {
                return rebuilt
            }
            AppLog.app.error("清库后仍失败，回退内存容器（缓存本次不落盘）")
            let memory = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true, cloudKitDatabase: .none)
            return try! ModelContainer(for: schema, configurations: [memory])
        }
    }()

    /// 启动早期在主线程串行预热一次实体解析。Sentry APPLE-IOS-Y（1.8.2+27~1.8.3+30）：
    /// iOS 17.x 冷启动后数秒内的首次 fetch 抛「could not locate an NSEntityDescription for
    /// entity name 'CachedZone'」，疑为首次实体解析的并发竞态（@Query / Intents 查询 /
    /// ViewModel fetch 同窗口首触）。在任何并发访问前做一次守卫下的空谓词 fetch 灌热实体
    /// 描述；即便竞态假说不成立，异常也会被 SafeCache 兜住并计入店健康。
    @MainActor
    static func warmUp() {
        _ = SafeCache.fetch(FetchDescriptor<CachedZone>(), context: shared.mainContext)
    }

    // MARK: - 店健康记录（TF 崩溃点 D8tiH4pqdctLgx_nCLGnZ：单机纯谓词 fetch 也抛 NSException）

    private static let rebuildFlagKey = "ocCacheStoreNeedsRebuild"
    private static let fetchExceptionCountKey = "ocCacheFetchExceptionCount"
    /// 连续异常达到该数即标记下次启动清库（缓存可随时从 API 重拉，重建成本 ≈ 一次刷新）
    private static let rebuildThreshold = 2

    /// fetch 抛 ObjC 异常时调用（CachePolicy.safeFetch）。计数持久化，跨启动累计。
    static func noteFetchException() {
        let count = UserDefaults.standard.integer(forKey: fetchExceptionCountKey) + 1
        UserDefaults.standard.set(count, forKey: fetchExceptionCountKey)
        if count >= rebuildThreshold {
            UserDefaults.standard.set(true, forKey: rebuildFlagKey)
            AppLog.app.error("缓存 fetch 连续 \(count) 次抛 ObjC 异常，已标记下次启动清库重建")
        }
    }

    /// fetch 正常完成时调用：清零连续异常计数（偶发异常不触发重建）。
    static func noteFetchHealthy() {
        if UserDefaults.standard.integer(forKey: fetchExceptionCountKey) != 0 {
            UserDefaults.standard.removeObject(forKey: fetchExceptionCountKey)
        }
    }

    /// 删除磁盘上的 SwiftData 存储文件（含 -wal / -shm 旁文件），供损坏后清库重建。
    private static func destroyStoreFiles(at storeURL: URL) {
        let fm = FileManager.default
        let dir = storeURL.deletingLastPathComponent()
        let name = storeURL.lastPathComponent          // 默认为 "default.store"
        for suffix in ["", "-wal", "-shm"] {
            try? fm.removeItem(at: dir.appendingPathComponent(name + suffix))
        }
    }
}
