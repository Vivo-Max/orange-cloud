//
//  AccessAppEditorView.swift
//  Orange Cloud
//
//  新建 / 编辑 Access 自托管应用（Sheet）。应用字段 + 一条可复用策略的规则编辑器
//  （决策 + include 规则行：所有人 / 邮箱 / 邮箱域名 / IP / 国家）。
//  编辑时若策略含未建模规则或 exclude/require，则规则只读保护、仅改应用字段。
//  入口已按 access.write 门控。
//

import SwiftUI

struct AccessAppEditorView: View {

    enum Mode: Equatable {
        case create
        case edit(appId: String)
    }

    let mode: Mode
    let viewModel: AccessAppsViewModel
    let onSuccess: () -> Void

    @Environment(\.dismiss) private var dismiss

    private struct RuleRow: Identifiable {
        let id = UUID()
        var kind: AccessRuleKind
        var value: String
    }

    private struct HostnameRow: Identifiable {
        let id = UUID()
        var value: String
    }

    @State private var name = ""
    @State private var hostnames: [HostnameRow] = [HostnameRow(value: "")]
    /// 原应用的非 public 目标（私有网络目标），编辑公共主机名时原样保留回写，避免误删
    @State private var preservedDestinations: [AccessDestination] = []
    @State private var sessionDuration = AccessSessionDuration.h24.rawValue
    @State private var decision = AccessDecision.allow
    @State private var rules: [RuleRow] = [RuleRow(kind: .everyone, value: "")]

    // 编辑态
    @State private var loadingDetail = false
    @State private var prefilled = false
    @State private var policyIds: [String] = []
    @State private var editablePolicyId: String?       // 可改规则的策略 id；nil = 规则只读
    @State private var policyTooComplex = false

    private var isCreate: Bool { mode == .create }

    private var trimmedName: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }
    /// 去空白、去空值、保序去重后的全部公共主机名
    private var trimmedHostnames: [String] {
        var seen = Set<String>()
        return hostnames
            .map { $0.value.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty && seen.insert($0).inserted }
    }

    /// 规则有效：所有人无需值；其余需非空值
    private var rulesValid: Bool {
        !rules.isEmpty && rules.allSatisfy { $0.kind == .everyone || !$0.value.trimmingCharacters(in: .whitespaces).isEmpty }
    }

    private var canEditRules: Bool { isCreate || editablePolicyId != nil }

    private var canSave: Bool {
        guard !trimmedName.isEmpty, !trimmedHostnames.isEmpty, !viewModel.isSaving else { return false }
        return canEditRules ? rulesValid : true
    }

    var body: some View {
        NavigationStack {
            Form {
                if loadingDetail {
                    Section { HStack { ProgressView(); Text("读取应用…").foregroundStyle(.secondary) } }
                } else {
                    appSection
                    policySection
                    if let error = viewModel.error {
                        Section { Text(error).font(.footnote).foregroundStyle(.red) }
                    }
                }
            }
            .navigationTitle(isCreate ? String(localized: "新建 Access 应用") : String(localized: "编辑 Access 应用"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }.disabled(viewModel.isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task { await save() }
                    } label: {
                        if viewModel.isSaving { ProgressView() } else { Text(isCreate ? String(localized: "创建") : String(localized: "保存")).fontWeight(.semibold) }
                    }
                    .disabled(!canSave || loadingDetail)
                }
            }
            .interactiveDismissDisabled(viewModel.isSaving)
            .task { await prefillIfNeeded() }
        }
    }

    // MARK: - 应用字段

    private var appSection: some View {
        Group {
            Section {
                TextField("名称", text: $name).disabled(viewModel.isSaving)
            } header: {
                Text("应用")
            }
            Section {
                ForEach($hostnames) { $row in
                    TextField("应用域名（如 app.example.com）", text: $row.value)
                        .textInputAutocapitalization(.never).autocorrectionDisabled()
                        .keyboardType(.URL).disabled(viewModel.isSaving)
                }
                .onDelete { hostnames.remove(atOffsets: $0) }
                Button {
                    hostnames.append(HostnameRow(value: ""))
                } label: {
                    Label("添加主机名", systemImage: "plus")
                }
                .disabled(viewModel.isSaving)
            } header: {
                Text("公共主机名")
            } footer: {
                Text("自托管应用可保护多个公共主机名（及路径），第一个为主域名。左滑删除。")
            }
            Section {
                Picker("会话时长", selection: $sessionDuration) {
                    ForEach(AccessSessionDuration.allCases) { Text($0.label).tag($0.rawValue) }
                    if !AccessSessionDuration.allCases.map(\.rawValue).contains(sessionDuration) {
                        Text(sessionDuration).tag(sessionDuration)   // 保留非预设原值
                    }
                }
                .disabled(viewModel.isSaving)
            }
        }
    }

    // MARK: - 策略规则

    @ViewBuilder
    private var policySection: some View {
        Section {
            Picker("决策", selection: $decision) {
                ForEach(AccessDecision.allCases) { Text($0.label).tag($0) }
            }
            .disabled(!canEditRules || viewModel.isSaving)
        } header: {
            Text("策略决策")
        } footer: {
            Text("允许：满足条件可访问；拒绝：满足即禁止；绕过：对满足条件的流量跳过 Access 校验。")
        }

        Section {
            if !canEditRules {
                Label("该应用的策略较复杂（含未支持的规则或 exclude/require），规则在此只读。可改上方应用字段，规则请到 Cloudflare Dashboard 编辑。", systemImage: "lock")
                    .font(.caption).foregroundStyle(.secondary)
            } else {
                ForEach($rules) { $rule in
                    VStack(alignment: .leading, spacing: 6) {
                        Picker("类型", selection: $rule.kind) {
                            ForEach(AccessRuleKind.allCases) { Text($0.label).tag($0) }
                        }
                        if rule.kind.needsValue {
                            TextField(rule.kind.placeholder, text: $rule.value)
                                .textInputAutocapitalization(.never).autocorrectionDisabled()
                                .font(.callout.monospaced())
                        }
                    }
                    .disabled(viewModel.isSaving)
                }
                .onDelete { rules.remove(atOffsets: $0) }

                Button {
                    rules.append(RuleRow(kind: .email, value: ""))
                } label: {
                    Label("添加规则", systemImage: "plus")
                }
                .disabled(viewModel.isSaving)
            }
        } header: {
            Text("包含规则（满足任一即匹配）")
        } footer: {
            if canEditRules {
                Text("「所有人」表示任何已认证用户。国家用两位代码（如 US、CN）。")
            }
        }
    }

    // MARK: - 逻辑

    private func prefillIfNeeded() async {
        guard case .edit(let appId) = mode, !prefilled else { return }
        loadingDetail = true
        defer { loadingDetail = false }
        guard let app = await viewModel.detail(appId: appId) else { prefilled = true; return }
        name = app.name ?? ""
        let hosts = app.publicHostnames
        hostnames = hosts.isEmpty ? [HostnameRow(value: "")] : hosts.map { HostnameRow(value: $0) }
        preservedDestinations = app.nonPublicDestinations
        if let sd = app.sessionDuration, !sd.isEmpty { sessionDuration = sd }
        policyIds = (app.policies ?? []).compactMap(\.id)

        if let policy = app.policies?.first, app.policies?.count == 1, policy.isSimplyEditable, let pid = policy.id {
            editablePolicyId = pid
            decision = AccessDecision(rawValue: policy.decision ?? "allow") ?? .allow
            rules = (policy.include ?? []).compactMap { rule in
                AccessRuleKind(rawValue: rule.kind).map { RuleRow(kind: $0, value: rule.value) }
            }
            if rules.isEmpty { rules = [RuleRow(kind: .everyone, value: "")] }
        } else {
            policyTooComplex = !(app.policies?.isEmpty ?? true)
            editablePolicyId = nil
        }
        prefilled = true
    }

    private func buildInclude() -> [AccessRule] {
        rules.map { AccessRule(kind: $0.kind.rawValue, value: $0.kind == .everyone ? "" : $0.value.trimmingCharacters(in: .whitespaces)) }
    }

    private func save() async {
        guard canSave else { return }
        viewModel.error = nil
        let ok: Bool
        switch mode {
        case .create:
            ok = await viewModel.create(
                name: trimmedName, hostnames: trimmedHostnames,
                sessionDuration: sessionDuration, decision: decision.rawValue, include: buildInclude()
            )
        case .edit(let appId):
            let patch: (id: String, decision: String, include: [AccessRule])? =
                editablePolicyId.map { ($0, decision.rawValue, buildInclude()) }
            ok = await viewModel.update(
                appId: appId, policyIds: policyIds,
                name: trimmedName, hostnames: trimmedHostnames,
                preservedDestinations: preservedDestinations,
                sessionDuration: sessionDuration, policyPatch: patch
            )
        }
        if ok { onSuccess(); dismiss() }
    }
}
