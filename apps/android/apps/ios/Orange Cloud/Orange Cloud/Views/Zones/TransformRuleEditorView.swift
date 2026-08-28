//
//  TransformRuleEditorView.swift
//  Orange Cloud
//
//  Transform Rule 新建 / 编辑表单（按 phase 切换字段）：
//  URL 重写 → 路径 / 查询串静态值；请求/响应头 → 头操作列表（设置/追加/删除）。
//

import SwiftUI

struct TransformRuleEditorView: View {

    let phase: TransformPhase
    let existing: TransformRule?
    let viewModel: ZoneTransformRulesViewModel

    @Environment(\.dismiss) private var dismiss

    @State private var ruleDescription: String
    @State private var expression: String
    @State private var enabled: Bool
    @State private var pathValue: String
    @State private var queryValue: String
    @State private var headerRows: [HeaderRow]

    init(phase: TransformPhase, existing: TransformRule?, viewModel: ZoneTransformRulesViewModel) {
        self.phase = phase
        self.existing = existing
        self.viewModel = viewModel
        _ruleDescription = State(initialValue: existing?.description ?? "")
        _expression = State(initialValue: existing?.expression ?? "")
        _enabled = State(initialValue: existing?.enabled ?? true)
        _pathValue = State(initialValue: existing?.actionParameters?.uri?.path?.value ?? "")
        _queryValue = State(initialValue: existing?.actionParameters?.uri?.query?.value ?? "")
        let rows = (existing?.actionParameters?.headers ?? [:])
            .map { name, h in HeaderRow(name: name, operation: HeaderOperation(rawValue: h.operation) ?? .set, value: h.value ?? "") }
            .sorted { $0.name < $1.name }
        _headerRows = State(initialValue: rows.isEmpty ? [HeaderRow()] : rows)
    }

    private var isEditing: Bool { existing != nil }

    private var canSave: Bool {
        guard !expression.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty, !viewModel.isSaving else { return false }
        if phase.isURLRewrite {
            return !pathValue.trimmingCharacters(in: .whitespaces).isEmpty
                || !queryValue.trimmingCharacters(in: .whitespaces).isEmpty
        }
        return headerRows.contains { row in
            !row.name.trimmingCharacters(in: .whitespaces).isEmpty
                && (row.operation == .remove || !row.value.trimmingCharacters(in: .whitespaces).isEmpty)
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("规则") {
                    TextField("规则说明（可选）", text: $ruleDescription)
                    Toggle("启用", isOn: $enabled)
                }

                Section {
                    TextEditor(text: $expression)
                        .font(.callout.monospaced())
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .frame(minHeight: 90)
                } header: {
                    Text("匹配表达式")
                } footer: {
                    Text("Cloudflare Rules 语言，例如：\n(http.request.uri.path eq \"/old\")")
                }

                if phase.isURLRewrite {
                    urlSection
                } else {
                    headerSection
                }

                if let error = viewModel.error {
                    Section {
                        Text(error).font(.footnote).foregroundStyle(.red)
                    }
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

    private var urlSection: some View {
        Section {
            TextField("新路径，如 /new-path", text: $pathValue)
                .textInputAutocapitalization(.never).autocorrectionDisabled()
            TextField("新查询串，如 a=1&b=2", text: $queryValue)
                .textInputAutocapitalization(.never).autocorrectionDisabled()
        } header: {
            Text("重写为")
        } footer: {
            Text("留空表示不改写该部分；按静态值重写。")
        }
    }

    private var headerSection: some View {
        Section {
            ForEach($headerRows) { $row in
                VStack(spacing: 8) {
                    HStack {
                        TextField("头名称，如 X-Foo", text: $row.name)
                            .textInputAutocapitalization(.never).autocorrectionDisabled()
                        Picker("", selection: $row.operation) {
                            ForEach(HeaderOperation.allCases) { Text($0.label).tag($0) }
                        }
                        .labelsHidden()
                    }
                    if row.operation != .remove {
                        TextField("值", text: $row.value)
                            .textInputAutocapitalization(.never).autocorrectionDisabled()
                    }
                }
            }
            .onDelete { headerRows.remove(atOffsets: $0) }

            Button("添加一个头", systemImage: "plus") {
                headerRows.append(HeaderRow())
            }
        } header: {
            Text("头操作")
        } footer: {
            Text("设置 = 覆盖或新增，追加 = 多值追加，删除 = 移除该头。")
        }
    }

    private func save() async {
        viewModel.error = nil
        let trimmedExpr = expression.trimmingCharacters(in: .whitespacesAndNewlines)
        var params = TransformActionParameters()

        if phase.isURLRewrite {
            let p = pathValue.trimmingCharacters(in: .whitespaces)
            let q = queryValue.trimmingCharacters(in: .whitespaces)
            params.uri = URIRewrite(
                path:  p.isEmpty ? nil : RewriteTarget(value: p, expression: nil),
                query: q.isEmpty ? nil : RewriteTarget(value: q, expression: nil)
            )
        } else {
            var dict: [String: HeaderTransform] = [:]
            for row in headerRows {
                let name = row.name.trimmingCharacters(in: .whitespaces)
                guard !name.isEmpty else { continue }
                if row.operation == .remove {
                    dict[name] = HeaderTransform(operation: "remove", value: nil, expression: nil)
                } else {
                    let v = row.value.trimmingCharacters(in: .whitespaces)
                    guard !v.isEmpty else { continue }
                    dict[name] = HeaderTransform(operation: row.operation.rawValue, value: v, expression: nil)
                }
            }
            params.headers = dict
        }

        let trimmedDesc = ruleDescription.trimmingCharacters(in: .whitespaces)
        let draft = TransformRuleCreate(
            action: "rewrite",
            expression: trimmedExpr,
            description: trimmedDesc.isEmpty ? nil : trimmedDesc,
            enabled: enabled,
            actionParameters: params
        )
        if await viewModel.save(phase: phase, ruleId: existing?.id, draft: draft) {
            dismiss()
        }
    }
}

private struct HeaderRow: Identifiable {
    let id = UUID()
    var name: String = ""
    var operation: HeaderOperation = .set
    var value: String = ""
}
