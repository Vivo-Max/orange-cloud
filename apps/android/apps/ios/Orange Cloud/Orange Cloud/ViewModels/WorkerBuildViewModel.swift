//
//  WorkerBuildViewModel.swift
//  Orange Cloud
//

import Foundation
import Observation

@Observable
@MainActor
final class WorkerBuildViewModel {

    private(set) var builds: [WorkerBuild] = []
    private(set) var logs: [BuildLogLine] = []
    private(set) var logsBuildUuid: String?
    var isLoading = false
    var loaded = false
    var isLoadingLogs = false
    var isMutating = false
    var error: String?
    var didMutate = false

    private let service: WorkerBuildService
    private let accountId: String
    private let scriptId: String

    init(service: WorkerBuildService, accountId: String, scriptId: String) {
        self.service = service
        self.accountId = accountId
        self.scriptId = scriptId
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            builds = try await service.builds(accountId: accountId, scriptId: scriptId)
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func loadLogs(for build: WorkerBuild) async {
        guard !isLoadingLogs else { return }
        isLoadingLogs = true
        logsBuildUuid = build.buildUuid
        logs = []
        do {
            logs = try await service.logs(accountId: accountId, buildUuid: build.buildUuid)
        } catch {
            self.error = error.localizedDescription
        }
        isLoadingLogs = false
    }

    func cancel(_ build: WorkerBuild) async {
        guard !isMutating else { return }
        isMutating = true
        error = nil
        do {
            try await service.cancel(accountId: accountId, buildUuid: build.buildUuid)
            didMutate.toggle()
            await load()
        } catch {
            self.error = error.localizedDescription
        }
        isMutating = false
    }
}
