//
//  WorkerTriggersViewModel.swift
//  Orange Cloud
//
//  Worker Cron 定时触发器。整组 read-modify-write：PUT 替换全集，加/删都回写完整 cron 列表。
//

import Foundation
import Observation

@Observable
@MainActor
final class WorkerTriggersViewModel {

    private(set) var schedules: [WorkerSchedule] = []
    private(set) var loaded = false
    var isLoading = false
    var isSaving  = false
    var error: String?

    private let service: WorkerService
    let accountId:  String
    let scriptName: String

    init(service: WorkerService, accountId: String, scriptName: String) {
        self.service    = service
        self.accountId  = accountId
        self.scriptName = scriptName
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            schedules = try await service.schedules(accountId: accountId, scriptName: scriptName)
            loaded = true
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func addCron(_ cron: String) async -> Bool {
        guard !isSaving else { return false }
        let trimmed = cron.trimmingCharacters(in: .whitespaces)
        guard !trimmed.isEmpty, !schedules.contains(where: { $0.cron == trimmed }) else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        let crons = schedules.map(\.cron) + [trimmed]
        do {
            try await service.putSchedules(accountId: accountId, scriptName: scriptName, crons: crons)
            await reload()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func deleteCron(_ schedule: WorkerSchedule) async {
        error = nil
        let crons = schedules.filter { $0.cron != schedule.cron }.map(\.cron)
        do {
            try await service.putSchedules(accountId: accountId, scriptName: scriptName, crons: crons)
            await reload()
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func reload() async {
        if let list = try? await service.schedules(accountId: accountId, scriptName: scriptName) {
            schedules = list
        }
    }
}
