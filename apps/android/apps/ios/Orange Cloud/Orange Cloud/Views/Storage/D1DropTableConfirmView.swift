//
//  D1DropTableConfirmView.swift
//  Orange Cloud
//
//  D1 表删除二次确认（Sheet）：必须原样输入表名才能启用删除按钮，与数据库删除确认一致。
//  入口（D1QueryView 表列表长按菜单）已按 d1.write 门控。DROP TABLE 连同全部数据，不可恢复。
//

import SwiftUI

struct D1DropTableConfirmView: View {

    let tableName: String
    let viewModel: D1QueryViewModel

    @Environment(\.dismiss) private var dismiss
    @State private var typedName = ""
    @FocusState private var fieldFocused: Bool

    /// 输入与表名完全一致（去空白）才允许删除
    private var nameMatches: Bool {
        typedName.trimmingCharacters(in: .whitespacesAndNewlines) == tableName
    }

    private var canDelete: Bool {
        nameMatches && !viewModel.isDroppingTable
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    VStack(spacing: 12) {
                        Image(systemName: "exclamationmark.triangle.fill")
                            .font(.system(size: 44))
                            .symbolRenderingMode(.hierarchical)
                            .foregroundStyle(.red)
                        Text("永久删除表")
                            .font(.headline)
                        Text("此操作将永久删除表 \(tableName) 及其全部数据，无法撤销，也无法恢复。")
                            .font(.callout)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .listRowBackground(Color.clear)
                }

                Section {
                    TextField(tableName, text: $typedName)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(.callout.monospaced())
                        .focused($fieldFocused)
                        .submitLabel(.done)
                        .onSubmit { Task { await performDrop() } }
                } header: {
                    Text("输入表名以确认")
                } footer: {
                    Text("请输入 \(tableName) 以启用删除。")
                }

                if let error = viewModel.dropError {
                    Section {
                        Text(error)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }

                Section {
                    Button(role: .destructive) {
                        Task { await performDrop() }
                    } label: {
                        HStack {
                            Spacer()
                            if viewModel.isDroppingTable {
                                ProgressView()
                            } else {
                                Label("永久删除", systemImage: "trash")
                                    .fontWeight(.semibold)
                            }
                            Spacer()
                        }
                    }
                    .disabled(!canDelete)
                }
            }
            .navigationTitle("删除表")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
            }
            .onAppear { fieldFocused = true }
            .interactiveDismissDisabled(viewModel.isDroppingTable)
        }
    }

    private func performDrop() async {
        guard canDelete else { return }
        fieldFocused = false
        if await viewModel.dropTable(tableName) {
            dismiss()
        }
    }
}
