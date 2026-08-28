//
//  TunnelCreateView.swift
//  Orange Cloud
//
//  新建隧道（Sheet）：填名称 → 创建远程托管隧道 → 转连接信息页展示令牌/命令。
//

import SwiftUI

struct TunnelCreateView: View {

    let viewModel: TunnelListViewModel
    let accountId: String
    let session: SessionStore

    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var created: Tunnel?

    private var canSave: Bool {
        !name.trimmingCharacters(in: .whitespaces).isEmpty && !viewModel.isSaving
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("隧道名称", text: $name)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                } header: {
                    Text("名称")
                } footer: {
                    Text("将创建一个远程托管隧道（由 Dashboard / 本应用管理配置）。创建后会显示连接命令。")
                }

                if let error = viewModel.error {
                    Section {
                        Text(error)
                            .font(.footnote)
                            .foregroundStyle(.red)
                    }
                }
            }
            .navigationTitle("新建隧道")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        Task {
                            created = await viewModel.createTunnel(
                                name: name.trimmingCharacters(in: .whitespaces),
                                accountId: accountId
                            )
                        }
                    } label: {
                        if viewModel.isSaving {
                            ProgressView()
                        } else {
                            Text("创建").fontWeight(.semibold)
                        }
                    }
                    .disabled(!canSave)
                }
            }
            .navigationDestination(item: $created) { tunnel in
                TunnelConnectView(tunnel: tunnel, accountId: accountId, session: session) {
                    dismiss()
                }
            }
            .interactiveDismissDisabled(viewModel.isSaving)
        }
    }
}
