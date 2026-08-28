//
//  SnippetEditorView.swift
//  Orange Cloud
//
//  新建 / 编辑 Snippet 代码（multipart 上传 JS 模块）。
//  新建可填名称（[a-zA-Z0-9_]，不可改名）；编辑只改代码。
//

import SwiftUI

struct SnippetEditorView: View {

    let viewModel: SnippetsViewModel
    /// nil = 新建
    let existing: Snippet?

    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var code = ""
    @State private var loadingCode = false

    private var isNew: Bool { existing == nil }

    private var nameValid: Bool {
        name.range(of: "^[a-zA-Z0-9_]+$", options: .regularExpression) != nil
    }

    private var canSave: Bool {
        (isNew ? nameValid : true)
            && !code.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !viewModel.isSaving
    }

    var body: some View {
        NavigationStack {
            Form {
                if let existing {
                    Section("名称") {
                        Text(existing.snippetName)
                            .font(.callout.monospaced())
                            .foregroundStyle(.secondary)
                    }
                } else {
                    Section {
                        TextField("snippet_name", text: $name)
                            .font(.callout.monospaced())
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                    } header: {
                        Text("名称")
                    } footer: {
                        Text("只能包含字母、数字和下划线，创建后不可重命名。")
                    }
                }

                Section {
                    if loadingCode {
                        HStack { Spacer(); ProgressView(); Spacer() }
                            .padding(.vertical, 8)
                    } else {
                        TextEditor(text: $code)
                            .font(.callout.monospaced())
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .frame(minHeight: 220)
                            // JS 代码始终 LTR，避免在 RTL 语言下镜像
                            .environment(\.layoutDirection, .leftToRight)
                    }
                } header: {
                    Text("代码")
                } footer: {
                    Text("运行在 Cloudflare 边缘的 JavaScript 模块（export default { async fetch(request) { … } }）。")
                }

                if let error = viewModel.error {
                    Section {
                        Text(error).font(.footnote).foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle(existing?.snippetName ?? String(localized: "新建 Snippet"))
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
            .task {
                if let existing {
                    loadingCode = true
                    code = await viewModel.code(for: existing.snippetName) ?? ""
                    loadingCode = false
                } else if code.isEmpty {
                    code = Self.template
                }
            }
        }
    }

    private func save() async {
        viewModel.error = nil
        let targetName = isNew ? name.trimmingCharacters(in: .whitespaces) : (existing?.snippetName ?? "")
        guard !targetName.isEmpty else { return }
        if await viewModel.saveSnippet(name: targetName, code: code) {
            dismiss()
        }
    }

    private static let template = """
    export default {
      async fetch(request) {
        return fetch(request);
      }
    };
    """
}
