//
//  EmailRoutingView.swift
//  Orange Cloud
//
//  Email Routing：开关 + 路由规则（转发）增删改 + 账号级目的地址。
//  写操作受 email-routing-rule.write 门控；地址受 email-routing-address.* 门控。
//

import SwiftUI

struct EmailRoutingView: View {

    let zoneId: String
    let zoneName: String
    let session: SessionStore

    @Environment(AuthManager.self) private var auth

    @State private var vm: EmailRoutingViewModel?
    @State private var editingRule: EmailRoutingRule?
    @State private var showNewRule = false
    @State private var showAddAddress = false

    private var canEditRules: Bool { auth.hasScope("email-routing-rule.write") }
    private var canEditAddresses: Bool { auth.hasScope("email-routing-address.write") }

    var body: some View {
        Group {
            if let vm {
                content(vm)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background { SkyBackground() }
        .navigationTitle("Email Routing")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await session.ensureAccounts()
            guard vm == nil else { return }
            let accountId = auth.hasScope("email-routing-address.read") ? session.selectedAccount?.id : nil
            let model = EmailRoutingViewModel(
                service: session.emailRoutingService, zoneId: zoneId, accountId: accountId
            )
            vm = model
            await model.load()
        }
    }

    @ViewBuilder
    private func content(_ vm: EmailRoutingViewModel) -> some View {
        List {
            // ── 状态 ──
            Section {
                Toggle(isOn: Binding(
                    get: { vm.settings?.isEnabled ?? false },
                    set: { on in Task { await vm.setEnabled(on) } }
                )) {
                    HStack(spacing: 12) {
                        TintIcon(systemImage: "envelope", color: .ocOrange)
                        Text("启用 Email Routing")
                    }
                }
                .disabled(!canEditRules || vm.isMutating)

                if let status = vm.settings?.status {
                    HStack {
                        Text("状态")
                        Spacer()
                        Text(statusLabel(status))
                            .foregroundStyle(.secondary)
                    }
                }
            } footer: {
                Text("关闭后该域名停止接收与转发邮件。转发到的目的地址需先验证。")
            }
            .glassRow()

            // ── 路由规则 ──
            Section {
                if vm.rules.isEmpty {
                    Text("还没有路由规则。")
                        .foregroundStyle(.secondary)
                        .font(.callout)
                } else {
                    ForEach(vm.rules) { rule in
                        ruleRow(vm, rule)
                    }
                }
                if canEditRules {
                    Button {
                        showNewRule = true
                    } label: {
                        Label("新建转发规则", systemImage: "plus")
                            .foregroundStyle(Color.ocOrangeText)
                    }
                    .disabled(vm.verifiedAddresses.isEmpty)
                }
            } header: {
                Text("路由规则")
            } footer: {
                if canEditRules && vm.verifiedAddresses.isEmpty {
                    Text("新建转发规则前，请先在下方添加并验证至少一个目的地址。")
                } else {
                    Text("把发往某个地址的邮件转发到已验证的目的地址。")
                }
            }
            .glassRow()

            // ── 目的地址 ──
            if vm.hasAddressScope {
                Section {
                    if vm.addresses.isEmpty {
                        Text("还没有目的地址。")
                            .foregroundStyle(.secondary)
                            .font(.callout)
                    } else {
                        ForEach(vm.addresses) { addr in
                            addressRow(vm, addr)
                        }
                    }
                    if canEditAddresses {
                        Button {
                            showAddAddress = true
                        } label: {
                            Label("新增目的地址", systemImage: "plus")
                                .foregroundStyle(Color.ocOrangeText)
                        }
                    }
                } header: {
                    Text("目的地址")
                } footer: {
                    Text("目的地址在账号内共享。新增后需在该邮箱点验证链接才能用于转发。")
                }
                .glassRow()
            }

            // 抑制列表：规则决定往哪转，抑制决定不往哪转——
            // 「规则配了却收不到」多半就是命中了这里，所以要能看见
            if vm.suppressionsAvailable {
                Section {
                    if vm.suppressions.isEmpty {
                        Text("没有被抑制的地址。")
                            .font(.footnote).foregroundStyle(.secondary)
                    } else {
                        ForEach(vm.suppressions) { item in
                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.email ?? item.id)
                                    .font(.callout)
                                    .lineLimit(1)
                                    .truncationMode(.middle)
                                HStack(spacing: 6) {
                                    Text(item.reasonText)
                                    if let expires = WorkerScript.parseDate(item.expiresAt) {
                                        Text(expires, format: .dateTime.year().month().day())
                                    } else {
                                        Text("永久")
                                    }
                                }
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            }
                            .swipeActions(edge: .trailing) {
                                if canEditRules {
                                    Button(role: .destructive) {
                                        Task { await vm.removeSuppression(item) }
                                    } label: {
                                        Label("解除", systemImage: "arrow.uturn.backward")
                                    }
                                }
                            }
                        }
                    }
                } header: {
                    Text("已抑制地址")
                } footer: {
                    Text("这些地址退信或投诉过，Cloudflare 已停止向其转发。左滑可解除。")
                }
                .glassRow()
            }

            if let error = vm.error {
                Section {
                    Text(error).font(.footnote).foregroundStyle(.red)
                }
                .glassRow()
            }
        }
        .daybreakList()
        .refreshable { await vm.load() }
        .overlay {
            if vm.isLoading && vm.settings == nil {
                ProgressView()
            }
        }
        .sheet(isPresented: $showNewRule) {
            RuleEditorSheet(zoneName: zoneName, rule: nil, verified: vm.verifiedAddresses) { match, dest, name in
                await vm.createForwardRule(matchAddress: match, destination: dest, name: name)
            }
        }
        .sheet(item: $editingRule) { rule in
            RuleEditorSheet(zoneName: zoneName, rule: rule, verified: vm.verifiedAddresses) { match, dest, name in
                await vm.updateForwardRule(rule, matchAddress: match, destination: dest, name: name)
            }
        }
        .sheet(isPresented: $showAddAddress) {
            AddAddressSheet { email in
                await vm.createAddress(email)
            }
        }
    }

    // MARK: - 行

    @ViewBuilder
    private func ruleRow(_ vm: EmailRoutingViewModel, _ rule: EmailRoutingRule) -> some View {
        HStack(spacing: 12) {
            TintIcon(systemImage: rule.isCatchAll ? "tray.full" : "arrow.turn.down.right",
                     color: rule.isEnabled ? .ocOrange : .gray)
            VStack(alignment: .leading, spacing: 3) {
                Text(rule.isCatchAll ? String(localized: "全部邮件（catch-all）") : (rule.matchAddress ?? rule.name ?? "—"))
                    .font(.callout)
                    .lineLimit(1)
                Text(rule.actionSummary)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            if canEditRules {
                Toggle("", isOn: Binding(
                    get: { rule.isEnabled },
                    set: { on in Task { await vm.setRuleEnabled(rule, enabled: on) } }
                ))
                .labelsHidden()
            } else if !rule.isEnabled {
                Text("已停用").font(.caption2).foregroundStyle(.tertiary)
            }
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
        .onTapGesture {
            // 仅转发型、非 catch-all 规则可编辑
            if canEditRules, !rule.isCatchAll, rule.actions.first?.type == "forward" {
                editingRule = rule
            }
        }
        .swipeActions(edge: .trailing) {
            if canEditRules {
                Button(role: .destructive) {
                    Task { await vm.deleteRule(rule) }
                } label: {
                    Label("删除", systemImage: "trash")
                }
            }
        }
    }

    @ViewBuilder
    private func addressRow(_ vm: EmailRoutingViewModel, _ addr: EmailDestinationAddress) -> some View {
        HStack(spacing: 12) {
            TintIcon(systemImage: addr.isVerified ? "checkmark.seal" : "clock",
                     color: addr.isVerified ? .green : .orange)
            Text(addr.email)
                .font(.callout)
                .lineLimit(1)
            Spacer(minLength: 8)
            if !addr.isVerified {
                Text("待验证").font(.caption2).foregroundStyle(.orange)
            }
        }
        .padding(.vertical, 2)
        .swipeActions(edge: .trailing) {
            if canEditAddresses {
                Button(role: .destructive) {
                    Task { await vm.deleteAddress(addr) }
                } label: {
                    Label("删除", systemImage: "trash")
                }
            }
        }
    }

    private func statusLabel(_ status: String) -> String {
        switch status {
        case "ready":         String(localized: "已就绪")
        case "unconfigured":  String(localized: "未配置")
        case "misconfigured": String(localized: "配置有误")
        default:              status
        }
    }
}

// MARK: - 规则编辑（新建 / 改转发目标）

private struct RuleEditorSheet: View {

    let zoneName: String
    let rule: EmailRoutingRule?
    let verified: [EmailDestinationAddress]
    let onSave: (_ match: String, _ destination: String, _ name: String?) async -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var match = ""
    @State private var destination = ""
    @State private var isSaving = false

    private var isValid: Bool {
        match.contains("@") && !destination.isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("收件地址，如 hello@\(zoneName)", text: $match)
                        .keyboardType(.emailAddress)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                } header: {
                    Text("当邮件发往")
                }

                Section {
                    Picker("转发到", selection: $destination) {
                        Text("请选择").tag("")
                        ForEach(verified) { addr in
                            Text(addr.email).tag(addr.email)
                        }
                    }
                } header: {
                    Text("转发到（已验证地址）")
                }
            }
            .scrollContentBackground(.hidden)
            .background { SkyBackground() }
            .navigationTitle(rule == nil ? String(localized: "新建转发规则") : String(localized: "编辑规则"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }.disabled(isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isSaving {
                        ProgressView()
                    } else {
                        Button("保存") {
                            let m = match, d = destination
                            Task {
                                isSaving = true
                                await onSave(m, d, nil)
                                isSaving = false
                                dismiss()
                            }
                        }
                        .disabled(!isValid)
                    }
                }
            }
            .onAppear {
                if let rule {
                    match = rule.matchAddress ?? ""
                    destination = rule.actions.first?.value?.first ?? ""
                }
            }
        }
    }
}

// MARK: - 新增目的地址

private struct AddAddressSheet: View {

    let onSave: (_ email: String) async -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var email = ""
    @State private var isSaving = false

    private var isValid: Bool { email.contains("@") && email.contains(".") }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("destination@example.com", text: $email)
                        .keyboardType(.emailAddress)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                } footer: {
                    Text("提交后 Cloudflare 会向该邮箱发送验证邮件，点击其中链接后方可用于转发。")
                }
            }
            .scrollContentBackground(.hidden)
            .background { SkyBackground() }
            .navigationTitle("新增目的地址")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }.disabled(isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if isSaving {
                        ProgressView()
                    } else {
                        Button("发送验证") {
                            let e = email
                            Task {
                                isSaving = true
                                await onSave(e)
                                isSaving = false
                                dismiss()
                            }
                        }
                        .disabled(!isValid)
                    }
                }
            }
        }
    }
}
