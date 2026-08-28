//
//  WorkerDeploymentsViewModel.swift
//  Orange Cloud
//
//  Worker 部署历史：列出部署、删除历史部署（活跃部署由 Cloudflare 拒绝删除）。
//

import Foundation
import Observation

@Observable
@MainActor
final class WorkerDeploymentsViewModel {

    var deployments: [WorkerDeployment] = []
    var isLoading = false
    var loaded = false
    var error: String?
    var isDeleting = false
    var didDelete = false       // sensoryFeedback 触发器

    private let service: WorkerService
    let accountId: String
    let scriptName: String

    init(service: WorkerService, accountId: String, scriptName: String) {
        self.service = service
        self.accountId = accountId
        self.scriptName = scriptName
    }

    func load() async {
        isLoading = true
        error = nil
        defer { isLoading = false; loaded = true }
        do {
            deployments = try await service.listDeployments(accountId: accountId, scriptName: scriptName)
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
    }

    /// 删除某次部署。活跃部署 Cloudflare 会拒绝（错误透传到 error）。
    func delete(_ deployment: WorkerDeployment) async -> Bool {
        guard !isDeleting else { return false }
        isDeleting = true
        error = nil
        defer { isDeleting = false }
        do {
            try await service.deleteDeployment(accountId: accountId, scriptName: scriptName, deploymentId: deployment.id)
            deployments.removeAll { $0.id == deployment.id }
            didDelete.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }
}
