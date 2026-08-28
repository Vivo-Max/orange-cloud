//
//  WorkerMetricsViewModel.swift
//  Orange Cloud
//
//  单 Worker 指标：摘要 + 状态分解 + 时间序列，按时间范围会话级缓存。
//

import Foundation
import Observation

@Observable
@MainActor
final class WorkerMetricsViewModel {

    var range: AnalyticsTimeRange = .last24h
    private(set) var metrics: WorkerMetrics?
    private(set) var series: [WorkerSeriesPoint] = []
    var isLoading = false
    var error: String?
    /// 账户级数据集未授权（免费账号）：视图显示「无账户级数据权限」而非报错
    private(set) var accountAnalyticsUnavailable = false

    private var cache: [AnalyticsTimeRange: (metrics: WorkerMetrics, series: [WorkerSeriesPoint])] = [:]
    private let analyticsService: AnalyticsService
    private let accountId: String
    private let scriptName: String

    init(analyticsService: AnalyticsService, accountId: String, scriptName: String) {
        self.analyticsService = analyticsService
        self.accountId = accountId
        self.scriptName = scriptName
    }

    func load(force: Bool = false) async {
        if !force, let cached = cache[range] {
            metrics = cached.metrics
            series = cached.series
            return
        }
        isLoading = true
        error = nil
        accountAnalyticsUnavailable = false
        do {
            // 序列查询失败不阻塞摘要（datetimeHour/date 分组属较新 schema）
            async let metricsTask = analyticsService.workerMetrics(
                accountId: accountId, scriptName: scriptName, range: range
            )
            async let seriesTask = analyticsService.workerSeries(
                accountId: accountId, scriptName: scriptName, range: range
            )
            let metrics = try await metricsTask
            let series = (try? await seriesTask) ?? []

            cache[range] = (metrics, series)
            self.metrics = metrics
            self.series = series
        } catch let error as APIError where error.isAccountNotAuthorized {
            accountAnalyticsUnavailable = true
            self.error = nil
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func refresh() async {
        cache.removeAll()
        await load(force: true)
    }
}
