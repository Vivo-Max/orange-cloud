//
//  HealthCheckAlertViewModel.swift
//  Orange Cloud
//
//  「源站异常时通知我」——把 CF 原生的 health_check_status_notification 告警
//  一键接到推送端点上。
//
//  为什么不像构建那样走后台轮询：健康检查在 Cloudflare 侧**有原生告警类型**，
//  服务端直接投递 webhook，App 关着也能收到，比客户端比对可靠得多。
//  通用告警页（CFAlertingView）本来就能开这一项，但混在 69 个告警类型里没人找得到，
//  所以在健康检查页给一个直达开关。
//

import Foundation
import Observation

@Observable
@MainActor
final class HealthCheckAlertViewModel {

    /// CF 的健康检查状态告警类型
    static let alertType = "health_check_status_notification"

    private(set) var isEnabled = false
    private(set) var isAvailable = false
    var isLoading = false
    var isMutating = false
    var error: String?

    private let service: AlertingService
    private let accountId: String
    private var policyId: String?
    private var webhookId: String?

    init(service: AlertingService, accountId: String) {
        self.service = service
        self.accountId = accountId
    }

    /// 端点没注册（用户还没开通推送中心）时整个开关不显示
    var pushEndpoint: String? { PushConfig.endpointURL }

    private var webhookURL: String? {
        pushEndpoint.map { "\($0)/cf" }
    }

    func load() async {
        guard pushEndpoint != nil else { return }
        isLoading = true
        error = nil
        // 该账号是否支持这个告警类型（免费套餐可能没有）
        if let groups = try? await service.availableAlerts(accountId: accountId) {
            isAvailable = groups.values.flatMap { $0 }.contains { $0.type == Self.alertType }
        }
        if let policies = try? await service.policies(accountId: accountId) {
            let existing = policies.first { $0.alertType == Self.alertType }
            policyId = existing?.id
            isEnabled = existing != nil
        }
        if let hooks = try? await service.webhooks(accountId: accountId), let url = webhookURL {
            webhookId = hooks.first { $0.url == url }?.id
        }
        isLoading = false
    }

    func setEnabled(_ on: Bool) async {
        guard !isMutating, let url = webhookURL else { return }
        isMutating = true
        error = nil
        do {
            if on {
                // 复用已有的推送 webhook，没有才建——避免每开一项告警就多一个目标
                let hookId: String
                if let webhookId {
                    hookId = webhookId
                } else {
                    hookId = try await service.createWebhook(
                        accountId: accountId, name: "Orange Cloud Push", url: url
                    )
                    webhookId = hookId
                }
                policyId = try await service.createPolicy(
                    accountId: accountId,
                    name: String(localized: "OC：源站健康检查"),
                    alertType: Self.alertType,
                    webhookId: hookId
                )
                isEnabled = true
            } else if let policyId {
                try await service.deletePolicy(accountId: accountId, id: policyId)
                self.policyId = nil
                isEnabled = false
            }
        } catch {
            self.error = error.localizedDescription
        }
        isMutating = false
    }
}
