//
//  RateLimitRulesView.swift
//  Orange Cloud
//
//  Rate Limiting：限速规则列表 + 启停 / 删除 / 新建·编辑。
//  写操作受 zone-waf.write 门控（与 WAF 同权限组）。
//

import SwiftUI

struct RateLimitRulesView: View {

    let zoneId: String
    let zoneName: String
    let session: SessionStore

    @Environment(AuthManager.self) private var auth

    @State private var vm: RateLimitViewModel?
    @State private var editingRule: RateLimitRule?
    @State private var showNewRule = false

    private var canEdit: Bool { auth.hasScope("zone-waf.write") }

    var body: some View {
        Group {
            if let vm {
                content(vm)
            } else {
                ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background { SkyBackground() }
        .navigationTitle("Rate Limiting")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            guard vm == nil else { return }
            let model = RateLimitViewModel(service: session.rateLimitService, zoneId: zoneId)
            vm = model
            await model.load()
        }
    }

    @ViewBuilder
    private func content(_ vm: RateLimitViewModel) -> some View {
        if vm.isLoading && !vm.loaded {
            ProgressView().frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if vm.rules.isEmpty {
            ContentUnavailableView {
                Label("还没有限速规则", systemImage: "gauge.with.dots.needle.bottom.50percent")
            } description: {
                Text(vm.error ?? String(localized: "限速规则可在单位时间内限制来自同一访客的请求次数。"))
            } actions: {
                if canEdit {
                    Button {
                        showNewRule = true
                    } label: {
                        Label("新建限速规则", systemImage: "plus")
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Color.ocOrangePressed)
                }
            }
            .sheet(isPresented: $showNewRule) { editorSheet(vm, rule: nil) }
        } else {
            List {
                Section {
                    ForEach(vm.rules) { rule in
                        ruleRow(vm, rule)
                    }
                    if canEdit {
                        Button {
                            showNewRule = true
                        } label: {
                            Label("新建限速规则", systemImage: "plus")
                                .foregroundStyle(Color.ocOrangeText)
                        }
                    }
                } footer: {
                    Text("在 \(zoneName) 上，按访客 IP 在时间窗内限制请求次数。规则自上而下匹配。")
                }
                .glassRow()

                if let error = vm.error {
                    Section { Text(error).font(.footnote).foregroundStyle(.red) }
                        .glassRow()
                }
            }
            .daybreakList()
            .refreshable { await vm.load() }
            .sheet(isPresented: $showNewRule) { editorSheet(vm, rule: nil) }
            .sheet(item: $editingRule) { rule in editorSheet(vm, rule: rule) }
        }
    }

    @ViewBuilder
    private func ruleRow(_ vm: RateLimitViewModel, _ rule: RateLimitRule) -> some View {
        HStack(spacing: 12) {
            TintIcon(systemImage: "gauge.with.dots.needle.bottom.50percent",
                     color: rule.isEnabled ? .ocOrange : .gray)
            VStack(alignment: .leading, spacing: 3) {
                Text(rule.description?.nilIfBlank ?? rule.expression?.nilIfBlank ?? String(localized: "限速规则"))
                    .font(.callout)
                    .lineLimit(1)
                Text(thresholdSummary(rule))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            if canEdit {
                Toggle("", isOn: Binding(
                    get: { rule.isEnabled },
                    set: { on in Task { await vm.setEnabled(rule, enabled: on) } }
                ))
                .labelsHidden()
            }
        }
        .padding(.vertical, 2)
        .contentShape(Rectangle())
        .onTapGesture { if canEdit { editingRule = rule } }
        .swipeActions(edge: .trailing) {
            if canEdit {
                Button(role: .destructive) {
                    Task { await vm.delete(rule) }
                } label: { Label("删除", systemImage: "trash") }
            }
        }
    }

    private func thresholdSummary(_ rule: RateLimitRule) -> String {
        let reqs = rule.ratelimit?.requestsPerPeriod ?? 0
        let period = rule.ratelimit?.period ?? 0
        let periodLabel = RateLimitPeriod(rawValue: period)?.label ?? String(localized: "\(period) 秒")
        let action = RateLimitAction(rawValue: rule.action ?? "")?.label ?? (rule.action ?? "")
        return String(localized: "\(reqs) 次 / \(periodLabel) → \(action)")
    }

    private func editorSheet(_ vm: RateLimitViewModel, rule: RateLimitRule?) -> some View {
        RateLimitEditorSheet(rule: rule) { create in
            await vm.save(existing: rule, rule: create)
        }
    }
}

// MARK: - 编辑器

private struct RateLimitEditorSheet: View {

    let rule: RateLimitRule?
    let onSave: (_ create: RateLimitRuleCreate) async -> Void

    @Environment(\.dismiss) private var dismiss

    @State private var description = ""
    @State private var expression = "(http.request.uri.path contains \"/\")"
    @State private var requestsText = "100"
    @State private var period: RateLimitPeriod = .s60
    @State private var action: RateLimitAction = .block
    @State private var timeout: RateLimitPeriod = .s600
    @State private var isSaving = false

    private var requests: Int? { Int(requestsText) }

    private var isValid: Bool {
        !expression.trimmingCharacters(in: .whitespaces).isEmpty &&
        (requests ?? 0) >= 1
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField(String(localized: "名称（可选）"), text: $description)
                } header: {
                    Text("名称")
                }

                Section {
                    TextField("(http.request.uri.path contains \"/login\")", text: $expression, axis: .vertical)
                        .font(.callout.monospaced())
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .lineLimit(2...5)
                } header: {
                    Text("匹配表达式")
                } footer: {
                    Text("命中此表达式的请求计入限速统计。例如按路径限制登录接口。")
                }

                Section {
                    HStack {
                        Text("请求次数")
                        Spacer()
                        TextField("100", text: $requestsText)
                            .keyboardType(.numberPad)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 100)
                    }
                    Picker("时间窗", selection: $period) {
                        ForEach(RateLimitPeriod.allCases) { p in Text(p.label).tag(p) }
                    }
                } header: {
                    Text("阈值")
                } footer: {
                    Text("同一 IP 在所选时间窗内超过该次数即触发动作。")
                }

                Section {
                    Picker("动作", selection: $action) {
                        ForEach(RateLimitAction.allCases) { a in Text(a.label).tag(a) }
                    }
                    Picker("封禁时长", selection: $timeout) {
                        ForEach(RateLimitPeriod.allCases) { p in Text(p.label).tag(p) }
                    }
                } header: {
                    Text("触发后")
                }
            }
            .scrollContentBackground(.hidden)
            .background { SkyBackground() }
            .navigationTitle(rule == nil ? String(localized: "新建限速规则") : String(localized: "编辑限速规则"))
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
                            guard let reqs = requests else { return }
                            let create = RateLimitRuleCreate.make(
                                expression: expression,
                                requests: reqs,
                                period: period.rawValue,
                                action: action.rawValue,
                                mitigationTimeout: timeout.rawValue,
                                description: description.nilIfBlank,
                                enabled: rule?.isEnabled ?? true
                            )
                            Task {
                                isSaving = true
                                await onSave(create)
                                isSaving = false
                                dismiss()
                            }
                        }
                        .disabled(!isValid)
                    }
                }
            }
            .onAppear { prefill() }
        }
    }

    private func prefill() {
        guard let rule else { return }
        description = rule.description ?? ""
        expression = rule.expression ?? expression
        if let r = rule.ratelimit?.requestsPerPeriod { requestsText = String(r) }
        if let p = rule.ratelimit?.period, let match = RateLimitPeriod(rawValue: p) { period = match }
        if let a = rule.action, let match = RateLimitAction(rawValue: a) { action = match }
        if let t = rule.ratelimit?.mitigationTimeout, let match = RateLimitPeriod(rawValue: t) { timeout = match }
    }
}

private extension String {
    var nilIfBlank: String? {
        let t = trimmingCharacters(in: .whitespacesAndNewlines)
        return t.isEmpty ? nil : t
    }
}
