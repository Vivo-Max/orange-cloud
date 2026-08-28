//
//  PinnedResource.swift
//  Orange Cloud
//
//  跨资源类型的「固定到首页」：域名 / Workers / R2 / D1 / KV / Tunnel 六类共用一套置顶模型。
//
//  **只持久化 type + resourceId**，标题/副标题一律在展示时从当前数据源现查：
//  资源改名后置顶依旧有效（把展示文案编进持久化 key 的做法会让改名即失效），
//  查不到的固定项在首页显示为「未找到」并允许一键取消固定。
//

import Foundation
import Observation

/// 可固定的资源类型（rawValue 进持久化，勿改）
nonisolated enum PinnedResourceType: String, Codable, Sendable, Hashable, CaseIterable {
    case zone
    case worker
    case r2
    case d1
    case kv
    case tunnel

    var symbolName: String {
        switch self {
        case .zone:   "globe"
        case .worker: "bolt.fill"
        case .r2:     "externaldrive"
        case .d1:     "cylinder"
        case .kv:     "key"
        case .tunnel: "arrow.triangle.2.circlepath"
        }
    }

    /// 分类名（搜索面板分组标签用）
    var label: String {
        switch self {
        case .zone:   String(localized: "域名")
        case .worker: "Workers"
        case .r2:     "R2"
        case .d1:     "D1"
        case .kv:     "KV"
        case .tunnel: "Tunnel"
        }
    }
}

/// 一条固定记录：类型 + 资源标识（不含任何会变的展示文案）
nonisolated struct PinnedResource: Codable, Hashable, Identifiable, Sendable {

    let type: PinnedResourceType
    /// 资源在其类型内的稳定标识：zone id / worker 脚本名 / bucket 名 / d1 uuid / kv namespace id / tunnel id
    let resourceId: String

    var id: String { "\(type.rawValue)|\(resourceId)" }

    init(type: PinnedResourceType, resourceId: String) {
        self.type = type
        self.resourceId = resourceId
    }

    enum CodingKeys: String, CodingKey {
        case type
        case resourceId
    }
}

/// 按 Cloudflare 账户隔离的置顶存储（仿 AccountPrefsStore 的写法：账号维度字典 + UserDefaults）。
///
/// 未与 AccountPrefsStore 合并的原因：Prefs 是套餐 / 账单日这类**低频配置**且整块镜像给 Widget，
/// 置顶是高频增删的资源状态，混进去会让每次点星标都重写 App Group 镜像，
/// 也要冒着往已持久化的 Prefs JSON 里加非可选字段的解码迁移风险。
@Observable
@MainActor
final class PinnedResourceStore {

    static let shared = PinnedResourceStore()

    /// accountId -> 有序固定列表（保持用户固定的先后顺序）
    private(set) var all: [String: [PinnedResource]] = [:]

    private static let storeKey = "pinnedResourcesByAccountId"
    private static let migratedKey = "pinnedZonesMigratedAccountIds"

    private init() {
        if let data = UserDefaults.standard.data(forKey: Self.storeKey),
           let decoded = try? JSONDecoder().decode([String: [PinnedResource]].self, from: data) {
            all = decoded
        }
    }

    // MARK: - 读

    func pins(for accountId: String) -> [PinnedResource] {
        all[accountId] ?? []
    }

    func pins(for accountId: String, type: PinnedResourceType) -> [PinnedResource] {
        pins(for: accountId).filter { $0.type == type }
    }

    func isPinned(_ resource: PinnedResource, accountId: String) -> Bool {
        pins(for: accountId).contains(resource)
    }

    // MARK: - 写

    /// 切换固定状态，返回切换后的状态（true = 现在已固定）
    @discardableResult
    func toggle(_ resource: PinnedResource, accountId: String) -> Bool {
        guard !accountId.isEmpty else { return false }
        var list = pins(for: accountId)
        if let index = list.firstIndex(of: resource) {
            list.remove(at: index)
            all[accountId] = list
            persist()
            return false
        }
        list.append(resource)
        all[accountId] = list
        persist()
        return true
    }

    func unpin(_ resource: PinnedResource, accountId: String) {
        guard !accountId.isEmpty else { return }
        var list = pins(for: accountId)
        guard let index = list.firstIndex(of: resource) else { return }
        list.remove(at: index)
        all[accountId] = list
        persist()
    }

    // MARK: - 旧数据迁移（CachedZone.pinned → 统一置顶）

    /// 一次性把旧的 `CachedZone.pinned` 迁移进本 store（按账号只做一次）。
    ///
    /// 迁移而非「双读去重」的理由：双读要求两处写入永远同步，一旦某个入口只写一边就会出现
    /// 「首页还固定着、详情页显示未固定」的分裂态；一次性迁移后统一以本 store 为准，
    /// `CachedZone.pinned` 字段保留（不删，老版本回滚/其它入口仍能读到）并在置顶按钮处镜像写入。
    ///
    /// - Parameter pinnedZoneIds: 当前账号下 `pinned == true` 的域名 id
    /// - Parameter hasZoneData: 该账号的域名缓存是否已就绪（为 false 时不落迁移标记，等数据到位再迁）
    func migrateZonePinsIfNeeded(accountId: String, pinnedZoneIds: [String], hasZoneData: Bool) {
        guard !accountId.isEmpty, hasZoneData else { return }
        var migrated = Set(UserDefaults.standard.stringArray(forKey: Self.migratedKey) ?? [])
        guard !migrated.contains(accountId) else { return }

        var list = pins(for: accountId)
        for zoneId in pinnedZoneIds {
            let resource = PinnedResource(type: .zone, resourceId: zoneId)
            if !list.contains(resource) { list.append(resource) }
        }
        all[accountId] = list
        persist()

        migrated.insert(accountId)
        UserDefaults.standard.set(Array(migrated), forKey: Self.migratedKey)
    }

    private func persist() {
        if let data = try? JSONEncoder().encode(all) {
            UserDefaults.standard.set(data, forKey: Self.storeKey)
        }
    }
}
