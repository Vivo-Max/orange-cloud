//
//  AccessRuleEditorView.swift
//  Orange Cloud
//
//  IP 访问规则 新建 / 编辑。新建可选匹配对象（IP/IPv6/IP 段/ASN/国家），
//  编辑仅改动作与备注（匹配对象不可变）。
//

import SwiftUI

struct AccessRuleEditorView: View {

    let existing: FirewallAccessRule?
    let viewModel: ZoneAccessRulesViewModel

    @Environment(\.dismiss) private var dismiss

    @State private var mode: AccessRuleMode
    @State private var target: AccessRuleTarget
    @State private var value: String
    @State private var notes: String

    init(existing: FirewallAccessRule?, viewModel: ZoneAccessRulesViewModel) {
        self.existing = existing
        self.viewModel = viewModel
        _mode = State(initialValue: AccessRuleMode(rawValue: existing?.mode ?? "block") ?? .block)
        _target = State(initialValue: AccessRuleTarget(rawValue: existing?.configuration?.target ?? "ip") ?? .ip)
        _value = State(initialValue: existing?.configuration?.value ?? "")
        _notes = State(initialValue: existing?.notes ?? "")
    }

    private var isEditing: Bool { existing != nil }

    private var canSave: Bool {
        guard !viewModel.isSaving else { return false }
        return isEditing || !value.trimmingCharacters(in: .whitespaces).isEmpty
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("匹配对象") {
                    Picker("类型", selection: $target) {
                        ForEach(AccessRuleTarget.allCases) { Text($0.label).tag($0) }
                    }
                    .disabled(isEditing)
                    TextField(target.placeholder, text: $value)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .disabled(isEditing)
                }

                Section("动作") {
                    Picker("动作", selection: $mode) {
                        ForEach(AccessRuleMode.allCases) { Text($0.label).tag($0) }
                    }
                }

                Section("备注") {
                    TextField("备注（可选）", text: $notes, axis: .vertical).lineLimit(1...3)
                }

                if isEditing {
                    Section {
                        Text("匹配对象创建后不可修改，如需更改请删除后重建。")
                            .font(.footnote).foregroundStyle(.secondary)
                    }
                }

                if let error = viewModel.error {
                    Section { Text(error).font(.footnote).foregroundStyle(.red) }
                }
            }
            .navigationTitle(isEditing ? Text("编辑规则") : Text("新建规则"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task { await save() }
                    } label: {
                        if viewModel.isSaving {
                            ProgressView()
                        } else {
                            Text("保存").fontWeight(.semibold)
                        }
                    }
                    .disabled(!canSave)
                }
            }
            .interactiveDismissDisabled(viewModel.isSaving)
            .onDisappear { viewModel.error = nil }
        }
    }

    private func save() async {
        viewModel.error = nil
        let trimmedNotes = notes.trimmingCharacters(in: .whitespaces)
        let n = trimmedNotes.isEmpty ? nil : trimmedNotes
        let ok: Bool
        if let existing {
            ok = await viewModel.update(ruleId: existing.id, mode: mode.rawValue, notes: n)
        } else {
            ok = await viewModel.create(
                mode: mode.rawValue, target: target,
                value: value.trimmingCharacters(in: .whitespaces), notes: n
            )
        }
        if ok { dismiss() }
    }
}
