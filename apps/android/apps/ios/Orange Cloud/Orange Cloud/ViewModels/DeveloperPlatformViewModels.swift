//
//  DeveloperPlatformViewModels.swift
//  Orange Cloud
//
//  Queues / AI Gateway / Durable Objects / Workers AI 的 ViewModel。
//

import Foundation
import Observation

// MARK: - Queues

@Observable
@MainActor
final class QueuesViewModel {

    private(set) var queues: [CFQueue] = []
    var isLoading = false
    var loaded = false
    var isSaving = false
    var error: String?
    var didChange = false

    private let service: QueueService
    let accountId: String?

    init(service: QueueService, accountId: String?) {
        self.service = service
        self.accountId = accountId
    }

    func load() async {
        guard let accountId, !isLoading else { return }
        isLoading = true
        error = nil
        do {
            queues = try await service.list(accountId: accountId)
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func create(name: String) async -> Bool {
        guard let accountId, !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let created = try await service.create(accountId: accountId, name: name)
            queues.insert(created, at: 0)
            didChange.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func delete(_ queue: CFQueue) async {
        guard let accountId, !isSaving else { return }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            try await service.delete(accountId: accountId, queueId: queue.queueId)
            queues.removeAll { $0.queueId == queue.queueId }
            didChange.toggle()
        } catch {
            self.error = error.localizedDescription
        }
    }

    /// 改名 / 改投递设置，成功后用返回值替换列表内对应项（详情 sheet 读列表即时反映）
    @discardableResult
    func update(queueId: String, queueName: String? = nil, settings: CFQueueSettingsPatch? = nil) async -> Bool {
        guard let accountId, !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let updated = try await service.update(
                accountId: accountId, queueId: queueId,
                body: CFQueueUpdate(queueName: queueName, settings: settings)
            )
            if let idx = queues.firstIndex(where: { $0.queueId == queueId }) { queues[idx] = updated }
            didChange.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    @discardableResult
    func togglePause(queueId: String, paused: Bool) async -> Bool {
        await update(queueId: queueId, settings: CFQueueSettingsPatch(deliveryPaused: paused))
    }

    @discardableResult
    func purge(queueId: String) async -> Bool {
        guard let accountId, !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            try await service.purge(accountId: accountId, queueId: queueId)
            didChange.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }
}

// MARK: - AI Gateway

@Observable
@MainActor
final class AIGatewayViewModel {

    private(set) var gateways: [AIGateway] = []
    var isLoading = false
    var loaded = false
    var isSaving = false
    var error: String?
    var didChange = false

    private let service: AIGatewayService
    let accountId: String?

    init(service: AIGatewayService, accountId: String?) {
        self.service = service
        self.accountId = accountId
    }

    func load() async {
        guard let accountId, !isLoading else { return }
        isLoading = true
        error = nil
        do {
            gateways = try await service.list(accountId: accountId)
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func create(_ body: AIGatewayCreate) async -> Bool {
        guard let accountId, !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let created = try await service.create(accountId: accountId, body: body)
            gateways.insert(created, at: 0)
            didChange.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func delete(_ gateway: AIGateway) async {
        guard let accountId, !isSaving else { return }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            try await service.delete(accountId: accountId, gatewayId: gateway.id)
            gateways.removeAll { $0.id == gateway.id }
            didChange.toggle()
        } catch {
            self.error = error.localizedDescription
        }
    }
}

// MARK: - Durable Objects（只读）

@Observable
@MainActor
final class DurableObjectsViewModel {

    private(set) var namespaces: [DurableObjectNamespace] = []
    var isLoading = false
    var loaded = false
    var error: String?

    private let service: DurableObjectService
    let accountId: String?

    init(service: DurableObjectService, accountId: String?) {
        self.service = service
        self.accountId = accountId
    }

    func load() async {
        guard let accountId, !isLoading else { return }
        isLoading = true
        error = nil
        do {
            namespaces = try await service.listNamespaces(accountId: accountId)
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

// MARK: - Workers AI（只读模型目录）

@Observable
@MainActor
final class WorkersAIViewModel {

    private(set) var models: [AIModel] = []
    var isLoading = false
    var loaded = false
    var error: String?

    private let service: WorkersAIService
    let accountId: String?

    init(service: WorkersAIService, accountId: String?) {
        self.service = service
        self.accountId = accountId
    }

    /// 按任务类型分组（Text Generation / Text-to-Image 等）
    var grouped: [(task: String, models: [AIModel])] {
        let groups = Dictionary(grouping: models) { $0.taskName.isEmpty ? String(localized: "其它") : $0.taskName }
        return groups.map { (task: $0.key, models: $0.value.sorted { $0.shortName < $1.shortName }) }
            .sorted { $0.task < $1.task }
    }

    func load() async {
        guard let accountId, !isLoading else { return }
        isLoading = true
        error = nil
        do {
            models = try await service.listModels(accountId: accountId)
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

// MARK: - Hyperdrive

@Observable
@MainActor
final class HyperdriveViewModel {

    private(set) var configs: [HyperdriveConfig] = []
    var isLoading = false
    var loaded = false
    var isSaving = false
    var error: String?
    var didChange = false

    private let service: HyperdriveService
    let accountId: String?

    init(service: HyperdriveService, accountId: String?) {
        self.service = service
        self.accountId = accountId
    }

    func load() async {
        guard let accountId, !isLoading else { return }
        isLoading = true
        error = nil
        do {
            configs = try await service.list(accountId: accountId)
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func create(_ body: HyperdriveCreate) async -> Bool {
        guard let accountId, !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let created = try await service.create(accountId: accountId, body: body)
            configs.insert(created, at: 0)
            didChange.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func delete(_ config: HyperdriveConfig) async {
        guard let accountId, !isSaving else { return }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            try await service.delete(accountId: accountId, configId: config.id)
            configs.removeAll { $0.id == config.id }
            didChange.toggle()
        } catch {
            self.error = error.localizedDescription
        }
    }

    /// 改名 / 改缓存 / 改源连接，成功后用返回值替换列表内对应项
    @discardableResult
    func update(configId: String, patch: HyperdrivePatch) async -> Bool {
        guard let accountId, !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let updated = try await service.update(accountId: accountId, configId: configId, body: patch)
            if let idx = configs.firstIndex(where: { $0.id == configId }) { configs[idx] = updated }
            didChange.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }
}

// MARK: - Durable Objects 对象实例（只读，游标分页）

@Observable
@MainActor
final class DurableObjectInstancesViewModel {

    private(set) var instances: [DurableObjectInstance] = []
    var isLoading = false
    var loaded = false
    var error: String?

    private var cursor: String?
    var hasMore: Bool { cursor != nil }

    private let service: DurableObjectService
    let accountId: String?
    let namespaceId: String

    // MARK: 内存用量（账户级 GraphQL，免费账号常被 authz 挡）

    private(set) var memory: DurableObjectMemory?

    init(service: DurableObjectService, accountId: String?, namespaceId: String) {
        self.service = service
        self.accountId = accountId
        self.namespaceId = namespaceId
    }

    /// 拿不到就静默——与账号用量同一条 authz 链路，弹错只会打扰用户
    func loadMemory() async {
        guard let accountId else { return }
        memory = try? await service.memoryUsage(accountId: accountId, namespaceId: namespaceId)
    }


    func load() async {
        guard let accountId, !isLoading else { return }
        isLoading = true
        error = nil
        do {
            let page = try await service.listObjects(accountId: accountId, namespaceId: namespaceId)
            instances = page.items
            cursor = page.cursor
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func loadMore() async {
        guard let accountId, !isLoading, let cursor else { return }
        isLoading = true
        do {
            let page = try await service.listObjects(accountId: accountId, namespaceId: namespaceId, cursor: cursor)
            instances.append(contentsOf: page.items)
            self.cursor = page.cursor
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

// MARK: - Workers AI 文本生成 Playground

@Observable
@MainActor
final class AIPlaygroundViewModel {

    var prompt = ""
    private(set) var output = ""
    var isRunning = false
    var error: String?

    private let service: WorkersAIService
    let accountId: String?
    let model: AIModel

    init(service: WorkersAIService, accountId: String?, model: AIModel) {
        self.service = service
        self.accountId = accountId
        self.model = model
    }

    var trimmedPrompt: String { prompt.trimmingCharacters(in: .whitespacesAndNewlines) }
    var canRun: Bool { !trimmedPrompt.isEmpty && !isRunning && accountId != nil }

    func run() async {
        guard let accountId, canRun else { return }
        isRunning = true
        error = nil
        output = ""
        defer { isRunning = false }
        do {
            output = try await service.runTextGeneration(
                accountId: accountId,
                model: model.name ?? model.id,
                messages: [AIChatMessage(role: "user", content: trimmedPrompt)]
            )
        } catch {
            self.error = error.localizedDescription
        }
    }
}
