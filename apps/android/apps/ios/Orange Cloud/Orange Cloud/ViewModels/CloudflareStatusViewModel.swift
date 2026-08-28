//
//  CloudflareStatusViewModel.swift
//  Orange Cloud
//
//  设置页「Cloudflare 状态」：总体状态、进行中的事件、计划维护、
//  受影响组件与近期事件历史。实时拉取，不进 SwiftData。
//

import Foundation
import Observation

@Observable
@MainActor
final class CloudflareStatusViewModel {

    private(set) var overall: StatusPageOverall?
    private(set) var activeIncidents: [StatusPageIncident] = []
    private(set) var maintenances: [StatusPageIncident] = []
    /// 非正常状态的产品服务（「Cloudflare Sites and Services」分组的叶子组件）
    private(set) var affectedProducts: [StatusPageComponent] = []
    private(set) var productTotal = 0
    /// 边缘网络按大区汇总（PoP 节点常态有几十个在维护/重路由，不逐个列出）
    private(set) var regions: [StatusPageRegion] = []
    /// 已解决的近期事件（剔除进行中的，最多 10 条）
    private(set) var recentIncidents: [StatusPageIncident] = []
    var isLoading = false
    var error: String?

    private static let serviceGroupName = "Cloudflare Sites and Services"

    private let service: StatusPageService

    init(service: StatusPageService = StatusPageService()) {
        self.service = service
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            async let summaryFetch = service.summary()
            async let historyFetch = service.recentIncidents()
            let (summary, history) = try await (summaryFetch, historyFetch)

            overall = summary.status
            activeIncidents = summary.incidents
            maintenances = summary.scheduledMaintenances
            splitComponents(summary.components)
            let activeIds = Set(summary.incidents.map(\.id))
            recentIncidents = Array(history.filter { !activeIds.contains($0.id) }.prefix(10))
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    /// 组件拆成「产品服务」与「边缘网络大区」。分组结构变化时兜底为直接列异常叶子组件。
    private func splitComponents(_ components: [StatusPageComponent]) {
        guard let serviceGroup = components.first(where: { $0.group == true && $0.name == Self.serviceGroupName }) else {
            let leaves = components.filter { $0.group != true }
            productTotal = leaves.count
            affectedProducts = leaves.filter { $0.status != "operational" }
            regions = []
            return
        }
        let products = components.filter { $0.groupId == serviceGroup.id }
        productTotal = products.count
        affectedProducts = products.filter { $0.status != "operational" }
        regions = components
            .filter { $0.group == true && $0.id != serviceGroup.id }
            .map { group in
                let nodes = components.filter { $0.groupId == group.id }
                return StatusPageRegion(
                    id: group.id,
                    name: group.name,
                    total: nodes.count,
                    impacted: nodes.count { $0.status != "operational" }
                )
            }
    }
}
