//
//  SnippetsViewModel.swift
//  Orange Cloud
//
//  Snippets：列表 + 正文 + 增删改 + 触发规则。规则一律 read-modify-write 整组回写，
//  绝不发局部，否则会抹掉 zone 下其他 snippet 的规则。
//

import Foundation
import Observation

@Observable
@MainActor
final class SnippetsViewModel {

    private(set) var snippets: [Snippet] = []
    private(set) var rules:    [SnippetRule] = []
    private(set) var loaded = false        // 区分"未加载"与"加载过但为空"
    var isLoading = false
    var isSaving  = false
    var togglingRuleId: String?
    var error: String?

    private let service: SnippetService
    let zoneId: String

    init(service: SnippetService, zoneId: String) {
        self.service = service
        self.zoneId  = zoneId
    }

    /// 某 snippet 的触发规则
    func rules(for snippetName: String) -> [SnippetRule] {
        rules.filter { $0.snippetName == snippetName }
    }

    func load() async {
        isLoading = true
        error = nil
        // 两个请求并发，但各自独立 await：任一失败都不连累另一个。
        // 若用单个 do 把两次 try await 串起来，前一个抛错会让 async let 作用域提前退出，
        // 把仍在飞行的兄弟请求一并取消（日志里现形为「已取消」红鲱鱼）——这里分开 catch 规避。
        async let snippetsTask = service.list(zoneId: zoneId)
        async let rulesTask    = service.rules(zoneId: zoneId)

        do {
            snippets = try await snippetsTask
            loaded = true
        } catch is CancellationError {
            // 下拉刷新 / searchable 取消，不算加载失败
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            self.error = error.localizedDescription
        }
        do {
            rules = try await rulesTask
        } catch is CancellationError {
        } catch let urlError as URLError where urlError.code == .cancelled {
        } catch {
            if self.error == nil { self.error = error.localizedDescription }
        }
        isLoading = false
    }

    /// 读取 snippet 正文（详情/编辑器按需调用）
    func code(for name: String) async -> String? {
        do {
            return try await service.content(zoneId: zoneId, name: name)
        } catch {
            self.error = error.localizedDescription
            return nil
        }
    }

    /// 创建或更新 snippet 代码。成功返回 true。
    func saveSnippet(name: String, code: String) async -> Bool {
        guard !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            try await service.put(zoneId: zoneId, name: name, code: code)
            await reloadSnippets()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func deleteSnippet(_ snippet: Snippet) async {
        error = nil
        do {
            try await service.delete(zoneId: zoneId, name: snippet.snippetName)
            // 同步清掉指向它的触发规则（整组回写剩余规则）
            let remaining = rules.filter { $0.snippetName != snippet.snippetName }
            if remaining.count != rules.count {
                try await service.putRules(zoneId: zoneId, rules: remaining.map { $0.toInput() })
            }
            await load()
        } catch {
            self.error = error.localizedDescription
        }
    }

    // MARK: - 触发规则（整组 read-modify-write）

    func setRule(_ rule: SnippetRule, enabled: Bool) async {
        guard togglingRuleId == nil else { return }
        togglingRuleId = rule.id
        error = nil
        let inputs = rules.map { $0.id == rule.id ? $0.toInput(enabled: enabled) : $0.toInput() }
        do {
            try await service.putRules(zoneId: zoneId, rules: inputs)
            await reloadRules()
        } catch {
            self.error = error.localizedDescription
        }
        togglingRuleId = nil
    }

    func addRule(snippetName: String, expression: String, description: String?, enabled: Bool) async -> Bool {
        guard !isSaving else { return false }
        isSaving = true
        error = nil
        defer { isSaving = false }
        let new = SnippetRuleInput(
            snippetName: snippetName, expression: expression, description: description, enabled: enabled
        )
        let inputs = rules.map { $0.toInput() } + [new]
        do {
            try await service.putRules(zoneId: zoneId, rules: inputs)
            await reloadRules()
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func deleteRule(_ rule: SnippetRule) async {
        error = nil
        let inputs = rules.filter { $0.id != rule.id }.map { $0.toInput() }
        do {
            try await service.putRules(zoneId: zoneId, rules: inputs)
            await reloadRules()
        } catch {
            self.error = error.localizedDescription
        }
    }

    private func reloadSnippets() async {
        if let list = try? await service.list(zoneId: zoneId) { snippets = list }
    }

    private func reloadRules() async {
        if let list = try? await service.rules(zoneId: zoneId) { rules = list }
    }
}
