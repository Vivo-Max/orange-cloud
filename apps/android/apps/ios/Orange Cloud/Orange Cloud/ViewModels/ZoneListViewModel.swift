//
//  ZoneListViewModel.swift
//  Orange Cloud
//
//  拉取 Zone 列表并同步进 SwiftData 缓存，View 通过 @Query 渲染。
//

import Foundation
import Observation
import SwiftData

@Observable
@MainActor
final class ZoneListViewModel {

    var isLoading = false
    var error: String?

    private let zoneService: ZoneService
    /// 进行中的加载任务。.task 首屏加载与 .refreshable 下拉刷新都会触发刷新：
    /// ① 多次触发复用同一个任务，避免并发重复请求；
    /// ② 用独立的 `Task`（非结构化、不是下拉手势那个子任务）承载实际网络加载——
    ///    SwiftUI 在下拉手势完成 / searchable 重建时会取消 .refreshable 的子任务，
    ///    若加载直接跑在该子任务里，URLSession 会抛 .cancelled，下拉刷新便 100% 报
    ///    「网络错误：已取消」。放进独立任务后，手势取消不会波及加载，数据照常写入缓存。
    private var loadTask: Task<Void, Never>?

    init(zoneService: ZoneService) {
        self.zoneService = zoneService
    }

    /// 从 API 刷新并 upsert 进缓存（共享逻辑见 CacheSync，含 Widget 快照与 Spotlight 索引）
    func refresh(accountId: String, accountName: String = "", context: ModelContext, force: Bool = false) async {
        // 非强制（首屏/切 Tab）且缓存仍新鲜：用缓存（@Query 已渲染），不重发请求
        if !force, CachePolicy.zonesFresh(accountId: accountId, context: context) { return }
        // 已有加载在跑：等它结束即可，不另起重复请求
        if let loadTask {
            await loadTask.value
            return
        }
        let task = Task { [weak self] in
            guard let self else { return }
            await self.load(accountId: accountId, accountName: accountName, context: context)
        }
        loadTask = task
        defer { loadTask = nil }
        await task.value
    }

    private func load(accountId: String, accountName: String, context: ModelContext) async {
        isLoading = true
        error = nil
        do {
            let zones = try await zoneService.listZones(accountId: accountId)
            CacheSync.syncZones(zones, accountId: accountId, accountName: accountName, context: context)
        } catch is CancellationError {
            // 任务取消属正常生命周期，不算加载失败
        } catch let urlError as URLError where urlError.code == .cancelled {
            // URLSession 把任务取消转成 .cancelled，同样不展示为错误
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}
