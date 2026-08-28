//
//  WorkerDeleteConfirmView.swift
//  Orange Cloud
//
//  Worker 脚本删除二次确认（Sheet）：必须原样输入脚本名称才能启用删除按钮，
//  与 Cloudflare Dashboard 的删除确认一致。入口（WorkerListView 滑动删除）已按
//  workers-scripts.write 门控。删除连同其部署与路由绑定，不可恢复。
//

import SwiftUI
import SwiftData

struct WorkerDeleteConfirmView: View {

    let script: CachedWorkerScript
    let viewModel: WorkerListViewModel
    let accountId: String

    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var modelContext
    @State private var typedName = ""
    @FocusState private var fieldFocused: Bool

    /// 输入与脚本名完全一致（去空白）才允许删除
    private var nameMatches: Bool {
        typedName.trimmingCharacters(in: .whitespacesAndNewlines) == script.id
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
                        Text("永久删除 Worker")
                            .font(.headline)
                        Text("此操作将永久删除 Worker \(script.id) 及其部署与路由绑定，无法撤销，也无法恢复。")
                            .font(.callout)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)
                    .listRowBackground(Color.clear)
                }

                Section {
                    TextField(script.id, text: $typedName)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .font(.callout.monospaced())
                        .focused($fieldFocused)
                        .submitLabel(.done)
                        .onSubmit { Task { await performDelete() } }
                } header: {
                    Text("输入 Worker 名称以确认")
                } footer: {
                    Text("请输入 \(script.id) 以启用删除。")
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
            .navigationTitle("删除 Worker")
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
        if await viewModel.delete(accountId: accountId, script: script, context: modelContext) {
            dismiss()
        }
    }
}
