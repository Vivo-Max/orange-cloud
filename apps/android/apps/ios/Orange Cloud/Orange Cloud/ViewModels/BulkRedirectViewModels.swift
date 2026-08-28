//
//  BulkRedirectViewModels.swift
//  Orange Cloud
//
//  Bulk Redirects：重定向列表 CRUD + 列表详情（条目异步增删 + 启用规则开关）。
//

import Foundation
import Observation

@Observable
@MainActor
final class RedirectListsViewModel {

    private(set) var lists: [RedirectList] = []
    var isLoading = false
    var loaded = false
    var isMutating = false
    var error: String?
    var didMutate = false

    private let service: BulkRedirectService
    let accountId: String

    init(service: BulkRedirectService, accountId: String) {
        self.service = service
        self.accountId = accountId
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            lists = try await service.listRedirectLists(accountId: accountId)
            loaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func create(name: String, description: String?) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            _ = try await service.createList(accountId: accountId, name: name, description: description)
            await reload()
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func delete(_ list: RedirectList) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            try await service.deleteList(accountId: accountId, listId: list.id)
            lists.removeAll { $0.id == list.id }
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    private func reload() async {
        if let result = try? await service.listRedirectLists(accountId: accountId) { lists = result }
    }
}

@Observable
@MainActor
final class RedirectListDetailViewModel {

    var list: RedirectList
    private(set) var items: [RedirectListItem] = []
    var isLoadingItems = false
    var itemsLoaded = false
    var isMutating = false
    /// 异步批量操作进行中的提示（轮询期间）
    var statusText: String?
    var error: String?
    var didMutate = false

    // 启用状态（http_request_redirect ruleset 中引用本列表的规则）
    private(set) var enableLoaded = false
    private(set) var isEnabled = false
    private var enableRulesetId: String?
    private var enableRuleId: String?

    private let service: BulkRedirectService
    let accountId: String

    var listName: String { list.name ?? "" }

    init(list: RedirectList, accountId: String, service: BulkRedirectService) {
        self.list = list
        self.accountId = accountId
        self.service = service
    }

    func loadItems() async {
        isLoadingItems = true
        error = nil
        do {
            items = try await service.listItems(accountId: accountId, listId: list.id)
            itemsLoaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        isLoadingItems = false
    }

    func loadEnableStatus() async {
        do {
            let ruleset = try await service.redirectEntrypoint(accountId: accountId)
            enableRulesetId = ruleset?.id
            if let rule = ruleset?.rules?.first(where: { $0.actionParameters?.fromList?.name == list.name }) {
                enableRuleId = rule.id
                isEnabled = rule.enabled ?? false
            } else {
                enableRuleId = nil
                isEnabled = false
            }
            enableLoaded = true
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
    }

    /// 追加一条重定向（异步：等待批量操作完成后刷新）
    func addItem(_ redirect: RedirectRule) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        statusText = String(localized: "应用中…")
        error = nil
        defer { isMutating = false; statusText = nil }
        do {
            let opId = try await service.createItems(
                accountId: accountId, listId: list.id, items: [RedirectItemInput(redirect: redirect)]
            )
            try await service.waitForOperation(accountId: accountId, operationId: opId)
            await loadItems()
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func deleteItem(_ item: RedirectListItem) async -> Bool {
        guard !isMutating else { return false }
        isMutating = true
        statusText = String(localized: "应用中…")
        error = nil
        defer { isMutating = false; statusText = nil }
        do {
            let opId = try await service.deleteItems(accountId: accountId, listId: list.id, itemIds: [item.id])
            try await service.waitForOperation(accountId: accountId, operationId: opId)
            await loadItems()
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    /// 启用 / 停用本列表（管理 http_request_redirect ruleset 里的规则）
    func setEnabled(_ on: Bool) async -> Bool {
        guard !isMutating, !listName.isEmpty else { return false }
        isMutating = true
        error = nil
        defer { isMutating = false }
        do {
            if on {
                if let rulesetId = enableRulesetId, let ruleId = enableRuleId {
                    _ = try await service.setRuleEnabled(accountId: accountId, rulesetId: rulesetId, ruleId: ruleId, enabled: true)
                } else if let rulesetId = enableRulesetId {
                    _ = try await service.addRule(accountId: accountId, rulesetId: rulesetId, rule: .enabling(listName: listName))
                } else {
                    _ = try await service.createEntrypoint(accountId: accountId, rule: .enabling(listName: listName))
                }
            } else if let rulesetId = enableRulesetId, let ruleId = enableRuleId {
                _ = try await service.setRuleEnabled(accountId: accountId, rulesetId: rulesetId, ruleId: ruleId, enabled: false)
            }
            await loadEnableStatus()
            didMutate.toggle()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }
}
