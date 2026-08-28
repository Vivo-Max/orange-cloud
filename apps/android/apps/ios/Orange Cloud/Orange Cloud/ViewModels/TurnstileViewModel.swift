//
//  TurnstileViewModel.swift
//  Orange Cloud
//
//  Turnstile widget 列表与增删改、密钥轮换。
//

import Foundation
import Observation

@Observable
@MainActor
final class TurnstileViewModel {

    private(set) var widgets: [TurnstileWidget] = []
    private(set) var loaded = false
    var isLoading = false
    var isSaving  = false
    var error: String?

    private let service: TurnstileService
    let accountId: String

    init(service: TurnstileService, accountId: String) {
        self.service = service
        self.accountId = accountId
    }

    func load() async {
        isLoading = true
        error = nil
        do {
            widgets = try await service.listWidgets(accountId: accountId)
            loaded = true
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func create(input: TurnstileWidgetInput) async -> TurnstileWidget? {
        guard !isSaving else { return nil }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let widget = try await service.createWidget(accountId: accountId, input: input)
            widgets.append(widget)
            return widget
        } catch {
            self.error = error.localizedDescription
            return nil
        }
    }

    func update(sitekey: String, input: TurnstileWidgetInput) async -> TurnstileWidget? {
        guard !isSaving else { return nil }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let widget = try await service.updateWidget(accountId: accountId, sitekey: sitekey, input: input)
            replace(widget)
            return widget
        } catch {
            self.error = error.localizedDescription
            return nil
        }
    }

    func delete(_ widget: TurnstileWidget) async -> Bool {
        error = nil
        do {
            try await service.deleteWidget(accountId: accountId, sitekey: widget.sitekey)
            widgets.removeAll { $0.sitekey == widget.sitekey }
            return true
        } catch {
            self.error = error.localizedDescription
            return false
        }
    }

    func rotateSecret(sitekey: String, immediately: Bool) async -> TurnstileWidget? {
        guard !isSaving else { return nil }
        isSaving = true
        error = nil
        defer { isSaving = false }
        do {
            let widget = try await service.rotateSecret(
                accountId: accountId, sitekey: sitekey, immediately: immediately
            )
            replace(widget)
            return widget
        } catch {
            self.error = error.localizedDescription
            return nil
        }
    }

    /// 详情页拉带 secret 的完整对象（列表响应含 secret，但以单查为准兜底）
    func detail(sitekey: String) async -> TurnstileWidget? {
        do {
            let widget = try await service.widget(accountId: accountId, sitekey: sitekey)
            replace(widget)
            return widget
        } catch {
            self.error = error.localizedDescription
            return nil
        }
    }

    private func replace(_ widget: TurnstileWidget) {
        if let index = widgets.firstIndex(where: { $0.sitekey == widget.sitekey }) {
            widgets[index] = widget
        } else {
            widgets.append(widget)
        }
    }
}
