//
//  GatewayRuleEditorView.swift
//  Orange Cloud
//
//  新建 / 编辑 Gateway 策略（Sheet）。核心是 traffic 表达式编辑器：
//  原生 Wirefilter 文本框 + 按类型给出的「选择器调色板」（点一下插入正确语法的片段），
//  既灵活又少出错；保存时 Cloudflare 会校验并规范化表达式。
//  入口已按 teams.write 门控。
//

import SwiftUI

struct GatewayRuleEditorView: View {

    enum Mode {
        case create
        case edit(GatewayRule)
    }

    let mode: Mode
    let viewModel: GatewayRulesViewModel

    @Environment(\.dismiss) private var dismiss

    @State private var type: GatewayFilterType = .dns
    @State private var name = ""
    @State private var ruleDescription = ""
    @State private var enabled = true
    @State private var action = "block"
    @State private var precedenceText = ""
    @State private var traffic = ""
    @State private var identity = ""
    @State private var devicePosture = ""
    @State private var showAdvanced = false
    @FocusState private var trafficFocused: Bool

    private var isEdit: Bool { if case .edit = mode { return true }; return false }

    private var editingRule: GatewayRule? {
        if case .edit(let rule) = mode { return rule }
        return nil
    }

    private var trimmedName: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var trimmedTraffic: String { traffic.trimmingCharacters(in: .whitespacesAndNewlines) }

    private var canSave: Bool {
        !trimmedName.isEmpty && !trimmedTraffic.isEmpty && !viewModel.isSaving
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("类型", selection: $type) {
                        ForEach(GatewayFilterType.allCases) { Text($0.label).tag($0) }
                    }
                    .pickerStyle(.segmented)
                    .disabled(isEdit || viewModel.isSaving)   // 类型决定字段集，编辑时不改
                    Picker("动作", selection: $action) {
                        ForEach(type.actions, id: \.value) { Text($0.label).tag($0.value) }
                    }
                    .disabled(viewModel.isSaving)
                } footer: {
                    if isEdit { Text("策略类型创建后不可更改。") }
                }

                Section {
                    TextField("名称", text: $name).disabled(viewModel.isSaving)
                    TextField("描述（可选）", text: $ruleDescription, axis: .vertical)
                        .lineLimit(1...3).disabled(viewModel.isSaving)
                    Toggle("启用", isOn: $enabled).disabled(viewModel.isSaving)
                } header: {
                    Text("基本")
                }

                expressionSection

                Section {
                    TextField("优先级（可选，数字越小越靠前）", text: $precedenceText)
                        .keyboardType(.numberPad)
                        .disabled(viewModel.isSaving)
                } header: {
                    Text("优先级")
                }

                advancedSection

                if let error = viewModel.error {
                    Section { Text(error).font(.footnote).foregroundStyle(.red) }
                }
            }
            .navigationTitle(isEdit ? String(localized: "编辑策略") : String(localized: "新建策略"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }.disabled(viewModel.isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task { await save() }
                    } label: {
                        if viewModel.isSaving { ProgressView() } else { Text("保存").fontWeight(.semibold) }
                    }
                    .disabled(!canSave)
                }
            }
            .interactiveDismissDisabled(viewModel.isSaving)
            .onAppear(perform: prefill)
            .onChange(of: type) { _, newType in
                // 切换类型后，若当前动作不在新类型动作集中，回落到首个合法动作
                if !newType.actions.contains(where: { $0.value == action }) {
                    action = newType.actions.first?.value ?? newType.defaultAction
                }
            }
        }
    }

    // MARK: - 表达式编辑器

    private var expressionSection: some View {
        Section {
            TextEditor(text: $traffic)
                .font(.callout.monospaced())
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .frame(minHeight: 110)
                .focused($trafficFocused)
                .disabled(viewModel.isSaving)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(GatewayExpressionCatalog.selectors(for: type)) { selector in
                        Button {
                            insert(selector.snippet)
                        } label: {
                            Text(selector.label)
                                .font(.caption.weight(.medium))
                                .padding(.horizontal, 10)
                                .padding(.vertical, 6)
                                .background(Color.ocOrange.opacity(0.14), in: Capsule())
                                .foregroundStyle(Color.ocOrangeText)
                        }
                        .buttonStyle(.plain)
                        .disabled(viewModel.isSaving)
                    }
                }
                .padding(.vertical, 2)
            }
            .listRowInsets(EdgeInsets(top: 6, leading: 16, bottom: 6, trailing: 0))
        } header: {
            Text("流量匹配表达式")
        } footer: {
            Text(GatewayExpressionCatalog.syntaxHint)
        }
    }

    // MARK: - 高级（可选身份 / 设备态势表达式）

    @ViewBuilder
    private var advancedSection: some View {
        Section {
            DisclosureGroup(isExpanded: $showAdvanced) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("身份匹配（可选）").font(.caption).foregroundStyle(.secondary)
                    TextField("identity.email == \"user@example.com\"", text: $identity, axis: .vertical)
                        .font(.callout.monospaced()).lineLimit(1...4).disabled(viewModel.isSaving)
                }
                VStack(alignment: .leading, spacing: 4) {
                    Text("设备态势匹配（可选）").font(.caption).foregroundStyle(.secondary)
                    TextField("any(device_posture.checks.passed[*] in {\"...\"})", text: $devicePosture, axis: .vertical)
                        .font(.callout.monospaced()).lineLimit(1...4).disabled(viewModel.isSaving)
                }
            } label: {
                Text("高级条件")
            }
        }
    }

    // MARK: - 逻辑

    private func prefill() {
        viewModel.error = nil
        guard let rule = editingRule else { return }
        type = rule.filterType
        name = rule.name ?? ""
        ruleDescription = rule.description ?? ""
        enabled = rule.isEnabled
        action = rule.action ?? rule.filterType.defaultAction
        precedenceText = rule.precedence.map(String.init) ?? ""
        traffic = rule.traffic ?? ""
        identity = rule.identity ?? ""
        devicePosture = rule.devicePosture ?? ""
        if !(rule.identity ?? "").isEmpty || !(rule.devicePosture ?? "").isEmpty {
            showAdvanced = true
        }
    }

    /// 插入片段：表达式为空则直接放入，否则用 and 连接
    private func insert(_ snippet: String) {
        if trimmedTraffic.isEmpty {
            traffic = snippet
        } else {
            traffic = traffic.trimmingCharacters(in: .whitespacesAndNewlines) + " and " + snippet
        }
    }

    private func save() async {
        guard canSave else { return }
        trafficFocused = false
        let precedence = Int(precedenceText.trimmingCharacters(in: .whitespaces))
        let input = GatewayRuleInput(
            name: trimmedName,
            description: ruleDescription.isEmpty ? nil : ruleDescription,
            action: action,
            enabled: enabled,
            filters: [type.rawValue],
            traffic: trimmedTraffic,
            identity: identity.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : identity,
            devicePosture: devicePosture.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : devicePosture,
            precedence: precedence,
            ruleSettings: editingRule?.ruleSettings   // 编辑时保留原 rule_settings
        )
        let ok: Bool
        if let rule = editingRule {
            ok = await viewModel.update(ruleId: rule.id, input)
        } else {
            ok = await viewModel.create(input)
        }
        if ok { dismiss() }
    }
}
