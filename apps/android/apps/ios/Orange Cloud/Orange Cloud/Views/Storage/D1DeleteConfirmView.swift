//
//  D1DeleteConfirmView.swift
//  Orange Cloud
//
//  D1 数据库删除二次确认（Sheet）：必须原样输入数据库名称才能启用删除按钮，
//  与 Cloudflare Dashboard 的删除确认一致。入口（StorageView 滑动删除）已按
//  d1.write 门控。删除连同全部表与数据，不可恢复。
//

import SwiftUI

struct D1DeleteConfirmView: View {

    let database: D1Database
    let viewModel: D1DatabaseListViewModel
    let accountId: String

    @Environment(\.dismiss) private var dismiss
    @State private var typedName = ""
    @FocusState private var fieldFocused: Bool

    /// 输入与库名完全一致（去空白）才允许删除
    private var nameMatches: Bool {
        typedName.trimmingCharacters(in: .whitespacesAndNewlines) == database.name
    }

    private var canDelete: Bool {
        nameMatches && !accountId.isEmpty && !viewModel.isDeleting
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
                        Text("永久删除数据库")
                            .font(.headline)
                        Text("此操作将永久删除数据库 \(database.name) 及其全部表和数据，无法撤销，也无法恢复。")
                            .font(.callout)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .listRowBackground(Color.clear)
                }

                Section {
                    TextField(database.name, text: $typedName)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(.callout.monospaced())
                        .focused($fieldFocused)
                        .submitLabel(.done)
                        .onSubmit { Task { await performDelete() } }
                } header: {
                    Text("输入数据库名称以确认")
                } footer: {
                    Text("请输入 \(database.name) 以启用删除。")
                }

                if let error = viewModel.error {
                    Section {
                        Text(error)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }

                Section {
                    Button(role: .destructive) {
                        Task { await performDelete() }
                    } label: {
                        HStack {
                            Spacer()
                            if viewModel.isDeleting {
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
            .navigationTitle("删除数据库")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
            }
            .onAppear { fieldFocused = true }
            .interactiveDismissDisabled(viewModel.isDeleting)
        }
    }

    private func performDelete() async {
        guard canDelete else { return }
        fieldFocused = false
        if await viewModel.delete(accountId: accountId, database: database) {
            dismiss()
        }
    }
}
